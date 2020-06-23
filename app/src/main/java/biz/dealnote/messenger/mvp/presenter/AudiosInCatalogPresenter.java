package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.CatalogBlock;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosInCatalogView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

public class AudiosInCatalogPresenter extends AccountDependencyPresenter<IAudiosInCatalogView> {

    private final IAudioInteractor audioInteractor;
    private final ArrayList<Audio> audios;
    private boolean actualReceived;
    private String block_id;
    private String next_from;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();
    private boolean loadingNow;
    private boolean endOfContent;

    public AudiosInCatalogPresenter(int accountId, String block_id, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
        this.audios = new ArrayList<>();
        this.block_id = block_id;
    }

    public void LoadAudiosTool() {
        fireRefresh();
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

    public void requestList() {
        setLoadingNow(true);
        audioListDisposable.add(audioInteractor.getCatalogBlockById(getAccountId(), block_id, next_from)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onListReceived, this::onListGetError));
    }

    private void onListReceived(CatalogBlock data) {
        if (data == null || Utils.isEmpty(data.getAudios())) {
            actualReceived = true;
            setLoadingNow(false);
            endOfContent = true;
            return;
        }
        if (Utils.isEmpty(next_from)) {
            audios.clear();
        }
        next_from = data.getNext_from();
        endOfContent = Utils.isEmpty(next_from);
        actualReceived = true;
        setLoadingNow(false);
        audios.addAll(data.getAudios());
        callView(IAudiosInCatalogView::notifyListChanged);
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
                    callView(IAudiosInCatalogView::notifyListChanged);
                    return pos;
                }
                pos++;
            }
        }
        return -1;
    }

    public void fireRefresh() {
        audioListDisposable.clear();
        next_from = null;
        requestList();
    }

    public void fireScrollToEnd() {
        if (actualReceived && !endOfContent) {
            requestList();
        }
    }

    @Override
    public void onGuiCreated(@NonNull IAudiosInCatalogView view) {
        super.onGuiCreated(view);
        view.displayList(audios);
    }

}
