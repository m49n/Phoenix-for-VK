package biz.dealnote.messenger.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.SelectProfilesActivity;
import biz.dealnote.messenger.adapter.CommunityManagersAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.PeopleSearchCriteria;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.Manager;
import biz.dealnote.messenger.model.SelectProfileCriteria;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.CommunityManagersPresenter;
import biz.dealnote.messenger.mvp.view.ICommunityManagersView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

public class CommunityManagersFragment extends BaseMvpFragment<CommunityManagersPresenter, ICommunityManagersView>
        implements ICommunityManagersView {

    private static final int REQUEST_SELECT_PROFILES = 19;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CommunityManagersAdapter mAdapter;

    public static CommunityManagersFragment newInstance(int accountId, Community groupId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.GROUP_ID, groupId);
        CommunityManagersFragment fragment = new CommunityManagersFragment();
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

        mAdapter = new CommunityManagersAdapter(requireActivity(), Collections.emptyList());
        mAdapter.setActionListener(new CommunityManagersAdapter.ActionListener() {
            @Override
            public void onManagerClick(Manager manager) {
                getPresenter().fireManagerClick(manager);
            }

            @Override
            public void onManagerLongClick(Manager manager) {
                showManagerContextMenu(manager);
            }
        });

        recyclerView.setAdapter(mAdapter);
        root.findViewById(R.id.button_add).setOnClickListener(v -> getPresenter().fireButtonAddClick());
        return root;
    }

    private void showManagerContextMenu(Manager manager) {
        String[] items = {getString(R.string.delete)};
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(manager.getUser().getFullName())
                .setItems(items, (dialog, which) -> getPresenter().fireRemoveClick(manager))
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    @NotNull
    @Override
    public IPresenterFactory<CommunityManagersPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new CommunityManagersPresenter(
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
    public void goToManagerEditing(int accountId, int groupId, Manager manager) {
        PlaceFactory.getCommunityManagerEditPlace(accountId, groupId, manager).tryOpenWith(requireActivity());
    }

    @Override
    public void showUserProfile(int accountId, User user) {
        PlaceFactory.getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity());
    }

    @Override
    public void startSelectProfilesActivity(int accountId, int groupId) {
        PeopleSearchCriteria criteria = new PeopleSearchCriteria("").setGroupId(groupId);

        SelectProfileCriteria c = new SelectProfileCriteria();

        Place place = PlaceFactory.getSingleTabSearchPlace(accountId, SearchContentType.PEOPLE, criteria);
        Intent intent = SelectProfilesActivity.createIntent(requireActivity(), place, c);
        startActivityForResult(intent, REQUEST_SELECT_PROFILES);
    }

    @Override
    public void startAddingUsersToManagers(int accountId, int groupId, ArrayList<User> users) {
        PlaceFactory.getCommunityManagerAddPlace(accountId, groupId, users).tryOpenWith(requireActivity());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_PROFILES && resultCode == Activity.RESULT_OK) {
            ArrayList<User> users = data.getParcelableArrayListExtra(Extra.USERS);
            AssertUtils.requireNonNull(users);
            postPrenseterReceive(presenter -> presenter.fireProfilesSelected(users));
        }
    }
}