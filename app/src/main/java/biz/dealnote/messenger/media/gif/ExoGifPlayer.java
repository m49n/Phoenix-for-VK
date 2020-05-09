package biz.dealnote.messenger.media.gif;

import android.net.Uri;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.video.VideoListener;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.App;
import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.media.exo.CustomHttpDataSourceFactory;
import biz.dealnote.messenger.media.exo.ExoEventAdapter;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.model.VideoSize;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.Logger;

import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Created by admin on 13.08.2017.
 * phoenix
 */
public class ExoGifPlayer implements IGifPlayer {

    private final String url;
    private final ProxyConfig proxyConfig;
    private final List<IVideoSizeChangeListener> videoSizeChangeListeners = new ArrayList<>(1);
    private final List<IStatusChangeListener> statusChangeListeners = new ArrayList<>(1);
    private int status;
    private VideoSize size;
    private final VideoListener videoListener = new VideoListener() {
        @Override
        public void onVideoSizeChanged(int i, int i1, int i2, float v) {
            size = new VideoSize(i, i1);
            ExoGifPlayer.this.onVideoSizeChanged();
        }

        @Override
        public void onRenderedFirstFrame() {

        }
    };
    private SimpleExoPlayer internalPlayer;
    private boolean supposedToBePlaying;

    public ExoGifPlayer(String url, ProxyConfig proxyConfig) {
        this.url = url;
        this.proxyConfig = proxyConfig;
        this.status = IStatus.INIT;
    }

    private static void pausePlayer(SimpleExoPlayer internalPlayer) {
        internalPlayer.setPlayWhenReady(false);
        internalPlayer.getPlaybackState();
    }

    private static void startPlayer(SimpleExoPlayer internalPlayer) {
        internalPlayer.setPlayWhenReady(true);
        internalPlayer.getPlaybackState();
    }

    @Override
    public VideoSize getVideoSize() {
        return size;
    }

    @Override
    public void play() {
        if (supposedToBePlaying) return;

        supposedToBePlaying = true;

        switch (status) {
            case IStatus.PREPARED:
                AssertUtils.requireNonNull(this.internalPlayer);
                startPlayer(this.internalPlayer);
                break;
            case IStatus.INIT:
                preparePlayer();
                break;
            case IStatus.PREPARING:
                //do nothing
                break;
        }
    }

    private void preparePlayer() {
        this.setStatus(IStatus.PREPARING);
        internalPlayer = new SimpleExoPlayer.Builder(App.getInstance()).build();


        // DefaultBandwidthMeter bandwidthMeterA = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        // DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoplayer2example"), bandwidthMeterA);
        // DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(App.getInstance(), Util.getUserAgent(App.getInstance(), "exoplayer2example"), bandwidthMeterA);

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
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url));
        internalPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        internalPlayer.addListener(new ExoEventAdapter() {
            @Override
            public void onPlayerStateChanged(boolean b, int i) {
                Logger.d("PhoenixExo", "onPlayerStateChanged, b: " + b + ", i: " + i);
                onInternalPlayerStateChanged(i);
            }
        });

        internalPlayer.addVideoListener(videoListener);
        internalPlayer.setPlayWhenReady(true);
        internalPlayer.prepare(mediaSource);
    }

    private void onInternalPlayerStateChanged(int state) {
        if (state == Player.STATE_READY) {
            setStatus(IStatus.PREPARED);
        }
    }

    private void onVideoSizeChanged() {
        for (IVideoSizeChangeListener listener : videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, this.size);
        }
    }

    @Override
    public void pause() {
        if (!supposedToBePlaying) return;

        supposedToBePlaying = false;

        if (nonNull(internalPlayer)) {
            try {
                pausePlayer(this.internalPlayer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        if (nonNull(internalPlayer)) {
            internalPlayer.setVideoSurfaceHolder(holder);
        }
    }

    @Override
    public void release() {
        if (nonNull(internalPlayer)) {
            try {
                internalPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setStatus(int newStatus) {
        final int oldStatus = this.status;

        if (this.status == newStatus) {
            return;
        }

        this.status = newStatus;
        for (IStatusChangeListener listener : statusChangeListeners) {
            listener.onPlayerStatusChange(this, oldStatus, newStatus);
        }
    }

    @Override
    public void addVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        this.videoSizeChangeListeners.add(listener);
    }

    @Override
    public void addStatusChangeListener(IStatusChangeListener listener) {
        this.statusChangeListeners.add(listener);
    }

    @Override
    public void removeVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        this.videoSizeChangeListeners.remove(listener);
    }

    @Override
    public void removeStatusChangeListener(IStatusChangeListener listener) {
        this.statusChangeListeners.remove(listener);
    }

    @Override
    public int getPlayerStatus() {
        return status;
    }
}