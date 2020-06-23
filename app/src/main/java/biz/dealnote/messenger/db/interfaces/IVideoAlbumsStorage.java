package biz.dealnote.messenger.db.interfaces;

import androidx.annotation.NonNull;

import java.util.List;

import biz.dealnote.messenger.db.model.entity.VideoAlbumEntity;
import biz.dealnote.messenger.model.VideoAlbumCriteria;
import io.reactivex.Completable;
import io.reactivex.Single;


public interface IVideoAlbumsStorage extends IStorage {
    Single<List<VideoAlbumEntity>> findByCriteria(@NonNull VideoAlbumCriteria criteria);

    Completable insertData(int accountId, int ownerId, @NonNull List<VideoAlbumEntity> data, boolean invalidateBefore);
}