package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IFeedbackInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.AnswerVKOfficial;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAnswerVKOfficialView;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

/**
 * Created by Ruslan Kolbasa on 11.09.2017.
 * phoenix
 */
public class AnswerVKOfficialPresenter extends AccountDependencyPresenter<IAnswerVKOfficialView> {

    private final List<AnswerVKOfficial> pages;

    private final IFeedbackInteractor fInteractor;

    private boolean actualDataReceived;

    private boolean endOfContent;

    public AnswerVKOfficialPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.pages = new ArrayList<>();
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

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(int offset, List<AnswerVKOfficial> data) {

        this.actualDataLoading = false;
        this.endOfContent = (data.size() < 100);
        this.actualDataReceived = true;

        if (offset == 0) {
            this.pages.clear();
            this.pages.addAll(data);
            callView(IAnswerVKOfficialView::notifyDataSetChanged);
        } else {
            int startSize = this.pages.size();
            this.pages.addAll(data);
            callView(view -> view.notifyDataAdded(startSize, data.size()));
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
        if (!endOfContent && nonEmpty(pages) && actualDataReceived && !actualDataLoading) {
            loadActualData(this.pages.size());
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
