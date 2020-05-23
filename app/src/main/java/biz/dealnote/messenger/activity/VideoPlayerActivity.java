package biz.dealnote.messenger.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.media.exo.CustomHttpDataSourceFactory;
import biz.dealnote.messenger.model.InternalVideoSize;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.push.OwnerInfo;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_SIZE = "size";

    private View mDecorView;

    private Player mPlayer;

    private Video video;
    private int size;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private static MediaSource createMediaSource(String url, ProxyConfig proxyConfig, boolean isHLS) {
        Proxy proxy = null;
        if (nonNull(proxyConfig)) {
            proxy = new Proxy(Proxy.Type.HTTP, ProxyUtil.obtainAddress(proxyConfig));

            if (proxyConfig.isAuthEnabled()) {
                Authenticator authenticator = new Authenticator() {
                    @Override
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
        if (!isHLS)
            return new ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url));
        else
            return new HlsMediaSource.Factory(factory).createMediaSource(Uri.parse(url));
    }

    private void onOpen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_OPEN_WALL);
        intent.putExtra(Extra.OWNER_ID, video.getOwnerId());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Settings.get().ui().getMainTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        if (Utils.hasLollipop()) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        video = getIntent().getParcelableExtra(EXTRA_VIDEO);
        size = getIntent().getIntExtra(EXTRA_SIZE, InternalVideoSize.SIZE_240);

        mDecorView = getWindow().getDecorView();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (toolbar != null) {
            mCompositeDisposable.add(OwnerInfo.getRx(this, Settings.get().accounts().getCurrent(), video.getOwnerId())
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(userInfo -> {
                        ImageView av = findViewById(R.id.toolbar_avatar);
                        av.setImageBitmap(userInfo.getAvatar());
                        av.setOnClickListener(v -> onOpen());
                        if (Objects.isNullOrEmptyString(video.getDescription()))
                            toolbar.setSubtitle(userInfo.getOwner().getFullName());
                    }, throwable -> {
                    }));
            toolbar.setNavigationIcon(R.drawable.arrow_left);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(video.getTitle());
            actionBar.setSubtitle(video.getDescription());
        }

        PlayerView mSurfaceView = findViewById(R.id.videoSurface);

        resolveControlsVisibility(false);

        mPlayer = createPlayer(size == InternalVideoSize.SIZE_HLS || size == InternalVideoSize.SIZE_LIVE);
        mPlayer.setPlayWhenReady(true);

        mSurfaceView.setPlayer(mPlayer);
        mSurfaceView.setControllerVisibilityListener(visibility -> resolveControlsVisibility(visibility == View.VISIBLE));
    }

    private Player createPlayer(boolean isHLS) {
        IProxySettings settings = Injection.provideProxySettings();
        ProxyConfig config = settings.getActiveProxy();

        final String url = getFileUrl();
        SimpleExoPlayer ret = new SimpleExoPlayer.Builder(this).build();
        ret.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MOVIE).setUsage(C.USAGE_MEDIA).build(), true);
        ret.prepare(createMediaSource(url, config, isHLS));
        ret.setPlayWhenReady(true);
        return ret;
    }

    private void resolveControlsVisibility(boolean show) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        if (actionBar.isShowing() && !show) {
            //toolbar_with_elevation.animate().translationY(-toolbar_with_elevation.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
            actionBar.hide();
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else if (!actionBar.isShowing() && show) {
            //toolbar_with_elevation.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
            actionBar.show();
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        mPlayer.release();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mPlayer.setPlayWhenReady(false);
        mPlayer.getPlaybackState();
        super.onPause();
    }

    private String getFileUrl() {
        switch (size) {
            case InternalVideoSize.SIZE_240:
                return video.getMp4link240();
            case InternalVideoSize.SIZE_360:
                return video.getMp4link360();
            case InternalVideoSize.SIZE_480:
                return video.getMp4link480();
            case InternalVideoSize.SIZE_720:
                return video.getMp4link720();
            case InternalVideoSize.SIZE_1080:
                return video.getMp4link1080();
            case InternalVideoSize.SIZE_HLS:
                return video.getHls();
            case InternalVideoSize.SIZE_LIVE:
                return video.getLive();
            default:
                throw new IllegalArgumentException("Unknown video size");
        }
    }
}