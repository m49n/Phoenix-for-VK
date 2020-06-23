package biz.dealnote.messenger.api.services;

import java.util.List;

import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.api.model.VKApiAudioCatalog;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.api.model.VkApiLyrics;
import biz.dealnote.messenger.api.model.response.BaseResponse;
import biz.dealnote.messenger.api.model.response.BlockResponse;
import biz.dealnote.messenger.api.model.response.CatalogResponse;
import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface IAudioService {

    @FormUrlEncoded
    @POST("audio.setBroadcast")
    Single<BaseResponse<int[]>> setBroadcast(@Field("audio") String audio,
                                             @Field("target_ids") String targetIds);

    //https://vk.com/dev/audio.search
    @FormUrlEncoded
    @POST("audio.search")
    Single<BaseResponse<Items<VKApiAudio>>> search(@Field("q") String query,
                                                   @Field("auto_complete") Integer autoComplete,
                                                   @Field("lyrics") Integer lyrics,
                                                   @Field("performer_only") Integer performerOnly,
                                                   @Field("sort") Integer sort,
                                                   @Field("search_own") Integer searchOwn,
                                                   @Field("offset") Integer offset);

    //https://vk.com/dev/audio.search
    @FormUrlEncoded
    @POST("audio.search")
    Single<BaseResponse<Items<VKApiAudio>>> searchOld(@Field("q") String query,
                                                      @Field("auto_complete") Integer autoComplete,
                                                      @Field("lyrics") Integer lyrics,
                                                      @Field("performer_only") Integer performerOnly,
                                                      @Field("sort") Integer sort,
                                                      @Field("search_own") Integer searchOwn,
                                                      @Field("offset") Integer offset,
                                                      @Field("v") String version);

    //https://vk.com/dev/audio.restore
    @FormUrlEncoded
    @POST("audio.restore")
    Single<BaseResponse<VKApiAudio>> restore(@Field("audio_id") int audioId,
                                             @Field("owner_id") Integer ownerId);

    //https://vk.com/dev/audio.delete
    @FormUrlEncoded
    @POST("audio.delete")
    Single<BaseResponse<Integer>> delete(@Field("audio_id") int audioId,
                                         @Field("owner_id") int ownerId);

    //https://vk.com/dev/audio.add
    @FormUrlEncoded
    @POST("audio.add")
    Single<BaseResponse<Integer>> add(@Field("audio_id") int audioId,
                                      @Field("owner_id") int ownerId,
                                      @Field("group_id") Integer groupId,
                                      @Field("album_id") Integer albumId);

    /**
     * Returns a list of audio files of a user or community.
     *
     * @param ownerId ID of the user or community that owns the audio file.
     *                Use a negative value to designate a community ID.
     *                Current user id is used by default
     * @param offset  Offset needed to return a specific subset of audio files.
     * @param count   Number of audio files to return.
     * @return Returns the total results number in count field and an array of objects describing audio in items field.
     */
    //https://vk.com/dev/audio.get
    @FormUrlEncoded
    @POST("audio.get")
    Single<BaseResponse<Items<VKApiAudio>>> get(@Field("album_id") Integer album_id,
                                                @Field("owner_id") Integer ownerId,
                                                @Field("offset") Integer offset,
                                                @Field("count") Integer count,
                                                @Field("access_key") String accessKey);

    //https://vk.com/dev/audio.get
    @FormUrlEncoded
    @POST("audio.get")
    Single<BaseResponse<Items<VKApiAudio>>> getOld(@Field("album_id") Integer album_id,
                                                   @Field("owner_id") Integer ownerId,
                                                   @Field("offset") Integer offset,
                                                   @Field("count") Integer count,
                                                   @Field("v") String version,
                                                   @Field("access_key") String accessKey);

    @FormUrlEncoded
    @POST("audio.getPopular")
    Single<BaseResponse<List<VKApiAudio>>> getPopular(@Field("only_eng") Integer foreign,
                                                      @Field("genre_id") Integer genre,
                                                      @Field("count") Integer count);

    @FormUrlEncoded
    @POST("audio.getRecommendations")
    Single<BaseResponse<Items<VKApiAudio>>> getRecommendations(@Field("user_id") Integer user_id,
                                                               @Field("count") Integer count);

    @FormUrlEncoded
    @POST("audio.getRecommendations")
    Single<BaseResponse<Items<VKApiAudio>>> getRecommendationsOld(@Field("user_id") Integer user_id,
                                                                  @Field("count") Integer count,
                                                                  @Field("v") String version);

    @FormUrlEncoded
    @POST("audio.getRecommendations")
    Single<BaseResponse<Items<VKApiAudio>>> getRecommendationsByAudio(@Field("target_audio") String audio,
                                                                      @Field("count") Integer count);

    @FormUrlEncoded
    @POST("audio.getRecommendations")
    Single<BaseResponse<Items<VKApiAudio>>> getRecommendationsByAudioOld(@Field("target_audio") String audio,
                                                                         @Field("count") Integer count,
                                                                         @Field("v") String version);

    @FormUrlEncoded
    @POST("audio.getById")
    Single<BaseResponse<List<VKApiAudio>>> getById(@Field("audios") String audios);

    @FormUrlEncoded
    @POST("audio.getById")
    Single<BaseResponse<List<VKApiAudio>>> getByIdOld(@Field("audios") String audios, @Field("v") String version);

    @FormUrlEncoded
    @POST("audio.getLyrics")
    Single<BaseResponse<VkApiLyrics>> getLyrics(@Field("lyrics_id") int lyrics_id);

    @FormUrlEncoded
    @POST("audio.getPlaylists")
    Single<BaseResponse<Items<VKApiAudioPlaylist>>> getPlaylists(@Field("owner_id") int owner_id,
                                                                 @Field("offset") int offset,
                                                                 @Field("count") int count);

    @FormUrlEncoded
    @POST("audio.deletePlaylist")
    Single<BaseResponse<Integer>> deletePlaylist(@Field("playlist_id") int playlist_id,
                                                 @Field("owner_id") int ownerId);

    @FormUrlEncoded
    @POST("audio.followPlaylist")
    Single<BaseResponse<VKApiAudioPlaylist>> followPlaylist(@Field("playlist_id") int playlist_id,
                                                            @Field("owner_id") int ownerId,
                                                            @Field("access_key") String accessKey);

    @FormUrlEncoded
    @POST("audio.getPlaylistById")
    Single<BaseResponse<VKApiAudioPlaylist>> getPlaylistById(@Field("playlist_id") int playlist_id,
                                                             @Field("owner_id") int ownerId,
                                                             @Field("access_key") String accessKey);

    @FormUrlEncoded
    @POST("audio.getCatalog")
    Single<BaseResponse<Items<VKApiAudioCatalog>>> getCatalog(@Field("artist_id") String artist_id);

    @FormUrlEncoded
    @POST("audio.getCatalog")
    Single<BaseResponse<Items<VKApiAudioCatalog>>> getCatalogOld(@Field("artist_id") String artist_id,
                                                                 @Field("v") String version);

    @FormUrlEncoded
    @POST("audio.getCatalogBlockById")
    Single<BaseResponse<BlockResponse<CatalogResponse>>> getCatalogBlockById(@Field("block_id") String block_id,
                                                                             @Field("start_from") String start_from);

    @FormUrlEncoded
    @POST("audio.getCatalogBlockById")
    Single<BaseResponse<BlockResponse<CatalogResponse>>> getCatalogBlockByIdOld(@Field("block_id") String block_id,
                                                                                @Field("start_from") String start_from,
                                                                                @Field("v") String version);
}
