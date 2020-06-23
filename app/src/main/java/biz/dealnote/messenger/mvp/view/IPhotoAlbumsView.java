package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.PhotoAlbumEditor;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;

public interface IPhotoAlbumsView extends IMvpView, IAccountDependencyView, IErrorView {

    void displayData(@NonNull List<PhotoAlbum> data);

    void displayLoading(boolean loading);

    void notifyDataSetChanged();

    void setToolbarSubtitle(String subtitle);

    void openAlbum(int accountId, @NonNull PhotoAlbum album, @Nullable Owner owner, @Nullable String action);

    void showAlbumContextMenu(@NonNull PhotoAlbum album);

    void showDeleteConfirmDialog(@NonNull PhotoAlbum album);

    void doSelection(@NonNull PhotoAlbum album);

    void setCreateAlbumFabVisible(boolean visible);

    void goToAlbumCreation(int accountId, int ownerId);

    void goToAlbumEditing(int accountId, @NonNull PhotoAlbum album, @NonNull PhotoAlbumEditor editor);

    void seDrawertPhotoSectionActive(boolean active);

    void notifyItemRemoved(int index);

    void notifyDataAdded(int position, int size);
}