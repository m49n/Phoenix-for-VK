package biz.dealnote.messenger.api;

import biz.dealnote.messenger.api.services.IAuthService;
import io.reactivex.Single;

public interface IDirectLoginSeviceProvider {
    Single<IAuthService> provideAuthService();
}