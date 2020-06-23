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
import biz.dealnote.messenger.mvp.view.IAudiosLocalView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

public class AudiosLocalPresenter extends AccountDependencyPresenter<IAudiosLocalView> {

    private final IAudioInteractor audioInteractor;
    private final ArrayList<Audio> origin_audios;
    private final ArrayList<Audio> audios;
    private boolean actualReceived;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();
    private boolean loadingNow;
    private Context context;
    private String query;

    public AudiosLocalPresenter(int accountId, Context context, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
        this.audios = new ArrayList<>();
        this.origin_audios = new ArrayList<>();
        this.context = context;
    }

    public void LoadAudiosTool() {
        fireRefresh();
    }

    public void setLoadingNow(boolean loadingNow) {
        this.loadingNow = loadingNow;
        resolveRefreshingView();
    }

    public void updateCriteria() {
        setLoadingNow(true);
        audios.clear();
        if (Utils.isEmpty(query)) {
            audios.addAll(origin_audios);
            setLoadingNow(false);
            callView(IAudiosLocalView::notifyListChanged);
            return;
        }
        for (Audio i : origin_audios) {
            if (i.getTitle().toLowerCase().contains(query.toLowerCase()) || i.getArtist().toLowerCase().contains(query.toLowerCase())) {
                audios.add(i);
            }
        }
        setLoadingNow(false);
        callView(IAudiosLocalView::notifyListChanged);
    }

    public void fireQuery(String q) {
        if (Utils.isEmpty(q))
            this.query = null;
        else {
            this.query = q;
        }
        updateCriteria();
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

    public void requestList() {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.loadLocalAudios(getAccountId(), context)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onListReceived, this::onListGetError));
    }

    private void onListReceived(List<Audio> data) {
        if (Utils.isEmpty(data)) {
            actualReceived = true;
            setLoadingNow(false);
            return;
        }
        origin_audios.clear();
        actualReceived = true;
        origin_audios.addAll(data);
        updateCriteria();
        setLoadingNow(false);
    }

    public void playAudio(Context context, int position) {
        MusicPlaybackService.startForPlayList(context, audios, position, false);
        if (!Settings.get().other().isShow_mini_player())
            PlaceFactory.getPlayerPlace(getAccountId()).tryOpenWith(context);
    }

    @Override
    public void onDestroyed() {
        audioListDisposable.dispose();
        super.onDestroyed();
    }

    private void onListGetError(Throwable t) {
        setLoadingNow(false);
        if (isGuiResumed()) {
            showError(getView(), Utils.getCauseIfRuntime(t));
        }
    }

    public int getAudioPos(Audio audio) {
        if (!Utils.isEmpty(audios) && audio != null) {
            int pos = 0;
            for (final Audio i : audios) {
                if (i.getId() == audio.getId() && i.getOwnerId() == audio.getOwnerId()) {
                    i.setAnimationNow(true);
                    callView(IAudiosLocalView::notifyListChanged);
                    return pos;
                }
                pos++;
            }
        }
        return -1;
    }

    public void fireRefresh() {
        audioListDisposable.clear();
        requestList();
    }

    public void fireScrollToEnd() {
        if (actualReceived) {
            requestList();
        }
    }

    @Override
    public void onGuiCreated(@NonNull IAudiosLocalView view) {
        super.onGuiCreated(view);
        view.displayList(audios);
    }

}
