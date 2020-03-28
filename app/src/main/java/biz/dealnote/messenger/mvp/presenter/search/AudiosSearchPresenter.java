package biz.dealnote.messenger.mvp.presenter.search;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

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
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.Single;

/**
 * Created by admin on 1/4/2018.
 * Phoenix-for-VK
 */
public class AudiosSearchPresenter extends AbsSearchPresenter<IAudioSearchView, AudioSearchCriteria, Audio, IntNextFrom> {

    private final IAudioInteractor audioInteractor;

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

    public ArrayList<Audio> listFiles(String query) {
        if(query == null)
            return new ArrayList<>();
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString());
        if(dir.listFiles() == null || dir.listFiles().length <= 0)
            return new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        int id = 0;
        for (File file : dir.listFiles()) {
            if (!file.isDirectory() && file.getName().contains(".mp3") && file.getName().toLowerCase().contains(query.toLowerCase())) {
                files.add(file);
            }
        }
        if(files.size() <= 0)
            return new ArrayList<>();
        Collections.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        ArrayList<Audio> audios = new ArrayList<>(files.size());
        for (File file : files) {

            Audio rt = new Audio().setId(++id).setUrl("file://" + file.getPath());
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

    @Override
    void onSeacrhError(Throwable throwable) {
        super.onSeacrhError(throwable);
        if (isGuiResumed()) {
            getView().ProvideReadCachedAudio();
            LocalSeached(listFiles(getCriteria().getQuery()), getCriteria());
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
    }

    @Override
    boolean canSearch(AudioSearchCriteria criteria) {
        return true;
    }

    @Override
    AudioSearchCriteria instantiateEmptyCriteria() {
        return new AudioSearchCriteria("", false);
    }

}