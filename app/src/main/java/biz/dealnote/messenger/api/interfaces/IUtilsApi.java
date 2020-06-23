package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import biz.dealnote.messenger.api.model.response.ResolveDomailResponse;
import io.reactivex.Single;

public interface IUtilsApi {

    @CheckResult
    Single<ResolveDomailResponse> resolveScreenName(String screenName);

}
