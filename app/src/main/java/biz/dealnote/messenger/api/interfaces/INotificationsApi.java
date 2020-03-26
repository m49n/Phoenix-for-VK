package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import biz.dealnote.messenger.api.model.response.NotificationsResponse;
import biz.dealnote.messenger.model.AnswerVKOfficialList;
import io.reactivex.Single;

/**
 * Created by admin on 03.01.2017.
 * phoenix
 */
public interface INotificationsApi {

    @CheckResult
    Single<Integer> markAsViewed();

    @CheckResult
    Single<NotificationsResponse> get(Integer count, String startFrom, String filters,
                                                    Long startTime, Long endTime);

    @CheckResult
    Single<AnswerVKOfficialList> getOfficial(Integer count, Integer startFrom, String filters, Long startTime, Long endTime);

}
