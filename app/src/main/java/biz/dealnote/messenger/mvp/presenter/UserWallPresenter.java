package biz.dealnote.messenger.mvp.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.domain.IAccountsInteractor;
import biz.dealnote.messenger.domain.IFaveInteractor;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.IPhotosInteractor;
import biz.dealnote.messenger.domain.IRelationshipInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.fragment.friends.FriendsTabsFragment;
import biz.dealnote.messenger.model.FriendsCounters;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.PostFilter;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.model.UserDetails;
import biz.dealnote.messenger.model.criteria.WallCriteria;
import biz.dealnote.messenger.mvp.view.IUserWallView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.upload.IUploadManager;
import biz.dealnote.messenger.upload.Method;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.upload.UploadDestination;
import biz.dealnote.messenger.upload.UploadIntent;
import biz.dealnote.messenger.upload.UploadResult;
import biz.dealnote.messenger.util.Pair;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;

public class UserWallPresenter extends AbsWallPresenter<IUserWallView> {

    private final List<PostFilter> filters;
    private final IOwnersRepository ownersRepository;
    private final IRelationshipInteractor relationshipInteractor;
    private final IAccountsInteractor accountInteractor;
    private final IPhotosInteractor photosInteractor;
    private final IFaveInteractor faveInteractor;
    private final IUploadManager uploadManager;
    private User user;
    private UserDetails details;
    private boolean loadingAvatarPhotosNow;
    private Context context;

