package biz.dealnote.messenger.mvp.presenter.photo;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import biz.dealnote.messenger.App;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IPhotosInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IPhotoPagerView;
import biz.dealnote.messenger.push.OwnerInfo;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.task.DownloadImageTask;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.DownloadUtil;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Completable;

import static biz.dealnote.messenger.util.Utils.findIndexById;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;

/**
 * Created by admin on 24.09.2016.
 * phoenix
 */
public class PhotoPagerPresenter extends AccountDependencyPresenter<IPhotoPagerView> {

    private static final String TAG = PhotoPagerPresenter.class.getSimpleName();

    private static final String SAVE_INDEX = "save-index";
    private static final String SAVE_DATA = "save-data";

    ArrayList<Photo> mPhotos;
    private int mCurrentIndex;
    private boolean mLoadingNow;
    private boolean mFullScreen;
    private boolean isStory;
    private Context context;

    final IPhotosInteractor photosInteractor;

    PhotoPagerPresenter(@NonNull ArrayList<Photo> initialData, int accountId, boolean Story, Context context, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.photosInteractor = InteractorFactory.createPhotosInteractor();
        this.isStory = Story;
        this.context = context;

        if(Objects.nonNull(savedInstanceState)){
            mCurrentIndex = savedInstanceState.getInt(SAVE_INDEX);
        }

        initPhotosData(initialData, savedInstanceState);

        AssertUtils.requireNonNull(mPhotos, "'mPhotos' not initialized");
    }

    @Override
    public void saveState(@NonNull Bundle outState) {
        super.saveState(outState);
        outState.putInt(SAVE_INDEX, mCurrentIndex);
        savePhotosState(outState);
    }

    void savePhotosState(@NonNull Bundle outState){
        outState.putParcelableArrayList(SAVE_DATA, mPhotos);
    }

    void initPhotosData(@NonNull ArrayList<Photo> initialData, @Nullable Bundle savedInstanceState){
        if (savedInstanceState == null) {
            mPhotos = initialData;
        } else {
            mPhotos = savedInstanceState.getParcelableArrayList(SAVE_DATA);
        }
    }

    void changeLoadingNowState(boolean loading) {
        mLoadingNow = loading;
        resolveLoadingView();
    }

    private void resolveLoadingView() {
        if (isGuiReady()) {
            getView().displayPhotoListLoading(mLoadingNow);
        }
    }

    void refreshPagerView() {
        if (isGuiReady()) {
            getView().displayPhotos(mPhotos, mCurrentIndex);
        }
    }

    void setCurrentIndex(int currentIndex) {
        this.mCurrentIndex = currentIndex;
    }

    @NonNull
    protected ArrayList<Photo> getData() {
        return mPhotos;
    }

    @NonNull
    protected void setData(List<Photo> Photos) {
        mPhotos = (ArrayList<Photo>) Photos;
    }

    @Override
    public void onViewHostAttached(@NonNull IPhotoPagerView viewHost) {
        super.onViewHostAttached(viewHost);
        resolveOptionMenu();
    }

    private void resolveOptionMenu() {
        if (isViewHostAttached()) {
            getViewHost().setupOptionMenu(canSaveYourself(), canDelete());
        }
    }

    private boolean canDelete() {
        return hasPhotos() && getCurrent().getOwnerId() == getAccountId();
    }

    private boolean canSaveYourself() {
        return hasPhotos() && getCurrent().getOwnerId() != getAccountId();
    }

    @Override
    public void onGuiCreated(@NonNull IPhotoPagerView viewHost) {
        super.onGuiCreated(viewHost);
        getView().displayPhotos(mPhotos, mCurrentIndex);

        refreshInfoViews();
        resolveRestoreButtonVisibility();
        resolveToolbarVisibility();
        resolveButtonsBarVisible();
        resolveLoadingView();
    }

    public final void firePageSelected(int position) {
        int old = mCurrentIndex;
        changePageTo(position);
        afterPageChangedFromUi(old, position);
    }

    protected void afterPageChangedFromUi(int oldPage, int newPage) {

    }

    void changePageTo(int position) {
        if (mCurrentIndex == position) return;

        mCurrentIndex = position;
        onPositionChanged();
    }

    private void resolveLikeView() {
        if (isGuiReady() && hasPhotos()) {
            if(isStory) {
                getView().setupLikeButton(false, false, 0);
                return;
            }
            Photo photo = getCurrent();
            getView().setupLikeButton(true, photo.isUserLikes(), photo.getLikesCount());
        }
    }

