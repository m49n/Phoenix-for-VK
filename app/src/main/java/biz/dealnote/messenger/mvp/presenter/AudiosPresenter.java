package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosView;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
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
    private int isAlbum;
    private boolean LoadFromCache;
    private boolean iSSelectMode;
    private List<VKApiAudioPlaylist> Curr;
    private String accessKey;

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

    public void LoadAudiosTool()
    {
        if(audios.size() <= 0) {
            if (!iSSelectMode && isAlbum == 0 && option_menu_id == -1 && MusicUtils.Audios.containsKey(ownerId)) {
                audios.addAll(Objects.requireNonNull(MusicUtils.Audios.get(ownerId)));
                actualReceived = true;
                setLoadingNow(false);
                callView(IAudiosView::notifyListChanged);
            } else
                fireRefresh();
        }
    }

    private void loadedPlaylist(VKApiAudioPlaylist t)
    {
        List<VKApiAudioPlaylist> ret = new ArrayList<>(1);
        ret.add(t);
        Objects.requireNonNull(getView()).updatePlaylists(ret);
        Curr = ret;
    }

    public boolean isMyAudio() {
        return isAlbum == 0 && option_menu_id == -1 && ownerId == getAccountId();
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
        if (isAlbum == 0 && option_menu_id == -1)
            requestList(offset, null);
        else if(isAlbum == 1)
            requestList(offset, option_menu_id);
    }

    public void requestList(int offset, Integer album_id) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.get(getAccountId(), album_id, ownerId, offset, accessKey)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(offset == 0 ? this::onListReceived : this::onNextListReceived, this::onListGetError));
    }

    private void onNextListReceived(List<Audio> next) {
        LoadFromCache = false;
        audios.addAll(next);
        endOfContent = next.isEmpty();
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
        if (isAlbum == 0 && option_menu_id == -1 && !iSSelectMode) {
            MusicUtils.Audios.put(ownerId, audios);
        }
    }

    private void onListReceived(List<Audio> data) {
        LoadFromCache = false;
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
        LoadFromCache = false;
        audios.clear();
        audios.addAll(data);
        endOfContent = true;
        actualReceived = true;
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
    }

    public void playAudio(Context context, int position) {
        MusicPlaybackService.startForPlayList(context, audios, position, false);
    }

    public void getListByGenre(boolean foreign, int genre) {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.getPopular(getAccountId(), foreign ? 1 : 0, genre)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onEndlessListReceived, this::onListGetError));
    }

    public void getRecommendations() {
        setLoadingNow(true);
        if(isAlbum == 2)
        {
            audioListDisposable.add(audioInteractor.getRecommendationsByAudio(getAccountId(), ownerId + "_" + option_menu_id)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onEndlessListReceived));
        }
        else {
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

    private ArrayList<Audio> listFiles() {

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
        if(dir.listFiles() == null || dir.listFiles().length <= 0)
            return new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        int id = 0;
        for (File file : dir.listFiles()) {
            if (!file.isDirectory() && file.getName().contains(".mp3")) {
                files.add(file);
            }
        }
        if(files.size() <= 0)
            return new ArrayList<>();
        Collections.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        ArrayList<Audio> audios = new ArrayList<>(files.size());
        for (File file : files) {

            Audio rt = new Audio().setId(++id).setUrl("file://" + file.getAbsolutePath());
            String TrackName = file.getName().replace(".mp3", "");
            String Artist = "";
            String[] arr = TrackName.split(" - ");
            if (arr.length > 1) {
                Artist = arr[0];
                TrackName = TrackName.replace(Artist + " - ", "");
            }
            rt.setArtist(Artist);
            rt.setTitle(TrackName);

            audios.add(rt);
        }
        return audios;
    }

    public void doLoadCache()
    {
        LoadFromCache = true;
        getView().ProvideReadCachedAudio();
        audios.clear();
        audios.addAll(listFiles());
        endOfContent = true;
        actualReceived = true;
        setLoadingNow(false);
        callView(IAudiosView::notifyListChanged);
    }

    private void onListGetError(Throwable t) {
        setLoadingNow(false);

        if(ownerId != getAccountId())
        {
            showError(getView(), Utils.getCauseIfRuntime(t));
            return;
        }
        if (isGuiResumed()) {
            if(!LoadFromCache) {
                showError(getView(), Utils.getCauseIfRuntime(t));
                callView(IAudiosView::doesLoadCache);
            }
            else
                doLoadCache();
        }
    }

    public ArrayList<Audio> getSelected()
    {
        ArrayList<Audio> ret = new ArrayList<>();
        for(Audio i : audios) {
            if(i.isSelected())
                ret.add(i);
        }
        return ret;
    }

    public int getAudioPos(Audio audio)
    {
        if(audios != null && !audios.isEmpty() && audio != null) {
            int pos = 0;
            for(final Audio i : audios)
            {
                if(i.getId() == audio.getId() && i.getOwnerId() == audio.getOwnerId()) {
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
        } else if (isAlbum == 0  && option_menu_id != -2) {
            getListByGenre(false, option_menu_id);
        }
        else if (isAlbum == 0 || isAlbum == 2) {
            getRecommendations();
        }
        else {
            if(isAlbum == 1) {
                audioListDisposable.add(audioInteractor.getPlaylistById(getAccountId(), option_menu_id, ownerId, accessKey)
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .subscribe(this::loadedPlaylist, t -> showError(getView(), Utils.getCauseIfRuntime(t))));
            }
            requestList(0, option_menu_id);
        }
    }

    public void onDelete(VKApiAudioPlaylist album)
    {
        final int accountId = super.getAccountId();
        audioListDisposable.add(audioInteractor.deletePlaylist(accountId, album.id, album.owner_id)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable -> {
                    getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage());}));
    }

    public void onAdd(VKApiAudioPlaylist album)
    {
        final int accountId = super.getAccountId();
        audioListDisposable.add(audioInteractor.followPlaylist(accountId, album.id, album.owner_id, album.access_key)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable -> {
                    getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage());}));
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
        if(Curr != null)
            Objects.requireNonNull(getView()).updatePlaylists(Curr);
    }

}