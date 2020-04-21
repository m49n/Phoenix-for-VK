package biz.dealnote.messenger.domain;

import biz.dealnote.messenger.model.SectionCounters;
import io.reactivex.Single;

/**
 * Created by Ruslan Kolbasa on 30.06.2017.
 * phoenix
 */
public interface ICountersInteractor {
    Single<SectionCounters> getApiCounters(int accountId);
}