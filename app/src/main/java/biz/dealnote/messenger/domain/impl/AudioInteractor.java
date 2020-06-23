package biz.dealnote.messenger.domain.impl;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.mappers.Dto2Model;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.fragment.search.options.SpinnerOption;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioCatalog;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.model.CatalogBlock;
import biz.dealnote.messenger.model.IdPair;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Completable;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Utils.listEmptyIfNull;

public class AudioInteractor implements IAudioInteractor {

    private final INetworker networker;
    private final int AccountId;

    public AudioInteractor(INetworker networker, int AccountId) {
        this.networker = networker;
        this.AccountId = AccountId;
    }

    protected static String join(Collection<IdPair> audios, String delimiter) {
        if (isNull(audios)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (IdPair pair : audios) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }

            sb.append(pair.ownerId).append("_").append(pair.id);
        }

        return sb.toString();
    }

    @Override
    public Single<Audio> add(int accountId, Audio orig, Integer groupId, Integer albumId) {
        return networker.vkDefault(accountId)
                .audio()
                .add(orig.getId(), orig.getOwnerId(), groupId, albumId)
                .map(resultId -> {
                    final int targetOwnerId = Objects.nonNull(groupId) ? -groupId : accountId;
                    //clone
                    return orig
                            .setId(resultId)
                            .setOwnerId(targetOwnerId)
                            .setAlbumId(Objects.nonNull(albumId) ? albumId : 0);
                });
    }

    @Override
    public Completable delete(int accountId, int audioId, int ownerId) {
        return networker.vkDefault(accountId)
                .audio()
                .delete(audioId, ownerId)
                .ignoreElement();
    }

    @Override
    public Completable restore(int accountId, int audioId, int ownerId) {
        return networker.vkDefault(accountId)
                .audio()
                .restore(audioId, ownerId)
                .ignoreElement();
    }

    @Override
    public Completable sendBroadcast(int accountId, int audioOwnerId, int audioId, Collection<Integer> targetIds) {
        return networker.vkDefault(accountId)
                .audio()
                .setBroadcast(new biz.dealnote.messenger.api.model.IdPair(audioId, audioOwnerId), targetIds)
                .ignoreElement();
    }

    @Override
    public Single<List<Audio>> get(int accountId, Integer album_id, int ownerId, int offset, String accessKey) {

        return networker.vkDefault(accountId)
                .audio()
                .get(album_id, ownerId, offset, accessKey)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<List<Audio>> getById(List<IdPair> audios) {
        return networker.vkDefault(AccountId)
                .audio()
                .getById(join(audios, ","))
                .map(Utils::listEmptyIfNull)
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<String> getLyrics(int lyrics_id) {
        return networker.vkDefault(AccountId)
                .audio().getLyrics(lyrics_id).map(out -> out.text);
    }

    @Override
    public Single<List<Audio>> getPopular(int accountId, int foreign, int genre) {

        return networker.vkDefault(accountId)
                .audio()
                .getPopular(foreign, genre)
                .map(Utils::listEmptyIfNull)
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<List<Audio>> getRecommendations(int accountId, int audioOwnerId) {
        return networker.vkDefault(accountId)
                .audio()
                .getRecommendations(audioOwnerId)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<List<Audio>> getRecommendationsByAudio(int accountId, String audio) {
        return networker.vkDefault(accountId)
                .audio()
                .getRecommendationsByAudio(audio)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<List<AudioPlaylist>> getPlaylists(int accountId, int owner_id, int offset) {
        return networker.vkDefault(accountId)
                .audio()
                .getPlaylists(owner_id, offset)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<AudioPlaylist> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<List<AudioCatalog>> getCatalog(int accountId, String artist_id) {
        return networker.vkDefault(accountId)
                .audio()
                .getCatalog(artist_id)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<AudioCatalog> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<AudioPlaylist> followPlaylist(int accountId, int playlist_id, int ownerId, String accessKey) {
        return networker.vkDefault(accountId)
                .audio()
                .followPlaylist(playlist_id, ownerId, accessKey)
                .map(Dto2Model::transform);
    }

    @Override
    public Single<AudioPlaylist> getPlaylistById(int accountId, int playlist_id, int ownerId, String accessKey) {
        return networker.vkDefault(accountId)
                .audio()
                .getPlaylistById(playlist_id, ownerId, accessKey)
                .map(Dto2Model::transform);
    }

    @Override
    public Single<Integer> deletePlaylist(int accountId, int playlist_id, int ownerId) {
        return networker.vkDefault(accountId)
                .audio()
                .deletePlaylist(playlist_id, ownerId)
                .map(resultId -> resultId);
    }

    @Override
    public Single<List<Audio>> search(int accountId, AudioSearchCriteria criteria, int offset) {
        Boolean isMyAudio = criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_ADDED);
        Boolean isbyArtist = criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_BY_ARTIST);
        Boolean isautocmp = criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_AUTOCOMPLETE);
        Boolean islyrics = criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_WITH_LYRICS);
        SpinnerOption sortOption = criteria.findOptionByKey(AudioSearchCriteria.KEY_SORT);
        Integer sort = (sortOption == null || sortOption.value == null) ? null : sortOption.value.id;

        return networker.vkDefault(accountId)
                .audio()
                .search(criteria.getQuery(), isautocmp, islyrics, isbyArtist, sort, isMyAudio, offset)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)));
                    return ret;
                });
    }

    @Override
    public Single<CatalogBlock> getCatalogBlockById(int accountId, String block_id, String start_from) {
        return networker.vkDefault(accountId)
                .audio()
                .getCatalogBlockById(block_id, start_from)
                .map(Dto2Model::transform);
    }

    @Override
    public Single<List<Audio>> loadLocalAudios(int accountId, Context context) {
        if (!AppPerms.hasReadWriteStoragePermision(context)) {
            AppPerms.requestReadWriteStoragePermission((Activity) context);
            return Single.just(new ArrayList<>());
        }
        File dir = new File(Settings.get().other().getMusicDir());
        if (dir.listFiles() == null || java.util.Objects.requireNonNull(dir.listFiles()).length <= 0)
            return Single.just(new ArrayList<>());
        ArrayList<File> files = new ArrayList<>();
        for (File file : java.util.Objects.requireNonNull(dir.listFiles())) {
            if (!file.isDirectory() && file.getName().contains(".mp3")) {
                files.add(file);
            }
        }
        if (files.isEmpty())
            return Single.just(new ArrayList<>());
        Collections.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        ArrayList<Audio> audios = new ArrayList<>(files.size());
        for (File file : files) {

            Audio rt = new Audio().setId(file.getAbsolutePath().hashCode()).setUrl("file://" + file.getAbsolutePath()).setIsLocal(true).setOwnerId(accountId);
            String TrackName = file.getName().replace(".mp3", "");
            String Artist = "";
            String[] arr = TrackName.split(" - ");
            if (arr.length > 1) {
                Artist = arr[0];
                TrackName = TrackName.replace(Artist + " - ", "");
            }
            rt.setArtist(Artist);
            rt.setTitle(TrackName);

            audios.add(rt);
        }
        return Single.just(audios);
    }
}