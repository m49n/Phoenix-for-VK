package biz.dealnote.messenger.fragment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.activity.SendAttachmentsActivity;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.ui.PlayPauseButton;
import biz.dealnote.messenger.player.ui.RepeatButton;
import biz.dealnote.messenger.player.ui.RepeatingImageButton;
import biz.dealnote.messenger.player.ui.ShuffleButton;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.DownloadUtil;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.view.CircleCounterButton;
import biz.dealnote.messenger.view.verticalswipe.BelowFractionalClamp;
import biz.dealnote.messenger.view.verticalswipe.NegativeFactorFilterSideEffect;
import biz.dealnote.messenger.view.verticalswipe.PropertySideEffect;
import biz.dealnote.messenger.view.verticalswipe.SensitivityClamp;
import biz.dealnote.messenger.view.verticalswipe.SettleOnTopAction;
import biz.dealnote.messenger.view.verticalswipe.VerticalSwipeBehavior;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.player.util.MusicUtils.isPlaying;
import static biz.dealnote.messenger.player.util.MusicUtils.mService;
import static biz.dealnote.messenger.player.util.MusicUtils.observeServiceBinding;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;

public class AudioPlayerFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {
    // Message to refresh the time
    private static final int REFRESH_TIME = 1;
    private static final int REQUEST_EQ = 139;
    // Play and pause button
    private PlayPauseButton mPlayPauseButton;
    // Repeat button
    private RepeatButton mRepeatButton;
    // Shuffle button
    private ShuffleButton mShuffleButton;
    // Current time
    private TextView mCurrentTime;
    // Total time
    private TextView mTotalTime;
    private TextView mGetLyrics;
    // Progress
    private SeekBar mProgress;
    // VK Additional action
    private CircleCounterButton ivAdd;
    private RepeatingImageButton ivSave;
    private CircleCounterButton ivTranslate;
    private TextView tvTitle;
    private TextView tvAlbum;
    private TextView tvSubtitle;
    private ImageView ivCover;
    private boolean isCaptured = false;
    // Broadcast receiver
    private PlaybackStatus mPlaybackStatus;
    // Handler used to update the current time
    private TimeHandler mTimeHandler;
    private long mPosOverride = -1;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private boolean mIsPaused = false;
    private boolean mFromTouch = false;
    private String[] mPlayerProgressStrings;
    /**
     * Used to scan backwards through the track
     */
    private final RepeatingImageButton.RepeatListener mRewindListener = ((v, howlong, repcnt) ->
            scanBackward(repcnt, howlong));
    /**
     * Used to scan ahead through the track
     */
    private final RepeatingImageButton.RepeatListener mFastForwardListener = ((v, howlong, repcnt) ->
            scanForward(repcnt, howlong));
    private IAudioInteractor mAudioInteractor;

    private int mAccountId;
    private CompositeDisposable mBroadcastDisposable = new CompositeDisposable();

    public static Bundle buildArgs(int accountId) {
        Bundle bundle = new Bundle();
        bundle.putInt(Extra.ACCOUNT_ID, accountId);
        return bundle;
    }

    public static AudioPlayerFragment newInstance(int accountId) {
        return newInstance(buildArgs(accountId));
    }

    public static AudioPlayerFragment newInstance(Bundle args) {
        AudioPlayerFragment fragment = new AudioPlayerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mAccountId = requireArguments().getInt(Extra.ACCOUNT_ID);
        mAudioInteractor = InteractorFactory.createAudioInteractor();

        mTimeHandler = new TimeHandler(this);
        mPlaybackStatus = new PlaybackStatus(this);
        mPlayerProgressStrings = getResources().getStringArray(R.array.player_progress_state);

        appendDisposable(observeServiceBinding()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(ignore -> onServiceBindEvent()));
    }

    private void onServiceBindEvent() {
        updatePlaybackControls();
        updateNowPlayingInfo();
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.audio_player_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NotNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.eq).setVisible(isEqualizerAvailable());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.eq:
                startEffectsPanel();
                return true;

