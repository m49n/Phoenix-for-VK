package biz.dealnote.messenger.fragment;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ortiz.touchview.TouchImageView;
import com.squareup.picasso.Callback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
import biz.dealnote.messenger.api.model.VKApiPhotoTags;
import biz.dealnote.messenger.domain.ILikesInteractor;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.BackPressCallback;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.model.TmpSource;
import biz.dealnote.messenger.mvp.presenter.photo.FavePhotoPagerPresenter;
import biz.dealnote.messenger.mvp.presenter.photo.PhotoAlbumPagerPresenter;
import biz.dealnote.messenger.mvp.presenter.photo.PhotoPagerPresenter;
import biz.dealnote.messenger.mvp.presenter.photo.SimplePhotoPresenter;
import biz.dealnote.messenger.mvp.presenter.photo.TmpGalleryPagerPresenter;
import biz.dealnote.messenger.mvp.view.IPhotoPagerView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.place.PlaceUtil;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
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
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class PhotoPagerFragment extends BaseMvpFragment<PhotoPagerPresenter, IPhotoPagerView>
        implements IPhotoPagerView, GoBackCallback, BackPressCallback {

    private static final String EXTRA_PHOTOS = "photos";
    private static final String EXTRA_NEED_UPDATE = "need_update";
    private static final int REQUEST_PERMISSION_WRITE_STORAGE = 9020;

    private static final SparseIntArray SIZES = new SparseIntArray();

    private static final int DEFAULT_PHOTO_SIZE = PhotoSize.W;

    static {
        SIZES.put(1, PhotoSize.X);
        SIZES.put(2, PhotoSize.Y);
        SIZES.put(3, PhotoSize.Z);
        SIZES.put(4, PhotoSize.W);
    }

    private ViewPager2 mViewPager;
    private CircleCounterButton mButtonLike;
    private CircleCounterButton mButtonComments;
    private CircleCounterButton buttonShare;
    private ProgressBar mLoadingProgressBar;
    private Toolbar mToolbar;
    private View mButtonsRoot;
    private Button mButtonRestore;
    private Adapter mPagerAdapter;
    private boolean mCanSaveYourself;
    private boolean mCanDelete;
    private WeakGoBackAnimationAdapter mGoBackAnimationAdapter = new WeakGoBackAnimationAdapter(this);

    public static Bundle buildArgsForSimpleGallery(int aid, int index, ArrayList<Photo> photos,
                                                   boolean needUpdate) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, aid);
        args.putParcelableArrayList(EXTRA_PHOTOS, photos);
        args.putInt(Extra.INDEX, index);
        args.putBoolean(EXTRA_NEED_UPDATE, needUpdate);
        return args;
    }

    public static Bundle buildArgsForSimpleGalleryHistory(int aid, int index, ArrayList<Photo> photos,
                                                          boolean needUpdate) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, aid);
        args.putParcelableArrayList(EXTRA_PHOTOS, photos);
        args.putInt(Extra.INDEX, index);
        args.putInt(Extra.STORY, 1);
        args.putBoolean(EXTRA_NEED_UPDATE, needUpdate);
        return args;
    }

    public static Bundle buildArgsForAlbum(int aid, int albumId, int ownerId, ArrayList<Photo> photos, int position) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, aid);
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putInt(Extra.ALBUM_ID, albumId);
        args.putInt(Extra.INDEX, position);
        args.putParcelableArrayList(EXTRA_PHOTOS, photos);

        return args;
    }

    public static Bundle buildArgsForFave(int aid, @NonNull ArrayList<Photo> photos, int index) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, aid);
        args.putParcelableArrayList(EXTRA_PHOTOS, photos);
        args.putInt(Extra.INDEX, index);
        return args;
    }

    public static PhotoPagerFragment newInstance(int placeType, Bundle args) {
        Bundle targetArgs = new Bundle();
        targetArgs.putAll(args);
        targetArgs.putInt(Extra.PLACE_TYPE, placeType);
        PhotoPagerFragment fragment = new PhotoPagerFragment();
        fragment.setArguments(targetArgs);
        return fragment;
    }

    private static void addPhotoSizeToMenu(PopupMenu menu, int id, int size, int selectedItem) {
        menu.getMenu()
                .add(0, id, 0, getTitleForPhotoSize(size))
                .setChecked(selectedItem == size);
    }

    private static String getTitleForPhotoSize(int size) {
        switch (size) {
            case PhotoSize.X:
                return 604 + "px";
            case PhotoSize.Y:
                return 807 + "px";
            case PhotoSize.Z:
                return 1024 + "px";
            case PhotoSize.W:
                return 2048 + "px";
            default:
                throw new IllegalArgumentException("Unsupported size");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_photo_pager_new, container, false);

        mLoadingProgressBar = root.findViewById(R.id.loading_progress_bar);

        mButtonRestore = root.findViewById(R.id.button_restore);
        mButtonsRoot = root.findViewById(R.id.buttons);
        mToolbar = root.findViewById(R.id.toolbar);

        ((AppCompatActivity) requireActivity()).setSupportActionBar(mToolbar);

        mViewPager = root.findViewById(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                getPresenter().firePageSelected(position);
            }
        });

        mButtonLike = root.findViewById(R.id.like_button);
        mButtonLike.setOnClickListener(v -> getPresenter().fireLikeClick());

        mButtonLike.setOnLongClickListener(v -> {
            getPresenter().fireLikeLongClick();
            return false;
        });

        mButtonComments = root.findViewById(R.id.comments_button);
        mButtonComments.setOnClickListener(v -> getPresenter().fireCommentsButtonClick());

        buttonShare = root.findViewById(R.id.share_button);
        buttonShare.setOnClickListener(v -> getPresenter().fireShareButtonClick());

        mButtonRestore.setOnClickListener(v -> getPresenter().fireButtonRestoreClick());

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.vkphoto_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.photo_size:
                onPhotoSizeClicked();
                break;
            case R.id.save_on_drive:
                if (isPresenterPrepared()) getPresenter().fireSaveOnDriveClick();
                return true;
            case R.id.save_yourself:
                if (isPresenterPrepared()) getPresenter().fireSaveYourselfClick();
                break;
            case R.id.action_delete:
                if (isPresenterPrepared()) getPresenter().fireDeleteClick();
                break;
            case R.id.info:
                if (isPresenterPrepared()) getPresenter().fireInfoButtonClick();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void goToLikesList(int accountId, int ownerId, int photoId) {
        PlaceFactory.getLikesCopiesPlace(accountId, "photo", ownerId, photoId, ILikesInteractor.FILTER_LIKES).tryOpenWith(requireActivity());
    }

    @Override
    public void onPrepareOptionsMenu(@NotNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!Settings.get().accounts().getType(Settings.get().accounts().getCurrent()).equals("hacked")) {
            menu.findItem(R.id.save_yourself).setVisible(mCanSaveYourself);
            menu.findItem(R.id.action_delete).setVisible(mCanDelete);
        } else {
            menu.findItem(R.id.save_yourself).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
        }

        int imageSize = getPhotoSizeFromPrefs();
        menu.findItem(R.id.photo_size).setTitle(getTitleForPhotoSize(imageSize));
    }

    private void onPhotoSizeClicked() {
        View view = requireActivity().findViewById(R.id.photo_size);

        int current = getPhotoSizeFromPrefs();

        PopupMenu popupMenu = new PopupMenu(requireActivity(), view);
        for (int i = 0; i < SIZES.size(); i++) {
            int key = SIZES.keyAt(i);
            int value = SIZES.get(key);

            addPhotoSizeToMenu(popupMenu, key, value, current);
        }

        popupMenu.getMenu().setGroupCheckable(0, true, true);

        popupMenu.setOnMenuItemClickListener(item -> {
            int key = item.getItemId();

            //noinspection ResourceType
            Settings.get()
                    .main()
                    .setPrefDisplayImageSize(SIZES.get(key));

            requireActivity().invalidateOptionsMenu();
            return true;
        });

        popupMenu.show();
    }

    @Override
    public void displayAccountNotSupported() {

    }

    @NotNull
    @Override
    public IPresenterFactory<PhotoPagerPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            int placeType = requireArguments().getInt(Extra.PLACE_TYPE);
            int aid = requireArguments().getInt(Extra.ACCOUNT_ID);

            switch (placeType) {
                case Place.SIMPLE_PHOTO_GALLERY:
                    int index = requireArguments().getInt(Extra.INDEX);
                    boolean needUpdate = requireArguments().getBoolean(EXTRA_NEED_UPDATE);
                    ArrayList<Photo> photos = requireArguments().getParcelableArrayList(EXTRA_PHOTOS);
                    AssertUtils.requireNonNull(photos);
                    return new SimplePhotoPresenter(photos, index, needUpdate, aid, requireArguments().getInt(Extra.STORY), requireActivity(), saveInstanceState);

                case Place.VK_PHOTO_ALBUM_GALLERY:
                    int indexx = requireArguments().getInt(Extra.INDEX);
                    int ownerId = requireArguments().getInt(Extra.OWNER_ID);
                    int albumId = requireArguments().getInt(Extra.ALBUM_ID);
                    ArrayList<Photo> photos_album = requireArguments().getParcelableArrayList(EXTRA_PHOTOS);
                    AssertUtils.requireNonNull(photos_album);
                    return new PhotoAlbumPagerPresenter(indexx, aid, ownerId, albumId, photos_album, requireActivity(), saveInstanceState);

                case Place.FAVE_PHOTOS_GALLERY:
                    int findex = requireArguments().getInt(Extra.INDEX);
                    ArrayList<Photo> favePhotos = requireArguments().getParcelableArrayList(EXTRA_PHOTOS);
                    AssertUtils.requireNonNull(favePhotos);
                    return new FavePhotoPagerPresenter(favePhotos, findex, aid, requireActivity(), saveInstanceState);

                case Place.VK_PHOTO_TMP_SOURCE:
                    TmpSource source = requireArguments().getParcelable(Extra.SOURCE);
                    AssertUtils.requireNonNull(source);
                    return new TmpGalleryPagerPresenter(aid, source, requireArguments().getInt(Extra.INDEX), requireActivity(), saveInstanceState);
            }

            throw new UnsupportedOperationException();
        };
    }

    @Override
    public void setupLikeButton(boolean visible, boolean like, int likes) {
        if (nonNull(mButtonLike)) {
            mButtonLike.setVisibility(visible ? View.VISIBLE : View.GONE);
            mButtonLike.setActive(like);
            mButtonLike.setCount(likes);
            mButtonLike.setIcon(like ? R.drawable.heart_filled : R.drawable.heart);
        }
    }

    @Override
    public void setupShareButton(boolean visible) {
        if (nonNull(buttonShare)) {
            buttonShare.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setupCommentsButton(boolean visible, int count) {
        if (nonNull(mButtonComments)) {
            mButtonComments.setVisibility(visible ? View.VISIBLE : View.GONE);
            mButtonComments.setCount(count);
        }
    }

    @Override
    public void displayPhotos(@NonNull List<Photo> photos, int initialIndex) {
        if (nonNull(mViewPager)) {
            mPagerAdapter = new Adapter(photos);
            mViewPager.setAdapter(mPagerAdapter);
            mViewPager.setCurrentItem(initialIndex, false);
        }
    }

    @Override
    public void setToolbarTitle(String title) {
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (nonNull(actionBar)) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void setToolbarSubtitle(String subtitle) {
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (nonNull(actionBar)) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void sharePhoto(int accountId, @NonNull Photo photo) {
        String[] items = new String[]{
                getString(R.string.share_link),
                getString(R.string.repost_send_message),
                getString(R.string.repost_to_wall)
        };

        new MaterialAlertDialogBuilder(requireActivity())
                .setItems(items, (dialogInterface, i) -> {
                    switch (i) {
                        case 0:
                            Utils.shareLink(requireActivity(), photo.generateWebLink(), photo.getText());
                            break;
                        case 1:
                            SendAttachmentsActivity.startForSendAttachments(requireActivity(), accountId, photo);
                            break;
                        case 2:
                            getPresenter().firePostToMyWallClick();
                            break;
                    }
                })
                .setCancelable(true)
                .setTitle(R.string.share_photo_title)
                .show();
    }

    @Override
    public void postToMyWall(@NonNull Photo photo, int accountId) {
        PlaceUtil.goToPostCreation(requireActivity(), accountId, accountId, EditingPostType.TEMP, Collections.singletonList(photo));
    }

    @Override
    public void requestWriteToExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_STORAGE);
    }

    @Override
    public void setButtonRestoreVisible(boolean visible) {
        if (nonNull(mButtonRestore)) {
            mButtonRestore.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setupOptionMenu(boolean canSaveYourself, boolean canDelete) {
        mCanSaveYourself = canSaveYourself;
        mCanDelete = canDelete;
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void goToComments(int aid, @NonNull Commented commented) {
        PlaceFactory.getCommentsPlace(aid, commented, null).tryOpenWith(requireActivity());
    }

    @Override
    public void displayPhotoListLoading(boolean loading) {
        if (nonNull(mLoadingProgressBar)) {
            mLoadingProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setButtonsBarVisible(boolean visible) {
        if (nonNull(mButtonsRoot)) {
            mButtonsRoot.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setToolbarVisible(boolean visible) {
        if (nonNull(mToolbar)) {
            mToolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void rebindPhotoAt(int position) {
        if (nonNull(mPagerAdapter)) {
            mPagerAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void showPhotoInfo(Photo photo) {
        AlertDialog dlg = new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(requireContext().getString(R.string.uploaded) + " " + AppTextUtils.getDateFromUnixTime(photo.getDate()))
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .create();
        String res = "";
        res += "<p><i><a href=\"" + "https://vk.com/album" + photo.getOwnerId() + "_" + photo.getAlbumId() + "\">" + requireContext().getString(R.string.open_photo_album) + "</a></i></p>";
        if (photo.getOwnerId() >= 0)
            res += "<p><i><a href=\"" + "https://vk.com/id" + photo.getOwnerId() + "\">" + requireContext().getString(R.string.goto_user) + "</a></i></p>";
        else
            res += "<p><i><a href=\"" + "https://vk.com/club" + (photo.getOwnerId() * -1) + "\">" + requireContext().getString(R.string.goto_community) + "</a></i></p>";
        if (photo.getText().length() > 0)
            res += ("<p><b>" + requireContext().getString(R.string.description_hint) + ":</b></p>" + photo.getText());
        if (photo.getTagsCount() > 0) {
            String finalRes = res;
            getPresenter().appendDisposable(
                    Injection.provideNetworkInterfaces().vkDefault(Settings.get().accounts().getCurrent()).photos().getTags(photo.getOwnerId(), photo.getId(), photo.getAccessKey())
                            .compose(RxUtils.applySingleIOToMainSchedulers())
                            .subscribe(userInfo -> {
                                StringBuilder tmp = new StringBuilder(finalRes);
                                tmp.append("<p><b>").append(requireContext().getString(R.string.has_tags)).append(":</b></p>").append(photo.getText());
                                for (VKApiPhotoTags i : userInfo) {
                                    if (i.user_id != 0)
                                        tmp.append("<i><a href=\"https://vk.com/id").append(i.user_id).append("\">").append(i.tagged_name != null ? i.tagged_name : "").append("</a></i>").append(" ");
                                    else
                                        tmp.append(i.tagged_name != null ? i.tagged_name : "").append(" ");
                                }
                                dlg.setMessage(Html.fromHtml(tmp.toString()));
                                dlg.show();
                                try {
                                    TextView tv = dlg.findViewById(android.R.id.message);
                                    if (tv != null)
                                        tv.setMovementMethod(LinkMovementMethod.getInstance());
                                } catch (Exception ignored) {
                                }
                            }, throwable -> {
                                dlg.setMessage(Html.fromHtml(finalRes));
                                dlg.show();
                                try {
                                    TextView tv = dlg.findViewById(android.R.id.message);
                                    if (tv != null)
                                        tv.setMovementMethod(LinkMovementMethod.getInstance());
                                } catch (Exception ignored) {
                                }
                            }));
            return;
        }

        dlg.setMessage(Html.fromHtml(res));
        dlg.show();
        try {
            TextView tv = dlg.findViewById(android.R.id.message);
            if (tv != null) tv.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_WRITE_STORAGE && isPresenterPrepared()) {
            getPresenter().fireWriteExternalStoragePermissionResolved();
        }
    }

    @Override
    public void goBack() {
        if (isAdded() && canGoBack()) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
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

    private boolean canGoBack() {
        return requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 1;
    }

    @PhotoSize
    public int getPhotoSizeFromPrefs() {
        return Settings.get()
                .main()
                .getPrefDisplayImageSize(DEFAULT_PHOTO_SIZE);
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

    private class PhotoViewHolder extends RecyclerView.ViewHolder implements Callback {
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

            photo.setOnClickListener(v -> callPresenter(PhotoPagerPresenter::firePhotoTap));
        }

        public void bindTo(@NonNull Photo photo_image) {
            int size = getPhotoSizeFromPrefs();

            String url = photo_image.getUrlForSize(size, true);

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
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.empty_url);
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

    private class Adapter extends RecyclerView.Adapter<PhotoViewHolder> {

        final List<Photo> mPhotos;

        Adapter(List<Photo> data) {
            mPhotos = data;
        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
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
                if (Settings.get().other().isDownload_photo_tap()) {
                    if (isPresenterPrepared()) getPresenter().fireSaveOnDriveClick();
                }
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
        public void onViewDetachedFromWindow(@NotNull PhotoViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            PicassoInstance.with().cancelRequest(holder.photo);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            Photo photo = mPhotos.get(position);
            holder.bindTo(photo);
        }

        @Override
        public int getItemCount() {
            return mPhotos.size();
        }
    }
}
