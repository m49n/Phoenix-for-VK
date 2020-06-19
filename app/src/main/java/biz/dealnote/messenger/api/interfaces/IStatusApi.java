package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import io.reactivex.Single;


public interface IStatusApi {

    @CheckResult
    Single<Boolean> set(String text, Integer groupId);

}
