package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

public class AudiosPresenter extends AccountDependencyPresenter<IAudiosView> {

    private final IAudioInteractor audioInteractor;
    private final ArrayList<Audio> audios;
    private final int ownerId;
    private boolean actualReceived;
    private int option_menu_id;
    private int isAlbum;
    private boolean iSSelectMode;
    private List<AudioPlaylist> Curr;
    private String accessKey;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();
    private boolean loadingNow;
    private boolean endOfContent;

    public AudiosPresenter(int accountId, int ownerId, int option_menu_id, int isAlbum, boolean iSSelectMode, String accessKey, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
        this.audios = new ArrayList<>();
        this.ownerId = ownerId;
        this.option_menu_id = option_menu_id;
        this.isAlbum = isAlbum;
        this.iSSelectMode = iSSelectMode;
        this.accessKey = accessKey;
    }

    public void LoadAudiosTool() {
        if (audios.isEmpty()) {
            if (!iSSelectMode && isAlbum == 0 && option_menu_id == -1 && MusicUtils.Audios.containsKey(ownerId)) {
                audios.addAll(Objects.requireNonNull(MusicUtils.Audios.get(ownerId)));
                actualReceived = true;
                setLoadingNow(false);
                callView(IAudiosView::notifyListChanged);
            } else
                fireRefresh();
        }
    }

    private void loadedPlaylist(AudioPlaylist t) {
        List<AudioPlaylist> ret = new ArrayList<>(1);
        ret.add(t);
        Objects.requireNonNull(getView()).updatePlaylists(ret);
        Curr = ret;
    }

    public boolean isMyAudio() {
        return isAlbum == 0 && option_menu_id == -1 && ownerId == getAccountId();
    }

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

    private void requestNext() {
        setLoadingNow(true);
        final int offset = audios.size();
        if (isAlbum == 0 && option_menu_id == -1)
            requestList(offset, null);
        else if (isAlbum == 1)
            requestList(offset, option_menu_id);
    }

    public void requestList(int offset, Integer album_id) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.get(getAccountId(), album_id, ownerId, offset, accessKey)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(offset == 0 ? this::onListReceived : this::onNextListReceived, this::onListGetError));
    }

    private void onNextListReceived(List<Audio> next) {
        audios.addAll(next);
        endOfContent = next.isEmpty();
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
        if (isAlbum == 0 && option_menu_id == -1 && !iSSelectMode) {
            MusicUtils.Audios.put(ownerId, audios);
        }
    }

    private void onListReceived(List<Audio> data) {
        audios.clear();
        audios.addAll(data);
        endOfContent = data.isEmpty();
        actualReceived = true;
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);

        if (isAlbum == 0 && option_menu_id == -1 && !iSSelectMode) {
            MusicUtils.Audios.put(ownerId, audios);
        }
    }

    private void onEndlessListReceived(List<Audio> data) {
        audios.clear();
        audios.addAll(data);
        endOfContent = true;
        actualReceived = true;
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
    }

    public void playAudio(Context context, int position) {
        MusicPlaybackService.startForPlayList(context, audios, position, false);
        if (!Settings.get().other().isShow_mini_player())
            PlaceFactory.getPlayerPlace(getAccountId()).tryOpenWith(context);
    }

    public void getListByGenre(boolean foreign, int genre) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.getPopular(getAccountId(), foreign ? 1 : 0, genre)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onEndlessListReceived, this::onListGetError));
    }

    public void getRecommendations() {
        setLoadingNow(true);
        if (isAlbum == 2) {
            audioListDisposable.add(audioInteractor.getRecommendationsByAudio(getAccountId(), ownerId + "_" + option_menu_id)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onEndlessListReceived));
        } else {
            audioListDisposable.add(audioInteractor.getRecommendations(getAccountId(), ownerId)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onEndlessListReceived, this::onListGetError));
        }
    }

    @Override
    public void onDestroyed() {
        audioListDisposable.dispose();
        super.onDestroyed();
    }

    private void onListGetError(Throwable t) {
        setLoadingNow(false);

        if (ownerId != getAccountId()) {
            showError(getView(), Utils.getCauseIfRuntime(t));
            return;
        }
        if (isGuiResumed()) {
            showError(getView(), Utils.getCauseIfRuntime(t));
        }
    }

    public ArrayList<Audio> getSelected() {
        ArrayList<Audio> ret = new ArrayList<>();
        for (Audio i : audios) {
            if (i.isSelected())
                ret.add(i);
        }
        return ret;
    }

    public int getAudioPos(Audio audio) {
        if (!Utils.isEmpty(audios) && audio != null) {
            int pos = 0;
            for (final Audio i : audios) {
                if (i.getId() == audio.getId() && i.getOwnerId() == audio.getOwnerId()) {
                    i.setAnimationNow(true);
                    callView(IAudiosView::notifyListChanged);
                    return pos;
                }
                pos++;
            }
        }
        return -1;
    }

    public void fireRefresh() {
        audioListDisposable.clear();
        if (isAlbum == 0 && option_menu_id == -1) {
            requestList(0, null);
        } else if (isAlbum == 0 && option_menu_id != -2) {
            getListByGenre(false, option_menu_id);
        } else if (isAlbum == 0 || isAlbum == 2) {
            getRecommendations();
        } else {
            if (isAlbum == 1) {
                audioListDisposable.add(audioInteractor.getPlaylistById(getAccountId(), option_menu_id, ownerId, accessKey)
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .subscribe(this::loadedPlaylist, t -> showError(getView(), Utils.getCauseIfRuntime(t))));
            }
            requestList(0, option_menu_id);
        }
    }

    public void onDelete(AudioPlaylist album) {
        final int accountId = super.getAccountId();
        audioListDisposable.add(audioInteractor.deletePlaylist(accountId, album.getId(), album.getOwnerId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
    }

    public void onAdd(AudioPlaylist album) {
        final int accountId = super.getAccountId();
        audioListDisposable.add(audioInteractor.followPlaylist(accountId, album.getId(), album.getOwnerId(), album.getAccess_key())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
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
        if (Curr != null)
            Objects.requireNonNull(getView()).updatePlaylists(Curr);
    }

}