package biz.dealnote.messenger.crypt;

import io.reactivex.Single;

public interface ISessionIdGenerator {
    Single<Long> generateNextId();
}