package biz.dealnote.messenger.api.impl;

import java.util.Collection;
import java.util.List;

import biz.dealnote.messenger.api.IServiceProvider;
import biz.dealnote.messenger.api.interfaces.IAudioApi;
import biz.dealnote.messenger.api.model.IdPair;
import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.api.model.VkApiLyrics;
import biz.dealnote.messenger.api.services.IAudioService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;
import io.reactivex.Single;

/**
 * Created by admin on 08.01.2017.
 * phoenix
 */
class AudioApi extends AbsApi implements IAudioApi {

    AudioApi(int accountId, IServiceProvider provider) {
        super(accountId, provider);
    }

    @Override
    public Single<int[]> setBroadcast(IdPair audio, Collection<Integer> targetIds) {
        String audioStr = Objects.isNull(audio) ? null : audio.ownerId + "_" + audio.id;
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .setBroadcast(audioStr, join(targetIds, ","))
                        .map(extractResponseWithErrorHandling()));

    }

    @Override
    public Single<Items<VKApiAudio>> search(String query, Boolean autoComplete, Boolean lyrics, Boolean performerOnly, Integer sort, Boolean searchOwn, Integer offset) {

        if(Settings.get().other().isUse_old_vk_api())
        {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .searchOld(query, integerFromBoolean(autoComplete), integerFromBoolean(lyrics),
                                    integerFromBoolean(performerOnly), sort, integerFromBoolean(searchOwn), offset, "5.90")
                            .map(extractResponseWithErrorHandling()));
        }
        else {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .search(query, integerFromBoolean(autoComplete), integerFromBoolean(lyrics),
                                    integerFromBoolean(performerOnly), sort, integerFromBoolean(searchOwn), offset)
                            .map(extractResponseWithErrorHandling()));
        }
    }

    @Override
    public Single<VKApiAudio> restore(int audioId, Integer ownerId) {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .restore(audioId, ownerId)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Boolean> delete(int audioId, int ownerId) {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .delete(audioId, ownerId)
                        .map(extractResponseWithErrorHandling())
                        .map(response -> response == 1));
    }

    @Override
    public Single<Integer> add(int audioId, int ownerId, Integer groupId, Integer albumId) {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .add(audioId, ownerId, groupId, albumId)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Integer> deletePlaylist(int playlist_id, int ownerId) {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .deletePlaylist(playlist_id, ownerId)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<VKApiAudioPlaylist> followPlaylist(int playlist_id, int ownerId, String accessKey)
    {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .followPlaylist(playlist_id, ownerId, accessKey)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<VKApiAudioPlaylist> getPlaylistById(int playlist_id, int ownerId, String accessKey)
    {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .getPlaylistById(playlist_id, ownerId, accessKey)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<Items<VKApiAudio>> get(Integer album_id, Integer ownerId, Integer offset, String accessKey) {
        if(Settings.get().other().isUse_old_vk_api())
            return provideService(IAudioService.class).flatMap(service -> service.getOld(album_id, ownerId, offset, 100, "5.90", accessKey).map(extractResponseWithErrorHandling()));
        else
            return provideService(IAudioService.class).flatMap(service -> service.get(album_id, ownerId, offset, 100, accessKey).map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<List<VKApiAudio>> getPopular(Integer foreign,
                                                Integer genre) {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .getPopular(foreign, genre, 1000)
                        .map(extractResponseWithErrorHandling()));
    }
    @Override
    public Single<Items<VKApiAudio>> getRecommendations(Integer audioOwnerId)
    {
        if(Settings.get().other().isUse_old_vk_api())
        {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .getRecommendationsOld(audioOwnerId, 1000, "5.90")
                            .map(extractResponseWithErrorHandling()));
        }
        else {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .getRecommendations(audioOwnerId, 1000)
                            .map(extractResponseWithErrorHandling()));
        }
    }

    @Override
    public Single<Items<VKApiAudio>> getRecommendationsByAudio(String audio)
    {
        if(Settings.get().other().isUse_old_vk_api())
        {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .getRecommendationsByAudioOld(audio, 1000, "5.90")
                            .map(extractResponseWithErrorHandling()));
        }
        else {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .getRecommendationsByAudio(audio, 1000)
                            .map(extractResponseWithErrorHandling()));
        }
    }

    @Override
    public Single<Items<VKApiAudioPlaylist>> getPlaylists(int owner_id, int offset)
    {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .getPlaylists(owner_id, offset, 50)
                        .map(extractResponseWithErrorHandling()));
    }

    @Override
    public Single<List<VKApiAudio>> getById(String audios) {
        if(Settings.get().other().isUse_old_vk_api()) {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .getByIdOld(audios, "5.90")
                            .map(extractResponseWithErrorHandling()));
        }
        else
        {
            return provideService(IAudioService.class)
                    .flatMap(service -> service
                            .getById(audios)
                            .map(extractResponseWithErrorHandling()));
        }
    }

    @Override
    public Single<VkApiLyrics> getLyrics(int lyrics_id)
    {
        return provideService(IAudioService.class)
                .flatMap(service -> service
                        .getLyrics(lyrics_id)
                        .map(extractResponseWithErrorHandling()));
    }
}