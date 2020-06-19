package biz.dealnote.messenger.domain;

import java.util.List;

import biz.dealnote.messenger.model.EndlessData;
import biz.dealnote.messenger.model.FaveLink;
import biz.dealnote.messenger.model.FavePage;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Video;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface IFaveInteractor {
    Single<List<Post>> getPosts(int accountId, int count, int offset);

    Single<List<Post>> getCachedPosts(int accountId);

    Single<List<Photo>> getCachedPhotos(int accountId);

    Single<List<Photo>> getPhotos(int accountId, int count, int offset);

    Single<List<Video>> getCachedVideos(int accountId);

    Single<List<Video>> getVideos(int accountId, int count, int offset);

    Single<List<FavePage>> getCachedPages(int accountId, boolean isUser);

    Single<EndlessData<FavePage>> getPages(int accountId, int count, int offset, boolean isUser);

    Completable removePage(int accountId, int ownerId, boolean isUser);

    Single<List<FaveLink>> getCachedLinks(int accountId);

    Single<EndlessData<FaveLink>> getLinks(int accountId, int count, int offset);

    Completable removeLink(int accountId, String id);

    Completable addPage(int accountId, int ownerId);

    Completable addVideo(int accountId, Integer owner_id, Integer id, String access_key);

    Completable addPost(int accountId, Integer owner_id, Integer id, String access_key);
}
