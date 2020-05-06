package biz.dealnote.messenger.fragment;

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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.adapter.AudioRecyclerAdapter;
import biz.dealnote.messenger.adapter.horizontal.HorizontalPlaylistAdapter;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.AudiosPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Audio is not supported :-(
 */
public class AudiosFragment extends BaseMvpFragment<AudiosPresenter, IAudiosView>
        implements IAudiosView, HorizontalPlaylistAdapter.Listener {

    public static AudiosFragment newInstance(int accountId, int ownerId, int option_menu_id, int isAlbum, String access_key) {
        Bundle args = new Bundle();
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.ID, option_menu_id);
        args.putInt(Extra.ALBUM, isAlbum);
        args.putString(Extra.ACCESS_KEY, access_key);
        AudiosFragment fragment = new AudiosFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AudiosFragment newInstanceSelect(int accountId, int ownerId) {
        Bundle args = new Bundle();
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.ID, -1);
        args.putInt(Extra.ALBUM, 0);
        args.putBoolean(ACTION_SELECT, true);
        AudiosFragment fragment = new AudiosFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AudioRecyclerAdapter mAudioRecyclerAdapter;
    private PlaybackStatus mPlaybackStatus;
    private boolean inTabsContainer;
    private boolean doAudioLoadTabs;
    private boolean isSelectMode;
    private boolean isAlbum;
    private View headerPlaylist;
    private HorizontalPlaylistAdapter mPlaylistAdapter;

    public static final String EXTRA_IN_TABS_CONTAINER = "in_tabs_container";
    public static final String ACTION_SELECT = "AudiosFragment.ACTION_SELECT";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inTabsContainer = requireArguments().getBoolean(EXTRA_IN_TABS_CONTAINER);
        isSelectMode = requireArguments().getBoolean(ACTION_SELECT);
        isAlbum = requireArguments().getInt(Extra.ALBUM) == 1;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_music, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);

        if (!inTabsContainer) {
            toolbar.setVisibility(View.VISIBLE);
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        } else {
            toolbar.setVisibility(View.GONE);
        }

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        FloatingActionButton Goto = root.findViewById(R.id.goto_button);
        if (isSelectMode)
            Goto.setImageResource(R.drawable.check);
        else
            Goto.setImageResource(R.drawable.audio_player);
        if(!isSelectMode)
        {
            Goto.setOnLongClickListener(v -> {
                Audio curr = MusicUtils.getCurrentAudio();
                if (curr != null) {
                    PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(requireActivity());
                }
                else
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

        mAudioRecyclerAdapter = new AudioRecyclerAdapter(requireActivity(), Collections.emptyList(), getPresenter().isMyAudio(), isSelectMode);


        headerPlaylist = inflater.inflate(R.layout.header_audio_playlist, recyclerView, false);
        RecyclerView headerPlaylistRecyclerView = headerPlaylist.findViewById(R.id.header_audio_playlist);
        headerPlaylistRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false));
        mPlaylistAdapter = new HorizontalPlaylistAdapter(Collections.emptyList());
        mPlaylistAdapter.setListener(this);
        headerPlaylistRecyclerView.setAdapter(mPlaylistAdapter);

        mAudioRecyclerAdapter.setClickListener((position, audio) -> getPresenter().playAudio(requireActivity(), position));
        recyclerView.setAdapter(mAudioRecyclerAdapter);
        return root;
    }

    @Override
    public void updatePlaylists(List<VKApiAudioPlaylist> stories) {
        if (nonNull(mPlaylistAdapter)) {
            mPlaylistAdapter.setItems(stories);
            mPlaylistAdapter.notifyDataSetChanged();
            mAudioRecyclerAdapter.addHeader(headerPlaylist);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!inTabsContainer) {
            Settings.get().ui().notifyPlaceResumed(Place.AUDIOS);
            ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
            if (actionBar != null) {
                actionBar.setTitle(R.string.music);
                actionBar.setSubtitle(null);
            }

            if (requireActivity() instanceof OnSectionResumeCallback) {
                ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_AUDIOS);
            }

            new ActivityFeatures.Builder()
                    .begin()
                    .setHideNavigationMenu(false)
                    .setBarsColored(requireActivity(), true)
                    .build()
                    .apply(requireActivity());
        }
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

    @Override
    public IPresenterFactory<AudiosPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new AudiosPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireArguments().getInt(Extra.OWNER_ID),
                requireArguments().getInt(Extra.ID),
                requireArguments().getInt(Extra.ALBUM),
                requireArguments().getBoolean(ACTION_SELECT),
                requireArguments().getString(Extra.ACCESS_KEY),
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
    public void ProvideReadCachedAudio() {
        PhoenixToast.CreatePhoenixToast(requireActivity()).showToastInfo(R.string.audio_from_cache);
        if (!AppPerms.hasReadWriteStoragePermision(getContext())) {
            AppPerms.requestReadWriteStoragePermission(requireActivity());
        }
    }

    @Override
    public void doesLoadCache() {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.choose_action)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_go, (dialog, whichButton) -> getPresenter().doLoadCache())
                .setMessage(R.string.load_saved_audios).show();
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
    public void onPlayListClick(VKApiAudioPlaylist item, int pos) {
        if (item.owner_id == Settings.get().accounts().getCurrent())
            getPresenter().onDelete(item);
        else
            getPresenter().onAdd(item);
    }

    private final class PlaybackStatus extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (isNull(action)) return;

            switch (action) {
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    notifyListChanged();
                    break;
            }
        }
    }
}
