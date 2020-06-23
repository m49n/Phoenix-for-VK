package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;

import java.util.List;

import biz.dealnote.messenger.model.LocalImageAlbum;
import biz.dealnote.mvp.core.IMvpView;


public interface ILocalPhotoAlbumsView extends IMvpView {

    void displayData(@NonNull List<LocalImageAlbum> data);

    void setEmptyTextVisible(boolean visible);

    void displayProgress(boolean loading);

    void openAlbum(@NonNull LocalImageAlbum album);

    void notifyDataChanged();

    void requestReadExternalStoragePermission();
}
