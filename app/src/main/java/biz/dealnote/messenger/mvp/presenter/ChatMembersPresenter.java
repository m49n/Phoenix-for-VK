package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IMessagesRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.AppChatUser;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IChatMembersView;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;


public class ChatMembersPresenter extends AccountDependencyPresenter<IChatMembersView> {

    private final int chatId;

    private final IMessagesRepository messagesInteractor;

    private final List<AppChatUser> users;
    private boolean refreshing;

    public ChatMembersPresenter(int accountId, int chatId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.chatId = chatId;
        this.users = new ArrayList<>();
        this.messagesInteractor = Repository.INSTANCE.getMessages();

        requestData();
    }

    @Override
    public void onGuiCreated(@NonNull IChatMembersView view) {
        super.onGuiCreated(view);
        view.displayData(this.users);
    }

    private void resolveRefreshing() {
        if (isGuiResumed()) {
            getView().displayRefreshing(refreshing);
        }
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshing();
    }

    private void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
        resolveRefreshing();
    }

    private void requestData() {
        final int accountId = super.getAccountId();

        setRefreshing(true);
        appendDisposable(messagesInteractor.getChatUsers(accountId, chatId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onDataReceived, this::onDataGetError));
    }

    private void onDataGetError(Throwable t) {
        setRefreshing(false);
        showError(getView(), t);
    }

    private void onDataReceived(List<AppChatUser> users) {
        setRefreshing(false);

        this.users.clear();
        this.users.addAll(users);

        callView(IChatMembersView::notifyDataSetChanged);
    }

    public void fireRefresh() {
        if (!refreshing) {
            requestData();
        }
    }

    public void fireAddUserClick() {
        getView().startSelectUsersActivity(getAccountId());
    }

    public void fireUserDeteleConfirmed(AppChatUser user) {
        final int accountId = super.getAccountId();
        final int userId = user.getMember().getOwnerId();

        appendDisposable(messagesInteractor.removeChatMember(accountId, chatId, userId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onUserRemoved(userId), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onUserRemoved(int id) {
        int index = Utils.findIndexById(users, id);

        if (index != -1) {
            this.users.remove(index);
            callView(view -> view.notifyItemRemoved(index));
        }
    }

    public void fireUserSelected(ArrayList<User> users) {
        final int accountId = super.getAccountId();

        appendDisposable(messagesInteractor.addChatUsers(accountId, chatId, users)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onChatUsersAdded, this::onChatUsersAddError));
    }

    private void onChatUsersAddError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
        requestData(); // refresh data
    }

    private void onChatUsersAdded(List<AppChatUser> added) {
        int startSize = this.users.size();
        this.users.addAll(added);

        callView(view -> view.notifyDataAdded(startSize, added.size()));
    }

    public void fireUserClick(AppChatUser user) {
        getView().openUserWall(getAccountId(), user.getMember());
    }
}