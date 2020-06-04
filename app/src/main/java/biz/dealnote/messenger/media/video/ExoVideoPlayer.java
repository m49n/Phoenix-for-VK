package biz.dealnote.messenger.media.video;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.media.exo.ExoUtil;
import biz.dealnote.messenger.model.InternalVideoSize;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.model.VideoSize;
import biz.dealnote.messenger.util.Utils;

public class ExoVideoPlayer implements IVideoPlayer {

    private final SimpleExoPlayer player;

    private final MediaSource source;

    private final OnVideoSizeChangedListener onVideoSizeChangedListener = new OnVideoSizeChangedListener(this);
    private final List<IVideoSizeChangeListener> videoSizeChangeListeners = new ArrayList<>(1);
    private boolean supposedToBePlaying;
    private boolean prepareCalled;

    public ExoVideoPlayer(Context context, String url, ProxyConfig config, @InternalVideoSize int size) {
        this.player = createPlayer(context);
        this.player.addVideoListener(onVideoSizeChangedListener);
        this.source = createMediaSource(url, config, size == InternalVideoSize.SIZE_HLS || size == InternalVideoSize.SIZE_LIVE);
    }

    private static MediaSource createMediaSource(String url, ProxyConfig proxyConfig, boolean isHLS) {
        String userAgent = Constants.USER_AGENT(null);
        if (!isHLS)
            return new ProgressiveMediaSource.Factory(Utils.getExoPlayerFactory(userAgent, proxyConfig)).createMediaSource(Uri.parse(url));
        else
            return new HlsMediaSource.Factory(Utils.getExoPlayerFactory(userAgent, proxyConfig)).createMediaSource(Uri.parse(url));
    }

    private SimpleExoPlayer createPlayer(Context context) {
        SimpleExoPlayer ret = new SimpleExoPlayer.Builder(context).build();
        ret.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MOVIE).setUsage(C.USAGE_MEDIA).build(), true);
        return ret;
    }

    @Override
    public void play() {
        if (supposedToBePlaying) {
            return;
        }

        supposedToBePlaying = true;

        if (!prepareCalled) {
            player.prepare(source);
            prepareCalled = true;
        }

        ExoUtil.startPlayer(player);
    }

    @Override
    public void pause() {
        if (!supposedToBePlaying) {
            return;
        }

        supposedToBePlaying = false;
        ExoUtil.pausePlayer(player);
    }

    @Override
    public void release() {
        player.removeVideoListener(onVideoSizeChangedListener);
        player.release();
    }

    @Override
    public int getDuration() {
        return (int) player.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) player.getCurrentPosition();
    }

    @Override
    public void seekTo(int position) {
        player.seekTo(position);
    }

    @Override
    public boolean isPlaying() {
        return supposedToBePlaying;
    }

    @Override
    public int getBufferPercentage() {
        return player.getBufferedPercentage();
    }

    @Override
    public void setSurfaceHolder(SurfaceHolder holder) {
        player.setVideoSurfaceHolder(holder);
    }

    private void onVideoSizeChanged(int w, int h) {
        for (IVideoSizeChangeListener listener : videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, new VideoSize(w, h));
        }
    }

    @Override
    public void addVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        this.videoSizeChangeListeners.add(listener);
    }

    @Override
    public void removeVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        this.videoSizeChangeListeners.remove(listener);
    }

    private static final class OnVideoSizeChangedListener implements SimpleExoPlayer.VideoListener {

        final WeakReference<ExoVideoPlayer> ref;

        private OnVideoSizeChangedListener(ExoVideoPlayer player) {
            this.ref = new WeakReference<>(player);
        }

        @Override
        public void onVideoSizeChanged(int i, int i1, int i2, float v) {
            ExoVideoPlayer player = ref.get();
            if (player != null) {
                player.onVideoSizeChanged(i, i1);
            }
        }

        @Override
        public void onRenderedFirstFrame() {

        }
    }
}