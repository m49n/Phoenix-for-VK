package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import biz.dealnote.messenger.domain.IFeedbackInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.AnswerVKOfficialList;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAnswerVKOfficialView;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

/**
 * Created by Ruslan Kolbasa on 11.09.2017.
 * phoenix
 */
public class AnswerVKOfficialPresenter extends AccountDependencyPresenter<IAnswerVKOfficialView> {

    private final AnswerVKOfficialList pages;

    private final IFeedbackInteractor fInteractor;

    private boolean actualDataReceived;

    private boolean endOfContent;

    public AnswerVKOfficialPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.pages = new AnswerVKOfficialList();
        this.pages.fields = new ArrayList<>();
        this.pages.items = new ArrayList<>();
        this.fInteractor = InteractorFactory.createFeedbackInteractor();

        loadActualData(0);
    }

    @Override
    public void onGuiCreated(@NonNull IAnswerVKOfficialView view) {
        super.onGuiCreated(view);
        view.displayData(this.pages);
    }

    private boolean actualDataLoading;
    private CompositeDisposable actualDataDisposable = new CompositeDisposable();

    private void loadActualData(int offset) {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getOfficial(accountId, 100, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));
    }

    private void safelyMarkAsViewed() {
        final int accountId = super.getAccountId();

        appendDisposable(fInteractor.maskAaViewed(accountId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> callView(IAnswerVKOfficialView::notifyUpdateCounter), ignore()));
    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(int offset, AnswerVKOfficialList data) {

        this.actualDataLoading = false;
        this.endOfContent = (data.items.size() < 100);
        this.actualDataReceived = true;

        if (offset == 0) {
            safelyMarkAsViewed();
            this.pages.items.clear();
            this.pages.fields.clear();
            this.pages.items.addAll(data.items);
            this.pages.fields.addAll(data.fields);
            callView(IAnswerVKOfficialView::notifyDataSetChanged);
        } else {
            int startSize = this.pages.items.size();

            this.pages.items.addAll(data.items);
            this.pages.fields.addAll(data.fields);
            callView(view -> view.notifyDataAdded(startSize, data.items.size()));
        }

        resolveRefreshingView();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    private void resolveRefreshingView() {
        if (isGuiResumed()) {
            getView().showRefreshing(actualDataLoading);
        }
    }

    @Override
    public void onDestroyed() {
        actualDataDisposable.dispose();
        super.onDestroyed();
    }

    public boolean fireScrollToEnd() {
        if (!endOfContent && nonEmpty(pages.items) && actualDataReceived && !actualDataLoading) {
            loadActualData(this.pages.items.size());
            return false;
        }
        return true;
    }

    public void fireRefresh() {

        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        loadActualData(0);
    }
}
