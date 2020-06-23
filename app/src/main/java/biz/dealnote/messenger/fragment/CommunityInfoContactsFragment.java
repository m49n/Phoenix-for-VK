package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.CommunityInfoContactsAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.Manager;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.CommunityInfoContactsPresenter;
import biz.dealnote.messenger.mvp.view.ICommunityInfoContactsView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

public class CommunityInfoContactsFragment extends BaseMvpFragment<CommunityInfoContactsPresenter, ICommunityInfoContactsView>
        implements ICommunityInfoContactsView {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CommunityInfoContactsAdapter mAdapter;

    public static CommunityInfoContactsFragment newInstance(int accountId, Community groupId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.GROUP_ID, groupId);
        CommunityInfoContactsFragment fragment = new CommunityInfoContactsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_community_managers, container, false);

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        root.findViewById(R.id.button_add).setVisibility(View.INVISIBLE);

        mAdapter = new CommunityInfoContactsAdapter(requireActivity(), Collections.emptyList());
        mAdapter.setActionListener(manager -> getPresenter().fireManagerClick(manager));

        recyclerView.setAdapter(mAdapter);
        return root;
    }

    @NotNull
    @Override
    public IPresenterFactory<CommunityInfoContactsPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new CommunityInfoContactsPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireArguments().getParcelable(Extra.GROUP_ID),
                saveInstanceState
        );
    }

    @Override
    public void notifyDataSetChanged() {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayRefreshing(boolean loadingNow) {
        if (Objects.nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(loadingNow);
        }
    }

    @Override
    public void displayData(List<Manager> managers) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.setData(managers);
        }
    }

    @Override
    public void showUserProfile(int accountId, User user) {
        PlaceFactory.getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity());
    }

    @Override
    public void notifyItemRemoved(int index) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.notifyItemRemoved(index);
        }
    }

    @Override
    public void notifyItemChanged(int index) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.notifyItemChanged(index);
        }
    }

    @Override
    public void notifyItemAdded(int index) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.notifyItemInserted(index);
        }
    }
}
