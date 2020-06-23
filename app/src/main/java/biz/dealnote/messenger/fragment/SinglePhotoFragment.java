package biz.dealnote.messenger.fragment;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ortiz.touchview.TouchImageView;
import com.squareup.picasso.Callback;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import biz.dealnote.messenger.App;
import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.listener.BackPressCallback;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.task.DownloadImageTask;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.view.CircleCounterButton;
import biz.dealnote.messenger.view.pager.GoBackCallback;
import biz.dealnote.messenger.view.pager.WeakGoBackAnimationAdapter;
import biz.dealnote.messenger.view.pager.WeakPicassoLoadCallback;
import biz.dealnote.messenger.view.verticalswipe.BelowFractionalClamp;
import biz.dealnote.messenger.view.verticalswipe.NegativeFactorFilterSideEffect;
import biz.dealnote.messenger.view.verticalswipe.PropertySideEffect;
import biz.dealnote.messenger.view.verticalswipe.SensitivityClamp;
import biz.dealnote.messenger.view.verticalswipe.SettleOnTopAction;
import biz.dealnote.messenger.view.verticalswipe.VerticalSwipeBehavior;

import static biz.dealnote.messenger.util.Utils.nonEmpty;


public class SinglePhotoFragment extends BaseFragment
        implements GoBackCallback, BackPressCallback {

    private static final int REQUEST_WRITE_PERMISSION = 160;
    private String url;
    private String prefix;
    private String photo_prefix;

    private WeakGoBackAnimationAdapter mGoBackAnimationAdapter = new WeakGoBackAnimationAdapter(this);

    public static SinglePhotoFragment newInstance(Bundle args) {
        SinglePhotoFragment fragment = new SinglePhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle buildArgs(String url, String download_prefix, String photo_prefix) {
        Bundle args = new Bundle();
        args.putString(Extra.URL, url);
        args.putString(Extra.STATUS, download_prefix);
        args.putString(Extra.KEY, photo_prefix);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.url = requireArguments().getString(Extra.URL);
        this.prefix = requireArguments().getString(Extra.STATUS);
        this.photo_prefix = requireArguments().getString(Extra.KEY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_single_url_photo, container, false);

        CircleCounterButton mDownload = root.findViewById(R.id.button_download);

        PhotoViewHolder ret = new PhotoViewHolder(root);
        ret.bindTo(url);

        VerticalSwipeBehavior<TouchImageView> ui = VerticalSwipeBehavior.Companion.from(ret.photo);
        ui.setSettle(new SettleOnTopAction());
        PropertySideEffect sideDelegate = new PropertySideEffect(View.ALPHA, View.SCALE_X, View.SCALE_Y);
        ui.setSideEffect(new NegativeFactorFilterSideEffect(sideDelegate));
        BelowFractionalClamp clampDelegate = new BelowFractionalClamp(3f, 3f);
        ui.setClamp(new SensitivityClamp(0.5f, clampDelegate, 0.5f));
        ui.setListener(new VerticalSwipeBehavior.SwipeListener() {
            @Override
            public void onReleased() {
            }

            @Override
            public void onCaptured() {
            }

            @Override
            public void onPreSettled(int diff) {
            }

            @Override
            public void onPostSettled(int diff) {
                if (Settings.get().ui().isPhoto_swipe_pos_top_to_bottom() && diff >= 120 || !Settings.get().ui().isPhoto_swipe_pos_top_to_bottom() && diff <= -120) {
                    goBack();
                }
            }
        });

        ret.photo.setOnLongClickListener(v -> {
            doSaveOnDrive(true);
            return true;
        });
        mDownload.setOnClickListener(v -> doSaveOnDrive(true));

        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            doSaveOnDrive(false);
        }
    }

    private void doSaveOnDrive(boolean Request) {
        if (Request) {
            if (!AppPerms.hasWriteStoragePermision(App.getInstance())) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        }
        File dir = new File(Settings.get().other().getPhotoDir());
        if (!dir.isDirectory()) {
            boolean created = dir.mkdirs();
            if (!created) {
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError("Can't create directory " + dir);
                return;
            }
        } else
            dir.setLastModified(Calendar.getInstance().getTime().getTime());

        if (prefix != null && Settings.get().other().isPhoto_to_user_dir()) {
            File dir_final = new File(dir.getAbsolutePath() + "/" + prefix);
            if (!dir_final.isDirectory()) {
                boolean created = dir_final.mkdirs();
                if (!created) {
                    PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError("Can't create directory " + dir);
                    return;
                }
            } else
                dir_final.setLastModified(Calendar.getInstance().getTime().getTime());
            dir = dir_final;
        }
        DateFormat DOWNLOAD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String file = dir.getAbsolutePath() + "/" + prefix + "." + photo_prefix + ".profile." + DOWNLOAD_DATE_FORMAT.format(new Date()) + ".jpg";
        new InternalDownloader(requireActivity(), url, file, prefix).doDownload();
    }

    @Override
    public void goBack() {
        if (isAdded() && canGoBack()) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private boolean canGoBack() {
        return requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 1;
    }

    @Override
    public boolean onBackPressed() {
        ObjectAnimator objectAnimatorPosition;
        if (Settings.get().ui().isPhoto_swipe_pos_top_to_bottom()) {
            objectAnimatorPosition = ObjectAnimator.ofFloat(getView(), "translationY", 600);
        } else {
            objectAnimatorPosition = ObjectAnimator.ofFloat(getView(), "translationY", -600);
        }
        ObjectAnimator objectAnimatorAlpha = ObjectAnimator.ofFloat(getView(), View.ALPHA, 1, 0);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorPosition, objectAnimatorAlpha);
        animatorSet.setDuration(200);
        animatorSet.addListener(mGoBackAnimationAdapter);
        animatorSet.start();
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(true)
                .setBarsColored(false, false)
                .build()
                .apply(requireActivity());
    }

    private class PhotoViewHolder implements Callback {
        public TouchImageView photo;
        public ProgressBar progress;
        public FloatingActionButton reload;
        private boolean mLoadingNow;
        private WeakPicassoLoadCallback mPicassoLoadCallback;

        public PhotoViewHolder(View view) {
            photo = view.findViewById(R.id.image_view);
            progress = view.findViewById(R.id.progress_bar);
            photo = view.findViewById(idOfImageView());
            photo.setMaxZoom(8);
            photo.setDoubleTapScale(3);
            progress = view.findViewById(idOfProgressBar());
            reload = view.findViewById(R.id.goto_button);
            mPicassoLoadCallback = new WeakPicassoLoadCallback(this);

        }

        public void bindTo(@NonNull String url) {
            reload.setOnClickListener(v -> {
                reload.setVisibility(View.INVISIBLE);
                if (nonEmpty(url)) {
                    loadImage(url);
                } else
                    PicassoInstance.with().cancelRequest(photo);
            });

            if (nonEmpty(url)) {
                loadImage(url);
            } else {
                PicassoInstance.with().cancelRequest(photo);
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.empty_url);
            }

        }

        private void resolveProgressVisibility() {
            progress.setVisibility(mLoadingNow ? View.VISIBLE : View.GONE);
        }

        protected void loadImage(@NonNull String url) {
            mLoadingNow = true;

            resolveProgressVisibility();

            PicassoInstance.with()
                    .load(url)
                    .tag(Constants.PICASSO_TAG)
                    .into(photo, mPicassoLoadCallback);
        }

        @IdRes
        protected int idOfImageView() {
            return R.id.image_view;
        }

        @IdRes
        protected int idOfProgressBar() {
            return R.id.progress_bar;
        }

        @Override
        public void onSuccess() {
            mLoadingNow = false;
            resolveProgressVisibility();
            reload.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onError(Exception e) {
            mLoadingNow = false;
            resolveProgressVisibility();
            reload.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private final class InternalDownloader extends DownloadImageTask {

        InternalDownloader(Context context, String url, String file, String photo) {
            super(context, url, file, photo, true);
        }

        @Override
        protected void onPostExecute(String s) {
            if (Objects.isNull(s)) {
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastBottom(R.string.saved);
            } else {
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.error_with_message, s);
            }
        }
    }
}
