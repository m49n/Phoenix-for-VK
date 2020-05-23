package biz.dealnote.messenger.mvp.presenter.search;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.fragment.search.nextfrom.IntNextFrom;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.view.search.IAudioSearchView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Pair;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Single;

/**
 * Created by admin on 1/4/2018.
 * Phoenix-for-VK
 */
public class AudiosSearchPresenter extends AbsSearchPresenter<IAudioSearchView, AudioSearchCriteria, Audio, IntNextFrom> {

    private final IAudioInteractor audioInteractor;
    private boolean LoadFromCache;

    public AudiosSearchPresenter(int accountId, @Nullable AudioSearchCriteria criteria, @Nullable Bundle savedInstanceState) {
        super(accountId, criteria, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
    }

    @Override
    IntNextFrom getInitialNextFrom() {
        return new IntNextFrom(0);
    }

    @Override
    boolean isAtLast(IntNextFrom startFrom) {
        return startFrom.getOffset() == 0;
    }

    private ArrayList<Audio> listFiles(String query) {
        if (query == null)
            return new ArrayList<>();
        File dir = new File(Settings.get().other().getMusicDir());
        if (dir.listFiles() == null || dir.listFiles().length <= 0)
            return new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        int id = 0;
        for (File file : dir.listFiles()) {
            if (!file.isDirectory() && file.getName().contains(".mp3") && file.getName().toLowerCase().contains(query.toLowerCase())) {
                files.add(file);
            }
        }
        if (files.isEmpty())
            return new ArrayList<>();
        Collections.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        ArrayList<Audio> audios = new ArrayList<>(files.size());
        for (File file : files) {

            Audio rt = new Audio().setId(++id).setUrl("file://" + file.getAbsolutePath());
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
        return audios;
    }

    public void doLoadCache() {
        LoadFromCache = true;
        getView().ProvideReadCachedAudio();
        LocalSeached(listFiles(getCriteria().getQuery()), (AudioSearchCriteria) getCriteria().safellyClone(), getNextFrom());
    }

    @Override
    void onSeacrhError(Throwable throwable) {
        super.onSeacrhError(throwable);
        if (isGuiResumed()) {
            if (!LoadFromCache) {
                showError(getView(), Utils.getCauseIfRuntime(throwable));
                callView(IAudioSearchView::doesLoadCache);
            } else
                doLoadCache();
        }
    }

    @Override
    Single<Pair<List<Audio>, IntNextFrom>> doSearch(int accountId, AudioSearchCriteria criteria, IntNextFrom startFrom) {
        final IntNextFrom nextFrom = new IntNextFrom(startFrom.getOffset() + 50);
        return audioInteractor.search(accountId, criteria, startFrom.getOffset())
                .map(audio -> Pair.Companion.create(audio, nextFrom));
    }

    public void playAudio(Context context, int position) {
        MusicPlaybackService.startForPlayList(context, (ArrayList<Audio>) data, position, false);
        if (!Settings.get().other().isShow_mini_player())
            PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(context);
    }

    @Override
    boolean canSearch(AudioSearchCriteria criteria) {
        return true;
    }

    @Override
    AudioSearchCriteria instantiateEmptyCriteria() {
        return new AudioSearchCriteria("", false, false);
    }

}