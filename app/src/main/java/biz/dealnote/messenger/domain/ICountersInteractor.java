package biz.dealnote.messenger.domain;

import biz.dealnote.messenger.model.SectionCounters;
import io.reactivex.Single;

public interface ICountersInteractor {
    Single<SectionCounters> getApiCounters(int accountId);
}