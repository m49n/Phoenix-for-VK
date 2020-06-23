package biz.dealnote.messenger.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.media.video.ExoVideoPlayer;
import biz.dealnote.messenger.media.video.IVideoPlayer;
import biz.dealnote.messenger.model.InternalVideoSize;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VideoSize;
import biz.dealnote.messenger.push.OwnerInfo;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.view.AlternativeAspectRatioFrameLayout;
import biz.dealnote.messenger.view.VideoControllerView;
import io.reactivex.disposables.CompositeDisposable;

public class VideoPlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        VideoControllerView.MediaPlayerControl, IVideoPlayer.IVideoSizeChangeListener {

    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_SIZE = "size";

    private View mDecorView;
    private VideoControllerView mControllerView;
    private AlternativeAspectRatioFrameLayout Frame;

    private IVideoPlayer mPlayer;

    private Video video;
    private @InternalVideoSize
    int size;

    private boolean isLandscape = false;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

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
                        if (Utils.isEmpty(video.getDescription()))
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

        mControllerView = new VideoControllerView(this);

        SurfaceView mSurfaceView = findViewById(R.id.videoSurface);
        Frame = findViewById(R.id.aspect_ratio_layout);
        mSurfaceView.setOnClickListener(v -> resolveControlsVisibility());

        SurfaceHolder videoHolder = mSurfaceView.getHolder();
        videoHolder.addCallback(this);

        resolveControlsVisibility();

        try {
            mPlayer = createPlayer();
            mPlayer.addVideoSizeChangeListener(this);
            mPlayer.play();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mControllerView.setMediaPlayer(this);
        mControllerView.setAnchorView(findViewById(R.id.videoSurfaceContainer));
    }

    private IVideoPlayer createPlayer() throws IOException {
        IProxySettings settings = Injection.provideProxySettings();
        ProxyConfig config = settings.getActiveProxy();

        final String url = getFileUrl();
        return new ExoVideoPlayer(this, url, config, size);
    }

    private void resolveControlsVisibility() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        if (actionBar.isShowing()) {
            //toolbar_with_elevation.animate().translationY(-toolbar_with_elevation.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
            actionBar.hide();
            mControllerView.hide();
        } else {
            //toolbar_with_elevation.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
            actionBar.show();
            mControllerView.show();
        }
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            mDecorView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES));
    }

    @Override
    protected void onDestroy() {
        mPlayer.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            mDecorView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES));
    }

    @Override
    protected void onPause() {
        mPlayer.pause();
        mControllerView.updatePausePlay();
        super.onPause();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPlayer.setSurfaceHolder(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        return mPlayer.getBufferPercentage();
    }

    @Override
    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public void seekTo(int i) {
        mPlayer.seekTo(i);
    }

    @Override
    public void start() {
        mPlayer.play();
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false;
        }
    }

    @Override
    public void onVideoSizeChanged(@NonNull IVideoPlayer player, VideoSize size) {
        Frame.setAspectRatio(size.getWidth(), size.getHeight());
    }
}
