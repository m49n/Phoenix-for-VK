package biz.dealnote.messenger.api;

import io.reactivex.Single;

public interface IServiceProvider {
    <T> Single<T> provideService(int accountId, Class<T> serviceClass, int... tokenTypes);
}