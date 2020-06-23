package biz.dealnote.messenger.mvp.presenter.conversations;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.conversations.IBaseChatAttachmentsView;
import biz.dealnote.messenger.util.DisposableHolder;
import biz.dealnote.messenger.util.Pair;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.safeIsEmpty;

public abstract class BaseChatAttachmentsPresenter<T, V extends IBaseChatAttachmentsView<T>>
        extends PlaceSupportPresenter<V> {

    final List<T> data;
    private final int peerId;
    private String nextFrom;

    private boolean endOfContent;
    private DisposableHolder<Void> loadingHolder = new DisposableHolder<>();

    BaseChatAttachmentsPresenter(int peerId, int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.peerId = peerId;
        this.data = new ArrayList<>();

        initLoading();
    }

    @Override
    public void onGuiCreated(@NonNull V viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayAttachments(data);
    }

    @Override
    public void onDestroyed() {
        loadingHolder.dispose();
        loadingHolder = null;
        super.onDestroyed();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveLoadingView();
    }

    private void resolveLoadingView() {
        if (isGuiReady()) {
            getView().showLoading(loadingHolder.isActive());
        }
    }

    private void initLoading() {
        load(null);
    }

    private void load(String startFrom) {
        loadingHolder.append(requestAttachments(peerId, startFrom)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onDataReceived(startFrom, data),
                        throwable -> onRequestError(Utils.getCauseIfRuntime(throwable))));
        resolveLoadingView();
    }

    private void onRequestError(Throwable throwable) {
        loadingHolder.dispose();
        resolveLoadingView();

        safeShowError(getView(), throwable.getMessage());
    }

    private void onDataReceived(String startFrom, Pair<String, List<T>> result) {
        loadingHolder.dispose();
        resolveLoadingView();

        this.nextFrom = result.getFirst();
        this.endOfContent = Utils.isEmpty(nextFrom);

        List<T> newData = result.getSecond();

        if (nonNull(startFrom)) {
            int startSize = data.size();

            data.addAll(newData);

            if (isGuiReady()) {
                getView().notifyDataAdded(startSize, newData.size());
            }
        } else {
            data.clear();
            data.addAll(newData);

            if (isGuiReady()) {
                getView().notifyDatasetChanged();
            }
        }

        resolveEmptyTextVisiblity();
        onDataChanged();
    }

    @OnGuiCreated
    private void resolveEmptyTextVisiblity() {
        if (isGuiReady()) {
            getView().setEmptyTextVisible(safeIsEmpty(data));
        }
    }

    void onDataChanged() {

    }

    private boolean canLoadMore() {
        return !endOfContent && !loadingHolder.isActive();
    }

    public void fireScrollToEnd() {
        if (canLoadMore()) {
            load(nextFrom);
        }
    }

    public void fireRefresh() {
        loadingHolder.dispose();
        this.nextFrom = null;

        initLoading();
    }

    abstract Single<Pair<String, List<T>>> requestAttachments(int peerId, String nextFrom);
}
