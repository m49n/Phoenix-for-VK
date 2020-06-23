package biz.dealnote.messenger.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.adapter.CommunitiesAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.AppStyleable;
import biz.dealnote.messenger.listener.BackPressCallback;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.DataWrapper;
import biz.dealnote.messenger.mvp.presenter.CommunitiesPresenter;
import biz.dealnote.messenger.mvp.view.ICommunitiesView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.MySearchView;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class CommunitiesFragment extends BaseMvpFragment<CommunitiesPresenter, ICommunitiesView>
        implements ICommunitiesView, MySearchView.OnQueryTextListener, CommunitiesAdapter.ActionListener, BackPressCallback, MySearchView.OnBackButtonClickListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CommunitiesAdapter mAdapter;
    private MySearchView mSearchView;

    public static CommunitiesFragment newInstance(int accountId, int userId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.USER_ID, userId);
        CommunitiesFragment fragment = new CommunitiesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_communities, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        mAdapter = new CommunitiesAdapter(requireActivity(), Collections.emptyList(), new int[0]);
        mAdapter.setActionListener(this);

        recyclerView.setAdapter(mAdapter);

        mSearchView = root.findViewById(R.id.searchview);
        mSearchView.setOnBackButtonClickListener(this);
        mSearchView.setRightButtonVisibility(false);
        mSearchView.setOnQueryTextListener(this);

        mSearchView.setLeftIcon(R.drawable.magnify);
        return root;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void displayData(DataWrapper<Community> own, DataWrapper<Community> filtered, DataWrapper<Community> seacrh) {
        if (nonNull(mAdapter)) {
            List<DataWrapper<Community>> wrappers = new ArrayList<>();
            wrappers.add(own);
            wrappers.add(filtered);
            wrappers.add(seacrh);

            int[] titles = {R.string.my_communities_title, R.string.quick_search_title, R.string.other};
            mAdapter.setData(wrappers, titles);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.COMMUNITIES);

        ActivityUtils.setToolbarTitle(this, R.string.groups);
        ActivityUtils.setToolbarSubtitle(this, null); // TODO: 04.10.2017

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void notifyDataSetChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyOwnDataAdded(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRangeInserted(0, position, count);
        }
    }

    @Override
    public void displayRefreshing(boolean refreshing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    @Override
    public void showCommunityWall(int accountId, Community community) {
        PlaceFactory.getOwnerWallPlace(accountId, community).tryOpenWith(requireActivity());
    }

    @Override
    public void notifySeacrhDataAdded(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRangeInserted(2, position, count);
        }
    }

    @NotNull
    @Override
    public IPresenterFactory<CommunitiesPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new CommunitiesPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireArguments().getInt(Extra.USER_ID),
                saveInstanceState
        );
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        getPresenter().fireSearchQueryChanged(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        getPresenter().fireSearchQueryChanged(newText);
        return true;
    }

    @Override
    public void onCommunityClick(Community community) {
        getPresenter().fireCommunityClick(community);
    }

    @Override
    public boolean onBackPressed() {
        CharSequence query = mSearchView.getText();
        if (Utils.isEmpty(query)) {
            return true;
        }

        mSearchView.setQuery("");
        return false;
    }

    @Override
    public void onBackButtonClick() {
        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() == 1 && requireActivity() instanceof AppStyleable) {
            ((AppStyleable) requireActivity()).openMenu(true);
        } else {
            requireActivity().onBackPressed();
        }
    }
}