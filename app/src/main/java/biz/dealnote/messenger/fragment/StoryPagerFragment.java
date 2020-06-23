package biz.dealnote.messenger.fragment;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ortiz.touchview.TouchImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Transformation;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.BackPressCallback;
import biz.dealnote.messenger.media.gif.IGifPlayer;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.mvp.presenter.StoryPagerPresenter;
import biz.dealnote.messenger.mvp.view.IStoryPagerView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.AlternativeAspectRatioFrameLayout;
import biz.dealnote.messenger.view.CircleCounterButton;
import biz.dealnote.messenger.view.FlingRelativeLayout;
import biz.dealnote.messenger.view.pager.CloseOnFlingListener;
import biz.dealnote.messenger.view.pager.GoBackCallback;
import biz.dealnote.messenger.view.pager.WeakGoBackAnimationAdapter;
import biz.dealnote.messenger.view.pager.WeakPicassoLoadCallback;
import biz.dealnote.messenger.view.verticalswipe.BelowFractionalClamp;
import biz.dealnote.messenger.view.verticalswipe.NegativeFactorFilterSideEffect;
import biz.dealnote.messenger.view.verticalswipe.PropertySideEffect;
import biz.dealnote.messenger.view.verticalswipe.SensitivityClamp;
import biz.dealnote.messenger.view.verticalswipe.SettleOnTopAction;
import biz.dealnote.messenger.view.verticalswipe.VerticalSwipeBehavior;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Utils.nonEmpty;


