package biz.dealnote.messenger.mvp.presenter.photo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IPhotosInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.util.RxUtils;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;

/**
 * Created by admin on 25.09.2016.
 * phoenix
 */
public class PhotoAlbumPagerPresenter extends PhotoPagerPresenter {

    private static final int COUNT_PER_LOAD = 100;

    private int mOwnerId;
    private int mAlbumId;
    private boolean canLoad;
    private final IPhotosInteractor photosInteractor;

    public PhotoAlbumPagerPresenter(int indexx, int accountId, int ownerId, int albumId, ArrayList<Photo>photos,
                                    @Nullable Bundle savedInstanceState) {
        super(new ArrayList<>(0), accountId, savedInstanceState);
        this.photosInteractor = InteractorFactory.createPhotosInteractor();
        this.mOwnerId = ownerId;
        this.mAlbumId = albumId;
        this.canLoad = true;

        getData().addAll(photos);
        setCurrentIndex(indexx);

        refreshPagerView();
        resolveButtonsBarVisible();
        resolveToolbarVisibility();
        refreshInfoViews();
    }

    @Override
    void initPhotosData(@NonNull ArrayList<Photo> initialData, @Nullable Bundle savedInstanceState) {
        super.mPhotos = initialData;
    }

    @Override
    void savePhotosState(@NonNull Bundle outState) {
        //no saving state
    }

    private void loadData()
    {
        if(!canLoad)
            return;
        changeLoadingNowState(true);

        if(mAlbumId != -9001 && mAlbumId != -9000) {
            appendDisposable(photosInteractor.get(getAccountId(), mOwnerId, mAlbumId, COUNT_PER_LOAD, mPhotos.size(), true)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onActualPhotosReceived, this::onActualDataGetError));
        }
        else if(mAlbumId == -9000)
        {
            appendDisposable(photosInteractor.getUsersPhoto(getAccountId(), mOwnerId, 1, mPhotos.size(), COUNT_PER_LOAD)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onActualPhotosReceived, this::onActualDataGetError));
        }
        else {
            appendDisposable(photosInteractor.getAll(getAccountId(), mOwnerId, 1, 1, mPhotos.size(), COUNT_PER_LOAD)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onActualPhotosReceived, this::onActualDataGetError));
        }
    }

    private void onActualDataGetError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onActualPhotosReceived(List<Photo> data) {
        changeLoadingNowState(false);
        if(data.isEmpty()) {
            canLoad = false;
            return;
        }

        getData().addAll(data);

        refreshPagerView();
        resolveButtonsBarVisible();
        resolveToolbarVisibility();
        refreshInfoViews();
    }

    @Override
    protected void afterPageChangedFromUi(int oldPage, int newPage) {
        super.afterPageChangedFromUi(oldPage, newPage);
        if(oldPage == newPage)
            return;

        if (newPage == count() - 1) {
            loadData();
        }
    }
}