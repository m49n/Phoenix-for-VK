package biz.dealnote.messenger.push;

import io.reactivex.Completable;

public interface IPushRegistrationResolver {
    boolean canReceivePushNotification();

    Completable resolvePushRegistration();
}