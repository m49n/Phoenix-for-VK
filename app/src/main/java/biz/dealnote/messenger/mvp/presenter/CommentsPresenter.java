package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.model.VKApiCommunity;
import biz.dealnote.messenger.db.AttachToType;
import biz.dealnote.messenger.domain.IAttachmentsRepository;
import biz.dealnote.messenger.domain.ICommentsInteractor;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.domain.impl.CommentsInteractor;
import biz.dealnote.messenger.exception.NotFoundException;
import biz.dealnote.messenger.model.Comment;
import biz.dealnote.messenger.model.CommentIntent;
import biz.dealnote.messenger.model.CommentUpdate;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.CommentedType;
import biz.dealnote.messenger.model.CommentsBundle;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.DraftComment;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.ICommentsView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.DisposableHolder;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.RxUtils.dummy;
import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;
import static biz.dealnote.messenger.util.Utils.trimmedNonEmpty;


public class CommentsPresenter extends PlaceSupportPresenter<ICommentsView> {

    private static final String TAG = CommentsPresenter.class.getSimpleName();
    private static final int COUNT = 20;
    private static final String REPLY_PATTERN = "[post%s|%s], ";
    private final Commented commented;
    private final IOwnersRepository ownersRepository;
    private final ICommentsInteractor interactor;
    private final List<Comment> data;
    private Integer focusToComment;
    private Integer CommentThread;
    private CommentedState commentedState;
    private int authorId;
    private Owner author;
    private boolean directionDesc;
    private Context context;
    private CompositeDisposable actualLoadingDisposable = new CompositeDisposable();
    private int loadingState;
    private int adminLevel;
    private String draftCommentBody;
    private int draftCommentAttachmentsCount;
    private Integer draftCommentId;
    private Comment replyTo;
    private DisposableHolder<Void> deepLookingHolder = new DisposableHolder<>();
    private boolean sendingNow;
    private Poll topicPoll;
    private boolean loadingAvailableAuthorsNow;
    private CompositeDisposable cacheLoadingDisposable = new CompositeDisposable();

    public CommentsPresenter(int accountId, Commented commented, Integer focusToComment, Context context, Integer CommentThread, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.authorId = accountId;
        this.ownersRepository = Repository.INSTANCE.getOwners();
        this.interactor = new CommentsInteractor(Injection.provideNetworkInterfaces(), Injection.provideStores(), Repository.INSTANCE.getOwners());
        this.commented = commented;
        this.focusToComment = focusToComment;
        this.context = context;
        this.directionDesc = Settings.get().other().isCommentsDesc();
        this.CommentThread = CommentThread;
        this.data = new ArrayList<>();

        if (Objects.isNull(focusToComment) && Objects.isNull(CommentThread)) {
            // если надо сфокусироваться на каком-то комментарии - не грузим из кэша
            loadCachedData();
        }

        IAttachmentsRepository attachmentsRepository = Injection.provideAttachmentsRepository();

        appendDisposable(attachmentsRepository
                .observeAdding()
                .filter(this::filterAttachmentEvent)
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onAttchmentAddEvent));

