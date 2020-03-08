package biz.dealnote.messenger.mvp.presenter.photo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.VKApiPhoto;
import biz.dealnote.messenger.domain.IPhotosInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.mappers.Dto2Model;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;

/**
 * Created by admin on 25.09.2016.
 * phoenix
 */
public class PhotoAlbumPagerPresenter extends PhotoPagerPresenter {

    private final INetworker networker;
    private static final int COUNT_PER_LOAD = 100;

    private int mOwnerId;
    private int mAlbumId;
    private int indexx;
    private Integer mFocusPhotoId;
    private final IPhotosInteractor photosInteractor;

    public PhotoAlbumPagerPresenter(int indexx, INetworker networker, int accountId, int ownerId, int albumId, Integer focusPhotoId,
                                    @Nullable Bundle savedInstanceState) {
        super(new ArrayList<>(0), accountId, savedInstanceState);
        this.networker = networker;
        this.photosInteractor = InteractorFactory.createPhotosInteractor();
        this.mOwnerId = ownerId;
        this.mAlbumId = albumId;
        this.indexx = indexx;

        if (nonNull(savedInstanceState)) {
            this.mFocusPhotoId = null; // because has saved last view index
        } else {
            this.mFocusPhotoId = focusPhotoId;
        }
        loadData(ownerId, albumId, focusPhotoId);
    }

    @Override
    void initPhotosData(@NonNull ArrayList<Photo> initialData, @Nullable Bundle savedInstanceState) {
        super.mPhotos = initialData;
    }

    @Override
    void savePhotosState(@NonNull Bundle outState) {
        //no saving state
    }

    public Single<List<Photo>> get(int accountId, int ownerId, int albumId, int count, int offset, boolean rev) {
        return networker.vkDefault(accountId)
                .photos()
                .get(ownerId, String.valueOf(albumId), null, rev, offset, count)
                .map(items -> Utils.listEmptyIfNull(items.getItems()))
                .flatMap(dtos -> {
                    List<Photo> photos = new ArrayList<>(dtos.size());

                    for(VKApiPhoto dto : dtos){
                        photos.add(Dto2Model.transform(dto));
                    }

                    return Single.just(photos);
                });
    }

    public Single<List<Photo>> getUsersPhoto(int accountId, Integer ownerId, Integer extended, Integer offset, Integer count) {
        return networker.vkDefault(accountId)
                .photos()
                .getUsersPhoto(ownerId, extended, offset, count)
                .map(items -> Utils.listEmptyIfNull(items.getItems()))
                .flatMap(dtos -> {
                    List<Photo> photos = new ArrayList<>(dtos.size());

                    for(VKApiPhoto dto : dtos){
                        photos.add(Dto2Model.transform(dto));
                    }

                    return Single.just(photos);
                });
    }

    public Single<List<Photo>> getAll(int accountId, Integer ownerId, Integer extended, Integer photo_sizes, Integer offset, Integer count) {
        return networker.vkDefault(accountId)
                .photos()
                .getAll(ownerId, extended, photo_sizes, offset, count)
                .map(items -> Utils.listEmptyIfNull(items.getItems()))
                .flatMap(dtos -> {
                    List<Photo> photos = new ArrayList<>(dtos.size());

                    for(VKApiPhoto dto : dtos){
                        photos.add(Dto2Model.transform(dto));
                    }

                    return Single.just(photos);
                });
    }

    private void loadData(int ownerId, int albumId, Integer focusPhotoId)
    {
        changeLoadingNowState(true);

        if(albumId != -9001 && albumId != -9000) {
            appendDisposable(get(getAccountId(), ownerId, albumId, COUNT_PER_LOAD, indexx, true)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(photos -> onActualPhotosReceived(photos), this::onActualDataGetError));
        }
        else if(albumId == -9000)
        {
            appendDisposable(getUsersPhoto(getAccountId(), ownerId, 1, indexx, COUNT_PER_LOAD)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(photos -> onActualPhotosReceived(photos), this::onActualDataGetError));
        }
        else if(albumId == -9001)
        {
            appendDisposable(getAll(getAccountId(), ownerId, 1, 1, indexx, COUNT_PER_LOAD)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(photos -> onActualPhotosReceived(photos), this::onActualDataGetError));
        }
    }

    private void onActualDataGetError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onActualPhotosReceived(List<Photo> data) {
        changeLoadingNowState(false);

        getData().addAll(data);

        if (mFocusPhotoId != null) {
            for (int i = 0; i < data.size(); i++) {
                if (mFocusPhotoId == data.get(i).getId()) {
                    setCurrentIndex(i);
                    break;
                }
            }
        }

        refreshPagerView();
        resolveButtonsBarVisible();
        resolveToolbarVisibility();
        refreshInfoViews();
    }

    private void onInitialDataGetError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
        changeLoadingNowState(true);
    }

    @Override
    protected void afterPageChangedFromUi(int oldPage, int newPage) {
        super.afterPageChangedFromUi(oldPage, newPage);

        if (newPage == count() - 1) {
            final int accountId = super.getAccountId();

            if(mAlbumId != -9001 && mAlbumId != -9000) {
                appendDisposable(get(getAccountId(), mOwnerId, mAlbumId, COUNT_PER_LOAD, count(), true)
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .subscribe(this::onLoadingAtRangeFinished, t -> showError(getView(), getCauseIfRuntime(t))));
            }
            else if(mAlbumId == -9000)
            {
                appendDisposable(getUsersPhoto(getAccountId(), mOwnerId, 1, count(), COUNT_PER_LOAD)
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .subscribe(this::onLoadingAtRangeFinished, t -> showError(getView(), getCauseIfRuntime(t))));
            }
            else if(mAlbumId == -9001)
            {
                appendDisposable(getAll(getAccountId(), mOwnerId, 1, 1, count(), COUNT_PER_LOAD)
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .subscribe(this::onLoadingAtRangeFinished, t -> showError(getView(), getCauseIfRuntime(t))));
            }
        }
    }

    private void onLoadingAtRangeFinished(List<Photo> photos) {
        getData().addAll(photos);
        refreshPagerView();
        resolveToolbarTitleSubtitleView();
    }
}