package biz.dealnote.messenger.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import biz.dealnote.messenger.api.IServiceProvider;
import biz.dealnote.messenger.api.TokenType;
import biz.dealnote.messenger.api.interfaces.IGroupsApi;
import biz.dealnote.messenger.api.model.GroupSettingsDto;
import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiCommunity;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.api.model.VkApiBanned;
import biz.dealnote.messenger.api.model.response.GroupLongpollServer;
import biz.dealnote.messenger.api.model.response.GroupWallInfoResponse;
import biz.dealnote.messenger.api.services.IGroupsService;
import biz.dealnote.messenger.exception.NotFoundException;
import io.reactivex.Completable;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.safeCountOf;


class GroupsApi extends AbsApi implements IGroupsApi {

    GroupsApi(int accountId, IServiceProvider provider) {
        super(accountId, provider);
    }

    private static VKApiCommunity createFrom(GroupWallInfoResponse info) {
        VKApiCommunity community = info.groups.get(0);

        if (isNull(community.counters)) {
            community.counters = new VKApiCommunity.Counters();
        }

        if (nonNull(info.allWallCount)) {
            community.counters.all_wall = info.allWallCount;
        }

        if (nonNull(info.ownerWallCount)) {
            community.counters.owner_wall = info.ownerWallCount;
        }

        if (nonNull(info.suggestsWallCount)) {
            community.counters.suggest_wall = info.suggestsWallCount;
        }

        if (nonNull(info.postponedWallCount)) {
            community.counters.postponed_wall = info.postponedWallCount;
        }

        return community;
    }

    @Override
    public Completable editManager(int groupId, int userId, String role, Boolean isContact, String contactPosition, String contactPhone, String contactEmail) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMapCompletable(service -> service
                        .editManager(groupId, userId, role, integerFromBoolean(isContact), contactPosition, contactPhone, contactEmail)
                        .map(extractResponseWithErrorHandling())
                        .ignoreElement());
    }

    @Override
    public Completable unban(int groupId, int ownerId) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMapCompletable(service -> service
                        .unban(groupId, ownerId)
                        .map(extractResponseWithErrorHandling())
                        .ignoreElement());
    }

    @Override
    public Completable ban(int groupId, int ownerId, Long endDate, Integer reason, String comment, Boolean commentVisible) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMapCompletable(service -> service
                        .ban(groupId, ownerId, endDate, reason, comment, integerFromBoolean(commentVisible))
                        .map(extractResponseWithErrorHandling())
                        .ignoreElement());
    }

    @Override
    public Single<GroupSettingsDto> getSettings(int groupId) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service
                        .getSettings(groupId)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VkApiBanned>> getBanned(int groupId, Integer offset, Integer count, String fields, Integer userId) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service.getBanned(groupId, offset, count, fields, userId)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<VKApiCommunity> getWallInfo(String groupId, String fields) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service.getGroupWallInfo("var group_id = Args.group_id; var fields =Args.fields; var negative_group_id = -group_id; var group_info = API.groups.getById({\"group_id\":group_id, \"fields\":fields}); var all_wall_count = API.wall.get({\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"all\"}).count; var owner_wall_count = API.wall.get({\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"owner\"}).count; var suggests_wall_count = API.wall.get({\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"suggests\"}).count; var postponed_wall_count = API.wall.get({\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"postponed\"}).count; return {\"group_info\": group_info, \"all_wall_count\":all_wall_count, \"owner_wall_count\":owner_wall_count, \"suggests_wall_count\":suggests_wall_count, \"postponed_wall_count\":postponed_wall_count };", groupId, fields)
                        .map(extractResponseWithErrorHandling()))
                .map(response -> {
                    if (safeCountOf(response.groups) != 1) {
                        throw new NotFoundException();
                    }

                    return createFrom(response);
                });
    }

    @Override
    public Single<Items<VKApiUser>> getMembers(String groupId, Integer sort, Integer offset, Integer count, String fields, String filter) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service.getMembers(groupId, sort, offset, count, fields, filter)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VKApiCommunity>> search(String query, String type, Integer countryId, Integer cityId, Boolean future, Boolean market, Integer sort, Integer offset, Integer count) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service
                        .search(query, type, countryId, cityId, integerFromBoolean(future),
                                integerFromBoolean(market), sort, offset, count)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Boolean> leave(int groupId) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service.leave(groupId)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Boolean> join(int groupId, Integer notSure) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service.join(groupId, notSure)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Items<VKApiCommunity>> get(Integer userId, Boolean extended, String filter, String fields, Integer offset, Integer count) {
        return provideService(IGroupsService.class, TokenType.USER)
                .flatMap(service -> service
                        .get(userId, integerFromBoolean(extended), filter, fields, offset, count)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<GroupLongpollServer> getLongPollServer(int groupId) {
        return provideService(IGroupsService.class, TokenType.COMMUNITY)
                .flatMap(service -> service
                        .getLongPollServer(groupId)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<List<VKApiCommunity>> getById(Collection<Integer> groupIds, Collection<String> domains, String groupId, String fields) {
        ArrayList<String> ids = new ArrayList<>(1);
        if (nonNull(groupIds)) {
            ids.add(join(groupIds, ","));
        }

        if (nonNull(domains)) {
            ids.add(join(domains, ","));
        }

        if (ids.isEmpty()) {
            ids = null;
        }

        ArrayList<String> finalIds = ids;

        return provideService(IGroupsService.class, TokenType.USER, TokenType.COMMUNITY, TokenType.SERVICE)
                .flatMap(service -> service
                        .getById(join(finalIds, ","), groupId, fields)
                        .map(extractResponseWithErrorHandling()));
    }
}