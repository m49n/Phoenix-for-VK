package biz.dealnote.messenger.api.services;

import biz.dealnote.messenger.api.model.CountersDto;
import biz.dealnote.messenger.api.model.response.AccountsBannedResponce;
import biz.dealnote.messenger.api.model.response.BaseResponse;
import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface IAccountService {

    @POST("account.banUser")
    @FormUrlEncoded
    Single<BaseResponse<Integer>> banUser(@Field("user_id") int userId);

    @POST("account.unbanUser")
    @FormUrlEncoded
    Single<BaseResponse<Integer>> unbanUser(@Field("user_id") int userId);

    @POST("account.getBanned")
    @FormUrlEncoded
    Single<BaseResponse<AccountsBannedResponce>> getBanned(@Field("count") Integer count,
                                                           @Field("offset") Integer offset,
                                                           @Field("fields") String fields);

    //https://vk.com/dev/account.getCounters

    /**
     * @param filter friends — новые заявки в друзья;
     *               friends_suggestions — предлагаемые друзья;
     *               messages — новые сообщения;
     *               photos — новые отметки на фотографиях;
     *               videos — новые отметки на видеозаписях;
     *               gifts — подарки;
     *               events — события;
     *               groups — сообщества;
     *               notifications — ответы;
     *               sdk — запросы в мобильных играх;
     *               app_requests — уведомления от приложений.
     */
    @POST("account.getCounters")
    @FormUrlEncoded
    Single<BaseResponse<CountersDto>> getCounters(@Field("filter") String filter);

    //https://vk.com/dev/account.unregisterDevice
    @FormUrlEncoded
    @POST("account.unregisterDevice")
    Single<BaseResponse<Integer>> unregisterDevice(@Field("device_id") String deviceId);

    //https://vk.com/dev/account.registerDevice
    @FormUrlEncoded
    @POST("account.registerDevice")
    Single<BaseResponse<Integer>> registerDevice(@Field("token") String token,
                                                 @Field("device_model") String deviceModel,
                                                 @Field("device_year") Integer deviceYear,
                                                 @Field("device_id") String deviceId,
                                                 @Field("system_version") String systemVersion,
                                                 @Field("settings") String settings);

    /**
     * Marks a current user as offline.
     *
     * @return In case of success returns 1.
     */
    @GET("account.setOffline")
    Single<BaseResponse<Integer>> setOffline();

}
