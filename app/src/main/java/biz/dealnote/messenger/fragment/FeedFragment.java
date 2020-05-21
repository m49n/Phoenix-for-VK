package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.adapter.FeedAdapter;
import biz.dealnote.messenger.adapter.horizontal.HorizontalOptionsAdapter;
import biz.dealnote.messenger.domain.ILikesInteractor;
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.CommentedType;
import biz.dealnote.messenger.model.FeedSource;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.News;
import biz.dealnote.messenger.mvp.presenter.FeedPresenter;
import biz.dealnote.messenger.mvp.view.IFeedView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.view.LoadMoreFooterHelper;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class FeedFragment extends PlaceSupportMvpFragment<FeedPresenter, IFeedView> implements IFeedView,
        SwipeRefreshLayout.OnRefreshListener, FeedAdapter.ClickListener, HorizontalOptionsAdapter.Listener<FeedSource> {

    private FeedAdapter mAdapter;
    private TextView mEmptyText;
    private RecyclerView mRecycleView;
    private RecyclerView.LayoutManager mFeedLayoutManager;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private LoadMoreFooterHelper mLoadMoreFooterHelper;

    private HorizontalOptionsAdapter<FeedSource> mFeedSourceAdapter;
    private LinearLayoutManager mHeaderLayoutManager;
    private Gson mGson = new Gson();

    public static Bundle buildArgs(int accountId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        return args;
    }

    public static FeedFragment newInstance(int accountId) {
        return newInstance(buildArgs(accountId));
    }

    public static FeedFragment newInstance(Bundle args) {
        FeedFragment feedFragment = new FeedFragment();
        feedFragment.setArguments(args);
        return feedFragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_feed, menu);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                getPresenter().fireRefresh();
                return true;
            case R.id.up:
                mRecycleView.stopScroll();
                mFeedLayoutManager.scrollToPosition(0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_new_feed, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        styleSwipeRefreshLayoutWithCurrentTheme(mSwipeRefreshLayout, true);

        if (Utils.is600dp(requireActivity())) {
            boolean land = Utils.isLandscape(requireActivity());
            mFeedLayoutManager = new StaggeredGridLayoutManager(land ? 2 : 1, StaggeredGridLayoutManager.VERTICAL);
        } else {
            mFeedLayoutManager = new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false);
        }

        mRecycleView = root.findViewById(R.id.fragment_feeds_list);
        mRecycleView.setLayoutManager(mFeedLayoutManager);
        mRecycleView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        mRecycleView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToBottom();
            }
        });

        FloatingActionButton Goto = root.findViewById(R.id.goto_button);
        Goto.setOnClickListener(v -> mRecycleView.scrollToPosition(0));
        Goto.setOnLongClickListener(v -> {
            mRecycleView.scrollToPosition(0);
            getPresenter().fireRefresh();
            return true;
        });

        mEmptyText = root.findViewById(R.id.fragment_feeds_empty_text);

        ViewGroup footerView = (ViewGroup) inflater.inflate(R.layout.footer_load_more, mRecycleView, false);

        mLoadMoreFooterHelper = LoadMoreFooterHelper.createFrom(footerView, () -> getPresenter().fireLoadMoreClick());
        mLoadMoreFooterHelper.setEndOfListText("");

        ViewGroup headerView = (ViewGroup) inflater.inflate(R.layout.header_feed, mRecycleView, false);
        RecyclerView headerRecyclerView = headerView.findViewById(R.id.header_list);

        mHeaderLayoutManager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false);
        headerRecyclerView.setLayoutManager(mHeaderLayoutManager);

        mAdapter = new FeedAdapter(requireActivity(), Collections.emptyList(), this);

        mAdapter.setClickListener(this);
        mAdapter.addFooter(footerView);
        mAdapter.addHeader(headerView);

        mRecycleView.setAdapter(mAdapter);

        mFeedSourceAdapter = new HorizontalOptionsAdapter<>(Collections.emptyList());
        mFeedSourceAdapter.setListener(this);
        headerRecyclerView.setAdapter(mFeedSourceAdapter);
        return root;
    }

    /*private void resolveEmptyText() {
        if(!isAdded()) return;
        mEmptyText.setVisibility(!nowRequest() && safeIsEmpty(mData) ? View.VISIBLE : View.INVISIBLE);
        mEmptyText.setText(R.string.feeds_empty_text);
    }*/

   /* private void loadFeedSourcesData() {
        restoreCurrentSourceIds();

        initFeedSources(null);
        mFeedListsLoader.loadAll(getAccountId());
        executeRequest(FeedRequestFactory.getFeedListsRequest());
    }*/

    /*private void loadFeedData() {
        // восстанавливаем из настроек последнее значение next_from, данные до которого
        // хранятся в базе данных
        mNextFrom = Settings.get()
                .other()
                .restoreFeedNextFrom(getAccountId());

        // загружаем все новости, которые сохранены в базу данных текущего аккаунта
        mFeedLoader.load(buildCriteria(), true);

        // если же предыдущего состояния next_from нет, то запрашиваем новости
        // с сервера (в противном случае ничего не загружаем с сервиса, пользователь
        // должен ПРОДОЛЖИТЬ читать ленту с того места, где закончил)
        if (TextUtils.isEmpty(mNextFrom)) {
            requestFeed();
        }
    }*/

    @Override
    public void onAvatarClick(int ownerId) {
        super.onOwnerClick(ownerId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.FEED);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.feed);
            actionBar.setSubtitle(null);
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_FEED);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void onOwnerClick(int ownerId) {
        this.onOpenOwner(ownerId);
    }

    @Override
    public void onRepostClick(News news) {
        getPresenter().fireNewsRepostClick(news);
    }

    @Override
    public void onPostClick(News news) {
        getPresenter().fireNewsBodyClick(news);
    }

    @Override
    public void onCommentButtonClick(News news) {
        getPresenter().fireNewsCommentClick(news);
    }

    @Override
    public void onLikeClick(News news, boolean add) {
        getPresenter().fireLikeClick(news);
    }

    @Override
    public boolean onLikeLongClick(News news) {
        getPresenter().fireNewsLikeLongClick(news);
        return true;
    }

    @Override
    public boolean onShareLongClick(News news) {
        getPresenter().fireNewsShareLongClick(news);
        return true;
    }

    @Override
    public void goToLikes(int accountId, String type, int ownerId, int id) {
        PlaceFactory.getLikesCopiesPlace(accountId, type, ownerId, id, ILikesInteractor.FILTER_LIKES).tryOpenWith(requireActivity());
    }

    @Override
    public void goToReposts(int accountId, String type, int ownerId, int id) {
        PlaceFactory.getLikesCopiesPlace(accountId, type, ownerId, id, ILikesInteractor.FILTER_COPIES).tryOpenWith(requireActivity());
    }

    @Override
    public void goToPostComments(int accountId, int postId, int ownerId) {
        Commented commented = new Commented(postId, ownerId, CommentedType.POST, null);
        PlaceFactory.getCommentsPlace(accountId, commented, null).tryOpenWith(requireActivity());
    }

    @Override
    public void onRefresh() {
        getPresenter().fireRefresh();
    }

    private void restoreRecycleViewManagerState(String state) {
        if (nonEmpty(state)) {
            if (mFeedLayoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager.SavedState savedState = gson().fromJson(state, LinearLayoutManager.SavedState.class);
                mFeedLayoutManager.onRestoreInstanceState(savedState);
            }

            if (mFeedLayoutManager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager.SavedState savedState = gson().fromJson(state, StaggeredGridLayoutManager.SavedState.class);
                mFeedLayoutManager.onRestoreInstanceState(savedState);
            }
        }
    }

    private Gson gson() {
        return mGson;
    }

    @Override
    public void onPause() {
        Parcelable parcelable = mFeedLayoutManager.onSaveInstanceState();
        String json = gson().toJson(parcelable);

        getPresenter().fireScrollStateOnPause(json);
        super.onPause();
    }

    /*@Override
    public void onFeedListsLoadFinished(List<VkApiFeedList> result) {
        initFeedSources(result);
        int selected = refreshFeedSourcesSelection();

        if (mFeedSourceAdapter != null) {
            mFeedSourceAdapter.notifyDataSetChanged();
        }

        if (selected != -1 && mHeaderLayoutManager != null) {
            mHeaderLayoutManager.scrollToPosition(selected);
        }
    }*/

    /*@Override
    protected void beforeAccountChange(int oldAid, int newAid) {
        super.beforeAccountChange(oldAid, newAid);
        mFeedLoader.stopIfLoading();
        mFeedListsLoader.stopIfLoading();

        storeListPosition();
        ignoreAll();
    }*/

    /*@Override
    protected void afterAccountChange(int oldAid, int newAid) {
        super.afterAccountChange(oldAid, newAid);
        restoreCurrentSourceIds();

        mEndOfContent = false;
        mData.clear();
        mFeedSources.clear();

        mAdapter.notifyDataSetChanged();
        mFeedSourceAdapter.notifyDataSetChanged();

        loadFeedData();
        loadFeedSourcesData();

        resolveEmptyText();
        resolveFooter();
    }*/

    @Override
    public void onOptionClick(FeedSource entry) {
        getPresenter().fireFeedSourceClick(entry);
    }

    @Override
    public void displayFeedSources(List<FeedSource> sources) {
        if (nonNull(mFeedSourceAdapter)) {
            mFeedSourceAdapter.setItems(sources);
        }
    }

    @Override
    public void notifyFeedSourcesChanged() {
        if (nonNull(mFeedSourceAdapter)) {
            mFeedSourceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayFeed(List<News> data, @Nullable String rawScrollState) {
        if (nonNull(mAdapter)) {
            mAdapter.setItems(data);
        }

        if (nonEmpty(rawScrollState) && nonNull(mFeedLayoutManager)) {
            try {
                restoreRecycleViewManagerState(rawScrollState);
            } catch (Exception ignored) {
            }
        }

        resolveEmptyTextVisibility();
    }

    @Override
    public void notifyFeedDataChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
        }

        resolveEmptyTextVisibility();
    }

    @Override
    public void notifyDataAdded(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRangeInserted(position + mAdapter.getHeadersCount(), count);
        }

        resolveEmptyTextVisibility();
    }

    @Override
    public void notifyItemChanged(int position) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemChanged(position + mAdapter.getHeadersCount());
        }
    }

    @Override
    public void setupLoadMoreFooter(@LoadMoreState int state) {
        if (nonNull(mLoadMoreFooterHelper)) {
            mLoadMoreFooterHelper.switchToState(state);
        }
    }

    public void resolveEmptyTextVisibility() {
        if (nonNull(mEmptyText) && nonNull(mAdapter)) {
            mEmptyText.setVisibility(mAdapter.getRealItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void showRefreshing(boolean refreshing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(refreshing));
        }
    }

    @Override
    public void scrollFeedSourcesToPosition(int position) {
        if (nonNull(mHeaderLayoutManager)) {
            mHeaderLayoutManager.scrollToPosition(position);
        }
    }

    @Override
    public IPresenterFactory<FeedPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new FeedPresenter(requireArguments().getInt(Extra.ACCOUNT_ID), saveInstanceState);
    }
}