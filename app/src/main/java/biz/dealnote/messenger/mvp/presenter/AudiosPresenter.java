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

    public ArrayList<Audio> listFiles() {

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString());
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

            Audio rt = new Audio().setId(++id).setUrl("file://" + file.getPath());
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

    private void onListGetError(Throwable t) {
        setLoadingNow(false);

        if(ownerId != getAccountId())
        {
            showError(getView(), Utils.getCauseIfRuntime(t));
            return;
        }
        if (isGuiResumed()) {
            getView().ProvideReadCachedAudio();
            audios.clear();
            audios.addAll(listFiles());
            endOfContent = true;
            actualReceived = true;
            setLoadingNow(false);
            callView(IAudiosView::notifyListChanged);
        }
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