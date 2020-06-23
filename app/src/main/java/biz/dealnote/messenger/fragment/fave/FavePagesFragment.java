package biz.dealnote.messenger.fragment.fave;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.fave.FavePagesAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.FavePage;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.mvp.presenter.FavePagesPresenter;
import biz.dealnote.messenger.mvp.view.IFaveUsersView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.MySearchView;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class FavePagesFragment extends BaseMvpFragment<FavePagesPresenter, IFaveUsersView> implements IFaveUsersView, FavePagesAdapter.ClickListener {

    private TextView mEmpty;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FavePagesAdapter mAdapter;
    private boolean isRequestLast = false;

    public static FavePagesFragment newInstance(int accountId, boolean isUser) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putBoolean(Extra.USER, isUser);
        FavePagesFragment fragment = new FavePagesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_fave_pages, container, false);
        mEmpty = root.findViewById(R.id.empty);

        RecyclerView recyclerView = root.findViewById(R.id.list);
        int columns = getContext().getResources().getInteger(R.integer.photos_column_count);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(), columns);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        MySearchView mySearchView = root.findViewById(R.id.searchview);
        mySearchView.setRightButtonVisibility(false);
        mySearchView.setLeftIcon(R.drawable.magnify);
        mySearchView.setOnQueryTextListener(new MySearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getPresenter().fireSearchRequestChanged(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                getPresenter().fireSearchRequestChanged(newText);
                return false;
            }
        });

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        mAdapter = new FavePagesAdapter(Collections.emptyList(), requireActivity());
        mAdapter.setClickListener(this);

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
    public void displayData(List<FavePage> users) {
        if (nonNull(mAdapter)) {
            mAdapter.setData(users);
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

    @Override
    public void openOwnerWall(int accountId, Owner owner) {
        PlaceFactory.getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity());
    }

    @Override
    public void notifyItemRemoved(int index) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRemoved(index);
            resolveEmptyText();
        }
    }

    @NotNull
    @Override
    public IPresenterFactory<FavePagesPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new FavePagesPresenter(
                getArguments().getInt(Extra.ACCOUNT_ID),
                getArguments().getBoolean(Extra.USER),
                saveInstanceState
        );
    }

    @Override
    public void onPageClick(int index, Owner owner) {
        getPresenter().fireOwnerClick(owner);
    }

    @Override
    public void onDelete(int index, Owner owner) {
        getPresenter().fireOwnerDelete(owner);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isRequestLast) {
            isRequestLast = true;
            getPresenter().LoadTool();
        }
    }
}