            case R.id.playlist:
                PlaceFactory.getPlaylistPlace().tryOpenWith(requireActivity());
                return true;
            case R.id.copy_track_info:
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                String Artist = MusicUtils.getCurrentAudio().getArtist() != null ? MusicUtils.getCurrentAudio().getArtist() : "";
                if (MusicUtils.getCurrentAudio().getAlbum_title() != null)
                    Artist += " (" + MusicUtils.getCurrentAudio().getAlbum_title() + ")";
                String Name = MusicUtils.getCurrentAudio().getTitle() != null ? MusicUtils.getCurrentAudio().getTitle() : "";
                ClipData clip = ClipData.newPlainText("response", Artist + " - " + Name);
                clipboard.setPrimaryClip(clip);
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.copied_to_clipboard);
                return true;
            case R.id.search_by_artist:
                PlaceFactory.getSingleTabSearchPlace(mAccountId, SearchContentType.AUDIOS, new AudioSearchCriteria(MusicUtils.getCurrentAudio().getArtist(), true, false)).tryOpenWith(requireActivity());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutRes;
        if (Utils.isLandscape(requireActivity()) && !Utils.is600dp(requireActivity())) {
            layoutRes = Settings.get().ui().isDisable_swipes() ? R.layout.fragment_player_land_no_swipe : R.layout.fragment_player_land;
        } else {
            layoutRes = Settings.get().ui().isDisable_swipes() ? R.layout.fragment_player_port_new_no_swipe : R.layout.fragment_player_port_new;
        }

        View root = inflater.inflate(layoutRes, container, false);

        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        ConstraintLayout Root = root.findViewById(R.id.player_layout);
        mProgress = root.findViewById(android.R.id.progress);

        if (!Settings.get().ui().isDisable_swipes()) {
            VerticalSwipeBehavior<ConstraintLayout> ui = VerticalSwipeBehavior.Companion.from(Root);
            ui.setSettle(new SettleOnTopAction());
            PropertySideEffect sideDelegate = new PropertySideEffect(View.ALPHA, View.SCALE_X, View.SCALE_Y);
            ui.setSideEffect(new NegativeFactorFilterSideEffect(sideDelegate));
            BelowFractionalClamp clampDelegate = new BelowFractionalClamp(3f, 3f);
            ui.setClamp(new SensitivityClamp(0.5f, clampDelegate, 0.5f));
            ui.setListener(new VerticalSwipeBehavior.SwipeListener() {
                @Override
                public void onReleased() {
                    isCaptured = false;
                }

                @Override
                public void onCaptured() {
                    isCaptured = true;
                }

                @Override
                public void onPreSettled(int diff) {
                }

                @Override
                public void onPostSettled(int diff) {
                    if (Settings.get().ui().isPhoto_swipe_pos_top_to_bottom() && diff >= 120 || !Settings.get().ui().isPhoto_swipe_pos_top_to_bottom() && diff <= -120) {
                        goBack();
                    } else
                        isCaptured = false;
                }
            });
            mProgress.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        ui.setCanSwipe(false);
                        break;
                    case MotionEvent.ACTION_UP:
                        ui.setCanSwipe(true);
                        break;
                }
                return false;
            });
        }

        mPlayPauseButton = root.findViewById(R.id.action_button_play);
        mShuffleButton = root.findViewById(R.id.action_button_shuffle);
        mRepeatButton = root.findViewById(R.id.action_button_repeat);

        RepeatingImageButton mPreviousButton = root.findViewById(R.id.action_button_previous);
        RepeatingImageButton mNextButton = root.findViewById(R.id.action_button_next);

        ivCover = root.findViewById(R.id.cover);

        mCurrentTime = root.findViewById(R.id.audio_player_current_time);
        mTotalTime = root.findViewById(R.id.audio_player_total_time);

        tvTitle = root.findViewById(R.id.audio_player_title);
        tvAlbum = root.findViewById(R.id.audio_player_album);
        tvSubtitle = root.findViewById(R.id.audio_player_subtitle);
        mGetLyrics = root.findViewById(R.id.audio_player_get_lyrics);
        mGetLyrics.setOnClickListener(v -> onLyrics());

        //to animate running text
        tvTitle.setSelected(true);
        tvSubtitle.setSelected(true);
        tvAlbum.setSelected(true);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.phoenix_player));
            actionBar.setSubtitle(null);
        }

        mPreviousButton.setRepeatListener(mRewindListener);
        mNextButton.setRepeatListener(mFastForwardListener);
        mProgress.setOnSeekBarChangeListener(this);

        ivSave = root.findViewById(R.id.audio_save);
        ivSave.setOnClickListener(v -> onSaveButtonClick());

        ivAdd = root.findViewById(R.id.audio_add);
        if (Settings.get().main().isPlayer_support_volume()) {
            ivAdd.setIcon(R.drawable.volume_minus);
            ivAdd.setOnClickListener(v -> {
                AudioManager audio = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamVolume(AudioManager.STREAM_MUSIC) - 1, 0);
            });
        } else {
            ivAdd.setIcon(R.drawable.plus);
            ivAdd.setOnClickListener(v -> onAddButtonClick());
        }

        CircleCounterButton ivShare = root.findViewById(R.id.audio_share);
        if (Settings.get().main().isPlayer_support_volume()) {
            ivShare.setIcon(R.drawable.volume_plus);
            ivShare.setOnClickListener(v -> {
                AudioManager audio = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamVolume(AudioManager.STREAM_MUSIC) + 1, 0);
            });
        } else {
            ivShare.setIcon(R.drawable.share_variant);
            ivShare.setOnClickListener(v -> shareAudio());
        }

