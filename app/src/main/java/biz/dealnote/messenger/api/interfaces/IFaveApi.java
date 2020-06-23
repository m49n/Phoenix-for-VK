package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import java.util.List;

import biz.dealnote.messenger.api.model.FaveLinkDto;
import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiPhoto;
import biz.dealnote.messenger.api.model.VKApiVideo;
import biz.dealnote.messenger.api.model.response.FavePageResponse;
import biz.dealnote.messenger.api.model.response.FavePostsResponse;
import io.reactivex.Single;


public interface IFaveApi {

    @CheckResult
    Single<Items<FavePageResponse>> getPages(Integer offset, Integer count, String fields, String type);

    @CheckResult
    Single<Items<VKApiPhoto>> getPhotos(Integer offset, Integer count);

    @CheckResult
    Single<List<VKApiVideo>> getVideos(Integer offset, Integer count);

    @CheckResult
    Single<FavePostsResponse> getPosts(Integer offset, Integer count);

    @CheckResult
    Single<Items<FaveLinkDto>> getLinks(Integer offset, Integer count);

    @CheckResult
    Single<Boolean> addPage(Integer userId, Integer groupId);

    @CheckResult
    Single<Boolean> removePage(Integer userId, Integer groupId);

    @CheckResult
    Single<Boolean> removeLink(String linkId);

    @CheckResult
    Single<Boolean> addVideo(Integer owner_id, Integer id, String access_key);

    @CheckResult
    Single<Boolean> addPost(Integer owner_id, Integer id, String access_key);

}
