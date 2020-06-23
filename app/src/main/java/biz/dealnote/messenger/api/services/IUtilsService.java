package biz.dealnote.messenger.api.services;

import biz.dealnote.messenger.api.model.response.BaseResponse;
import biz.dealnote.messenger.api.model.response.ResolveDomailResponse;
import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface IUtilsService {

    @FormUrlEncoded
    @POST("utils.resolveScreenName")
    Single<BaseResponse<ResolveDomailResponse>> resolveScreenName(@Field("screen_name") String screenName);
}
