package biz.dealnote.messenger.media.video;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.video.VideoListener;

import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.media.exo.CustomHttpDataSourceFactory;
import biz.dealnote.messenger.media.exo.ExoUtil;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.model.VideoSize;

import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Created by Ruslan Kolbasa on 14.08.2017.
 * phoenix
 */
public class ExoVideoPlayer implements IVideoPlayer {

    private final SimpleExoPlayer player;

    private final MediaSource source;

    private final OnVideoSizeChangedListener onVideoSizeChangedListener = new OnVideoSizeChangedListener(this);

    public ExoVideoPlayer(Context context, String url, ProxyConfig config) {
        this.player = createPlayer(context);
        this.player.addVideoListener(onVideoSizeChangedListener);
        this.source = createMediaSource(url, config);
    }

    private static MediaSource createMediaSource(String url, ProxyConfig proxyConfig) {
        Proxy proxy = null;
        if (nonNull(proxyConfig)) {
            proxy = new Proxy(Proxy.Type.HTTP, ProxyUtil.obtainAddress(proxyConfig));

            if (proxyConfig.isAuthEnabled()) {
                Authenticator authenticator = new Authenticator() {
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyConfig.getUser(), proxyConfig.getPass().toCharArray());
                    }
                };

                Authenticator.setDefault(authenticator);
            } else {
                Authenticator.setDefault(null);
            }
        }

        String userAgent = Constants.USER_AGENT(null);
        CustomHttpDataSourceFactory factory = new CustomHttpDataSourceFactory(userAgent, proxy);

        // This is the MediaSource representing the media to be played:
        // FOR SD CARD SOURCE:
        // MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
        // FOR LIVESTREAM LINK:
        return new ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url));
    }

    private static SimpleExoPlayer createPlayer(Context context) {
        return new SimpleExoPlayer.Builder(context.getApplicationContext()).build();
    }

    private boolean supposedToBePlaying;

    private boolean prepareCalled;

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
        if(!supposedToBePlaying){
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

    private static final class OnVideoSizeChangedListener implements VideoListener {

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

    private void onVideoSizeChanged(int w, int h) {
        for(IVideoSizeChangeListener listener : videoSizeChangeListeners){
            listener.onVideoSizeChanged(this, new VideoSize(w, h));
        }
    }

    private final List<IVideoSizeChangeListener> videoSizeChangeListeners = new ArrayList<>(1);

    @Override
    public void addVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        this.videoSizeChangeListeners.add(listener);
    }

    @Override
    public void removeVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        this.videoSizeChangeListeners.remove(listener);
    }
}