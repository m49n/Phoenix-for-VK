package biz.dealnote.messenger.mvp.presenter.wallattachments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.db.Stores;
import biz.dealnote.messenger.db.serialize.Serializers;
import biz.dealnote.messenger.domain.IWallsRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.TmpSource;
import biz.dealnote.messenger.model.criteria.WallCriteria;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.wallattachments.IWallPhotosAttachmentsView;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.safeCountOf;

public class WallPhotosAttachmentsPresenter extends PlaceSupportPresenter<IWallPhotosAttachmentsView> {

    private final ArrayList<Photo> mPhotos;
    private final IWallsRepository fInteractor;
    private int loaded;
    private boolean actualDataReceived;
    private int owner_id;

    private boolean endOfContent;
    private boolean actualDataLoading;
    private CompositeDisposable actualDataDisposable = new CompositeDisposable();

    public WallPhotosAttachmentsPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.owner_id = ownerId;
        this.mPhotos = new ArrayList<>();
        this.fInteractor = Repository.INSTANCE.getWalls();
        loadActualData(0);
    }

    @Override
    public void onGuiCreated(@NonNull IWallPhotosAttachmentsView view) {
        super.onGuiCreated(view);
        view.displayData(this.mPhotos);
    }

    @SuppressWarnings("unused")
    public void firePhotoClick(int position, Photo photo) {
        TmpSource source = new TmpSource(getInstanceId(), 0);

        fireTempDataUsage();

        actualDataDisposable.add(Stores.getInstance()
                .tempStore()
                .put(source.getOwnerId(), source.getSourceId(), mPhotos, Serializers.PHOTOS_SERIALIZER)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onPhotosSavedToTmpStore(position, source), Analytics::logUnexpectedError));
    }

    private void onPhotosSavedToTmpStore(int index, TmpSource source) {
        callView(view -> view.goToTempPhotosGallery(getAccountId(), source, index));
    }

    private void loadActualData(int offset) {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getWall(accountId, owner_id, offset, 100, WallCriteria.MODE_ALL)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));

    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void updatePhotos(List<Post> data) {
        for (Post i : data) {
            if (i.hasAttachments() && !isEmpty(i.getAttachments().getPhotos()))
                mPhotos.addAll(i.getAttachments().getPhotos());
            if (i.hasCopyHierarchy())
                updatePhotos(i.getCopyHierarchy());
        }
    }

    private void onActualDataReceived(int offset, List<Post> data) {

        this.actualDataLoading = false;
        this.endOfContent = data.isEmpty();
        this.actualDataReceived = true;
        if (this.endOfContent && isGuiResumed())
            getView().onSetLoadingStatus(2);

        if (offset == 0) {
            this.loaded = data.size();
            this.mPhotos.clear();
            updatePhotos(data);
            resolveToolbar();
            callView(IWallPhotosAttachmentsView::notifyDataSetChanged);
        } else {
            int startSize = mPhotos.size();
            this.loaded += data.size();
            updatePhotos(data);
            resolveToolbar();
            callView(view -> view.notifyDataAdded(startSize, mPhotos.size() - startSize));
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
            if (!endOfContent)
                getView().onSetLoadingStatus(actualDataLoading ? 1 : 0);
        }
    }

    @OnGuiCreated
    private void resolveToolbar() {
        if (isGuiReady()) {
            getView().setToolbarTitle(getString(R.string.attachments_in_wall));
            getView().setToolbarSubtitle(getString(R.string.photos_count, safeCountOf(mPhotos)) + " " + getString(R.string.posts_analized, loaded));
        }
    }

    @Override
    public void onDestroyed() {
        actualDataDisposable.dispose();
        super.onDestroyed();
    }

    public boolean fireScrollToEnd() {
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded);
            return false;
        }
        return true;
    }

    public void fireRefresh() {

        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        loadActualData(0);
    }
}
