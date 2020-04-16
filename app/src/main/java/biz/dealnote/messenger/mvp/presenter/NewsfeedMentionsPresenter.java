package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.INewsfeedInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Comment;
import biz.dealnote.messenger.model.NewsfeedComment;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.INewsfeedCommentsView;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.RxUtils;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;

/**
 * Created by admin on 08.05.2017.
 * phoenix
 */
public class NewsfeedMentionsPresenter extends PlaceSupportPresenter<INewsfeedCommentsView> {

    private static final String TAG = NewsfeedCommentsPresenter.class.getSimpleName();

    private final List<NewsfeedComment> data;
    private int ownerId;
    private boolean isEndOfContent;

    private final INewsfeedInteractor interactor;

    public NewsfeedMentionsPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.data = new ArrayList<>();
        this.interactor = InteractorFactory.createNewsfeedInteractor();
        this.ownerId = ownerId;

        loadAtLast();
    }

    private boolean loadingNow;

    private void setLoadingNow(boolean loadingNow) {
        this.loadingNow = loadingNow;
        resolveLoadingView();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveLoadingView();
    }

    private void resolveLoadingView(){
        if(isGuiResumed()){
            getView().showLoading(loadingNow);
        }
    }

    private void loadAtLast() {
        setLoadingNow(true);

        load(0);
    }

    private void load(int offset){
        appendDisposable(interactor.getMentions(getAccountId(), ownerId, 50, offset, null, null)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> onDataReceived(offset, pair.getFirst()), this::onRequestError));
    }

    private void onRequestError(Throwable throwable){
        showError(getView(), getCauseIfRuntime(throwable));
        setLoadingNow(false);
    }

    private void onDataReceived(int offset, List<NewsfeedComment> comments){
        setLoadingNow(false);

        isEndOfContent = comments.isEmpty();

        if(offset == 0){
            data.clear();
            data.addAll(comments);
            callView(INewsfeedCommentsView::notifyDataSetChanged);
        } else {
            int startCount = data.size();
            data.addAll(comments);
            callView(view -> view.notifyDataAdded(startCount, comments.size()));
        }
    }

    @Override
    public void onGuiCreated(@NonNull INewsfeedCommentsView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayData(data);
    }

    private boolean canLoadMore(){
        return !isEndOfContent && !loadingNow;
    }

    public void fireScrollToEnd() {
        if(canLoadMore()){
            load(this.data.size());
        }
    }

    public void fireRefresh() {
        if(loadingNow){
            return;
        }

        loadAtLast();
    }

    public void fireCommentBodyClick(NewsfeedComment newsfeedComment) {
        Comment comment = newsfeedComment.getComment();
        AssertUtils.requireNonNull(comment);
        
        getView().openComments(getAccountId(), comment.getCommented(), null);
    }
}
