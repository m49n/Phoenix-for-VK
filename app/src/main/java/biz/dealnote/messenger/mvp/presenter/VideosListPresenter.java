package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IVideosInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.nextfrom.IntNextFrom;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IVideosListView;
import biz.dealnote.messenger.upload.IUploadManager;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.upload.UploadDestination;
import biz.dealnote.messenger.upload.UploadIntent;
import biz.dealnote.messenger.upload.UploadResult;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.Pair;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.Injection.provideMainThreadScheduler;
import static biz.dealnote.messenger.util.Utils.findIndexById;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

/**
 * Created by admin on 21.11.2016.
 * phoenix
 */
public class VideosListPresenter extends AccountDependencyPresenter<IVideosListView> {

    private static final int COUNT = 50;

    private final int ownerId;
    private final int albumId;
    private final String action;
    private final List<Video> data;
    private final IVideosInteractor interactor;
    private final IUploadManager uploadManager;
    private Context context;
    private String albumTitle;
    private boolean endOfContent;
    private IntNextFrom intNextFrom;
    private boolean hasActualNetData;
    private UploadDestination destination;
    private List<Upload> uploadsData;
    private boolean requestNow;
    private CompositeDisposable netDisposable = new CompositeDisposable();
    private CompositeDisposable cacheDisposable = new CompositeDisposable();
    private boolean cacheNowLoading;

    public VideosListPresenter(int accountId, int ownerId, int albumId, String action,
                               @Nullable String albumTitle, Context context, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.interactor = InteractorFactory.createVideosInteractor();
        this.uploadManager = Injection.provideUploadManager();
        this.destination = UploadDestination.forVideo(IVideosListView.ACTION_SELECT.equalsIgnoreCase(action) ? 0 : 1);
        this.uploadsData = new ArrayList<>(0);
        this.context = context;

        this.ownerId = ownerId;
        this.albumId = albumId;
        this.action = action;
        this.albumTitle = albumTitle;

        this.intNextFrom = new IntNextFrom(0);

        this.data = new ArrayList<>();

        appendDisposable(uploadManager.get(getAccountId(), destination)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onUploadsDataReceived));

        appendDisposable(uploadManager.observeAdding()
                .observeOn(provideMainThreadScheduler())
                .subscribe(this::onUploadsAdded));

        appendDisposable(uploadManager.observeDeleting(true)
                .observeOn(provideMainThreadScheduler())
                .subscribe(this::onUploadDeleted));

        appendDisposable(uploadManager.observeResults()
                .filter(pair -> destination.compareTo(pair.getFirst().getDestination()))
                .observeOn(provideMainThreadScheduler())
                .subscribe(this::onUploadResults));

        appendDisposable(uploadManager.obseveStatus()
                .observeOn(provideMainThreadScheduler())
                .subscribe(this::onUploadStatusUpdate));

        appendDisposable(uploadManager.observeProgress()
                .observeOn(provideMainThreadScheduler())
                .subscribe(this::onProgressUpdates));


