package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IFeedbackInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.feedback.Feedback;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.IFeedbackView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.nonEmpty;


public class FeedbackPresenter extends PlaceSupportPresenter<IFeedbackView> {

    private static final String TAG = FeedbackPresenter.class.getSimpleName();
    private static final int COUNT_PER_REQUEST = 15;

    private final List<Feedback> mData;
    private final IFeedbackInteractor feedbackInteractor;
    private String mNextFrom;
    private boolean actualDataReceived;
    private boolean mEndOfContent;
    private CompositeDisposable cacheDisposable = new CompositeDisposable();
    private boolean cacheLoadingNow;
    private CompositeDisposable netDisposable = new CompositeDisposable();
    private boolean netLoadingNow;
    private String netLoadingStartFrom;

    public FeedbackPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);

        this.feedbackInteractor = InteractorFactory.createFeedbackInteractor();
        this.mData = new ArrayList<>();

        loadAllFromDb();
        requestActualData(null);
    }

    @OnGuiCreated
    private void resolveLoadMoreFooter() {
        if (!isGuiReady()) return;

        if (isEmpty(this.mData)) {
            getView().configLoadMore(LoadMoreState.INVISIBLE);
            return;
        }

        if (nonEmpty(this.mData) && netLoadingNow && nonEmpty(netLoadingStartFrom)) {
            getView().configLoadMore(LoadMoreState.LOADING);
            return;
        }

        if (canLoadMore()) {
            getView().configLoadMore(LoadMoreState.CAN_LOAD_MORE);
            return;
        }

        getView().configLoadMore(LoadMoreState.END_OF_LIST);
    }

    private void requestActualData(String startFrom) {
        this.netDisposable.clear();

        this.netLoadingNow = true;
        this.netLoadingStartFrom = startFrom;

        final int accountId = super.getAccountId();

        resolveLoadMoreFooter();
        resolveSwiperefreshLoadingView();

        netDisposable.add(feedbackInteractor.getActualFeedbacks(accountId, COUNT_PER_REQUEST, startFrom)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> onActualDataReceived(startFrom, pair.getFirst(), pair.getSecond()), this::onActualDataGetError));
    }

    private void onActualDataGetError(Throwable t) {
        t.printStackTrace();

        this.netLoadingNow = false;
        this.netLoadingStartFrom = null;

        showError(getView(), getCauseIfRuntime(t));

        resolveLoadMoreFooter();
        resolveSwiperefreshLoadingView();
    }

    private void safelyMarkAsViewed() {
        final int accountId = super.getAccountId();
        if (Settings.get().accounts().getType(accountId).equals("hacked"))
            return;

        appendDisposable(feedbackInteractor.maskAaViewed(accountId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> callView(IFeedbackView::notifyUpdateCounter), ignore()));
    }

    private void onActualDataReceived(String startFrom, List<Feedback> feedbacks, String nextFrom) {
        if (isEmpty(startFrom)) {
            safelyMarkAsViewed();
        }

        this.cacheDisposable.clear();
        this.cacheLoadingNow = false;
        this.netLoadingNow = false;
        this.netLoadingStartFrom = null;
        this.mNextFrom = nextFrom;
        this.mEndOfContent = isEmpty(nextFrom);
        this.actualDataReceived = true;

        if (isEmpty(startFrom)) {
            this.mData.clear();
            this.mData.addAll(feedbacks);
            callView(IFeedbackView::notifyDataSetChanged);
        } else {
            int sizeBefore = this.mData.size();
            this.mData.addAll(feedbacks);
            callView(view -> view.notifyDataAdding(sizeBefore, feedbacks.size()));
        }

        resolveLoadMoreFooter();
        resolveSwiperefreshLoadingView();
    }

    @OnGuiCreated
    private void resolveSwiperefreshLoadingView() {
        if (isGuiReady()) {
            getView().showLoading(netLoadingNow && isEmpty(netLoadingStartFrom));
        }
    }

    private boolean canLoadMore() {
        return nonEmpty(mNextFrom) && !mEndOfContent && !cacheLoadingNow && !netLoadingNow && actualDataReceived;
    }

    @Override
    public void onGuiCreated(@NonNull IFeedbackView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayData(mData);
    }

    private void loadAllFromDb() {
        this.cacheLoadingNow = true;
        final int accountId = super.getAccountId();

        cacheDisposable.add(feedbackInteractor.getCachedFeedbacks(accountId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCachedDataReceived, Throwable::printStackTrace));
    }

    private void onCachedDataReceived(List<Feedback> feedbacks) {
        this.cacheLoadingNow = false;
        this.mData.clear();
        this.mData.addAll(feedbacks);

        callView(IFeedbackView::notifyDataSetChanged);
    }

    @Override
    public void onDestroyed() {
        cacheDisposable.dispose();
        netDisposable.dispose();
        super.onDestroyed();
    }

    public void fireItemClick(@NonNull Feedback notification) {
        getView().showLinksDialog(getAccountId(), notification);
    }

    public void fireLoadMoreClick() {
        if (canLoadMore()) {
            requestActualData(this.mNextFrom);
        }
    }

    public void fireRefresh() {
        cacheDisposable.clear();
        cacheLoadingNow = false;

        netDisposable.clear();
        netLoadingNow = false;
        netLoadingStartFrom = null;

        requestActualData(null);
    }

    public void fireScrollToLast() {
        if (canLoadMore()) {
            requestActualData(this.mNextFrom);
        }
    }
}