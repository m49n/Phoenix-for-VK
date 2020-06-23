package biz.dealnote.messenger.api;

import biz.dealnote.messenger.api.services.IAudioCoverService;
import io.reactivex.Single;

public interface IAudioCoverSeviceProvider {
    Single<IAudioCoverService> provideAudioCoverService();
}
