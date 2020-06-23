package biz.dealnote.messenger.api.services;

import java.util.List;

import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiSticker;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.api.model.response.BaseResponse;
import biz.dealnote.messenger.api.model.response.StoryResponse;
import biz.dealnote.messenger.api.model.response.UserWallInfoResponse;
import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface IUsersService {

    @FormUrlEncoded
    @POST("execute")
    Single<BaseResponse<UserWallInfoResponse>> getUserWallInfo(@Field("code") String code,
                                                               @Field("user_id") int userId,
                                                               @Field("fields") String fields,
                                                               @Field("name_case") String nameCase);

    //https://vk.com/dev/users.getFollowers
    @FormUrlEncoded
    @POST("users.getFollowers")
    Single<BaseResponse<Items<VKApiUser>>> getFollowers(@Field("user_id") Integer userId,
                                                        @Field("offset") Integer offset,
                                                        @Field("count") Integer count,
                                                        @Field("fields") String fields,
                                                        @Field("name_case") String nameCase);

    @FormUrlEncoded
    @POST("friends.getRequests")
    Single<BaseResponse<Items<VKApiUser>>> getRequests(@Field("offset") Integer offset,
                                                       @Field("count") Integer count,
                                                       @Field("extended") Integer extended,
                                                       @Field("out") Integer out,
                                                       @Field("fields") String fields);

    //https://vk.com/dev/users.search
    @FormUrlEncoded
    @POST("users.search")
    Single<BaseResponse<Items<VKApiUser>>> search(@Field("q") String query,
                                                  @Field("sort") Integer sort,
                                                  @Field("offset") Integer offset,
                                                  @Field("count") Integer count,
                                                  @Field("fields") String fields,
                                                  @Field("city") Integer city,
                                                  @Field("country") Integer country,
                                                  @Field("hometown") String hometown,
                                                  @Field("university_country") Integer universityCountry,
                                                  @Field("university") Integer university,
                                                  @Field("university_year") Integer universityYear,
                                                  @Field("university_faculty") Integer universityFaculty,
                                                  @Field("university_chair") Integer universityChair,
                                                  @Field("sex") Integer sex,
                                                  @Field("status") Integer status,
                                                  @Field("age_from") Integer ageFrom,
                                                  @Field("age_to") Integer ageTo,
                                                  @Field("birth_day") Integer birthDay,
                                                  @Field("birth_month") Integer birthMonth,
                                                  @Field("birth_year") Integer birthYear,
                                                  @Field("online") Integer online,
                                                  @Field("has_photo") Integer hasPhoto,
                                                  @Field("school_country") Integer schoolCountry,
                                                  @Field("school_city") Integer schoolCity,
                                                  @Field("school_class") Integer schoolClass,
                                                  @Field("school") Integer school,
                                                  @Field("school_year") Integer schoolYear,
                                                  @Field("religion") String religion,
                                                  @Field("interests") String interests,
                                                  @Field("company") String company,
                                                  @Field("position") String position,
                                                  @Field("group_id") Integer groupId,
                                                  @Field("from_list") String fromList);


    /**
     * Returns detailed information on users.
     *
     * @param userIds  User IDs or screen names (screen_name). By default, current user ID.
     *                 List of comma-separated words, the maximum number of elements allowed is 1000
     * @param fields   Profile fields to return
     * @param nameCase Case for declension of user name and surname:
     *                 nom — nominative (default)
     *                 gen — genitive
     *                 dat — dative
     *                 acc — accusative
     *                 ins — instrumental
     *                 abl — prepositional
     * @return Returns a list of user objects.
     * A deactivated field may be returned with the value deleted or banned if a user has been suspended.
     */
    @FormUrlEncoded
    @POST("users.get")
    Single<BaseResponse<List<VKApiUser>>> get(@Field("user_ids") String userIds,
                                              @Field("fields") String fields,
                                              @Field("name_case") String nameCase);

    @POST("users.report")
    @FormUrlEncoded
    Single<BaseResponse<Integer>> report(@Field("user_id") Integer userId,
                                         @Field("type") String type,
                                         @Field("comment") String comment);

    @POST("stories.get")
    @FormUrlEncoded
    Single<BaseResponse<StoryResponse>> getStory(@Field("owner_id") Integer owner_id,
                                                 @Field("extended") Integer extended,
                                                 @Field("fields") String fields);

    @POST("stories.search")
    @FormUrlEncoded
    Single<BaseResponse<StoryResponse>> searchStory(@Field("q") String q,
                                                    @Field("mentioned_id") Integer mentioned_id,
                                                    @Field("count") Integer count,
                                                    @Field("extended") Integer extended,
                                                    @Field("fields") String fields);

    @POST("messages.getRecentStickers")
    Single<BaseResponse<Items<VKApiSticker>>> getRecentStickers();

}
