package biz.dealnote.messenger.domain;

import java.util.List;

import biz.dealnote.messenger.model.AnswerVKOfficialList;
import biz.dealnote.messenger.model.feedback.Feedback;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface IFeedbackInteractor {
    Single<List<Feedback>> getCachedFeedbacks(int accountId);

    Single<Pair<List<Feedback>, String>> getActualFeedbacks(int accountId, int count, String startFrom);

    Single<AnswerVKOfficialList> getOfficial(int accountId, Integer count, Integer startFrom);

    Completable maskAaViewed(int accountId);
}