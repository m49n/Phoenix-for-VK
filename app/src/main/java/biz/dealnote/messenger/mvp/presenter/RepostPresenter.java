package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IWallsRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.AttachmenEntry;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.mvp.view.IRepostView;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;


public class RepostPresenter extends AbsAttachmentsEditPresenter<IRepostView> {

    private final Post post;
    private final Integer targetGroupId;
    private final IWallsRepository walls;
    private boolean publishingNow;

    public RepostPresenter(int accountId, Post post, Integer targetGroupId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.walls = Repository.INSTANCE.getWalls();
        this.post = post;
        this.targetGroupId = targetGroupId;

        getData().add(new AttachmenEntry(false, post));
    }

    @Override
    public void onGuiCreated(@NonNull IRepostView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.setSupportedButtons(false, false, false, false, false, false);
    }

    @OnGuiCreated
    private void resolveProgressDialog() {
        if (isGuiReady()) {
            if (publishingNow) {
                getView().displayProgressDialog(R.string.please_wait, R.string.publication, false);
            } else {
                getView().dismissProgressDialog();
            }
        }
    }

    private void setPublishingNow(boolean publishingNow) {
        this.publishingNow = publishingNow;
        resolveProgressDialog();
    }

    private void onPublishError(Throwable throwable) {
        setPublishingNow(false);
        showError(getView(), throwable);
    }

    @SuppressWarnings("unused")
    private void onPublishComplete(Post post) {
        setPublishingNow(false);
        getView().goBack();
    }

    public final void fireReadyClick() {
        setPublishingNow(true);

        final int accountId = super.getAccountId();
        final String body = getTextBody();

        appendDisposable(walls.repost(accountId, post.getVkid(), post.getOwnerId(), targetGroupId, body)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onPublishComplete, this::onPublishError));
    }
}