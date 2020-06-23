package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.db.Stores;
import biz.dealnote.messenger.model.LocalImageAlbum;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.mvp.presenter.base.RxSupportPresenter;
import biz.dealnote.messenger.mvp.view.ILocalPhotosView;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;

public class LocalPhotosPresenter extends RxSupportPresenter<ILocalPhotosView> {

    private static final String TAG = LocalPhotosPresenter.class.getSimpleName();

    private LocalImageAlbum mLocalImageAlbum;
    private int mSelectionCountMax;

    private boolean permissionRequestedOnce;

    private List<LocalPhoto> mLocalPhotos;
    private boolean mLoadingNow;

    public LocalPhotosPresenter(LocalImageAlbum album, int maxSelectionCount,
                                @Nullable Bundle savedInstanceState) {
        super(savedInstanceState);
        mLocalImageAlbum = album;
        mSelectionCountMax = maxSelectionCount;

        mLocalPhotos = Collections.emptyList();
        /*
        if(mLocalImageAlbum == null && !AppPerms.hasReadStoragePermision(getApplicationContext())){
            if(!permissionRequestedOnce){
                permissionRequestedOnce = true;
                getView().requestReadExternalStoragePermission();
            }
        } else {
            loadData();
        }
         */
        loadData();
    }

    private void loadData() {
        if (mLoadingNow) return;

        changeLoadingState(true);
        if (mLocalImageAlbum != null) {
            appendDisposable(Stores.getInstance()
                    .localPhotos()
                    .getPhotos(mLocalImageAlbum.getId())
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onDataLoaded, this::onLoadError));
        } else {
            appendDisposable(Stores.getInstance()
                    .localPhotos()
                    .getPhotos()
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onDataLoaded, this::onLoadError));
        }
    }

    private void onLoadError(Throwable throwable) {
        changeLoadingState(false);
    }

    private void onDataLoaded(List<LocalPhoto> data) {
        changeLoadingState(false);
        mLocalPhotos = data;
        resolveListData();
        resolveEmptyTextVisibility();
    }

    @Override
    public void onGuiCreated(@NonNull ILocalPhotosView viewHost) {
        super.onGuiCreated(viewHost);
        resolveListData();
        resolveProgressView();
        resolveFabVisibility(false);
        resolveEmptyTextVisibility();
    }

    private void resolveEmptyTextVisibility() {
        if (isGuiReady()) getView().setEmptyTextVisible(Utils.safeIsEmpty(mLocalPhotos));
    }

    private void resolveListData() {
        if (isGuiReady())
            getView().displayData(mLocalPhotos);
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
        ArrayList<LocalPhoto> localPhotos = Utils.getSelected(mLocalPhotos);
        if (!localPhotos.isEmpty()) {
            getView().returnResultToParent(localPhotos);
        } else {
            safeShowError(getView(), R.string.select_attachments);
        }
    }


    public void firePhotoClick(@NonNull LocalPhoto photo) {
        photo.setSelected(!photo.isSelected());

        if (mSelectionCountMax == 1 && photo.isSelected()) {
            ArrayList<LocalPhoto> single = new ArrayList<>(1);
            single.add(photo);
            getView().returnResultToParent(single);
            return;
        }

        onSelectPhoto(photo);
        getView().updateSelectionAndIndexes();
    }

    private void onSelectPhoto(LocalPhoto selectedPhoto) {
        if (selectedPhoto.isSelected()) {
            int targetIndex = 1;
            for (LocalPhoto photo : mLocalPhotos) {
                if (photo.getIndex() >= targetIndex) {
                    targetIndex = photo.getIndex() + 1;
                }
            }

            selectedPhoto.setIndex(targetIndex);
        } else {
            for (int i = 0; i < mLocalPhotos.size(); i++) {
                LocalPhoto photo = mLocalPhotos.get(i);
                if (photo.getIndex() > selectedPhoto.getIndex()) {
                    photo.setIndex(photo.getIndex() - 1);
                }
            }

            selectedPhoto.setIndex(0);
        }

        if (selectedPhoto.isSelected()) {
            resolveFabVisibility(true, true);
        } else {
            resolveFabVisibility(true);
        }
    }

    private void resolveFabVisibility(boolean anim) {
        resolveFabVisibility(Utils.countOfSelection(mLocalPhotos) > 0, anim);
    }

    private void resolveFabVisibility(boolean visible, boolean anim) {
        if (isGuiReady()) {
            getView().setFabVisible(visible, anim);
        }
    }

    public void fireRefresh() {
        loadData();
    }

    public void fireReadExternalStoregePermissionResolved() {
        if (AppPerms.hasReadStoragePermision(getApplicationContext())) {
            loadData();
        }
    }
}