        loadAllFromCache();
        request(false);
        if (IVideosListView.ACTION_SELECT.equalsIgnoreCase(action)) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.confirmation)
                    .setMessage(R.string.do_upload_video)
                    .setPositiveButton(R.string.button_yes, (dialog, which) -> doUpload())
                    .setNegativeButton(R.string.button_no, null)
                    .show();
        }
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public Integer getAlbumId() {
        return albumId;
    }

    private void onUploadsDataReceived(List<Upload> data) {
        uploadsData.clear();
        uploadsData.addAll(data);

        callView(IVideosListView::notifyDataSetChanged);
        resolveUploadDataVisiblity();
    }

    private void onUploadResults(Pair<Upload, UploadResult<?>> pair) {
        Video obj = (Video) pair.getSecond().getResult();
        if (obj.getId() == 0)
            getView().getPhoenixToast().showToastError(R.string.error);
        else {
            getView().getPhoenixToast().showToast(R.string.uploaded);
            if (IVideosListView.ACTION_SELECT.equalsIgnoreCase(action)) {
                getView().onUploaded(obj);
            } else
                fireRefresh();
        }

    }

    private void onProgressUpdates(List<IUploadManager.IProgressUpdate> updates) {
        for (IUploadManager.IProgressUpdate update : updates) {
            int index = findIndexById(uploadsData, update.getId());
            if (index != -1) {
                callView(view -> view.notifyUploadProgressChanged(index, update.getProgress(), true));
            }
        }
    }

    private void onUploadStatusUpdate(Upload upload) {
        int index = findIndexById(uploadsData, upload.getId());
        if (index != -1) {
            callView(view -> view.notifyUploadItemChanged(index));
        }
    }

    private void onUploadsAdded(List<Upload> added) {
        for (Upload u : added) {
            if (destination.compareTo(u.getDestination())) {
                int index = uploadsData.size();
                uploadsData.add(u);
                callView(view -> view.notifyUploadItemsAdded(index, 1));
            }
        }

        resolveUploadDataVisiblity();
    }

    private void onUploadDeleted(int[] ids) {
        for (int id : ids) {
            int index = findIndexById(uploadsData, id);
            if (index != -1) {
                uploadsData.remove(index);
                callView(view -> view.notifyUploadItemRemoved(index));
            }
        }

        resolveUploadDataVisiblity();
    }

    @OnGuiCreated
    private void resolveUploadDataVisiblity() {
        if (isGuiReady()) {
            getView().setUploadDataVisible(!uploadsData.isEmpty());
        }
    }

    private void resolveRefreshingView() {
        if (isGuiResumed()) {
            getView().displayLoading(requestNow);
        }
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    private void setRequestNow(boolean requestNow) {
        this.requestNow = requestNow;
        resolveRefreshingView();
    }

    public void doUpload() {
        if (AppPerms.hasReadStoragePermision(getApplicationContext())) {
            getView().startSelectUploadFileActivity(getAccountId());
        } else {
            getView().requestReadExternalStoragePermission();
        }
    }

    public void fireRemoveClick(Upload upload) {
        uploadManager.cancel(upload.getId());
    }

    public void fireReadPermissionResolved() {
        if (AppPerms.hasReadStoragePermision(getApplicationContext())) {
            getView().startSelectUploadFileActivity(getAccountId());
        }
    }

    private void request(boolean more) {
        if (requestNow) return;

        setRequestNow(true);

        int accountId = super.getAccountId();

        final IntNextFrom startFrom = more ? this.intNextFrom : new IntNextFrom(0);

        netDisposable.add(interactor.get(accountId, ownerId, albumId, COUNT, startFrom.getOffset())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(videos -> {
                    IntNextFrom nextFrom = new IntNextFrom(startFrom.getOffset() + COUNT);
                    onRequestResposnse(videos, startFrom, nextFrom);
                }, this::onListGetError));
    }

    private void onListGetError(Throwable throwable) {
        setRequestNow(false);
        showError(getView(), throwable);
    }

    private void onRequestResposnse(List<Video> videos, IntNextFrom startFrom, IntNextFrom nextFrom) {
        this.cacheDisposable.clear();
        this.cacheNowLoading = false;

        this.hasActualNetData = true;
        this.intNextFrom = nextFrom;
        this.endOfContent = videos.isEmpty();

        if (startFrom.getOffset() == 0) {
            data.clear();
            data.addAll(videos);

            callView(IVideosListView::notifyDataSetChanged);
        } else {
            if (nonEmpty(videos)) {
                int startSize = data.size();
                data.addAll(videos);
                callView(view -> view.notifyDataAdded(startSize, videos.size()));
            }
        }

        setRequestNow(false);
    }

    @Override
    public void onGuiCreated(@NonNull IVideosListView view) {
        super.onGuiCreated(view);
        view.displayData(data);
        view.displayUploads(uploadsData);
        view.setToolbarSubtitle(albumTitle);
    }

    public void fireFileForUploadSelected(String file) {
        UploadIntent intent = new UploadIntent(getAccountId(), destination)
                .setAutoCommit(true)
                .setFileUri(Uri.parse(file));

        uploadManager.enqueue(Collections.singletonList(intent));
    }

    private void loadAllFromCache() {
        this.cacheNowLoading = true;
        final int accountId = super.getAccountId();

        cacheDisposable.add(interactor.getCachedVideos(accountId, ownerId, albumId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCachedDataReceived, Analytics::logUnexpectedError));
    }

    private void onCachedDataReceived(List<Video> videos) {
        this.data.clear();
        this.data.addAll(videos);

        callView(IVideosListView::notifyDataSetChanged);
    }

    @Override
    public void onDestroyed() {
        cacheDisposable.dispose();
        netDisposable.dispose();
        super.onDestroyed();
    }

    public void fireRefresh() {
        this.cacheDisposable.clear();
        this.cacheNowLoading = false;

        this.netDisposable.clear();

        request(false);
    }

    private boolean canLoadMore() {
        return !endOfContent && !requestNow && hasActualNetData && !cacheNowLoading && nonEmpty(data);
    }

    public void fireScrollToEnd() {
        if (canLoadMore()) {
            request(true);
        }
    }

    public void fireVideoClick(Video video) {
        if (IVideosListView.ACTION_SELECT.equalsIgnoreCase(action)) {
            getView().returnSelectionToParent(video);
        } else {
            getView().showVideoPreview(getAccountId(), video);
        }
    }
}