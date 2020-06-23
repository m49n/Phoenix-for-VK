package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.db.model.PostUpdate;
import biz.dealnote.messenger.domain.IFaveInteractor;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.IWallsRepository;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.CommentedType;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.ParcelableOwnerWrapper;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.IWallPostView;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.Completable;

import static biz.dealnote.messenger.api.model.VkApiPostSource.Data.PROFILE_ACTIVITY;
import static biz.dealnote.messenger.api.model.VkApiPostSource.Data.PROFILE_PHOTO;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;


public class WallPostPresenter extends PlaceSupportPresenter<IWallPostView> {

    private static final String SAVE_POST = "save-post";
    private static final String SAVE_OWNER = "save-owner";

    private final int postId;
    private final int ownerId;
    private final IWallsRepository wallInteractor;
    private final IOwnersRepository ownersRepository;
    private Context context;
    private Post post;
    private Owner owner;
    private IFaveInteractor faveInteractor;
    private boolean loadingPostNow;

    public WallPostPresenter(int accountId, int postId, int ownerId, @Nullable Post post,
                             @Nullable Owner owner, Context context, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.postId = postId;
        this.ownerId = ownerId;
        this.ownersRepository = Repository.INSTANCE.getOwners();
        this.wallInteractor = Repository.INSTANCE.getWalls();
        this.faveInteractor = InteractorFactory.createFaveInteractor();

        this.context = context;

        if (nonNull(savedInstanceState)) {
            ParcelableOwnerWrapper wrapper = savedInstanceState.getParcelable(SAVE_OWNER);
            AssertUtils.requireNonNull(wrapper);

            this.post = savedInstanceState.getParcelable(SAVE_POST);
            this.owner = wrapper.get();
        } else {
            this.post = post;
            this.owner = owner;

            loadActualPostInfo();
        }

        loadOwnerInfoIfNeed();

        appendDisposable(wallInteractor.observeMinorChanges()
                .filter(event -> event.getOwnerId() == ownerId && event.getPostId() == postId)
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onPostUpdate));

