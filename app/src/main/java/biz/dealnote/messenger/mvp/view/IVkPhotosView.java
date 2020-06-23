package biz.dealnote.messenger.mvp.view;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.wrappers.SelectablePhotoWrapper;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.mvp.core.IMvpView;


public interface IVkPhotosView extends IMvpView, IAccountDependencyView, IErrorView, IToolbarView {
    String ACTION_SHOW_PHOTOS = "biz.dealnote.messenger.ACTION_SHOW_PHOTOS";
    String ACTION_SELECT_PHOTOS = "biz.dealnote.messenger.ACTION_SELECT_PHOTOS";

    void displayData(List<SelectablePhotoWrapper> photos, List<Upload> uploads);

    void notifyDataSetChanged();

    void notifyPhotosAdded(int position, int count);

    void displayRefreshing(boolean refreshing);

    void notifyUploadAdded(int position, int count);

    void notifyUploadRemoved(int index);

    void setButtonAddVisible(boolean visible, boolean anim);

    void notifyUploadItemChanged(int index);

    void notifyUploadProgressChanged(int id, int progress);

    void displayGallery(int accountId, int albumId, int ownerId, ArrayList<Photo> photos, int position);

    void displayDefaultToolbarTitle();

    void setDrawerPhotosSelected(boolean selected);

    void returnSelectionToParent(List<Photo> selected);

    void showSelectPhotosToast();

    void startLocalPhotosSelection();

    void startLocalPhotosSelectionIfHasPermission();
}