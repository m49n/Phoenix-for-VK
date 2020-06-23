package biz.dealnote.messenger.db.interfaces;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import biz.dealnote.messenger.db.model.PostPatch;
import biz.dealnote.messenger.db.model.entity.OwnerEntities;
import biz.dealnote.messenger.db.model.entity.PostEntity;
import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.criteria.WallCriteria;
import biz.dealnote.messenger.util.Optional;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface IWallStorage extends IStorage {

    @CheckResult
    Single<int[]> storeWallEntities(int accountId, @NonNull List<PostEntity> posts,
                                    @Nullable OwnerEntities owners,
                                    @Nullable IClearWallTask clearWall);

    @CheckResult
    Single<Integer> replacePost(int accountId, @NonNull PostEntity post);

    @CheckResult
    Single<PostEntity> getEditingPost(int accountId, int ownerId, @EditingPostType int type, boolean includeAttachment);

    @CheckResult
    Completable deletePost(int accountId, int dbid);

    @CheckResult
    Single<Optional<PostEntity>> findPostById(int accountId, int dbid);

    @CheckResult
    Single<Optional<PostEntity>> findPostById(int accountId, int ownerId, int vkpostId, boolean includeAttachment);

    Single<List<PostEntity>> findDbosByCriteria(@NonNull WallCriteria criteria);

    @CheckResult
    Completable update(int accountId, int ownerId, int postId, @NonNull PostPatch update);

    /**
     * Уведомить хранилище, что пост более не существует
     *
     * @param accountId
     * @param postVkid
     * @param postOwnerId
     * @return
     */
    Completable invalidatePost(int accountId, int postVkid, int postOwnerId);

    interface IClearWallTask {
        int getOwnerId();
    }
}