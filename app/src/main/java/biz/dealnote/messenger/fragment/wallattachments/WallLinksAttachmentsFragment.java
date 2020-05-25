package biz.dealnote.messenger.fragment.wallattachments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.adapter.LinksAdapter;
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.mvp.presenter.wallattachments.WallLinksAttachmentsPresenter;
import biz.dealnote.messenger.mvp.view.wallattachments.IWallLinksAttachmentsView;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class WallLinksAttachmentsFragment extends PlaceSupportMvpFragment<WallLinksAttachmentsPresenter, IWallLinksAttachmentsView>
        implements IWallLinksAttachmentsView, LinksAdapter.ActionListener {

    private TextView mEmpty;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinksAdapter mAdapter;
    private FloatingActionButton mLoadMore;

    public static WallLinksAttachmentsFragment newInstance(int accountId, int ownerId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.OWNER_ID, ownerId);
        WallLinksAttachmentsFragment fragment = new WallLinksAttachmentsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wall_attachments, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));
        mEmpty = root.findViewById(R.id.empty);
        mLoadMore = root.findViewById(R.id.goto_button);

        RecyclerView recyclerView = root.findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false));
        recyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });
        mLoadMore.setOnClickListener(v -> getPresenter().fireScrollToEnd());

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        mAdapter = new LinksAdapter(Collections.emptyList(), requireActivity());
        mAdapter.setActionListner(this);

        recyclerView.setAdapter(mAdapter);

        resolveEmptyText();
        return root;
    }

    private void resolveEmptyText() {
        if (nonNull(mEmpty) && nonNull(mAdapter)) {
            mEmpty.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void displayData(List<Link> links) {
        if (nonNull(mAdapter)) {
            mAdapter.setItems(links);
            resolveEmptyText();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
            resolveEmptyText();
        }
    }

    @Override
    public void notifyDataAdded(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRangeInserted(position, count);
            resolveEmptyText();
        }
    }

    @Override
    public void showRefreshing(boolean refreshing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    @NotNull
    @Override
    public IPresenterFactory<WallLinksAttachmentsPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new WallLinksAttachmentsPresenter(
                getArguments().getInt(Extra.ACCOUNT_ID),
                getArguments().getInt(Extra.OWNER_ID),
                saveInstanceState
        );
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
    public void onResume() {
        super.onResume();
        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void onSetLoadingStatus(int isLoad) {
        switch (isLoad) {
            case 1:
                mLoadMore.setImageResource(R.drawable.audio_died);
                break;
            case 2:
                mLoadMore.setImageResource(R.drawable.view);
                break;
            default:
                mLoadMore.setImageResource(R.drawable.ic_arrow_down);
                break;
        }
    }

    @Override
    public void onLinkClick(int index, @NonNull Link link) {
        getPresenter().fireLinkClick(link);
    }
}
