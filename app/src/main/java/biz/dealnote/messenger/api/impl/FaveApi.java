package biz.dealnote.messenger.api.impl;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.api.IServiceProvider;
import biz.dealnote.messenger.api.interfaces.IFaveApi;
import biz.dealnote.messenger.api.model.FaveLinkDto;
import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiPhoto;
import biz.dealnote.messenger.api.model.VKApiVideo;
import biz.dealnote.messenger.api.model.VkApiAttachments;
import biz.dealnote.messenger.api.model.response.FavePageResponse;
import biz.dealnote.messenger.api.model.response.FavePostsResponse;
import biz.dealnote.messenger.api.services.IFaveService;
import biz.dealnote.messenger.db.column.UserColumns;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Utils.listEmptyIfNull;

/**
 * Created by admin on 09.01.2017.
 * phoenix
 */
class FaveApi extends AbsApi implements IFaveApi {

    FaveApi(int accountId, IServiceProvider provider) {
        super(accountId, provider);
    }

    @Override
    public Single<Items<FavePageResponse>> getPages(Integer offset, Integer count, String fields, String type) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.getPages(offset, count, type, fields)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VKApiPhoto>> getPhotos(Integer offset, Integer count) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.getPhotos(offset, count)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<List<VKApiVideo>> getVideos(Integer offset, Integer count) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.getVideos(offset, count, "video", 1, UserColumns.API_FIELDS)
                        .map(extractResponseWithErrorHandling()).flatMap(t -> {
                            List<VkApiAttachments.Entry> temp = listEmptyIfNull(t.items);
                            List<VKApiVideo> videos = new ArrayList<>();
                            for(VkApiAttachments.Entry i : temp)
                            {
                                if(i.attachment instanceof VKApiVideo)
                                    videos.add((VKApiVideo)i.attachment);
                            }
                            return Single.just(videos);
                        }
                        ));
    }

    @Override
    public Single<FavePostsResponse> getPosts(Integer offset, Integer count) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.getPosts(offset, count, "post", 1, UserColumns.API_FIELDS)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<FaveLinkDto>> getLinks(Integer offset, Integer count) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.getLinks(offset, count, "link", 1, UserColumns.API_FIELDS)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Boolean> addPage(Integer userId, Integer groupId) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.addPage(userId, groupId)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Boolean> addVideo(Integer owner_id, Integer id, String access_key) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.addVideo(owner_id, id, access_key)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Boolean> addPost(Integer owner_id, Integer id, String access_key) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.addPost(owner_id, id, access_key)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Boolean> removePage(Integer userId, Integer groupId) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.removePage(userId, groupId)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Boolean> removeLink(String linkId) {
        return provideService(IFaveService.class)
                .flatMap(service -> service.removeLink(linkId)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }
}
