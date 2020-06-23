package biz.dealnote.messenger.api.services;

import biz.dealnote.messenger.api.model.LoginResponse;
import biz.dealnote.messenger.api.model.VkApiValidationResponce;
import biz.dealnote.messenger.api.model.response.BaseResponse;
import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface IAuthService {

    @FormUrlEncoded
    @POST("token")
    Single<LoginResponse> directLogin(@Field("grant_type") String grantType,
                                      @Field("client_id") int clientId,
                                      @Field("client_secret") String clientSecret,
                                      @Field("username") String username,
                                      @Field("password") String password,
                                      @Field("v") String v,
                                      @Field("2fa_supported") int twoFaSupported,
                                      @Field("scope") String scope,
                                      @Field("code") String smscode,
                                      @Field("captcha_sid") String captchaSid,
                                      @Field("captcha_key") String captchaKey,
                                      @Field("force_sms") Integer forceSms,
                                      @Field("device_id") String device_id,
                                      @Field("libverify_support") Integer libverify_support);

    @FormUrlEncoded
    @POST("auth.validatePhone")
    Single<BaseResponse<VkApiValidationResponce>> validatePhone(@Field("api_id") int apiId,
                                                                @Field("client_id") int clientId,
                                                                @Field("client_secret") String clientSecret,
                                                                @Field("sid") String sid,
                                                                @Field("v") String v);

}