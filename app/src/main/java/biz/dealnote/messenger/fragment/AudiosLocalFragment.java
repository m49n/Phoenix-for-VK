package biz.dealnote.messenger.fragment;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.LocalAudioRecyclerAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.AudiosLocalPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosLocalView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.MySearchView;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;

public class AudiosLocalFragment extends BaseMvpFragment<AudiosLocalPresenter, IAudiosLocalView>
        implements MySearchView.OnQueryTextListener, IAudiosLocalView {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LocalAudioRecyclerAdapter mAudioRecyclerAdapter;
    private PlaybackStatus mPlaybackStatus;
    private boolean doAudioLoadTabs;

    public static AudiosLocalFragment newInstance(int accountId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        AudiosLocalFragment fragment = new AudiosLocalFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_local_music, container, false);

        MySearchView searchView = root.findViewById(R.id.searchview);
        searchView.setOnQueryTextListener(this);
        searchView.setQuery("", true);

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        FloatingActionButton Goto = root.findViewById(R.id.goto_button);
        Goto.setImageResource(R.drawable.audio_player);

        Goto.setOnLongClickListener(v -> {
            Audio curr = MusicUtils.getCurrentAudio();
            if (curr != null) {
                PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(requireActivity());
            } else
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.null_audio);
            return false;
        });
        Goto.setOnClickListener(v -> {
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
        });
        mAudioRecyclerAdapter = new LocalAudioRecyclerAdapter(requireActivity(), Collections.emptyList());
        mAudioRecyclerAdapter.setClickListener((position, audio) -> getPresenter().playAudio(requireActivity(), position));
        recyclerView.setAdapter(mAudioRecyclerAdapter);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!doAudioLoadTabs) {
            doAudioLoadTabs = true;
            getPresenter().LoadAudiosTool();
        }
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

    @NotNull
    @Override
    public IPresenterFactory<AudiosLocalPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new AudiosLocalPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireActivity(),
                saveInstanceState
        );
    }

    @Override
    public void displayList(List<Audio> audios) {
        if (nonNull(mAudioRecyclerAdapter)) {
            mAudioRecyclerAdapter.setData(audios);
        }
    }

    @Override
    public void notifyListChanged() {
        if (nonNull(mAudioRecyclerAdapter)) {
            mAudioRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayRefreshing(boolean refresing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(refresing);
        }
    }

    @Override
    public void onPause() {
        try {
            requireActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable ignored) {
        }
        super.onPause();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        getPresenter().fireQuery(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        getPresenter().fireQuery(newText);
        return false;
    }

    private final class PlaybackStatus extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (isNull(action)) return;

            if (MusicPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                notifyListChanged();
            }
        }
    }
}