public class StoryPagerFragment extends BaseMvpFragment<StoryPagerPresenter, IStoryPagerView>
        implements IStoryPagerView, GoBackCallback, BackPressCallback {

    private static final int REQUEST_WRITE_PERMISSION = 160;
    private ViewPager2 mViewPager;
    private Toolbar mToolbar;
    private ImageView Avatar;
    private TextView mExp;
    private Transformation transformation;
    private CircleCounterButton mDownload;
    private boolean mFullscreen;
    private SparseArray<WeakReference<MultiHolder>> mHolderSparseArray = new SparseArray<>();
    private WeakGoBackAnimationAdapter mGoBackAnimationAdapter = new WeakGoBackAnimationAdapter(this);

    public static StoryPagerFragment newInstance(Bundle args) {
        StoryPagerFragment fragment = new StoryPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle buildArgs(int aid, @NonNull ArrayList<Story> stories, int index) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, aid);
        args.putInt(Extra.INDEX, index);
        args.putParcelableArrayList(Extra.STORY, stories);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFullscreen = savedInstanceState.getBoolean("mFullscreen");
        }
        this.transformation = CurrentTheme.createTransformationForAvatar(requireActivity());
    }

    @Override
    public void requestWriteExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_story_pager, container, false);

        mToolbar = root.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(mToolbar);

        Avatar = root.findViewById(R.id.toolbar_avatar);
        mViewPager = root.findViewById(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(1);
        mExp = root.findViewById(R.id.item_story_expires);

        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                getPresenter().firePageSelected(position);
            }
        });

        mDownload = root.findViewById(R.id.button_download);
        mDownload.setOnClickListener(v -> getPresenter().fireDownloadButtonClick());

        resolveFullscreenViews();
        return root;
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mFullscreen", mFullscreen);
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

    private void toggleFullscreen() {
        mFullscreen = !mFullscreen;
        resolveFullscreenViews();
    }

    private void resolveFullscreenViews() {
        if (Objects.nonNull(mToolbar)) {
            mToolbar.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        }

        if (Objects.nonNull(mDownload)) {
            mDownload.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        }
    }

    @NotNull
    @Override
    public IPresenterFactory<StoryPagerPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            int aid = requireArguments().getInt(Extra.ACCOUNT_ID);
            int index = requireArguments().getInt(Extra.INDEX);

            ArrayList<Story> stories = requireArguments().getParcelableArrayList(Extra.STORY);
            AssertUtils.requireNonNull(stories);

            return new StoryPagerPresenter(aid, stories, index, saveInstanceState);
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION && isPresenterPrepared()) {
            getPresenter().fireWritePermissionResolved();
        }
    }

    @Override
    public void displayData(int pageCount, int selectedIndex) {
        if (Objects.nonNull(mViewPager)) {
            Adapter adapter = new Adapter(pageCount);
            mViewPager.setAdapter(adapter);
            mViewPager.setCurrentItem(selectedIndex, false);
        }
    }

    @Override
    public void setAspectRatioAt(int position, int w, int h) {
        MultiHolder holder = findByPosition(position);
        if (Objects.nonNull(holder)) {
            holder.SetAspectRatio(w, h);
        }
    }

    @Override
    public void setPreparingProgressVisible(int position, boolean preparing) {
        for (int i = 0; i < mHolderSparseArray.size(); i++) {
            int key = mHolderSparseArray.keyAt(i);
            MultiHolder holder = findByPosition(key);

            if (Objects.nonNull(holder)) {
                boolean isCurrent = position == key;
                boolean progressVisible = isCurrent && preparing;

                holder.setProgressVisible(progressVisible);
                holder.setSurfaceVisible(isCurrent && !preparing ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void attachDisplayToPlayer(int adapterPosition, IGifPlayer gifPlayer) {
        MultiHolder holder = findByPosition(adapterPosition);
        if (Objects.nonNull(holder) && Objects.nonNull(gifPlayer) && holder.isSurfaceReady()) {
            gifPlayer.setDisplay(holder.mSurfaceHolder);
        }
    }

    @Override
    public void setToolbarTitle(@StringRes int titleRes, Object... params) {
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (Objects.nonNull(actionBar)) {
            actionBar.setTitle(getString(titleRes, params));
        }
    }

    @Override
    public void setToolbarSubtitle(Story story) {
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (Objects.nonNull(actionBar)) {
            actionBar.setSubtitle(story.getOwner().getFullName());
        }
        Avatar.setOnClickListener(v -> PlaceFactory.getOwnerWallPlace(getPresenter().getAccountId(), story.getOwner()).tryOpenWith(requireActivity()));
        ViewUtils.displayAvatar(Avatar, transformation, story.getOwner().getMaxSquareAvatar(), Constants.PICASSO_TAG);

        if (Objects.nonNull(mExp)) {
            if (story.getExpires() <= 0)
                mExp.setVisibility(View.GONE);
            else {
                mExp.setVisibility(View.VISIBLE);
                Long exp = (story.getExpires() - Calendar.getInstance().getTime().getTime() / 1000) / 3600;
                mExp.setText(getString(R.string.expires, String.valueOf(exp), getString(Utils.declOfNum(exp, new int[]{R.string.hour, R.string.hour_sec, R.string.hours}))));
            }
        }
    }

    @Override
    public void configHolder(int adapterPosition, boolean progress, int aspectRatioW, int aspectRatioH) {
        MultiHolder holder = findByPosition(adapterPosition);
        if (Objects.nonNull(holder)) {
            holder.setProgressVisible(progress);
            holder.SetAspectRatio(aspectRatioW, aspectRatioH);
            holder.setSurfaceVisible(progress ? View.GONE : View.VISIBLE);
        }
    }

    private void fireHolderCreate(@NonNull MultiHolder holder) {
        getPresenter().fireHolderCreate(holder.getBindingAdapterPosition());
    }

    public MultiHolder findByPosition(int position) {
        WeakReference<MultiHolder> weak = mHolderSparseArray.get(position);
        return Objects.isNull(weak) ? null : weak.get();
    }

    private static class MultiHolder extends RecyclerView.ViewHolder {
        SurfaceHolder mSurfaceHolder;

        MultiHolder(View rootView) {
            super(rootView);
        }

        boolean isSurfaceReady() {
            return false;
        }

        void setProgressVisible(boolean visible) {

        }

        void SetAspectRatio(int w, int h) {

        }

        void setSurfaceVisible(int Vis) {
        }

        public void bindTo(@NonNull Story story) {

        }
    }

    private final class Holder extends MultiHolder implements SurfaceHolder.Callback {

        SurfaceView mSurfaceView;
        ProgressBar mProgressBar;
        AlternativeAspectRatioFrameLayout mAspectRatioLayout;
        boolean mSurfaceReady;

        Holder(View rootView) {
            super(rootView);
            FlingRelativeLayout flingRelativeLayout = rootView.findViewById(R.id.fling_root_view);
            flingRelativeLayout.setOnClickListener(v -> toggleFullscreen());
            flingRelativeLayout.setOnLongClickListener(v -> {
                if (isPresenterPrepared()) getPresenter().fireDownloadButtonClick();
                return true;
            });
            flingRelativeLayout.setOnSingleFlingListener(new CloseOnFlingListener(rootView.getContext()) {
                @Override
                public boolean onVerticalFling(float distanceByY) {
                    goBack();
                    return true;
                }
            });

            mSurfaceView = rootView.findViewById(R.id.surface_view);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(this);

            mAspectRatioLayout = rootView.findViewById(R.id.aspect_ratio_layout);
            mProgressBar = rootView.findViewById(R.id.preparing_progress_bar);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceReady = true;
            if (isPresenterPrepared()) {
                getPresenter().fireSurfaceCreated(getBindingAdapterPosition());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceReady = false;
        }

        @Override
        boolean isSurfaceReady() {
            return mSurfaceReady;
        }

        @Override
        void setProgressVisible(boolean visible) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        @Override
        void SetAspectRatio(int w, int h) {
            mAspectRatioLayout.setAspectRatio(w, h);
        }

        @Override
        void setSurfaceVisible(int Vis) {
            mSurfaceView.setVisibility(Vis);
        }
    }

    private class PhotoViewHolder extends MultiHolder implements Callback {
        public TouchImageView photo;
        public ProgressBar progress;
        public FloatingActionButton reload;
        private boolean mLoadingNow;
        private WeakPicassoLoadCallback mPicassoLoadCallback;

        public PhotoViewHolder(View view) {
            super(view);
            photo = view.findViewById(R.id.image_view);
            progress = view.findViewById(R.id.progress_bar);
            photo = view.findViewById(idOfImageView());
            photo.setMaxZoom(8);
            photo.setDoubleTapScale(3);
            progress = view.findViewById(idOfProgressBar());
            reload = view.findViewById(R.id.goto_button);
            mPicassoLoadCallback = new WeakPicassoLoadCallback(this);

            photo.setOnClickListener(v -> toggleFullscreen());
        }

        @Override
        public void bindTo(@NonNull Story story) {
            if (story.isIs_expired()) {
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.is_expired);
                mLoadingNow = false;
                resolveProgressVisibility();
                return;
            }
            if (story.getPhoto() == null)
                return;
            String url = story.getPhoto().getUrlForSize(PhotoSize.W, true);

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

    private class Adapter extends RecyclerView.Adapter<MultiHolder> {

        int mPageCount;

        Adapter(int count) {
            super();
            mPageCount = count;
            mHolderSparseArray.clear();
        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public MultiHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
            if (viewType == 0)
                return new Holder(LayoutInflater.from(container.getContext()).inflate(R.layout.content_gif_page, container, false));
            PhotoViewHolder ret = new PhotoViewHolder(LayoutInflater.from(container.getContext())
                    .inflate(R.layout.content_photo_page, container, false));


            VerticalSwipeBehavior<TouchImageView> ui = VerticalSwipeBehavior.Companion.from(ret.photo);
            ui.setSettle(new SettleOnTopAction());
            PropertySideEffect sideDelegate = new PropertySideEffect(View.ALPHA, View.SCALE_X, View.SCALE_Y);
            ui.setSideEffect(new NegativeFactorFilterSideEffect(sideDelegate));
            BelowFractionalClamp clampDelegate = new BelowFractionalClamp(3f, 3f);
            ui.setClamp(new SensitivityClamp(0.5f, clampDelegate, 0.5f));
            ui.setListener(new VerticalSwipeBehavior.SwipeListener() {
                @Override
                public void onReleased() {
                    container.requestDisallowInterceptTouchEvent(false);
                }

                @Override
                public void onCaptured() {
                    container.requestDisallowInterceptTouchEvent(true);
                }

                @Override
                public void onPreSettled(int diff) {
                }

                @Override
                public void onPostSettled(int diff) {
                    if (Settings.get().ui().isPhoto_swipe_pos_top_to_bottom() && diff >= 120 || !Settings.get().ui().isPhoto_swipe_pos_top_to_bottom() && diff <= -120) {
                        goBack();
                    } else
                        container.requestDisallowInterceptTouchEvent(false);
                }
            });

            ret.photo.setOnLongClickListener(v -> {
                if (isPresenterPrepared()) getPresenter().fireDownloadButtonClick();
                return true;
            });
            ret.photo.setOnTouchListener((view, event) -> {
                if (event.getPointerCount() >= 2 || view.canScrollHorizontally(1) && view.canScrollHorizontally(-1)) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            ui.setCanSwipe(false);
                            container.requestDisallowInterceptTouchEvent(true);
                            return false;
                        case MotionEvent.ACTION_UP:
                            ui.setCanSwipe(true);
                            container.requestDisallowInterceptTouchEvent(false);
                            return true;
                    }
                }
                return true;
            });
            return ret;
        }

        @Override
        public void onBindViewHolder(@NonNull MultiHolder holder, int position) {
            if (!getPresenter().isStoryIsVideo(position))
                holder.bindTo(getPresenter().getStory(position));
        }

        @Override
        public int getItemViewType(int position) {
            return getPresenter().isStoryIsVideo(position) ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return mPageCount;
        }

        @Override
        public void onViewDetachedFromWindow(@NotNull MultiHolder holder) {
            super.onViewDetachedFromWindow(holder);
            mHolderSparseArray.remove(holder.getBindingAdapterPosition());
        }

        @Override
        public void onViewAttachedToWindow(@NotNull MultiHolder holder) {
            super.onViewAttachedToWindow(holder);
            mHolderSparseArray.put(holder.getBindingAdapterPosition(), new WeakReference<>(holder));
            fireHolderCreate(holder);
        }
    }
}
