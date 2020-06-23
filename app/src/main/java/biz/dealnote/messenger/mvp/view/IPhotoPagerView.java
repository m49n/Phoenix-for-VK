package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;

import java.util.List;

import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;


public interface IPhotoPagerView extends IMvpView, IAccountDependencyView, IErrorView, IToastView {

    void goToLikesList(int accountId, int ownerId, int photoId);

    void setupLikeButton(boolean visible, boolean like, int likes);

    void setupShareButton(boolean visible);

    void setupCommentsButton(boolean visible, int count);

    void displayPhotos(@NonNull List<Photo> photos, int initialIndex);

    void setToolbarTitle(String title);

    void setToolbarSubtitle(String subtitle);

    void sharePhoto(int accountId, @NonNull Photo photo);

    void postToMyWall(@NonNull Photo photo, int accountId);

    void showPhotoInfo(Photo photo);

    void requestWriteToExternalStoragePermission();

    void setButtonRestoreVisible(boolean visible);

    void setupOptionMenu(boolean canSaveYourself, boolean canDelete);

    void goToComments(int accountId, @NonNull Commented commented);

    void displayPhotoListLoading(boolean loading);

    void setButtonsBarVisible(boolean visible);

    void setToolbarVisible(boolean visible);

    void rebindPhotoAt(int position);
}
