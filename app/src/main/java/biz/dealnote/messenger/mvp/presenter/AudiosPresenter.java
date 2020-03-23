package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
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
    private int option_menu_id;
    private boolean isAlbum;

    public AudiosPresenter(int accountId, int ownerId, int option_menu_id, boolean isAlbum, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
        this.audios = new ArrayList<>();
        this.ownerId = ownerId;
        this.option_menu_id = option_menu_id;
        this.isAlbum = isAlbum;
        fireRefresh();
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
        if (!isAlbum && option_menu_id == -1) {
            requestList(offset, null);
        } else if (!isAlbum  && option_menu_id != -2) {
            getListByGenre(offset, false, option_menu_id);
        }
        else if (!isAlbum) {
            getRecommendations(offset);
        }
        else requestList(offset, option_menu_id);
    }

    public void requestList(int offset, Integer album_id) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.get(getAccountId(), album_id, ownerId, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(offset == 0 ? this::onListReceived : this::onNextListReceived, this::onListGetError));
    }

    private void onNextListReceived(List<Audio> next) {
        audios.addAll(next);
        endOfContent = next.isEmpty() || next.size() < 50;
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
    }

    private void onListReceived(List<Audio> data) {
        audios.clear();
        audios.addAll(data);
        endOfContent = data.isEmpty() || data.size() < 50;
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
        if (!isAlbum && option_menu_id == -1) {
            requestList(0, null);
        } else if (!isAlbum  && option_menu_id != -2) {
            getListByGenre(0, false, option_menu_id);
        }
        else if (!isAlbum) {
            getRecommendations(0);
        }
        else requestList(0, option_menu_id);
    }

    public void fireScrollToEnd() {
        if (actualReceived && !endOfContent) {
            requestNext();
        }
    }

    @Override
    public void onGuiCreated(@NonNull IAudiosView view) {
        super.onGuiCreated(view);
        view.displayList(audios);
    }

}