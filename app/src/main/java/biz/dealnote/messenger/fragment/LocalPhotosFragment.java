package biz.dealnote.messenger.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.LocalPhotosAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.LocalImageAlbum;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.mvp.presenter.LocalPhotosPresenter;
import biz.dealnote.messenger.mvp.view.ILocalPhotosView;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

public class LocalPhotosFragment extends BaseMvpFragment<LocalPhotosPresenter, ILocalPhotosView>
        implements ILocalPhotosView, LocalPhotosAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String EXTRA_MAX_SELECTION_COUNT = "max_selection_count";
    private static final int REQYEST_PERMISSION_READ_EXTERNAL_STORAGE = 89;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LocalPhotosAdapter mAdapter;
    private TextView mEmptyTextView;
    private FloatingActionButton fabAttach;

    public static LocalPhotosFragment newInstance(int maxSelectionItemCount, LocalImageAlbum album, boolean hide_toolbar) {
        Bundle args = new Bundle();
        args.putInt(EXTRA_MAX_SELECTION_COUNT, maxSelectionItemCount);
        args.putParcelable(Extra.ALBUM, album);
        if (hide_toolbar)
            args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
        LocalPhotosFragment fragment = new LocalPhotosFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (!hasHideToolbarExtra()) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        } else {
            toolbar.setVisibility(View.GONE);
        }

        mSwipeRefreshLayout = view.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout, true);

        int columnCount = getResources().getInteger(R.integer.local_gallery_column_count);
        RecyclerView.LayoutManager manager = new GridLayoutManager(requireActivity(), columnCount);

        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(LocalPhotosAdapter.TAG));

        mEmptyTextView = view.findViewById(R.id.empty);

        fabAttach = view.findViewById(R.id.fr_photo_gallery_attach);
        fabAttach.setOnClickListener(v -> getPresenter().fireFabClick());

        return view;
    }

    @Override
    public void onPhotoClick(LocalPhotosAdapter.ViewHolder holder, LocalPhoto photo) {
        getPresenter().firePhotoClick(photo);
    }

    @Override
    public void onRefresh() {
        getPresenter().fireRefresh();
    }

    @Override
    public void displayData(@NonNull List<LocalPhoto> data) {
        mAdapter = new LocalPhotosAdapter(requireActivity(), data);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void setEmptyTextVisible(boolean visible) {
        if (Objects.nonNull(mEmptyTextView)) {
            mEmptyTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void displayProgress(boolean loading) {
        if (Objects.nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(loading));
        }
    }

    @Override
    public void returnResultToParent(ArrayList<LocalPhoto> photos) {
        Collections.sort(photos);

        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(Extra.PHOTOS, photos);

        requireActivity().setResult(Activity.RESULT_OK, intent);
        requireActivity().finish();
    }

    @Override
    public void updateSelectionAndIndexes() {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.updateHoldersSelectionAndIndexes();
        }
    }

    @Override
    public void setFabVisible(boolean visible, boolean anim) {
        if (visible && !fabAttach.isShown()) {
            fabAttach.show();
        }

        if (!visible && fabAttach.isShown()) {
            fabAttach.hide();
        }
    }

    @Override
    public void requestReadExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQYEST_PERMISSION_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQYEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            getPresenter().fireReadExternalStoregePermissionResolved();
        }
    }

    @Override
    public void showError(String text) {
        if (isAdded()) Toast.makeText(requireActivity(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showError(@StringRes int titleRes, Object... params) {
        if (isAdded()) showError(getString(titleRes, params));
    }

    @NotNull
    @Override
    public IPresenterFactory<LocalPhotosPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            int maxSelectionItemCount1 = requireArguments().getInt(EXTRA_MAX_SELECTION_COUNT, 10);
            LocalImageAlbum album = requireArguments().getParcelable(Extra.ALBUM);
            return new LocalPhotosPresenter(album, maxSelectionItemCount1, saveInstanceState);
        };
    }
}