    public UserWallPresenter(int accountId, int ownerId, @Nullable User owner, Context context, @Nullable Bundle savedInstanceState) {
        super(accountId, ownerId, savedInstanceState);
        this.context = context;

        ownersRepository = Repository.INSTANCE.getOwners();
        relationshipInteractor = InteractorFactory.createRelationshipInteractor();
        accountInteractor = InteractorFactory.createAccountInteractor();
        photosInteractor = InteractorFactory.createPhotosInteractor();
        faveInteractor = InteractorFactory.createFaveInteractor();
        uploadManager = Injection.provideUploadManager();

        filters = new ArrayList<>();
        filters.addAll(createPostFilters());

        user = nonNull(owner) ? owner : new User(ownerId);
        details = new UserDetails();

        syncFiltersWithSelectedMode();
        syncFilterCountersWithDetails();

        refreshUserDetails();

        appendDisposable(uploadManager.observeResults()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onUploadFinished, RxUtils.ignore()));
    }

    public User getUser() {
        return user;
    }

    @Override
    protected void onRefresh() {
        requestActualFullInfo();
    }

    private void onUploadFinished(Pair<Upload, UploadResult<?>> pair) {
        UploadDestination destination = pair.getFirst().getDestination();
        if (destination.getMethod() == Method.PHOTO_TO_PROFILE && destination.getOwnerId() == ownerId) {
            requestActualFullInfo();

            if (isGuiResumed()) {
                Post post = (Post) pair.getSecond().getResult();
                getView().showAvatarUploadedMessage(getAccountId(), post);
            }
        }
    }

    @OnGuiCreated
    private void resolveCounters() {
        if (isGuiReady()) {
            getView().displayCounters(details.getFriendsCount(),
                    details.getFollowersCount(),
                    details.getGroupsCount(),
                    details.getPhotosCount(),
                    details.getAudiosCount(),
                    details.getVideosCount());
        }
    }

    @OnGuiCreated
    private void resolveBaseUserInfoViews() {
        if (isGuiReady()) {
            getView().displayBaseUserInfo(user);
        }
    }

    private void refreshUserDetails() {
        final int accountId = super.getAccountId();
        appendDisposable(ownersRepository.getFullUserInfo(accountId, ownerId, IOwnersRepository.MODE_CACHE)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> {
                    onFullInfoReceived(pair.getFirst(), pair.getSecond());
                    requestActualFullInfo();
                }, RxUtils.ignore()));
    }

    private void requestActualFullInfo() {
        final int accountId = super.getAccountId();
        appendDisposable(ownersRepository.getFullUserInfo(accountId, ownerId, IOwnersRepository.MODE_NET)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> onFullInfoReceived(pair.getFirst(), pair.getSecond()), this::onDetailsGetError));
    }

    private void onFullInfoReceived(User user, UserDetails details) {
        if (nonNull(user)) {
            this.user = user;
            onUserInfoUpdated();
        }

        if (nonNull(details)) {
            this.details = details;
            onUserDetalsUpdated();
        }

        resolveStatusView();
        resolveMenu();
    }

    private void onUserDetalsUpdated() {
        syncFilterCountersWithDetails();
        callView(IUserWallView::notifyWallFiltersChanged);

        resolvePrimaryActionButton();
        resolveCounters();
    }

    private void onUserInfoUpdated() {
        resolveBaseUserInfoViews();
    }

    private void onDetailsGetError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void syncFiltersWithSelectedMode() {
        for (PostFilter filter : filters) {
            filter.setActive(filter.getMode() == getWallFilter());
        }
    }

    private void syncFilterCountersWithDetails() {
        for (PostFilter filter : filters) {
            switch (filter.getMode()) {
                case WallCriteria.MODE_ALL:
                    filter.setCount(details.getAllWallCount());
                    break;
                case WallCriteria.MODE_OWNER:
                    filter.setCount(details.getOwnWallCount());
                    break;
                case WallCriteria.MODE_SCHEDULED:
                    filter.setCount(details.getPostponedWallCount());
                    break;
            }
        }
    }

    @Override
    public void onGuiCreated(@NonNull IUserWallView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayWallFilters(filters);
    }

    private List<PostFilter> createPostFilters() {
        List<PostFilter> filters = new ArrayList<>();
        filters.add(new PostFilter(WallCriteria.MODE_ALL, getString(R.string.all_posts)));
        filters.add(new PostFilter(WallCriteria.MODE_OWNER, getString(R.string.owner_s_posts)));

        if (isMyWall()) {
            filters.add(new PostFilter(WallCriteria.MODE_SCHEDULED, getString(R.string.scheduled)));
        }

        return filters;
    }

    public void fireStatusClick() {
        //if (nonNull(owner) && nonNull(owner.status_audio)) {
        //    getView().playAudioList(getAccountId(), 0, Utils.singletonArrayList(Dto2Model.transform(owner.status_audio)));
        //}
    }

    public void fireMoreInfoClick() {
        getView().openUserDetails(getAccountId(), user, details);
    }

    public void fireFilterClick(PostFilter entry) {
        if (changeWallFilter(entry.getMode())) {
            syncFiltersWithSelectedMode();

            getView().notifyWallFiltersChanged();
        }
    }

    public void fireHeaderPhotosClick() {
        getView().openPhotoAlbums(getAccountId(), ownerId, user);
    }

    public void fireHeaderAudiosClick() {
        getView().openAudios(getAccountId(), ownerId, user);
    }

    public void fireHeaderFriendsClick() {
        getView().openFriends(getAccountId(), ownerId, FriendsTabsFragment.TAB_ALL_FRIENDS, getFriendsCounters());
    }

    private FriendsCounters getFriendsCounters() {
        return new FriendsCounters(
                details.getFriendsCount(),
                details.getOnlineFriendsCount(),
                details.getFollowersCount(),
                details.getMutualFriendsCount()
        );
    }

    public void fireHeaderFollowersClick() {
        getView().openFriends(getAccountId(), ownerId, FriendsTabsFragment.TAB_FOLLOWERS, getFriendsCounters());
    }

    public void fireHeaderGroupsClick() {
        getView().openGroups(getAccountId(), ownerId, user);
    }

    public void fireHeaderVideosClick() {
        getView().openVideosLibrary(getAccountId(), ownerId, user);
    }

    @SuppressLint("ResourceType")
    @OnGuiCreated
    private void resolvePrimaryActionButton() {
        if (!isGuiReady()) return;

        @StringRes
        Integer title = null;
        if (super.getAccountId() == ownerId) {
            title = R.string.edit_status;
        } else {
            switch (user.getFriendStatus()) {
                case VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND:
                    title = R.string.add_to_friends;
                    break;
                case VKApiUser.FRIEND_STATUS_REQUEST_SENT:
                    title = R.string.cancel_request;
                    break;
                case VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST:
                    title = R.string.accept_request;
                    break;
                case VKApiUser.FRIEND_STATUS_IS_FRIEDND:
                    title = R.string.delete_from_friends;
                    break;
            }
        }

        getView().setupPrimaryActionButton(title);
    }

    public void firePrimaryActionsClick() {
        if (getAccountId() == ownerId) {
            getView().showEditStatusDialog(user.getStatus());
            return;
        }

        switch (user.getFriendStatus()) {
            case VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND:
                getView().showAddToFriendsMessageDialog();
                break;

            case VKApiUser.FRIEND_STATUS_REQUEST_SENT:
            case VKApiUser.FRIEND_STATUS_IS_FRIEDND:
                getView().showDeleteFromFriendsMessageDialog();
                break;

            case VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST:
                executeAddToFriendsRequest(null, false);
                break;
        }
    }

    private void openAvatarPhotoAlbum() {
        appendDisposable(photosInteractor.get(getAccountId(), ownerId, -6, 200, 0, true)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::DisplayUserProfileAlbum, tt -> showError(getView(), getCauseIfRuntime(tt))));
    }

    private void DisplayUserProfileAlbum(List<Photo> photos) {
        Integer currentAvatarPhotoId = nonNull(details) && nonNull(details.getPhotoId()) ? details.getPhotoId().getId() : null;
        Integer currentAvatarOwner_id = nonNull(details) && nonNull(details.getPhotoId()) ? details.getPhotoId().getOwnerId() : null;
        int sel = 0;
        if (currentAvatarPhotoId != null && currentAvatarOwner_id != null) {
            int ut = 0;
            for (Photo i : photos) {
                if (i.getOwnerId() == currentAvatarOwner_id && i.getId() == currentAvatarPhotoId) {
                    sel = ut;
                    break;
                }
                ut++;
            }
        }
        final int curr = sel;
        callView(view -> view.openPhotoAlbum(getAccountId(), ownerId, -6, new ArrayList<Photo>(photos), curr));
    }

    private void onAddFriendResult(int resultCode) {
        Integer strRes = null;
        Integer newFriendStatus = null;

        switch (resultCode) {
            case IRelationshipInteractor.FRIEND_ADD_REQUEST_SENT:
                strRes = R.string.friend_request_sent;
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT;
                break;

            case IRelationshipInteractor.FRIEND_ADD_REQUEST_FROM_USER_APPROVED:
                strRes = R.string.friend_request_from_user_approved;
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_FRIEDND;
                break;

            case IRelationshipInteractor.FRIEND_ADD_RESENDING:
                strRes = R.string.request_resending;
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT;
                break;
        }

        if (nonNull(newFriendStatus)) {
            user.setFriendStatus(newFriendStatus);
        }

        if (nonNull(strRes) && isGuiReady()) {
            getView().showSnackbar(strRes, true);
        }

        resolvePrimaryActionButton();
    }

    public void fireDeleteFromFriends() {
        final int accountId = super.getAccountId();
        appendDisposable(relationshipInteractor.deleteFriends(accountId, ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onFriendsDeleteResult, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    public void fireNewStatusEntered(final String newValue) {
        final int accountId = super.getAccountId();
        appendDisposable(accountInteractor.changeStatus(accountId, newValue)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onStatusChanged(newValue), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onStatusChanged(String status) {
        this.user.setStatus(status);

        callView(view -> view.showSnackbar(R.string.status_was_changed, true));
        resolveStatusView();
    }

    @OnGuiCreated
    private void resolveStatusView() {
        if (isGuiReady()) {
            final String statusText;
            if (nonNull(this.details.getStatusAudio())) {
                statusText = this.details.getStatusAudio().getArtistAndTitle();
            } else {
                statusText = this.user.getStatus();
            }

            getView().displayUserStatus(statusText);
        }
    }

    @OnGuiCreated
    private void resolveMenu() {
        if (isGuiReady()) {
            getView().InvalidateOptionsMenu();
        }
    }

    public void fireAddToFrindsClick(String message) {
        executeAddToFriendsRequest(message, false);
    }

    public void fireAddToBookmarks() {
        final int accountId = super.getAccountId();
        appendDisposable(faveInteractor.addPage(accountId, ownerId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onUserAddedToBookmarks, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onUserAddedToBookmarks() {
        safeShowToast(getView(), R.string.success, false);
    }

    private void executeAddToFriendsRequest(String text, boolean follow) {
        final int accountId = super.getAccountId();

        appendDisposable(relationshipInteractor.addFriend(accountId, ownerId, text, follow)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onAddFriendResult, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onFriendsDeleteResult(int responseCode) {
        Integer strRes = null;
        Integer newFriendStatus = null;

        switch (responseCode) {
            case IRelationshipInteractor.DeletedCodes.FRIEND_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST;
                strRes = R.string.friend_deleted;
                break;

            case IRelationshipInteractor.DeletedCodes.OUT_REQUEST_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND;
                strRes = R.string.out_request_deleted;
                break;

            case IRelationshipInteractor.DeletedCodes.IN_REQUEST_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND;
                strRes = R.string.in_request_deleted;
                break;

            case IRelationshipInteractor.DeletedCodes.SUGGESTION_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND;
                strRes = R.string.suggestion_deleted;
                break;
        }

        if (newFriendStatus != null) {
            user.setFriendStatus(newFriendStatus);
        }

        if (nonNull(strRes) && isGuiReady()) {
            getView().showSnackbar(strRes, true);

            resolvePrimaryActionButton();
        }
    }

    private void prepareUserAvatarsAndShow() {
        setLoadingAvatarPhotosNow(true);

        final int accountId = super.getAccountId();

        appendDisposable(photosInteractor.get(accountId, ownerId, -6, 50, 0, true)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(photos -> onAvatarsAlbumPrepared(photos.size()), this::onAvatarAlbumPrepareFailed));
    }

    private void onAvatarAlbumPrepareFailed(Throwable t) {
        setLoadingAvatarPhotosNow(false);
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onAvatarsAlbumPrepared(int count) {
        setLoadingAvatarPhotosNow(false);

        if (count == 0) {
            callView(view -> view.showSnackbar(R.string.no_photos_found, true));
        } else {
            openAvatarPhotoAlbum();
        }
    }

    @OnGuiCreated
    private void resolveProgressDialogView() {
        if (isGuiReady()) {
            if (loadingAvatarPhotosNow) {
                getView().displayProgressDialog(R.string.please_wait, R.string.loading_owner_photo_album, false);
            } else {
                getView().dismissProgressDialog();
            }
        }
    }

    private void setLoadingAvatarPhotosNow(boolean loadingAvatarPhotosNow) {
        this.loadingAvatarPhotosNow = loadingAvatarPhotosNow;
        resolveProgressDialogView();
    }

    public void fireAvatarClick() {
        getView().showAvatarContextMenu(isMyWall());
    }

    public void fireOpenAvatarsPhotoAlbum() {
        prepareUserAvatarsAndShow();
    }

    public void fireAddToBlacklistClick() {
        final int accountId = super.getAccountId();

        appendDisposable(InteractorFactory.createAccountInteractor()
                .banUsers(accountId, Collections.singletonList(user))
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onBanComplete, this::onBanError));
    }

    public void fireMentions() {
        PlaceFactory.getMentionsPlace(getAccountId(), getOwnerId()).tryOpenWith(context);
    }

    public void fireReport() {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(context);
        alert.setTitle(R.string.report);
        CharSequence[] values = {"porn", "spam", "insult", "advertisement"};
        CharSequence[] items = {"Порнография", "Спам, Мошенничество", "Оскорбительное поведение", "Рекламная страница"};
        alert.setItems(items, (dialog, item) -> {
            String report = values[item].toString();
            appendDisposable(ownersRepository.report(getAccountId(), getOwnerId(), report, null)
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

    public void fireRemoveBlacklistClick() {
        final int accountId = super.getAccountId();

        appendDisposable(InteractorFactory.createAccountInteractor()
                .unbanUser(accountId, user.getId())
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onBanComplete, this::onBanError));
    }

    private void onBanError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onBanComplete() {
        onRefresh();
        getView().getPhoenixToast().showToast(R.string.success);
    }

    public void fireChatClick() {
        final int accountId = super.getAccountId();
        final Peer peer = new Peer(Peer.fromUserId(user.getId()))
                .setAvaUrl(user.getMaxSquareAvatar())
                .setTitle(user.getFullName());

        getView().openChatWith(accountId, accountId, peer);
    }

    public void fireNewAvatarPhotoSelected(LocalPhoto photo) {
        UploadIntent intent = new UploadIntent(getAccountId(), UploadDestination.forProfilePhoto(ownerId))
                .setAutoCommit(true)
                .setFileId(photo.getImageId())
                .setFileUri(photo.getFullImageUri())
                .setSize(Upload.IMAGE_SIZE_FULL);

        uploadManager.enqueue(Collections.singletonList(intent));
    }

    @Override
    public void searchStory(boolean ByName) {
        appendDisposable(ownersRepository.searchStory(getAccountId(), ByName ? user.getFullName() : null, ByName ? null : ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    if (!Utils.isEmpty(data)) {
                        stories.clear();
                        stories.addAll(data);
                        getView().updateStory(stories);
                    }
                }, t -> {
                }));
    }
}
