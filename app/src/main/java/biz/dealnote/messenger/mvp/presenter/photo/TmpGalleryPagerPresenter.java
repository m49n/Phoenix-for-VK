package biz.dealnote.messenger.mvp.presenter.photo;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.db.Stores;
import biz.dealnote.messenger.db.serialize.Serializers;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.TmpSource;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.RxUtils;

public class TmpGalleryPagerPresenter extends PhotoPagerPresenter {

    private final TmpSource source;

    public TmpGalleryPagerPresenter(int accountId, @NonNull TmpSource source, int index, Context context,
                                    @Nullable Bundle savedInstanceState) {
        super(new ArrayList<>(0), accountId, false, context, savedInstanceState);
        this.source = source;
        setCurrentIndex(index);

        loadDataFromDatabase();
    }

    @Override
    void initPhotosData(@NonNull ArrayList<Photo> initialData, @Nullable Bundle savedInstanceState) {
        super.mPhotos = initialData;
    }

    @Override
    void savePhotosState(@NonNull Bundle outState) {
        // no saving
    }

    private void loadDataFromDatabase() {
        changeLoadingNowState(true);
        appendDisposable(Stores.getInstance()
                .tempStore()
                .getData(source.getOwnerId(), source.getSourceId(), Serializers.PHOTOS_SERIALIZER)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onInitialLoadingFinished, Analytics::logUnexpectedError));
    }

    private void onInitialLoadingFinished(List<Photo> photos) {
        changeLoadingNowState(false);

        getData().addAll(photos);

        refreshPagerView();
        resolveButtonsBarVisible();
        resolveToolbarVisibility();
        refreshInfoViews();
    }
}
