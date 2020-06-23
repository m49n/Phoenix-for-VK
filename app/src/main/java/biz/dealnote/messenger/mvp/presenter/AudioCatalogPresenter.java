package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.AudioCatalog;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudioCatalogView;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;


public class AudioCatalogPresenter extends AccountDependencyPresenter<IAudioCatalogView> {

    private final List<AudioCatalog> pages;

    private final IAudioInteractor fInteractor;
    private String artist_id;
    private boolean actualDataLoading;
    private CompositeDisposable actualDataDisposable = new CompositeDisposable();

    public AudioCatalogPresenter(int accountId, String artist_id, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.pages = new ArrayList<>();
        this.artist_id = artist_id;
        this.fInteractor = InteractorFactory.createAudioInteractor();
    }

    public void LoadAudiosTool() {
        loadActualData();
    }

    @Override
    public void onGuiCreated(@NonNull IAudioCatalogView view) {
        super.onGuiCreated(view);
        view.displayData(this.pages);
    }

    private void loadActualData() {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getCatalog(accountId, artist_id)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(data), this::onActualDataGetError));

    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(List<AudioCatalog> data) {

        this.actualDataLoading = false;

        this.pages.clear();
        this.pages.addAll(data);
        callView(IAudioCatalogView::notifyDataSetChanged);

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

    public void onAdd(AudioPlaylist album) {
        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.followPlaylist(accountId, album.getId(), album.getOwnerId(), album.getAccess_key())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
    }

    @Override
    public void onDestroyed() {
        actualDataDisposable.dispose();
        super.onDestroyed();
    }

    public void fireRefresh() {

        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        loadActualData();
    }
}