        appendDisposable(attachmentsRepository
                .observeRemoving()
                .filter(this::filterAttachmentEvent)
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onAttchmentRemoveEvent));

        appendDisposable(Injection.provideStores()
                .comments()
                .observeMinorUpdates()
                .filter(update -> update.getCommented().equals(commented))
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onCommentMinorUpdate, Analytics::logUnexpectedError));

        restoreDraftCommentSync();
        requestInitialData();
        loadAuthorData();
    }

    private static String buildReplyTextFor(Comment comment) {
        String name = comment.getFromId() > 0 ? ((User) comment.getAuthor()).getFirstName() : ((Community) comment.getAuthor()).getName();
        return String.format(REPLY_PATTERN, comment.getId(), name);
    }

    private void loadAuthorData() {
        final int accountId = super.getAccountId();

        appendDisposable(ownersRepository.getBaseOwnerInfo(accountId, authorId, IOwnersRepository.MODE_ANY)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onAuthorDataReceived, this::onAuthorDataGetError));
    }

    @OnGuiCreated
    private void resolveAuthorAvatarView() {
        if (isGuiReady()) {
            String avatarUrl = nonNull(author) ? (author instanceof User ? ((User) author).getPhoto50() : ((Community) author).getPhoto50()) : null;
            getView().displayAuthorAvatar(avatarUrl);
        }
    }

    private void onAuthorDataGetError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onAuthorDataReceived(Owner owner) {
        this.author = owner;
        resolveAuthorAvatarView();
    }

    private void onCommentMinorUpdate(CommentUpdate update) {
        for (int i = 0; i < data.size(); i++) {
            Comment comment = data.get(i);
            if (comment.getId() == update.getCommentId()) {
                applyUpdate(comment, update);

                if (isGuiReady()) {
                    getView().notifyItemChanged(i);
                }
                break;
            }
        }
    }

    private void applyUpdate(Comment comment, CommentUpdate update) {
        if (update.hasLikesUpdate()) {
            comment.setLikesCount(update.getLikeUpdate().getCount());
            comment.setUserLikes(update.getLikeUpdate().isUserLikes());
        }

        if (update.hasDeleteUpdate()) {
            comment.setDeleted(update.getDeleteUpdate().isDeleted());
        }
    }

    @SuppressWarnings("unused")
    private void onAttchmentRemoveEvent(IAttachmentsRepository.IRemoveEvent event) {
        draftCommentAttachmentsCount--;
        onAttachmentCountChanged();
    }

    private void onAttachmentCountChanged() {
        resolveSendButtonAvailability();
        resolveAttachmentsCounter();
    }

    private void onAttchmentAddEvent(IAttachmentsRepository.IAddEvent event) {
        draftCommentAttachmentsCount = draftCommentAttachmentsCount + event.getAttachments().size();
        onAttachmentCountChanged();
    }

    private boolean filterAttachmentEvent(IAttachmentsRepository.IBaseEvent event) {
        return nonNull(draftCommentId)
                && event.getAttachToType() == AttachToType.COMMENT
                && event.getAccountId() == getAccountId()
                && event.getAttachToId() == draftCommentId;
    }

    private void restoreDraftCommentSync() {
        DraftComment draft = interactor.restoreDraftComment(getAccountId(), commented)
                .blockingGet();

        if (nonNull(draft)) {
            this.draftCommentBody = draft.getBody();
            this.draftCommentAttachmentsCount = draft.getAttachmentsCount();
            this.draftCommentId = draft.getId();
        }
    }

    private void requestInitialData() {
        final int accountId = super.getAccountId();

        Single<CommentsBundle> single;
        if (nonNull(focusToComment)) {
            single = interactor.getCommentsPortion(accountId, commented, -10, COUNT, focusToComment, CommentThread, true, "asc");
        } else if (directionDesc) {
            single = interactor.getCommentsPortion(accountId, commented, 0, COUNT, null, CommentThread, true, "desc");
        } else {
            single = interactor.getCommentsPortion(accountId, commented, 0, COUNT, null, CommentThread, true, "asc");
        }

        setLoadingState(LoadingState.INITIAL);
        actualLoadingDisposable.add(single
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onInitialDataReceived, this::onInitialDataError));
    }

    private void onInitialDataError(Throwable throwable) {
        setLoadingState(LoadingState.NO);
        showError(getView(), getCauseIfRuntime(throwable));
    }

    private void loadUp() {
        if (loadingState != LoadingState.NO) return;

        Comment first = getFirstCommentInList();
        if (isNull(first)) return;

        final int accountId = super.getAccountId();

        setLoadingState(LoadingState.UP);
        actualLoadingDisposable.add(interactor.getCommentsPortion(accountId, commented, 1, COUNT, first.getId(), CommentThread, false, "desc")
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCommentsPortionPortionReceived,
                        throwable -> onCommentPortionError(getCauseIfRuntime(throwable))));
    }

    private void loadDown() {
        if (loadingState != LoadingState.NO) return;

        Comment last = getLastCommentInList();
        if (isNull(last)) return;

        final int accountId = super.getAccountId();

        setLoadingState(LoadingState.DOWN);
        actualLoadingDisposable.add(interactor.getCommentsPortion(accountId, commented, 0, COUNT, last.getId(), CommentThread, false, "asc")
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCommentsPortionPortionReceived,
                        throwable -> onCommentPortionError(getCauseIfRuntime(throwable))));
    }

    private void onCommentPortionError(Throwable throwable) {
        setLoadingState(LoadingState.NO);
        showError(getView(), throwable);
    }

    private void onCommentsPortionPortionReceived(CommentsBundle bundle) {
        cacheLoadingDisposable.clear();

        List<Comment> comments = bundle.getComments();

        switch (loadingState) {
            case LoadingState.UP:
                data.addAll(comments);
                callView(view -> view.notifyDataAddedToTop(comments.size()));
                break;

            case LoadingState.DOWN:
                if (nonEmpty(comments)) {
                    comments.remove(comments.size() - 1); // последним комментарием приходит комментарий к кодом, который был передан в startCommentId
                }

                if (nonEmpty(comments)) {
                    data.addAll(0, comments);
                    callView(view -> view.notifyDataAddedToBottom(comments.size()));
                }
                break;
        }

        commentedState = new CommentedState(bundle.getFirstCommentId(), bundle.getLastCommentId());
        updateAdminLevel(nonNull(bundle.getAdminLevel()) ? bundle.getAdminLevel() : 0);

        setLoadingState(LoadingState.NO);
    }

    private void updateAdminLevel(int newValue) {
        this.adminLevel = newValue;
        resolveCanSendAsAdminView();
    }

    private boolean canDelete(Comment comment) {
        int currentSessionUserId = super.getAccountId();

        Owner author = comment.getAuthor();

        // если комментарий от имени сообщества и я админ или модератор, то могу удалить
        if (author instanceof Community && ((Community) author).getAdminLevel() >= VKApiCommunity.AdminLevel.MODERATOR) {
            return true;
        }

        return comment.getFromId() == currentSessionUserId
                || commented.getSourceOwnerId() == currentSessionUserId
                || adminLevel >= VKApiCommunity.AdminLevel.MODERATOR;
    }

    private boolean canEdit(Comment comment) {
        // нельзя редактировать комментарий со стикером
        return !comment.hasStickerOnly() && comment.isCanEdit();

        /*int myUserId = getAccountId();

        if (isTopic()) {
            // если я одмен или автор коммента в топике - я могу
            // редактировать в любое время
            return myUserId == comment.getFromId() || adminLevel == VKApiCommunity.AdminLevel.ADMIN;
        } else {
            // в обратном случае у меня есть только 24 часа
            // и я должен быть автором либо админом
            boolean canEditAsAdmin = ownerIsCommunity() && comment.getFromId() == commented.getSourceOwnerId() && adminLevel == VKApiCommunity.AdminLevel.ADMIN;
            boolean canPotencialEdit = myUserId == comment.getFromId() || canEditAsAdmin;

            long currentUnixtime = new Date().getTime() / 1000;
            long max24 = 24 * 60 * 60;
            return canPotencialEdit && (currentUnixtime - comment.getDate()) < max24;
        }*/
    }

    private void setLoadingState(int loadingState) {
        this.loadingState = loadingState;

        resolveEmptyTextVisibility();
        resolveHeaderFooterViews();
        resolveCenterProgressView();
    }

    @OnGuiCreated
    private void resolveEmptyTextVisibility() {
        if (isGuiReady()) {
            getView().setEpmtyTextVisible(loadingState == LoadingState.NO && data.isEmpty());
        }
    }

    @OnGuiCreated
    private void resolveHeaderFooterViews() {
        if (isGuiReady()) {
            if (data.isEmpty()) {
                // если комментариев к этому обьекту нет, то делать хидеры невидимыми
                getView().setupLoadUpHeader(LoadMoreState.INVISIBLE);
                getView().setupLoadDownFooter(LoadMoreState.INVISIBLE);
                return;
            }

            boolean lastResponseAvailable = nonNull(commentedState);

            if (!lastResponseAvailable) {
                // если мы еще не получили с сервера информацию о количестве комеентов, то делать хидеры невидимыми
                getView().setupLoadUpHeader(LoadMoreState.END_OF_LIST);
                getView().setupLoadDownFooter(LoadMoreState.END_OF_LIST);
                return;
            }

            switch (loadingState) {
                case LoadingState.NO:
                    getView().setupLoadUpHeader(isCommentsAvailableUp() ? LoadMoreState.CAN_LOAD_MORE : LoadMoreState.END_OF_LIST);
                    getView().setupLoadDownFooter(isCommentsAvailableDown() ? LoadMoreState.CAN_LOAD_MORE : LoadMoreState.END_OF_LIST);
                    break;

                case LoadingState.DOWN:
                    getView().setupLoadDownFooter(LoadMoreState.LOADING);
                    getView().setupLoadUpHeader(LoadMoreState.END_OF_LIST);
                    break;

                case LoadingState.UP:
                    getView().setupLoadDownFooter(LoadMoreState.END_OF_LIST);
                    getView().setupLoadUpHeader(LoadMoreState.LOADING);
                    break;

                case LoadingState.INITIAL:
                    getView().setupLoadDownFooter(LoadMoreState.END_OF_LIST);
                    getView().setupLoadUpHeader(LoadMoreState.END_OF_LIST);
                    break;
            }
        }
    }

    private boolean isCommentsAvailableUp() {
        if (isNull(commentedState) || isNull(commentedState.firstCommentId)) {
            return false;
        }

        Comment fisrt = getFirstCommentInList();
        return nonNull(fisrt) && fisrt.getId() > commentedState.firstCommentId;

    }

    private boolean isCommentsAvailableDown() {
        if (isNull(commentedState) || isNull(commentedState.lastCommentId)) {
            return false;
        }

        Comment last = getLastCommentInList();
        return nonNull(last) && last.getId() < commentedState.lastCommentId;
    }

    @Nullable
    private Comment getFirstCommentInList() {
        return nonEmpty(data) ? data.get(data.size() - 1) : null;
    }

    @OnGuiCreated
    private void resolveCenterProgressView() {
        if (isGuiReady()) {
            getView().setCenterProgressVisible(loadingState == LoadingState.INITIAL && data.isEmpty());
        }
    }

    @Nullable
    private Comment getLastCommentInList() {
        return nonEmpty(data) ? data.get(0) : null;
    }

    @OnGuiCreated
    private void resolveBodyView() {
        if (isGuiReady()) {
            getView().displayBody(draftCommentBody);
        }
    }

    private boolean canSendComment() {
        return draftCommentAttachmentsCount > 0 || trimmedNonEmpty(draftCommentBody);
    }

    @OnGuiCreated
    private void resolveSendButtonAvailability() {
        if (isGuiReady()) {
            getView().setButtonSendAvailable(canSendComment());
        }
    }

    private Single<Integer> saveSingle() {
        final int accountId = super.getAccountId();
        final int replyToComment = nonNull(replyTo) ? replyTo.getId() : 0;
        final int replyToUser = nonNull(replyTo) ? replyTo.getFromId() : 0;
        return interactor.safeDraftComment(accountId, commented, draftCommentBody, replyToComment, replyToUser);
    }

    private Integer saveDraftSync() {
        return saveSingle().blockingGet();
    }

    @OnGuiCreated
    private void resolveAttachmentsCounter() {
        if (isGuiReady()) {
            getView().displayAttachmentsCount(draftCommentAttachmentsCount);
        }
    }

    public void fireInputTextChanged(String s) {
        boolean canSend = canSendComment();

        this.draftCommentBody = s;

        if (canSend != canSendComment()) {
            resolveSendButtonAvailability();
        }
    }

    public void fireReplyToOwnerClick(int commentId) {
        for (int y = 0; y < data.size(); y++) {
            Comment comment = data.get(y);
            if (comment.getId() == commentId) {
                comment.setAnimationNow(true);

                getView().notifyItemChanged(y);
                getView().moveFocusTo(y, true);
                return;
            }
        }

        //safeShowToast(getView(), R.string.the_comment_is_not_in_the_list, false);

        startDeepCommentFinding(commentId);
    }

    private void startDeepCommentFinding(int commentId) {
        if (loadingState != LoadingState.NO) {
            // не грузить, если сейчас что-то грузится
            return;
        }

        Comment older = getFirstCommentInList();
        AssertUtils.requireNonNull(older);

        final int accountId = super.getAccountId();

        getView().displayDeepLookingCommentProgress();

        deepLookingHolder.append(interactor.getAllCommentsRange(accountId, commented, older.getId(), commentId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(comments -> onDeepCommentLoadingResponse(commentId, comments), this::onDeepCommentLoadingError));
    }

    private void onDeepCommentLoadingError(Throwable throwable) {
        getView().dismissDeepLookingCommentProgress();

        if (throwable instanceof NotFoundException) {
            safeShowToast(getView(), R.string.the_comment_is_not_in_the_list, false);
        } else {
            showError(getView(), throwable);
        }
    }

    @Override
    public void onGuiDestroyed() {
        deepLookingHolder.dispose();
        super.onGuiDestroyed();
    }

    public void fireDeepLookingCancelledByUser() {
        deepLookingHolder.dispose();
    }

    private void onDeepCommentLoadingResponse(int commentId, List<Comment> comments) {
        getView().dismissDeepLookingCommentProgress();

        this.data.addAll(comments);

        int index = 0;
        for (int i = 0; i < data.size(); i++) {
            Comment comment = data.get(i);

            if (comment.getId() == commentId) {
                index = i;
                comment.setAnimationNow(true);
                break;
            }
        }

        if (index == -1) {
            return;
        }

        callView(view -> view.notifyDataAddedToTop(comments.size()));

        int finalIndex = index;
        callView(view -> view.moveFocusTo(finalIndex, false));
    }

    public void fireAttachClick() {
        if (isNull(draftCommentId)) {
            draftCommentId = saveDraftSync();
        }

        final int accountId = super.getAccountId();
        getView().openAttachmentsManager(accountId, draftCommentId, commented.getSourceOwnerId(), draftCommentBody);
    }

    public void fireEditBodyResult(String newBody) {
        this.draftCommentBody = newBody;
        resolveSendButtonAvailability();
        resolveBodyView();
    }

    public void fireReplyToCommentClick(Comment comment) {
        if (commented.getSourceType() == CommentedType.TOPIC) {
            // в топиках механизм ответа отличается
            String replyText = buildReplyTextFor(comment);
            getView().replaceBodySelectionTextTo(replyText);
        } else {
            this.replyTo = comment;
            resolveReplyViews();
        }
    }

    public void fireReport(Comment comment) {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(context);
        alert.setTitle(R.string.report);
        CharSequence[] items = {"Спам", "Детская порнография", "Экстремизм", "Насилие", "Пропаганда наркотиков", "Материал для взрослых", "Оскорбление", "Призывы к суициду"};
        alert.setItems(items, (dialog, item) -> {
            appendDisposable(interactor.reportComment(getAccountId(), comment.getFromId(), comment.getId(), item)
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

    public void fireWhoLikesClick(Comment comment) {
        getView().goToLikes(getAccountId(), getApiCommentType(comment), commented.getSourceOwnerId(), comment.getId());
    }

    private String getApiCommentType(Comment comment) {
        switch (comment.getCommented().getSourceType()) {
            case CommentedType.PHOTO:
                return "photo_comment";
            case CommentedType.POST:
                return "comment";
            case CommentedType.VIDEO:
                return "video_comment";
            case CommentedType.TOPIC:
                return "topic_comment";
            default:
                throw new IllegalArgumentException();
        }
    }

    public void fireSendClick() {
        sendNormalComment();
    }

    private void sendNormalComment() {
        setSendingNow(true);

        final int accountId = super.getAccountId();
        final CommentIntent intent = createCommentIntent();
        if (intent.getReplyToComment() == null && CommentThread != null)
            intent.setReplyToComment(CommentThread);

        appendDisposable(interactor.send(accountId, commented, CommentThread, intent)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onNormalSendResponse, this::onSendError));
    }

    private void sendQuickComment(CommentIntent intent) {
        setSendingNow(true);

        if (intent.getReplyToComment() == null && CommentThread != null)
            intent.setReplyToComment(CommentThread);

        final int accountId = super.getAccountId();
        appendDisposable(interactor.send(accountId, commented, CommentThread, intent)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onQuickSendResponse, this::onSendError));
    }

    private void onSendError(Throwable t) {
        setSendingNow(false);
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onQuickSendResponse(Comment comment) {
        setSendingNow(false);

        handleCommentAdded(comment);

        this.replyTo = null;

        resolveReplyViews();
        resolveEmptyTextVisibility();
    }

    private void handleCommentAdded(Comment comment) {
        callView(ICommentsView::showCommentSentToast);
        fireRefreshClick();
    }

    private void handleCommentAddedNotFixable(Comment comment) {
        boolean canAdd;

        if (isNull(commentedState)) {
            canAdd = false;
        } else if (data.isEmpty()) {
            canAdd = true;
        } else if (isNull(commentedState.lastCommentId)) {
            canAdd = true;
        } else {
            Comment last = data.get(0);
            canAdd = last.getId() == commentedState.lastCommentId;
        }

        if (canAdd) {
            data.add(0, comment);
            commentedState.lastCommentId = comment.getId();
            callView(view -> view.notifyDataAddedToBottom(1));
        } else {
            callView(ICommentsView::showCommentSentToast);
        }
    }

    private void onNormalSendResponse(Comment comment) {
        setSendingNow(false);

        handleCommentAdded(comment);

        this.draftCommentAttachmentsCount = 0;
        this.draftCommentBody = null;
        this.draftCommentId = null;
        this.replyTo = null;

        resolveAttachmentsCounter();
        resolveBodyView();
        resolveReplyViews();
        resolveSendButtonAvailability();
        resolveEmptyTextVisibility();
    }

    private CommentIntent createCommentIntent() {
        final Integer replyToComment = isNull(replyTo) ? null : replyTo.getId();
        final String body = this.draftCommentBody;

        return new CommentIntent(authorId)
                .setMessage(body)
                .setReplyToComment(replyToComment)
                .setDraftMessageId(draftCommentId);
    }

    private void setSendingNow(boolean sendingNow) {
        this.sendingNow = sendingNow;
        resolveProgressDialog();
    }

    @OnGuiCreated
    private void resolveProgressDialog() {
        if (isGuiReady()) {
            if (sendingNow) {
                getView().displayProgressDialog(R.string.please_wait, R.string.publication, false);
            } else if (loadingAvailableAuthorsNow) {
                getView().displayProgressDialog(R.string.please_wait, R.string.getting_list_loading_message, false);
            } else {
                getView().dismissProgressDialog();
            }
        }
    }

    public void fireCommentContextViewCreated(ICommentsView.ICommentContextView view, Comment comment) {
        view.setCanDelete(canDelete(comment));
        view.setCanEdit(canEdit(comment));
        view.setCanBan(canBanAuthor(comment));
    }

    private boolean canBanAuthor(Comment comment) {
        return comment.getFromId() > 0 // только пользователей
                && comment.getFromId() != getAccountId() // не блокируем себя
                && adminLevel >= VKApiCommunity.AdminLevel.MODERATOR; // только если я модератор и выше
    }

    public void fireCommentDeleteClick(Comment comment) {
        deleteRestoreInternal(comment.getId(), true);
    }

    private void deleteRestoreInternal(int commentId, boolean delete) {
        int accountId = super.getAccountId();
        appendDisposable(interactor.deleteRestore(accountId, commented, commentId, delete)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(dummy(), t -> showError(getView(), t)));
    }

    public void fireCommentEditClick(Comment comment) {
        final int accountId = super.getAccountId();
        getView().goToCommentEdit(accountId, comment, CommentThread);
    }

    public void fireCommentLikeClick(Comment comment, boolean add) {
        likeInternal(add, comment);
    }

    private void likeInternal(boolean add, Comment comment) {
        final int accountId = super.getAccountId();

        appendDisposable(interactor.like(accountId, comment.getCommented(), comment.getId(), add)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(dummy(), t -> showError(getView(), t)));
    }

    public void fireCommentRestoreClick(int commentId) {
        deleteRestoreInternal(commentId, false);
    }

    public void fireStickerClick(Sticker sticker) {
        CommentIntent intent = new CommentIntent(authorId)
                .setReplyToComment(isNull(replyTo) ? null : replyTo.getId())
                .setStickerId(sticker.getId());

        sendQuickComment(intent);
    }

    public void fireGotoSourceClick() {
        switch (commented.getSourceType()) {
            case CommentedType.PHOTO:
                Photo photo = new Photo()
                        .setOwnerId(commented.getSourceOwnerId())
                        .setId(commented.getSourceId())
                        .setAccessKey(commented.getAccessKey());

                super.firePhotoClick(Utils.singletonArrayList(photo), 0);
                break;

            case CommentedType.POST:
                getView().goToWallPost(getAccountId(), commented.getSourceId(), commented.getSourceOwnerId());
                break;

            case CommentedType.VIDEO:
                getView().goToVideoPreview(getAccountId(), commented.getSourceId(), commented.getSourceId());
                break;

            case CommentedType.TOPIC:
                // not supported
                break;
        }
    }

    public void fireTopicPollClick() {
        super.firePollClick(topicPoll);
    }

    public void fireRefreshClick() {
        if (loadingState != LoadingState.INITIAL) {
            actualLoadingDisposable.clear();
            requestInitialData();
        }
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveOptionMenu();
    }

    private void resolveOptionMenu() {
        if (isGuiResumed()) {
            boolean hasPoll = nonNull(topicPoll);
            boolean hasGotoSource = commented.getSourceType() != CommentedType.TOPIC;

            @StringRes
            Integer gotoSourceText = null;
            if (hasGotoSource) {
                switch (commented.getSourceType()) {
                    case CommentedType.PHOTO:
                        gotoSourceText = R.string.go_to_photo;
                        break;
                    case CommentedType.VIDEO:
                        gotoSourceText = R.string.go_to_video;
                        break;
                    case CommentedType.POST:
                        gotoSourceText = R.string.go_to_post;
                        break;
                    case CommentedType.TOPIC:
                        // not supported
                        break;
                }
            }

            getView().setupOptionMenu(hasPoll, hasGotoSource, gotoSourceText);
        }
    }

    public void fireCommentEditResult(Comment comment) {
        if (this.commented.equals(comment.getCommented())) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getId() == comment.getId()) {
                    data.set(i, comment);

                    if (isGuiReady()) {
                        getView().notifyItemChanged(i);
                    }

                    break;
                }
            }
        }
    }

    public void fireBanClick(Comment comment) {
        final User user = (User) comment.getAuthor();
        final int groupId = Math.abs(commented.getSourceOwnerId());

        getView().banUser(getAccountId(), groupId, user);
    }

    private void setLoadingAvailableAuthorsNow(boolean loadingAvailableAuthorsNow) {
        this.loadingAvailableAuthorsNow = loadingAvailableAuthorsNow;
        resolveProgressDialog();
    }

    public void fireSendLongClick() {
        setLoadingAvailableAuthorsNow(true);

        final int accountId = super.getAccountId();

        boolean canSendFromAnyGroup = commented.getSourceType() == CommentedType.POST;

        Single<List<Owner>> single;

        if (canSendFromAnyGroup) {
            single = interactor.getAvailableAuthors(accountId);
        } else {
            Set<Integer> ids = new HashSet<>();
            ids.add(accountId);
            ids.add(commented.getSourceOwnerId());
            single = ownersRepository.findBaseOwnersDataAsList(accountId, ids, IOwnersRepository.MODE_ANY);
        }

        appendDisposable(single
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onAvailableAuthorsReceived, throwable -> onAvailableAuthorsGetError(getCauseIfRuntime(throwable))));
    }

    private void onAvailableAuthorsGetError(Throwable throwable) {
        setLoadingAvailableAuthorsNow(false);
    }

    private void onAvailableAuthorsReceived(List<Owner> owners) {
        setLoadingAvailableAuthorsNow(false);
        callView(view -> view.showAuthorSelectDialog(owners));
    }

    public void fireAuthorSelected(Owner owner) {
        this.author = owner;
        this.authorId = owner.getOwnerId();
        resolveAuthorAvatarView();
    }

    public void fireDirectionChanged() {
        this.data.clear();
        getView().notifyDataSetChanged();

        this.directionDesc = Settings.get().other().isCommentsDesc();
        requestInitialData();
    }

    @OnGuiCreated
    private void checkFocusToCommentDone() {
        if (isGuiReady() && nonNull(focusToComment)) {
            for (int i = 0; i < data.size(); i++) {
                Comment comment = data.get(i);
                if (comment.getId() == focusToComment) {
                    comment.setAnimationNow(true);
                    focusToComment = null;
                    getView().moveFocusTo(i, false);
                    break;
                }
            }
        }
    }

    private void onInitialDataReceived(CommentsBundle bundle) {
        // отменяем загрузку из БД если активна
        cacheLoadingDisposable.clear();

        data.clear();
        data.addAll(bundle.getComments());

        commentedState = new CommentedState(bundle.getFirstCommentId(), bundle.getLastCommentId());
        updateAdminLevel(nonNull(bundle.getAdminLevel()) ? bundle.getAdminLevel() : 0);

        // init poll once
        topicPoll = bundle.getTopicPoll();

        setLoadingState(LoadingState.NO);

        if (isGuiReady()) {
            getView().notifyDataSetChanged();
        }

        if (nonNull(focusToComment)) {
            checkFocusToCommentDone();
        } else if (!directionDesc) {
            callView(view -> view.scrollToPosition(data.size() - 1));
        }

        resolveOptionMenu();
        resolveHeaderFooterViews();
    }

    private void loadCachedData() {
        final int accountId = super.getAccountId();
        cacheLoadingDisposable.add(interactor.getAllCachedData(accountId, commented)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCachedDataReceived, ignore()));
    }

    @OnGuiCreated
    private void resolveCanSendAsAdminView() {
        if (isGuiReady()) {
            getView().setCanSendSelectAuthor(commented.getSourceType() == CommentedType.POST || adminLevel >= VKApiCommunity.AdminLevel.MODERATOR);
        }
    }

    @Override
    public void onGuiCreated(@NonNull ICommentsView view) {
        super.onGuiCreated(view);

        view.displayData(data);
        view.setToolbarTitle(getString(R.string.comments));

        switch (commented.getSourceType()) {
            case CommentedType.POST:
                view.setToolbarSubtitle(getString(R.string.for_wall_post));
                break;
            case CommentedType.PHOTO:
                view.setToolbarSubtitle(getString(R.string.for_photo));
                break;
            case CommentedType.VIDEO:
                view.setToolbarSubtitle(getString(R.string.for_video));
                break;
            case CommentedType.TOPIC:
                view.setToolbarSubtitle(getString(R.string.for_topic));
                break;
        }
    }

    private void onCachedDataReceived(List<Comment> comments) {
        data.clear();
        data.addAll(comments);

        resolveHeaderFooterViews();
        resolveEmptyTextVisibility();
        resolveCenterProgressView();

        if (isGuiReady()) {
            getView().notifyDataSetChanged();

            if (!directionDesc) {
                getView().scrollToPosition(data.size() - 1);
            }
        }
    }

    @Override
    public void onDestroyed() {
        cacheLoadingDisposable.dispose();
        actualLoadingDisposable.dispose();
        deepLookingHolder.dispose();

        // save draft async
        saveSingle().subscribeOn(Schedulers.io()).subscribe(ignore(), ignore());
        super.onDestroyed();
    }

    @OnGuiCreated
    private void resolveReplyViews() {
        if (isGuiReady()) {
            getView().setupReplyViews(nonNull(replyTo) ? replyTo.getFullAuthorName() : null);
        }
    }

    public void fireReplyCancelClick() {
        this.replyTo = null;
        resolveReplyViews();
    }

    public void fireUpLoadMoreClick() {
        loadUp();
    }

    public void fireDownLoadMoreClick() {
        loadDown();
    }

    public void fireScrollToTop() {
        if (isCommentsAvailableUp()) {
            loadUp();
        }
    }

    private static class CommentedState {

        Integer firstCommentId;
        Integer lastCommentId;

        CommentedState(Integer firstCommentId, Integer lastCommentId) {
            this.firstCommentId = firstCommentId;
            this.lastCommentId = lastCommentId;
        }
    }

    private static final class LoadingState {
        static final int NO = 0;
        static final int INITIAL = 1;
        static final int UP = 2;
        static final int DOWN = 3;
    }
}