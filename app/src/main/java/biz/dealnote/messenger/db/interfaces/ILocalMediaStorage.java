package biz.dealnote.messenger.db.interfaces;

import android.graphics.Bitmap;

import java.util.List;

import biz.dealnote.messenger.model.LocalImageAlbum;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.model.LocalVideo;
import io.reactivex.Single;


public interface ILocalMediaStorage extends IStorage {

    Single<List<LocalPhoto>> getPhotos(long albumId);

    Single<List<LocalPhoto>> getPhotos();

    Single<List<LocalImageAlbum>> getImageAlbums();

    Bitmap getImageThumbnail(long imageId);

    Single<List<LocalVideo>> getVideos();

    Bitmap getVideoThumbnail(long videoId);
}
