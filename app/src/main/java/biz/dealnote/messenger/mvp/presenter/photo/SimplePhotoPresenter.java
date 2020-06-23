package biz.dealnote.messenger.mvp.presenter.photo;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.model.AccessIdPair;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.util.RxUtils;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;

public class SimplePhotoPresenter extends PhotoPagerPresenter {

    private static final String SAVE_DATA_REFRESH_RESULT = "save-data-refresh-result";
    private boolean mDataRefreshSuccessfull;
    private boolean isHistory;

    public SimplePhotoPresenter(@NonNull ArrayList<Photo> photos, int index, boolean needToRefreshData,
                                int accountId, Integer History, Context context, @Nullable Bundle savedInstanceState) {
        super(photos, accountId, History == 1, context, savedInstanceState);

        isHistory = nonNull(History) && History == 1;

        if (savedInstanceState == null) {
            setCurrentIndex(index);
        } else {
            mDataRefreshSuccessfull = savedInstanceState.getBoolean(SAVE_DATA_REFRESH_RESULT);
        }

        if (needToRefreshData && !mDataRefreshSuccessfull) {
            refreshData();
        }
    }

    private void refreshData() {
        if (isHistory)
            return;
        final ArrayList<AccessIdPair> ids = new ArrayList<>(getData().size());
        final int accountId = super.getAccountId();

        for (Photo photo : getData()) {
            ids.add(new AccessIdPair(photo.getId(), photo.getOwnerId(), photo.getAccessKey()));
        }

        appendDisposable(photosInteractor.getPhotosByIds(accountId, ids)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onPhotosReceived, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onPhotosReceived(List<Photo> photos) {
        mDataRefreshSuccessfull = true;
        onPhotoListRefresh(photos);
    }

    private void onPhotoListRefresh(@NonNull List<Photo> photos) {
        List<Photo> originalData = super.getData();

        for (Photo photo : photos) {
            //замена старых обьектов новыми
            for (int i = 0; i < originalData.size(); i++) {
                Photo orig = originalData.get(i);

                if (orig.getId() == photo.getId() && orig.getOwnerId() == photo.getOwnerId()) {
                    originalData.set(i, photo);

                    // если у фото до этого не было ссылок на файлы
                    if (isGuiReady() && (isNull(orig.getSizes()) || orig.getSizes().isEmpty())) {
                        getView().rebindPhotoAt(i);
                    }
                    break;
                }
            }
        }

        super.refreshInfoViews();
    }

    @Override
    public void saveState(@NonNull Bundle outState) {
        super.saveState(outState);
        outState.putBoolean(SAVE_DATA_REFRESH_RESULT, mDataRefreshSuccessfull);
    }
}