    private void resolveShareView() {
        if (isGuiReady() && hasPhotos()) {
            getView().setupShareButton(!isStory);
        }
    }

    private void resolveCommentsView() {
        if (isGuiReady() && hasPhotos()) {
            Photo photo = getCurrent();
            if(isStory) {
                getView().setupCommentsButton(false, 0);
                return;
            }
            //boolean visible = photo.isCanComment() || photo.getCommentsCount() > 0;
            getView().setupCommentsButton(true, photo.getCommentsCount());
        }
    }

    int count() {
        return mPhotos.size();
    }

    void resolveToolbarTitleSubtitleView() {
        if (!isGuiReady() || !hasPhotos()) return;

        String title = App.getInstance().getString(R.string.image_number, mCurrentIndex + 1, count());
        getView().setToolbarTitle(title);
        getView().setToolbarSubtitle(getCurrent().getText());
    }

    @NonNull
    private Photo getCurrent() {
        return mPhotos.get(mCurrentIndex);
    }

    private void onPositionChanged() {
        refreshInfoViews();
        resolveRestoreButtonVisibility();
        resolveOptionMenu();
    }

    public void fireInfoButtonClick() {
        getView().showPhotoInfo(getCurrent());
    }

    public void fireShareButtonClick() {
        Photo current = getCurrent();
        getView().sharePhoto(getAccountId(), current);
    }

    public void firePostToMyWallClick() {
        Photo photo = getCurrent();
        getView().postToMyWall(photo, getAccountId());
    }

    void refreshInfoViews() {
        resolveToolbarTitleSubtitleView();
        resolveLikeView();
        resolveShareView();
        resolveCommentsView();
        resolveOptionMenu();
    }

    public void fireLikeClick() {
        addOrRemoveLike();
    }

