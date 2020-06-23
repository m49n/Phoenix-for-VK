package biz.dealnote.messenger.api.interfaces;

import biz.dealnote.messenger.api.model.AudioCoverAmazon;
import io.reactivex.Single;

public interface IAudioCoverApi {
    Single<AudioCoverAmazon> getAudioCover(String track, String artist);
}
