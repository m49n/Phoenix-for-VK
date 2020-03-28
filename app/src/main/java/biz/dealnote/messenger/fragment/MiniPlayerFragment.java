package biz.dealnote.messenger.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;

import static biz.dealnote.messenger.player.util.MusicUtils.mService;
import static biz.dealnote.messenger.player.util.MusicUtils.observeServiceBinding;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.firstNonEmptyString;

public class MiniPlayerFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {
    private int mAccountId;

    private IAudioInteractor mAudioInteractor;
    private static final int REFRESH_TIME = 1;
    private PlaybackStatus mPlaybackStatus;
    private ImageButton mPlay;
    private ImageButton mPClosePlay;
    private ImageButton mOpenPlayer;
    private TextView Title;
    private SeekBar mProgress;
    private boolean mFromTouch = false;
    private long mPosOverride = -1;
    private LinearLayout lnt;

    private boolean HideBySuper = false;

    private TimeHandler mTimeHandler;
    private boolean mIsPaused = false;
    private long mLastSeekEventTime;
    private Audio CurrentTrack = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAudioInteractor = InteractorFactory.createAudioInteractor();

        mTimeHandler = new TimeHandler(this);

        mAccountId = Settings.get()
                .accounts()
                .getCurrent();
        this.mPlaybackStatus = new PlaybackStatus(this);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        filter.addAction(MusicPlaybackService.META_CHANGED);
        filter.addAction(MusicPlaybackService.PREPARED);
        filter.addAction(MusicPlaybackService.REFRESH);
        requireActivity().registerReceiver(mPlaybackStatus, filter);
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
    }

    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);

            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.mini_player, container, false);
        mPlay = root.findViewById(R.id.btn_play_pause);
        lnt = root.findViewById(R.id.miniplayer_layout);
        lnt.setVisibility(View.INVISIBLE);
        mPClosePlay = root.findViewById(R.id.close_player);
        mPClosePlay.setOnClickListener(v -> {
            CurrentTrack = MusicUtils.getCurrentAudio();
            lnt.setVisibility(View.INVISIBLE);
            }
        );
        mOpenPlayer = root.findViewById(R.id.open_player);
        mOpenPlayer.setOnClickListener(v -> PlaceFactory.getPlayerPlace(mAccountId).tryOpenWith(requireActivity()));
        mPlay.setOnClickListener(v -> {
            MusicUtils.playOrPause();
            if (MusicUtils.isPlaying()) {
                mPlay.setImageResource(R.drawable.pause);
            } else {
                mPlay.setImageResource(R.drawable.play);
            }
        });
        Title = root.findViewById(R.id.mini_artist);
        mProgress = root.findViewById(R.id.SeekBar01);
        mProgress.setOnSeekBarChangeListener(this);
        appendDisposable(observeServiceBinding()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(ignore -> onServiceBindEvent()));
        return root;
    }

    private void updatePlaybackControls() {
        if (!isAdded()) {
            return;
        }
        if (nonNull(mPlay)) {
            if (MusicUtils.isPlaying()) {
                mPlay.setImageResource(R.drawable.pause);
            } else {
                mPlay.setImageResource(R.drawable.play);
            }
        }
    }

    private void onServiceBindEvent() {
        updatePlaybackControls();
        updateNowPlayingInfo();
    }

    public void ShowHide(boolean Show, boolean Super)
    {
        if(HideBySuper && Show && !Super)
            return;
        if(!Show && Super)
            HideBySuper = true;
        if(Show && MusicUtils.getCurrentAudio() == null)
            lnt.setVisibility(View.INVISIBLE);
        else if(Show) {
            lnt.setVisibility(View.VISIBLE);
            HideBySuper = false;
            if(CurrentTrack == MusicUtils.getCurrentAudio() && CurrentTrack != null) {
                lnt.setVisibility(View.INVISIBLE);
                return;
            }
            else
                CurrentTrack = null;
        }
        else
            lnt.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void updateNowPlayingInfo()
    {
        ShowHide(true, false);
        String artist = MusicUtils.getArtistName();
        String trackName = MusicUtils.getTrackName();
        Title.setText(firstNonEmptyString(artist, " ") + " - " + firstNonEmptyString(trackName, " "));

    }

    private void resolveControlViews() {
        if (!isAdded() || mProgress == null) return;

        boolean preparing = MusicUtils.isPreparing();
        boolean initialized = MusicUtils.isInitialized();
        mProgress.setEnabled(!preparing && initialized);
        //mProgress.setIndeterminate(preparing);
    }

    private long refreshCurrentTime() {
        if (mService == null) {
            return 500;
        }

        try {
            final long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            final long duration = MusicUtils.duration();

            if (pos >= 0 && duration > 0) {
                final int progress = (int) (1000 * pos / MusicUtils.duration());

                mProgress.setProgress(progress);

                int bufferProgress = (int) ((float) MusicUtils.bufferPercent() * 10F);
                mProgress.setSecondaryProgress(bufferProgress);

                if (mFromTouch) {
                    return 500;
                } else if (!MusicUtils.isPlaying()) {
                    return 500;
                }
            } else {
                mProgress.setProgress(0);
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

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mFromTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
            mPosOverride = -1;
        }

        mFromTouch = false;
    }

    private static final class TimeHandler extends Handler {

        private final WeakReference<MiniPlayerFragment> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        TimeHandler(final MiniPlayerFragment player) {
            mAudioPlayer = new WeakReference<>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = mAudioPlayer.get().refreshCurrentTime();
                    mAudioPlayer.get().queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);

        // Unregister the receiver
        try {
            requireActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable ignored) {
            //$FALL-THROUGH$
        }
    }

    private static final class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<MiniPlayerFragment> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final MiniPlayerFragment activity) {
            mReference = new WeakReference<>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            MiniPlayerFragment fragment = mReference.get();
            if (isNull(fragment) || isNull(action)) return;

            switch (action) {
                case MusicPlaybackService.META_CHANGED:
                case MusicPlaybackService.PREPARED:
                    // Current info
                    fragment.updateNowPlayingInfo();
                    fragment.resolveControlViews();
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    fragment.updatePlaybackControls();
                    fragment.resolveControlViews();
                    break;

            }
        }
    }
}