    private void addOrRemoveLike() {
        final Photo photo = getCurrent();

        final int ownerId = photo.getOwnerId();
        final int photoId = photo.getId();
        final int accountId = super.getAccountId();
        final boolean add = !photo.isUserLikes();

        appendDisposable(photosInteractor.like(accountId, ownerId, photoId, add, photo.getAccessKey())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(count -> interceptLike(ownerId, photoId, count, add), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onDeleteOrRestoreResult(int photoId, int ownerId, boolean deleted) {
        int index = findIndexById(this.mPhotos, photoId, ownerId);

        if(index != -1){
            Photo photo = mPhotos.get(index);
            photo.setDeleted(deleted);

            if (mCurrentIndex == index) {
                resolveRestoreButtonVisibility();
            }
        }
    }

    private void interceptLike(int ownerId, int photoId, int count, boolean userLikes) {
        for (Photo photo : mPhotos) {
            if (photo.getId() == photoId && photo.getOwnerId() == ownerId) {
                photo.setLikesCount(count);
                photo.setUserLikes(userLikes);
                resolveLikeView();
                break;
            }
        }
    }

    public void fireSaveOnDriveClick() {
        if (!AppPerms.hasWriteStoragePermision(App.getInstance())) {
            getView().requestWriteToExternalStoragePermission();
            return;
        }

        doSaveOnDrive();
    }

    private void doSaveOnDrive() {
        File dir = new File(Settings.get().other().getPhotoDir());
        if (!dir.isDirectory()) {
            boolean created = dir.mkdirs();
            if (!created) {
                safeShowError(getView(), "Can't create directory " + dir);
                return;
            }
        }
        else
            dir.setLastModified(Calendar.getInstance().getTime().getTime());

        Photo photo = getCurrent();

        appendDisposable(OwnerInfo.getRx(context, getAccountId(), photo.getOwnerId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(userInfo -> DownloadResult(DownloadUtil.makeLegalFilename(userInfo.getOwner().getFullName(), null), dir, photo), throwable -> DownloadResult(null, dir, photo)));
    }

    private String transform_owner(int owner_id)
    {
        if(owner_id < 0)
            return "club" + Math.abs(owner_id);
        else
            return "id" + owner_id;
    }

    private void DownloadResult(String Prefix, File dir, Photo photo)
    {
        if(Prefix != null && Settings.get().other().isPhoto_to_user_dir()) {
            File dir_final = new File(dir.getAbsolutePath() + "/" + Prefix);
            if (!dir_final.isDirectory()) {
                boolean created = dir_final.mkdirs();
                if (!created) {
                    safeShowError(getView(), "Can't create directory " + dir_final);
                    return;
                }
            }
            else
                dir_final.setLastModified(Calendar.getInstance().getTime().getTime());
            dir = dir_final;
        }
        String file = dir.getAbsolutePath() + "/" + (Prefix != null ? (Prefix + "_") : "") + transform_owner(photo.getOwnerId()) + "_" + photo.getId() + ".jpg";
        String url = photo.getUrlForSize(PhotoSize.W, true);
        do {
            File Temp = new File(file);
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().getTime().getTime());
                if(isGuiReady())
                    getView().getPhoenixToast().showToastError(R.string.exist_audio);
                return;
            }
        }
        while(false);
        new InternalDownloader(this, getApplicationContext(), url, file, photo).doDownload();
    }

    private final class InternalDownloader extends DownloadImageTask {

        final WeakReference<PhotoPagerPresenter> ref;

        InternalDownloader(PhotoPagerPresenter presenter, Context context, String url, String file, Photo photo) {
            super(context, url, file, photo.getId() + "_" + photo.getOwnerId(), true);
            this.ref = new WeakReference<>(presenter);
        }

        @Override
        protected void onPostExecute(String s) {
            PhotoPagerPresenter presenter = ref.get();

            if (Objects.isNull(presenter)) return;

            if(isGuiReady()) {
                if (Objects.isNull(s)) {
                    getView().getPhoenixToast().showToastBottom(R.string.saved);
                } else {
                    getView().getPhoenixToast().showToastError(R.string.error_with_message, s);
                }
            }
        }
    }

    public void fireSaveYourselfClick() {
        final Photo photo = getCurrent();
        final int accountId = super.getAccountId();

        appendDisposable(photosInteractor.copy(accountId, photo.getOwnerId(), photo.getId(), photo.getAccessKey())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(ignored -> onPhotoCopied(), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onPhotoCopied() {
        safeShowLongToast(getView(), R.string.photo_saved_yourself);
    }

    public void fireDeleteClick() {
        delete();
    }

    public void fireWriteExternalStoragePermissionResolved() {
        if (AppPerms.hasWriteStoragePermision(App.getInstance())) {
            doSaveOnDrive();
        }
    }

    public void fireButtonRestoreClick() {
        restore();
    }

    private void resolveRestoreButtonVisibility() {
        if (isGuiReady()) {
            getView().setButtonRestoreVisible(hasPhotos() && getCurrent().isDeleted());
        }
    }

    private void restore() {
        deleteOrRestore(false);
    }

    private void deleteOrRestore(boolean detele){
        final Photo photo = getCurrent();
        final int photoId = photo.getId();
        final int ownerId = photo.getOwnerId();
        final int accountId = super.getAccountId();

        Completable completable;
        if(detele){
            completable = photosInteractor.deletePhoto(accountId, ownerId, photoId);
        } else {
            completable = photosInteractor.restorePhoto(accountId, ownerId, photoId);
        }

        appendDisposable(completable.compose(RxUtils.applyCompletableIOToMainSchedulers())
        .subscribe(() -> onDeleteOrRestoreResult(photoId, ownerId, detele), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void delete() {
        deleteOrRestore(true);
    }

    public void fireCommentsButtonClick() {
        Photo photo = getCurrent();
        getView().goToComments(getAccountId(), Commented.from(photo));
    }

    private boolean hasPhotos() {
        return !Utils.safeIsEmpty(mPhotos);
    }

    public void firePhotoTap() {
        if (!hasPhotos()) return;

        mFullScreen = !mFullScreen;

        resolveToolbarVisibility();
        resolveButtonsBarVisible();
    }

    void resolveButtonsBarVisible() {
        if (isGuiReady()) {
            getView().setButtonsBarVisible(hasPhotos() && !mFullScreen);
        }
    }

    void resolveToolbarVisibility() {
        if (isGuiReady()) {
            getView().setToolbarVisible(hasPhotos() && !mFullScreen);
        }
    }

    int getCurrentIndex() {
        return mCurrentIndex;
    }

    public void fireLikeLongClick() {
        if (!hasPhotos()) return;

        Photo photo = getCurrent();
        getView().goToLikesList(getAccountId(), photo.getOwnerId(), photo.getId());
    }
}
