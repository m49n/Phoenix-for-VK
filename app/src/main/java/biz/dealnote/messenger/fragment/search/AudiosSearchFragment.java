package biz.dealnote.messenger.fragment.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.adapter.AudioRecyclerAdapter;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.search.AudiosSearchPresenter;
import biz.dealnote.messenger.mvp.view.search.IAudioSearchView;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;


public class AudiosSearchFragment extends AbsSearchFragment<AudiosSearchPresenter, IAudioSearchView, Audio>
        implements IAudioSearchView {

    private PlaybackStatus mPlaybackStatus;

    public static AudiosSearchFragment newInstance(int accountId, AudioSearchCriteria criteria) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.CRITERIA, criteria);
        AudiosSearchFragment fragment = new AudiosSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    void setAdapterData(RecyclerView.Adapter adapter, List<Audio> data) {
        ((AudioRecyclerAdapter) adapter).setData(data);
    }

    @Override
    RecyclerView.Adapter createAdapter(List<Audio> data) {
        AudioRecyclerAdapter adapter = new AudioRecyclerAdapter(requireActivity(), Collections.emptyList());
        adapter.setClickListener((position, audio) -> getPresenter().playAudio(requireActivity(), position));
        return adapter;
    }

    @Override
    RecyclerView.LayoutManager createLayoutManager() {
        return new LinearLayoutManager(requireActivity());
    }

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

            switch (action) {
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}