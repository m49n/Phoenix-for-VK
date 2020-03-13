package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import biz.dealnote.messenger.api.model.CountersDto;
import biz.dealnote.messenger.api.model.response.AccountsBannedResponce;
import io.reactivex.Single;

/**
 * Created by admin on 04.01.2017.
 * phoenix
 */
public interface IAccountApi {

    @CheckResult
    Single<Integer> banUser(int userId);

    @CheckResult
    Single<Integer> unbanUser(int userId);

    Single<AccountsBannedResponce> getBanned(Integer count, Integer offset, String fields);

    @CheckResult
    Single<Boolean> unregisterDevice(String deviceId);

    @CheckResult
    Single<Boolean> registerDevice(String token, String deviceModel, Integer deviceYear, String deviceId,
                                   String systemVersion, String settings);

    @CheckResult
    Single<Boolean> setOffline();

    @CheckResult
    Single<CountersDto> getCounters(String filter);
}