//        if (isAudioStreaming()) {
//            broadcastAudio();
//        }

        resolveAddButton();

        return root;
    }

    private boolean canGoBack() {
        return requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 1;
    }

    private void goBack() {
        if (isAdded() && canGoBack()) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void onAudioBroadcastButtonClick() {
        ivTranslate.setActive(!ivTranslate.isActive());

        Settings.get()
                .other()
                .setAudioBroadcastActive(ivTranslate.isActive());

        if (isAudioStreaming()) {
            broadcastAudio();
        }
    }

    private boolean isAudioStreaming() {
        return Settings.get()
                .other()
                .isAudioBroadcastActive();
    }

    private void onSaveButtonClick() {
        Audio audio = MusicUtils.getCurrentAudio();
        if (audio == null) {
            return;
        }
        if (!AppPerms.hasReadWriteStoragePermision(getContext())) {
            AppPerms.requestReadWriteStoragePermission(requireActivity());
            return;
        }

        int ret = DownloadUtil.downloadTrack(getContext(), audio, false);
        if (ret == 0)
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastBottom(R.string.saved_audio);
        else if (ret == 1) {
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.exist_audio);
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.error)
                    .setMessage(R.string.audio_force_download)
                    .setPositiveButton(R.string.button_yes, (dialog, which) -> DownloadUtil.downloadTrack(getContext(), audio, true))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastBottom(R.string.error_audio);
    }

    private void onAddButtonClick() {
        Audio audio = MusicUtils.getCurrentAudio();
        if (audio == null) {
            return;
        }

        if (audio.getOwnerId() == mAccountId) {
            if (!audio.isDeleted()) {
                delete(mAccountId, audio);
            } else {
                restore(mAccountId, audio);
            }
        } else {
            add(mAccountId, audio);
        }
    }

    private void onLyrics() {
        Audio audio = MusicUtils.getCurrentAudio();
        if (audio == null) {
            return;
        }
        get_lyrics(audio);
    }

    private void add(int accountId, Audio audio) {
        appendDisposable(mAudioInteractor.add(accountId, audio, null, null)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onAudioAdded, t -> {/*TODO*/}));
    }

    @SuppressWarnings("unused")
    private void onAudioAdded(Audio result) {
        PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.added);
        resolveAddButton();
    }

    private void delete(final int accoutnId, Audio audio) {
        final int id = audio.getId();
        final int ownerId = audio.getOwnerId();

        appendDisposable(mAudioInteractor.delete(accoutnId, id, ownerId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onAudioDeletedOrRestored(id, ownerId, true), t -> {/*TODO*/}));
    }

    private void restore(final int accountId, Audio audio) {
        final int id = audio.getId();
        final int ownerId = audio.getOwnerId();

        appendDisposable(mAudioInteractor.restore(accountId, id, ownerId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onAudioDeletedOrRestored(id, ownerId, false), t -> {/*TODO*/}));
    }

    private void get_lyrics(Audio audio) {
        appendDisposable(mAudioInteractor.getLyrics(audio.getLyricsId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onAudioLyricsRecived, t -> {/*TODO*/}));
    }

    private void onAudioLyricsRecived(String Text) {
        String title = null;
        if (MusicUtils.getCurrentAudio() != null)
            title = MusicUtils.getCurrentAudio().getArtistAndTitle();

        MaterialAlertDialogBuilder dlgAlert = new MaterialAlertDialogBuilder(requireActivity());
        dlgAlert.setIcon(R.drawable.dir_song);
        dlgAlert.setMessage(Text);
        dlgAlert.setTitle(title != null ? title : requireContext().getString(R.string.get_lyrics));

        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("response", Text);
        clipboard.setPrimaryClip(clip);

        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.copied_to_clipboard);
        dlgAlert.create().show();
    }

    private void onAudioDeletedOrRestored(int id, int ownerId, boolean deleted) {
        if (deleted) {
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.deleted);
        } else {
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.restored);
        }

        Audio current = MusicUtils.getCurrentAudio();

        if (nonNull(current) && current.getId() == id && current.getOwnerId() == ownerId) {
            current.setDeleted(deleted);
        }

        resolveAddButton();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProgressChanged(final SeekBar bar, final int progress, final boolean fromuser) {
        if (!fromuser || mService == null) {
            return;
        }

        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;

            refreshCurrentTime();

            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        }

        mPosOverride = MusicUtils.duration() * progress / 1000;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(final SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;

        mCurrentTime.setVisibility(View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(final SeekBar bar) {
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
            mPosOverride = -1;
        }

        mFromTouch = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onClearSelection();
        }

        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(true)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Player prepared
        filter.addAction(MusicPlaybackService.PREPARED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);

        requireActivity().registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);

        MusicUtils.notifyForegroundStateChanged(requireActivity(), isPlaying());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();
        MusicUtils.notifyForegroundStateChanged(requireActivity(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);
        mBroadcastDisposable.dispose();

        // Unregister the receiver
        try {
            requireActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable ignored) {
            //$FALL-THROUGH$
        }
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateNowPlayingInfo() {
        String artist = MusicUtils.getArtistName();
        String trackName = MusicUtils.getTrackName();
        String coverUrl = MusicUtils.getAlbumCoverBig();
        if (mGetLyrics != null) {
            if (MusicUtils.getCurrentAudio() != null && MusicUtils.getCurrentAudio().getLyricsId() != 0) {
                mGetLyrics.setText(R.string.get_lyrics);
                mGetLyrics.setVisibility(View.VISIBLE);
            } else
                mGetLyrics.setVisibility(View.INVISIBLE);
        }

        if (tvAlbum != null && MusicUtils.getCurrentAudio() != null) {
            String album = "";
            if (MusicUtils.getCurrentAudio().getAlbum_title() != null)
                album += (requireContext().getString(R.string.album) + " " + MusicUtils.getCurrentAudio().getAlbum_title());
            tvAlbum.setText(album);
        }
        if (tvTitle != null) {
            tvTitle.setText(artist == null ? null : artist.trim());
        }

        if (tvSubtitle != null) {
            tvSubtitle.setText(trackName == null ? null : trackName.trim());
        }

        if (coverUrl != null) {
            ivCover.setScaleType(ImageView.ScaleType.FIT_START);
            PicassoInstance.with().load(coverUrl).tag(Constants.PICASSO_TAG).into(ivCover);
        } else {
            ivCover.setScaleType(ImageView.ScaleType.CENTER);
            ivCover.setImageResource(R.drawable.itunes);
            ivCover.getDrawable().setTint(CurrentTheme.getColorOnSurface(requireContext()));
        }

        resolveAddButton();

        Audio current = MusicUtils.getCurrentAudio();

        if (current != null) {
            if (DownloadUtil.TrackIsDownloaded(current)) {
                ivSave.setImageResource(R.drawable.succ);
            } else if (Objects.isNullOrEmptyString(current.getUrl())) {
                ivSave.setImageResource(R.drawable.audio_died);
            } else
                ivSave.setImageResource(R.drawable.save);
        } else
            ivSave.setImageResource(R.drawable.save);

        //handle VK actions
        if (current != null && isAudioStreaming()) {
            broadcastAudio();
        }

        // Set the total time
        resolveTotalTime();
        // Update the current time
        queueNextRefresh(1);
    }

    private void resolveTotalTime() {
        if (!isAdded() || mTotalTime == null) {
            return;
        }

        if (MusicUtils.isInitialized()) {
            mTotalTime.setText(MusicUtils.makeTimeString(requireActivity(), MusicUtils.duration() / 1000));
        }
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        if (!isAdded()) {
            return;
        }

        // Set the play and pause image
        if (nonNull(mPlayPauseButton)) {
            mPlayPauseButton.updateState();
        }

        // Set the shuffle image
        if (nonNull(mShuffleButton)) {
            mShuffleButton.updateShuffleState();
        }

        // Set the repeat image
        if (nonNull(mRepeatButton)) {
            mRepeatButton.updateRepeatState();
        }
    }

    private void startEffectsPanel() {
        try {
            final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            effects.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().getPackageName());
            effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.getAudioSessionId());
            effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
            startActivityForResult(effects, REQUEST_EQ);
        } catch (final ActivityNotFoundException ignored) {
            Toast.makeText(requireActivity(), "No system equalizer found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEqualizerAvailable() {
        Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        PackageManager manager = requireActivity().getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return infos.size() > 0;
    }

    private void shareAudio() {
        Audio current = MusicUtils.getCurrentAudio();
        if (current == null) {
            return;
        }

        SendAttachmentsActivity.startForSendAttachments(requireActivity(), mAccountId, current);
    }

    private void resolveAddButton() {
        if (Settings.get().main().isPlayer_support_volume())
            return;

        if (!isAdded()) return;

        Audio currentAudio = MusicUtils.getCurrentAudio();
        //ivAdd.setVisibility(currentAudio == null ? View.INVISIBLE : View.VISIBLE);
        if (currentAudio == null) {
            return;
        }

        boolean myAudio = currentAudio.getOwnerId() == mAccountId;
        int icon = myAudio && !currentAudio.isDeleted() ? R.drawable.delete : R.drawable.plus;
        ivAdd.setIcon(icon);
    }

    private void broadcastAudio() {
        mBroadcastDisposable.clear();

        Audio currentAudio = MusicUtils.getCurrentAudio();

        if (currentAudio == null) {
            return;
        }

        final int accountId = mAccountId;
        final Collection<Integer> targetIds = Collections.singleton(accountId);
        final int id = currentAudio.getId();
        final int ownerId = currentAudio.getOwnerId();

        mBroadcastDisposable.add(mAudioInteractor.sendBroadcast(accountId, ownerId, id, targetIds)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> {/*ignore*/}, t -> {/*ignore*/}));
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);

            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    private void resolveControlViews() {
        if (!isAdded() || mProgress == null) return;

        boolean preparing = MusicUtils.isPreparing();
        boolean initialized = MusicUtils.isInitialized();
        mProgress.setEnabled(!preparing && initialized);
        //mProgress.setIndeterminate(preparing);
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta  The long press duration
     */
    private void scanBackward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(requireActivity());
                final long duration = MusicUtils.duration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }

            refreshCurrentTime();
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta  The long press duration
     */
    private void scanForward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }

        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }

            long newpos = mStartSeekPos + delta;
            final long duration = MusicUtils.duration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }

            refreshCurrentTime();
        }
    }

    private void refreshCurrentTimeText(final long pos) {
        mCurrentTime.setText(MusicUtils.makeTimeString(requireActivity(), pos / 1000));
    }

    private long refreshCurrentTime() {
        //Logger.d("refreshTime", String.valueOf(mService == null));

        if (mService == null) {
            return 500;
        }

        try {
            final long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            final long duration = MusicUtils.duration();

            if (pos >= 0 && duration > 0) {
                refreshCurrentTimeText(pos);
                final int progress = (int) (1000 * pos / MusicUtils.duration());

                mProgress.setProgress(progress);

                int bufferProgress = (int) ((float) MusicUtils.bufferPercent() * 10F);
                mProgress.setSecondaryProgress(bufferProgress);

                if (mFromTouch) {
                    return 500;
                } else if (MusicUtils.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    final int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(0);

                int current = mTotalTime.getTag() == null ? 0 : (int) mTotalTime.getTag();
                int next = current == mPlayerProgressStrings.length - 1 ? 0 : current + 1;

                mTotalTime.setTag(next);
                mTotalTime.setText(mPlayerProgressStrings[next]);
                return 500;
            }

            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            final long remaining = duration - pos % duration;

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }

            final long smoothrefreshtime = duration / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }

            if (smoothrefreshtime < 20) {
                return 20;
            }

            return smoothrefreshtime;
        } catch (final Exception ignored) {
        }

        return 500;
    }

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private final WeakReference<AudioPlayerFragment> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        TimeHandler(final AudioPlayerFragment player) {
            mAudioPlayer = new WeakReference<>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == REFRESH_TIME) {
                if (mAudioPlayer.get().isCaptured) {
                    mAudioPlayer.get().queueNextRefresh(300);
                    return;
                }
                final long next = mAudioPlayer.get().refreshCurrentTime();
                mAudioPlayer.get().queueNextRefresh(next);
            }
        }
    }

    /**
     * Used to monitor the state of playback
     */
    private static final class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<AudioPlayerFragment> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final AudioPlayerFragment activity) {
            mReference = new WeakReference<>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            AudioPlayerFragment fragment = mReference.get();
            if (isNull(fragment) || isNull(action)) return;

            switch (action) {
                case MusicPlaybackService.META_CHANGED:
                case MusicPlaybackService.PREPARED:
                    // Current info
                    fragment.updateNowPlayingInfo();
                    fragment.resolveControlViews();
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    // Set the play and pause image
                    fragment.mPlayPauseButton.updateState();
                    fragment.resolveTotalTime();
                    fragment.resolveControlViews();
                    break;
                case MusicPlaybackService.REPEATMODE_CHANGED:
                case MusicPlaybackService.SHUFFLEMODE_CHANGED:
                    // Set the repeat image
                    fragment.mRepeatButton.updateRepeatState();
                    // Set the shuffle image
                    fragment.mShuffleButton.updateShuffleState();
                    break;
            }
        }
    }
}