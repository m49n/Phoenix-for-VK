package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudioPlaylistsView;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class AudioPlaylistsPresenter extends AccountDependencyPresenter<IAudioPlaylistsView> {

    private final List<AudioPlaylist> pages;

    private final IAudioInteractor fInteractor;

    private boolean actualDataReceived;
    private int owner_id;

    private boolean endOfContent;
    private boolean actualDataLoading;
    private CompositeDisposable actualDataDisposable = new CompositeDisposable();

    public AudioPlaylistsPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.owner_id = ownerId;
        this.pages = new ArrayList<>();
        this.fInteractor = InteractorFactory.createAudioInteractor();
    }

    public void LoadAudiosTool() {
        loadActualData(0);
    }

    public int getOwner_id() {
        return owner_id;
    }

    @Override
    public void onGuiCreated(@NonNull IAudioPlaylistsView view) {
        super.onGuiCreated(view);
        view.displayData(this.pages);
    }

    private void loadActualData(int offset) {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getPlaylists(accountId, owner_id, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));

    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(int offset, List<AudioPlaylist> data) {

        this.actualDataLoading = false;
        this.endOfContent = data.isEmpty();
        this.actualDataReceived = true;

        if (offset == 0) {
            this.pages.clear();
            this.pages.addAll(data);
            callView(IAudioPlaylistsView::notifyDataSetChanged);
        } else {
            int startSize = this.pages.size();
            this.pages.addAll(data);
            callView(view -> view.notifyDataAdded(startSize, data.size()));
        }

        resolveRefreshingView();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    private void resolveRefreshingView() {
        if (isGuiResumed()) {
            getView().showRefreshing(actualDataLoading);
        }
    }

    @Override
    public void onDestroyed() {
        actualDataDisposable.dispose();
        super.onDestroyed();
    }

    public boolean fireScrollToEnd() {
        if (!endOfContent && nonEmpty(pages) && actualDataReceived && !actualDataLoading) {
            loadActualData(this.pages.size());
            return false;
        }
        return true;
    }

    public void onDelete(int index, AudioPlaylist album) {
        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.deletePlaylist(accountId, album.getId(), album.getOwnerId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    pages.remove(index);
                    callView(view -> view.notifyDataSetChanged());
                    getView().getPhoenixToast().showToast(R.string.success);
                }, throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
    }

    public void onAdd(AudioPlaylist album) {
        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.followPlaylist(accountId, album.getId(), album.getOwnerId(), album.getAccess_key())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
    }

    public void fireRefresh() {

        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        loadActualData(0);
    }
}
