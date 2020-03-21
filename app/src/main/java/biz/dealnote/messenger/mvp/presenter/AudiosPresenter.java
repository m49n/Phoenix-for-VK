package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioFilter;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by admin on 1/4/2018.
 * Phoenix-for-VK
 */
public class AudiosPresenter extends AccountDependencyPresenter<IAudiosView> {

    private final IAudioInteractor audioInteractor;
    private final ArrayList<Audio> audios;
    private final int ownerId;
    private boolean actualReceived;
    private List<AudioFilter> filters;
    private AudioFilter currentFilter;

    public AudiosPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
        this.audios = new ArrayList<>();
        this.ownerId = ownerId;
        this.filters = createFilterList();
        requestPlayLists();
        requestList(0, null);
    }

    private CompositeDisposable audioListDisposable = new CompositeDisposable();

    private boolean loadingNow;

    public void setLoadingNow(boolean loadingNow) {
        this.loadingNow = loadingNow;
        resolveRefreshingView();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    private void resolveRefreshingView() {
        if (isGuiResumed()) {
            getView().displayRefreshing(loadingNow);
        }
    }

    private boolean endOfContent;

    private void requestNext() {
        setLoadingNow(true);
        final int offset = audios.size();
        if (currentFilter == null || currentFilter.isFilterNone()) {
            requestList(offset, null);
        } else if(!currentFilter.isRecommendation() && !currentFilter.isAlbum()){
            getListByGenre(offset, false, currentFilter.getGenre());
        }
        else if(currentFilter.isRecommendation()) {
            getRecommendations(offset);
        }
        else if(currentFilter.isAlbum())
            requestList(offset, currentFilter.getGenre());
    }

    private List<AudioFilter> createFilterList() {
        List<AudioFilter> result = new ArrayList<>();
        result.add(new AudioFilter(false, AudioFilter.MY_AUDIO, true));
        if(ownerId >= 0)
            result.add(new AudioFilter(false, AudioFilter.MY_RECOMENDATIONS));
        if(getAccountId() == ownerId && Settings.get().other().isEnable_show_audio_top()) {
            result.add(new AudioFilter(false, AudioFilter.TOP_ALL));
            result.add(new AudioFilter(false, VKApiAudio.Genre.ETHNIC));
            result.add(new AudioFilter(false, VKApiAudio.Genre.INSTRUMENTAL));
            result.add(new AudioFilter(false, VKApiAudio.Genre.ACOUSTIC_AND_VOCAL));
            result.add(new AudioFilter(false, VKApiAudio.Genre.ALTERNATIVE));
            result.add(new AudioFilter(false, VKApiAudio.Genre.CLASSICAL));
            result.add(new AudioFilter(false, VKApiAudio.Genre.DANCE_AND_HOUSE));
            result.add(new AudioFilter(false, VKApiAudio.Genre.DRUM_AND_BASS));
            result.add(new AudioFilter(false, VKApiAudio.Genre.EASY_LISTENING));
            result.add(new AudioFilter(false, VKApiAudio.Genre.ELECTROPOP_AND_DISCO));
            result.add(new AudioFilter(false, VKApiAudio.Genre.INDIE_POP));
            result.add(new AudioFilter(false, VKApiAudio.Genre.METAL));
            result.add(new AudioFilter(false, VKApiAudio.Genre.OTHER));
            result.add(new AudioFilter(false, VKApiAudio.Genre.POP));
            result.add(new AudioFilter(false, VKApiAudio.Genre.REGGAE));
            result.add(new AudioFilter(false, VKApiAudio.Genre.ROCK));
            result.add(new AudioFilter(false, VKApiAudio.Genre.TRANCE));
        }
        return result;
    }

    public void requestList(int offset, Integer album_id) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.get(getAccountId(), album_id, ownerId, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(offset == 0 ? this::onListReceived : this::onNextListReceived, this::onListGetError));
    }

    public void requestPlayLists() {
        audioListDisposable.add(audioInteractor.getPlaylists(getAccountId(), ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onPlayListReceived, t -> {}));
    }

    private void onPlayListReceived(List<VKApiAudioPlaylist> playlists)
    {
        for(VKApiAudioPlaylist i : playlists)
            filters.add(new AudioFilter(true, i.id, i.title));
        callView(IAudiosView::notifyFilterListChanged);
    }

    private void onNextListReceived(List<Audio> next) {
        //next.removeAll(audios);
        audios.addAll(next);
        endOfContent = next.isEmpty();
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
    }

    private void onListReceived(List<Audio> data) {
        audios.clear();
        audios.addAll(data);
        endOfContent = data.isEmpty();
        actualReceived = true;
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
    }

    public void playAudio(Context context, int position) {
        MusicPlaybackService.startForPlayList(context, audios, position, false);
        if(!Settings.get().other().isPlayer_instead_feed())
            PlaceFactory.getPlayerPlace(getAccountId()).tryOpenWith(context);
    }

    public void getListByGenre(int offset, boolean foreign, int genre) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.getPopular(getAccountId(), foreign ? 1 : 0, genre, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(offset == 0 ? this::onListReceived : this::onNextListReceived, this::onListGetError));
    }

    public void getRecommendations(int offset) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.getRecommendations(getAccountId(), ownerId, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(offset == 0 ? this::onListReceived : this::onNextListReceived, this::onListGetError));
    }

    @Override
    public void onDestroyed() {
        audioListDisposable.dispose();
        super.onDestroyed();
    }

    private void onListGetError(Throwable t) {
        setLoadingNow(false);
        showError(getView(), Utils.getCauseIfRuntime(t));
    }

    public void fireRefresh() {
        audioListDisposable.clear();
        if (currentFilter == null || currentFilter.isFilterNone()) {
            requestList(0, null);
        } else if(!currentFilter.isRecommendation() && !currentFilter.isAlbum()){
            getListByGenre(0, false, currentFilter.getGenre());
        }
        else if(currentFilter.isRecommendation()) {
            getRecommendations(0);
        }
        else if(currentFilter.isAlbum())
            requestList(0, currentFilter.getGenre());
    }

    public void fireScrollToEnd() {
        if (actualReceived && !endOfContent) {
            requestNext();
        }
    }

    @Override
    public void onGuiCreated(@NonNull IAudiosView view) {
        super.onGuiCreated(view);
        view.showFilters(true);
        view.fillFilters(filters);
        view.displayList(audios);
    }

    public void fireFilterItemClick(AudioFilter source) {
        currentFilter = source;
        for (AudioFilter filter : filters) {
            filter.setActive(filter.getGenre() == source.getGenre() && filter.isAlbum() == source.isAlbum());
        }
        callView(IAudiosView::notifyFilterListChanged);
        fireRefresh();
    }
}