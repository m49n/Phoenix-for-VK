package biz.dealnote.messenger.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import biz.dealnote.messenger.api.IServiceProvider;
import biz.dealnote.messenger.api.TokenType;
import biz.dealnote.messenger.api.interfaces.IUsersApi;
import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiSticker;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.api.model.response.StoryResponse;
import biz.dealnote.messenger.api.model.response.UserWallInfoResponse;
import biz.dealnote.messenger.api.services.IUsersService;
import biz.dealnote.messenger.exception.NotFoundException;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;


class UsersApi extends AbsApi implements IUsersApi {

    UsersApi(int accountId, IServiceProvider provider) {
        super(accountId, provider);
    }

    private static VKApiUser createFrom(UserWallInfoResponse response) {
        VKApiUser user = response.users.get(0);

        if (isNull(user.counters)) {
            user.counters = new VKApiUser.Counters();
        }

        if (nonNull(response.allWallCount)) {
            user.counters.all_wall = response.allWallCount;
        }

        if (nonNull(response.ownerWallCount)) {
            user.counters.owner_wall = response.ownerWallCount;
        }

        if (nonNull(response.postponedWallCount)) {
            user.counters.postponed_wall = response.postponedWallCount;
        }

        return user;
    }

    @Override
    public Single<VKApiUser> getUserWallInfo(int userId, String fields, String nameCase) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service
                        .getUserWallInfo("var user_id = Args.user_id;\n" +
                                "var fields =Args.fields;\n" +
                                "var name_case = Args.name_case;\n" +
                                "\n" +
                                "var user_info = API.users.get({\"user_ids\":user_id,\n" +
                                "    \"fields\":fields, \"name_case\":name_case});\n" +
                                "\n" +
                                "var all_wall_count =API.wall.get({\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"all\"}).count;\n" +
                                "    \n" +
                                "var owner_wall_count =API.wall.get({\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"owner\"}).count;\n" +
                                "\n" +
                                "var postponed_wall_count =API.wall.get({\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"postponed\"}).count;\n" +
                                "\n" +
                                "return {\"user_info\": user_info, \n" +
                                "    \"all_wall_count\":all_wall_count,\n" +
                                "    \"owner_wall_count\":owner_wall_count,\n" +
                                "    \"postponed_wall_count\":postponed_wall_count\n" +
                                "};", userId, fields, nameCase)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> {
                            if (Utils.safeCountOf(response.users) != 1) {
                                throw new NotFoundException();
                            }

                            return createFrom(response);
                        }));
    }

    @Override
    public Single<Items<VKApiUser>> getFollowers(Integer userId, Integer offset, Integer count, String fields, String nameCase) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service.getFollowers(userId, offset, count, fields, nameCase)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VKApiUser>> getRequests(Integer offset, Integer count, Integer extended, Integer out, String fields) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service.getRequests(offset, count, extended, out, fields)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VKApiUser>> search(String query, Integer sort, Integer offset, Integer count, String fields, Integer city, Integer country, String hometown, Integer universityCountry, Integer university, Integer universityYear, Integer universityFaculty, Integer universityChair, Integer sex, Integer status, Integer ageFrom, Integer ageTo, Integer birthDay, Integer birthMonth, Integer birthYear, Boolean online, Boolean hasPhoto, Integer schoolCountry, Integer schoolCity, Integer schoolClass, Integer school, Integer schoolYear, String religion, String interests, String company, String position, Integer groupId, String fromList) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service
                        .search(query, sort, offset, count, fields, city, country, hometown, universityCountry,
                                university, universityYear, universityFaculty, universityChair, sex, status,
                                ageFrom, ageTo, birthDay, birthMonth, birthYear, integerFromBoolean(online),
                                integerFromBoolean(hasPhoto), schoolCountry, schoolCity, schoolClass, school,
                                schoolYear, religion, interests, company, position, groupId, fromList)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Integer> report(Integer userId, String type, String comment) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service
                        .report(userId, type, comment)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<StoryResponse> getStory(Integer owner_id, Integer extended, String fields) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service.getStory(owner_id, extended, fields)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<StoryResponse> searchStory(String q, Integer mentioned_id, Integer count, Integer extended, String fields) {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service.searchStory(q, mentioned_id, count, extended, fields)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VKApiSticker>> getRecentStickers() {
        return provideService(IUsersService.class, TokenType.USER)
                .flatMap(service -> service.getRecentStickers()
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<List<VKApiUser>> get(Collection<Integer> userIds, Collection<String> domains, String fields, String nameCase) {
        ArrayList<String> ids = new ArrayList<>(1);
        if (nonNull(userIds)) {
            ids.add(join(userIds, ","));
        }

        if (nonNull(domains)) {
            ids.add(join(domains, ","));
        }

        return provideService(IUsersService.class, TokenType.USER, TokenType.SERVICE)
                .flatMap(service -> service
                        .get(join(ids, ","), fields, nameCase)
                        .map(extractResponseWithErrorHandling()));
    }
}