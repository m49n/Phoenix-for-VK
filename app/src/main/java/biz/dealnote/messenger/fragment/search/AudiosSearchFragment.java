package biz.dealnote.messenger.fragment.search;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.AudioRecyclerAdapter;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.search.AudiosSearchPresenter;
import biz.dealnote.messenger.mvp.view.search.IAudioSearchView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;


public class AudiosSearchFragment extends AbsSearchFragment<AudiosSearchPresenter, IAudioSearchView, Audio, AudioRecyclerAdapter> {

    public static final String ACTION_SELECT = "AudiosSearchFragment.ACTION_SELECT";
    private PlaybackStatus mPlaybackStatus;
    private boolean isSelectMode;

    public static AudiosSearchFragment newInstance(int accountId, AudioSearchCriteria criteria) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.CRITERIA, criteria);
        AudiosSearchFragment fragment = new AudiosSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AudiosSearchFragment newInstanceSelect(int accountId, AudioSearchCriteria criteria) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.CRITERIA, criteria);
        args.putBoolean(ACTION_SELECT, true);
        AudiosSearchFragment fragment = new AudiosSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    void setAdapterData(AudioRecyclerAdapter adapter, List<Audio> data) {
        adapter.setData(data);
    }

    @Override
    public View createViewLayout(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return inflater.inflate(R.layout.fragment_search_audio, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isSelectMode = requireArguments().getBoolean(ACTION_SELECT);
    }

    @Override
    void postCreate(View root) {
        FloatingActionButton Goto = root.findViewById(R.id.goto_button);
        RecyclerView recyclerView = root.findViewById(R.id.list);
        if (isSelectMode)
            Goto.setImageResource(R.drawable.check);
        else
            Goto.setImageResource(R.drawable.audio_player);
        if (!isSelectMode) {
            Goto.setOnLongClickListener(v -> {
                Audio curr = MusicUtils.getCurrentAudio();
                if (curr != null) {
                    PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(requireActivity());
                } else
                    PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.null_audio);
                return false;
            });
        }
        Goto.setOnClickListener(v -> {
            if (isSelectMode) {
                Intent intent = new Intent();
                intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, getPresenter().getSelected());
                requireActivity().setResult(Activity.RESULT_OK, intent);
                requireActivity().finish();
            } else {
                Audio curr = MusicUtils.getCurrentAudio();
                if (curr != null) {
                    int index = getPresenter().getAudioPos(curr);
                    if (index >= 0) {
                        if (Settings.get().other().isShow_audio_cover())
                            recyclerView.scrollToPosition(index);
                        else
                            recyclerView.smoothScrollToPosition(index);
                    } else
                        PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.audio_not_found);
                } else
                    PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.null_audio);
            }
        });
    }

    @Override
    AudioRecyclerAdapter createAdapter(List<Audio> data) {
        AudioRecyclerAdapter adapter = new AudioRecyclerAdapter(requireActivity(), Collections.emptyList(), false, isSelectMode, 0);
        adapter.setClickListener((position, catalog, audio) -> getPresenter().playAudio(requireActivity(), position));
        return adapter;
    }

    @Override
    RecyclerView.LayoutManager createLayoutManager() {
        return new LinearLayoutManager(requireActivity());
    }

    @NotNull
    @Override
    public IPresenterFactory<AudiosSearchPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new AudiosSearchPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireArguments().getParcelable(Extra.CRITERIA),
                saveInstanceState
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mPlaybackStatus = new PlaybackStatus();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        filter.addAction(MusicPlaybackService.META_CHANGED);
        filter.addAction(MusicPlaybackService.PREPARED);
        filter.addAction(MusicPlaybackService.REFRESH);
        requireActivity().registerReceiver(mPlaybackStatus, filter);
    }

    @Override
    public void onPause() {
        try {
            requireActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable ignored) {
        }
        super.onPause();
    }

    private final class PlaybackStatus extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (isNull(action)) return;

            if (MusicPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }
}