        appendDisposable(wallInteractor.observeChanges()
                .filter(p -> postId == p.getVkid() && p.getOwnerId() == ownerId)
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onPostChanged));
    }

    private void onPostChanged(Post post) {
        this.post = post;

        resolveCommentsView();
        resolveLikesView();
        resolveToolbarView();
        resolveCommentsView();
        resolveRepostsView();
    }

    private void onPostUpdate(PostUpdate update) {
        if (isNull(this.post)) {
            return;
        }

        if (nonNull(update.getLikeUpdate())) {
            this.post.setLikesCount(update.getLikeUpdate().getCount());

            if (update.getAccountId() == getAccountId()) {
                this.post.setUserLikes(update.getLikeUpdate().isLiked());
            }

            resolveLikesView();
        }

        if (nonNull(update.getPinUpdate())) {
            this.post.setPinned(update.getPinUpdate().isPinned());
        }

        if (nonNull(update.getDeleteUpdate())) {
            this.post.setDeleted(update.getDeleteUpdate().isDeleted());
            resolveContentRootView();
        }
    }

    private void loadOwnerInfoIfNeed() {
        if (isNull(owner)) {
            final int accountId = super.getAccountId();
            appendDisposable(ownersRepository.getBaseOwnerInfo(accountId, ownerId, IOwnersRepository.MODE_NET)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(this::onOwnerInfoReceived, RxUtils.ignore()));
        }
    }

    private void onOwnerInfoReceived(Owner owner) {
        this.owner = owner;
    }

    private void setLoadingPostNow(boolean loadingPostNow) {
        this.loadingPostNow = loadingPostNow;
        resolveContentRootView();
    }

    private void loadActualPostInfo() {
        if (loadingPostNow) {
            return;
        }

        final int accountId = super.getAccountId();

        setLoadingPostNow(true);

        appendDisposable(wallInteractor.getById(accountId, ownerId, postId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onActualPostReceived, this::onLoadPostInfoError));
    }

    private void onLoadPostInfoError(Throwable t) {
        setLoadingPostNow(false);
        showError(getView(), t);
    }

    private void onActualPostReceived(Post post) {
        this.post = post;

        setLoadingPostNow(false);

        resolveToolbarView();
        resolveCommentsView();
        resolveLikesView();
        resolveRepostsView();
    }

    @OnGuiCreated
    private void resolveRepostsView() {
        if (isGuiReady() && nonNull(post)) {
            getView().displayReposts(post.getRepostCount(), post.isUserReposted());
        }
    }

    @OnGuiCreated
    private void resolveLikesView() {
        if (isGuiReady() && nonNull(post)) {
            getView().displayLikes(post.getLikesCount(), post.isUserLikes());
        }
    }

    @OnGuiCreated
    private void resolveCommentsView() {
        if (isGuiReady() && nonNull(post)) {
            getView().displayCommentCount(post.getCommentsCount());
            getView().setCommentButtonVisible(post.isCanPostComment() || post.getCommentsCount() > 0);
        }
    }

    @OnGuiCreated
    private void resolveContentRootView() {
        if (isGuiReady()) {
            if (nonNull(post)) {
                getView().displayPostInfo(post);
            } else if (loadingPostNow) {
                getView().displayLoading();
            } else {
                getView().displayLoadingFail();
            }
        }
    }

    @Override
    public void saveState(@NonNull Bundle outState) {
        super.saveState(outState);
        outState.putParcelable(SAVE_POST, post);
        outState.putParcelable(SAVE_OWNER, new ParcelableOwnerWrapper(owner));
    }

    public void fireOptionViewCreated(IWallPostView.IOptionView view) {
        view.setCanPin(nonNull(post) && !post.isPinned() && post.isCanPin() && !post.isDeleted());
        view.setCanUnpin(nonNull(post) && post.isPinned() && post.isCanPin() && !post.isDeleted());
        view.setCanDelete(canDelete());
        view.setCanRestore(nonNull(post) && post.isDeleted());
        view.setCanEdit(nonNull(post) && post.isCanEdit());
    }

    private boolean canDelete() {
        if (Objects.isNull(post) || post.isDeleted()) {
            return false;
        }

        final int accountId = super.getAccountId();

        boolean canDeleteAsAdmin = owner instanceof Community && ((Community) owner).isAdmin();
        boolean canDeleteAsOwner = ownerId == accountId || post.getAuthorId() == accountId;
        return canDeleteAsAdmin || canDeleteAsOwner;
    }

    public void fireGoToOwnerClick() {
        super.fireOwnerClick(ownerId);
    }

    public void firePostEditClick() {
        if (isNull(post)) {
            getView().showPostNotReadyToast();
            return;
        }

        getView().goToPostEditing(getAccountId(), post);
    }

    public void fireCommentClick() {
        final Commented commented = new Commented(postId, ownerId, CommentedType.POST, null);
        getView().openComments(getAccountId(), commented, null);
    }

    public void fireRepostLongClick() {
        getView().goToReposts(getAccountId(), "post", ownerId, postId);
    }

    public void fireLikeLongClick() {
        getView().goToLikes(getAccountId(), "post", ownerId, postId);
    }

    public void fireTryLoadAgainClick() {
        loadActualPostInfo();
    }

    public void fireShareClick() {
        if (nonNull(post)) {
            getView().repostPost(getAccountId(), post);
        } else {
            getView().showPostNotReadyToast();
        }
    }

    public void fireLikeClick() {
        if (nonNull(post)) {
            appendDisposable(wallInteractor.like(getAccountId(), ownerId, postId, !post.isUserLikes())
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(RxUtils.ignore(), t -> showError(getView(), t)));
        } else {
            getView().showPostNotReadyToast();
        }
    }

    public void fireAddBookmark() {
        appendDisposable(faveInteractor.addPost(getAccountId(), ownerId, postId, null)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onVideoAddedToBookmarks, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onVideoAddedToBookmarks() {
        if (isGuiReady())
            getView().showSuccessToast();
    }

    public void fireDeleteClick() {
        deleteOrRestore(true);
    }

    public void fireRestoreClick() {
        deleteOrRestore(false);
    }

    private void deleteOrRestore(boolean delete) {
        final int accountId = super.getAccountId();

        Completable completable = delete ? wallInteractor.delete(accountId, ownerId, postId)
                : wallInteractor.restore(accountId, ownerId, postId);

        appendDisposable(completable
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onDeleteOrRestoreComplete(delete), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onDeleteOrRestoreComplete(boolean deleted) {
        callView(view -> view.displayDeleteOrRestoreComplete(deleted));
    }

    public void firePinClick() {
        pinOrUnpin(true);
    }

    public void fireUnpinClick() {
        pinOrUnpin(false);
    }

    private void pinOrUnpin(boolean pin) {
        final int accountId = super.getAccountId();

        appendDisposable(wallInteractor.pinUnpin(accountId, ownerId, postId, pin)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onPinOrUnpinComplete(pin), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onPinOrUnpinComplete(boolean pinned) {
        callView(view -> view.displayPinComplete(pinned));
    }

    public void fireRefresh() {
        loadActualPostInfo();
    }

    public void fireCopyLinkClink() {
        String link = String.format("vk.com/wall%s_%s", ownerId, postId);
        getView().copyLinkToClipboard(link);
    }

    public void fireReport() {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(context);
        alert.setTitle(R.string.report);
        CharSequence[] items = {"Спам", "Детская порнография", "Экстремизм", "Насилие", "Пропаганда наркотиков", "Материал для взрослых", "Оскорбление", "Призывы к суициду"};
        alert.setItems(items, (dialog, item) -> {
            appendDisposable(wallInteractor.reportPost(getAccountId(), ownerId, postId, item)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(p -> {
                        if (p == 1)
                            getView().getPhoenixToast().showToast(R.string.success);
                        else
                            getView().getPhoenixToast().showToast(R.string.error);
                    }, t -> showError(getView(), getCauseIfRuntime(t))));
            dialog.dismiss();
        });
        alert.show();
    }

    @OnGuiCreated
    private void resolveToolbarView() {
        if (isGuiReady()) {
            if (nonNull(post)) {
                int type = IWallPostView.SUBTITLE_NORMAL;

                if (nonNull(post.getSource())) {
                    switch (post.getSource().getData()) {
                        case PROFILE_ACTIVITY:
                            type = IWallPostView.SUBTITLE_STATUS_UPDATE;
                            break;
                        case PROFILE_PHOTO:
                            type = IWallPostView.SUBTITLE_PHOTO_UPDATE;
                            break;
                    }
                }

                getView().displayToolbarTitle(post.getAuthorName());
                getView().displayToolbatSubtitle(type, post.getDate());
            } else {
                getView().displayDefaultToolbaTitle();
                getView().displayDefaultToolbaSubitle();
            }
        }
    }

    public void fireCopyTextClick() {
        if (isNull(post)) {
            getView().showPostNotReadyToast();
            return;
        }

        // Append post text
        StringBuilder builder = new StringBuilder();
        if (nonEmpty(post.getText())) {
            builder.append(post.getText()).append("\n");
        }

        // Append copies text if exists
        if (nonEmpty(post.getCopyHierarchy())) {
            for (Post copy : post.getCopyHierarchy()) {
                if (nonEmpty(copy.getText())) {
                    builder.append(copy.getText()).append("\n");
                }
            }
        }

        getView().copyTextToClipboard(builder.toString());
    }

    public void fireHasgTagClick(String hashTag) {
        getView().goToNewsSearch(getAccountId(), hashTag);
    }
}