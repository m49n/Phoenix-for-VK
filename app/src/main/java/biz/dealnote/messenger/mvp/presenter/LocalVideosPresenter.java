package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.db.Stores;
import biz.dealnote.messenger.model.LocalVideo;
import biz.dealnote.messenger.mvp.presenter.base.RxSupportPresenter;
import biz.dealnote.messenger.mvp.view.ILocalVideosView;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;


public class LocalVideosPresenter extends RxSupportPresenter<ILocalVideosView> {

    private static final String TAG = LocalVideosPresenter.class.getSimpleName();

    private List<LocalVideo> mLocalVideos;
    private boolean mLoadingNow;

    public LocalVideosPresenter(@Nullable Bundle savedInstanceState) {
        super(savedInstanceState);

        mLocalVideos = Collections.emptyList();
        loadData();
    }

    private void loadData() {
        if (mLoadingNow) return;

        changeLoadingState(true);
        appendDisposable(Stores.getInstance()
                .localPhotos()
                .getVideos()
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onDataLoaded, this::onLoadError));
    }

    private void onLoadError(Throwable throwable) {
        changeLoadingState(false);
    }

    private void onDataLoaded(List<LocalVideo> data) {
        changeLoadingState(false);
        mLocalVideos = data;
        resolveListData();
        resolveEmptyTextVisibility();
    }

    @Override
    public void onGuiCreated(@NonNull ILocalVideosView viewHost) {
        super.onGuiCreated(viewHost);
        resolveListData();
        resolveProgressView();
        resolveFabVisibility(false);
        resolveEmptyTextVisibility();
    }

    private void resolveEmptyTextVisibility() {
        if (isGuiReady()) getView().setEmptyTextVisible(Utils.safeIsEmpty(mLocalVideos));
    }

    private void resolveListData() {
        if (isGuiReady())
            getView().displayData(mLocalVideos);
    }

    private void changeLoadingState(boolean loading) {
        mLoadingNow = loading;
        resolveProgressView();
    }

    private void resolveProgressView() {
        if (isGuiReady()) {
            getView().displayProgress(mLoadingNow);
        }
    }

    public void fireFabClick() {
        ArrayList<LocalVideo> localVideos = Utils.getSelected(mLocalVideos);
        if (!localVideos.isEmpty()) {
            getView().returnResultToParent(localVideos);
        } else {
            safeShowError(getView(), R.string.select_attachments);
        }
    }


    public void fireVideoClick(@NonNull LocalVideo video) {
        video.setSelected(!video.isSelected());

        if (video.isSelected()) {
            ArrayList<LocalVideo> single = new ArrayList<>(1);
            single.add(video);
            getView().returnResultToParent(single);
        }
    }

    private void resolveFabVisibility(boolean anim) {
        resolveFabVisibility(Utils.countOfSelection(mLocalVideos) > 0, anim);
    }

    private void resolveFabVisibility(boolean visible, boolean anim) {
        if (isGuiReady()) {
            getView().setFabVisible(visible, anim);
        }
    }

    public void fireRefresh() {
        loadData();
    }
}
