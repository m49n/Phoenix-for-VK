package biz.dealnote.messenger.mvp.presenter;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IMessagesRepository;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.exception.UnauthorizedException;
import biz.dealnote.messenger.link.LinkHelper;
import biz.dealnote.messenger.longpoll.ILongpollManager;
import biz.dealnote.messenger.longpoll.LongpollInstance;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.Option;
import biz.dealnote.messenger.model.Dialog;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.PeerUpdate;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IDialogsView;
import biz.dealnote.messenger.settings.ISettings;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.Optional;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.ShortcutUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.RxUtils.dummy;
import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.indexOf;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.safeIsEmpty;


public class DialogsPresenter extends AccountDependencyPresenter<IDialogsView> {

    private static final int COUNT = 30;

    private static final String SAVE_DIALOGS_OWNER_ID = "save-dialogs-owner-id";
    private static final Comparator<Dialog> COMPARATOR = (rhs, lhs) -> Integer.compare(lhs.getLastMessageId(), rhs.getLastMessageId());
    private final ArrayList<Dialog> dialogs;
    private final IMessagesRepository messagesInteractor;
    private final ILongpollManager longpollManager;
    private int dialogsOwnerId;
    private boolean endOfContent;
    private int offset;
    private boolean netLoadnigNow;
    private CompositeDisposable netDisposable = new CompositeDisposable();
    private boolean cacheNowLoading;
    private CompositeDisposable cacheLoadingDisposable = new CompositeDisposable();

    public DialogsPresenter(int accountId, int initialDialogsOwnerId, int offset, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        setSupportAccountHotSwap(true);
        this.offset = offset;

        dialogs = new ArrayList<>();

        if (nonNull(savedInstanceState)) {
            dialogsOwnerId = savedInstanceState.getInt(SAVE_DIALOGS_OWNER_ID);
        } else {
            dialogsOwnerId = initialDialogsOwnerId;
        }

        messagesInteractor = Repository.INSTANCE.getMessages();
        longpollManager = LongpollInstance.get();

        appendDisposable(messagesInteractor
                .observePeerUpdates()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onPeerUpdate, ignore()));

        appendDisposable(messagesInteractor.observePeerDeleting()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(dialog -> onDialogDeleted(dialog.getAccountId(), dialog.getPeerId()), ignore()));

        appendDisposable(longpollManager.observeKeepAlive()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(ignore -> checkLongpoll(), ignore()));

        loadCachedThenActualData();
    }

    private static String getTitleIfEmpty(@NonNull Collection<User> users) {
        return Utils.join(users, ", ", User::getFirstName);
    }

    @Override
    public void saveState(@NonNull Bundle outState) {
        super.saveState(outState);
        outState.putInt(SAVE_DIALOGS_OWNER_ID, dialogsOwnerId);
    }

    @Override
    public void onGuiCreated(@NonNull IDialogsView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayData(dialogs);

        // only for user dialogs
        viewHost.setCreateGroupChatButtonVisible(dialogsOwnerId > 0);
    }

    private void onDialogsFisrtResponse(List<Dialog> data) {
        if (!Settings.get().other().isBe_online() || Settings.get().accounts().getType(getAccountId()).equals("hacked")) {
            netDisposable.add(Injection.provideNetworkInterfaces().vkDefault(dialogsOwnerId).account().setOffline()
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(t -> {
                    }, t -> {
                    }));
        }
        setNetLoadnigNow(false);

        endOfContent = false;
        dialogs.clear();
        dialogs.addAll(data);

        safeNotifyDataSetChanged();

        try {
            appendDisposable(InteractorFactory.createStickersInteractor()
                    .getAndStore(getAccountId())
                    .compose(RxUtils.applyCompletableIOToMainSchedulers())
                    .subscribe(dummy(), ignore()));
        } catch (Exception ignored) {
            /*ignore*/
        }

        if (offset > 0) {
            safeScroll(offset);
            offset = 0;
        }
    }

    public void fireDialogOptions(Context context, Option option) {
        switch (option.getId()) {
            case R.id.button_ok:
                appendDisposable(Injection.provideNetworkInterfaces().vkDefault(Settings.get().accounts().getCurrent()).account().setOffline()
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .subscribe(e -> OnSetOffline(context, e), t -> OnSetOffline(context, false)));
                break;
            case R.id.button_cancel:
                final ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipBoard != null && clipBoard.getPrimaryClip() != null && clipBoard.getPrimaryClip().getItemCount() > 0 && clipBoard.getPrimaryClip().getItemAt(0).getText() != null) {
                    String temp = clipBoard.getPrimaryClip().getItemAt(0).getText().toString();
                    LinkHelper.openUrl((Activity) context, getAccountId(), temp);
                }
                break;
        }
    }

    private void OnSetOffline(Context context, boolean succ) {
        if (succ)
            PhoenixToast.CreatePhoenixToast(context).showToast(R.string.succ_offline);
        else
            PhoenixToast.CreatePhoenixToast(context).showToastError(R.string.err_offline);
    }

    private void onDialogsGetError(Throwable t) {
        Throwable cause = getCauseIfRuntime(t);

        cause.printStackTrace();

        setNetLoadnigNow(false);

        if (cause instanceof UnauthorizedException) {
            return;
        }

        showError(getView(), cause);
    }

    private void setNetLoadnigNow(boolean netLoadnigNow) {
        this.netLoadnigNow = netLoadnigNow;
        resolveRefreshingView();
    }

    private void requestAtLast() {
        if (netLoadnigNow) {
            return;
        }

        setNetLoadnigNow(true);

        netDisposable.add(messagesInteractor.getDialogs(dialogsOwnerId, COUNT, null)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onDialogsFisrtResponse, this::onDialogsGetError));

        resolveRefreshingView();
    }

    private void requestNext() {
        if (netLoadnigNow) {
            return;
        }

        Integer lastMid = getLastDialogMessageId();
        if (isNull(lastMid)) {
            return;
        }

        setNetLoadnigNow(true);
        netDisposable.add(messagesInteractor.getDialogs(dialogsOwnerId, COUNT, lastMid)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onNextDialogsResponse,
                        throwable -> onDialogsGetError(getCauseIfRuntime(throwable))));
    }

    private void onNextDialogsResponse(List<Dialog> data) {
        if (!Settings.get().other().isBe_online() || Settings.get().accounts().getType(getAccountId()).equals("hacked")) {
            netDisposable.add(Injection.provideNetworkInterfaces().vkDefault(dialogsOwnerId).account().setOffline()
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(t -> {
                    }, t -> {
                    }));
        }

        setNetLoadnigNow(false);
        endOfContent = isEmpty(dialogs);

        int startSize = dialogs.size();
        dialogs.addAll(data);

        if (isGuiReady()) {
            getView().notifyDataAdded(startSize, data.size());
        }
    }

    private void onDialogRemovedSuccessfully(int accountId, int peeId) {
        callView(v -> v.showSnackbar(R.string.deleted, true));
        onDialogDeleted(accountId, peeId);
    }

    private void removeDialog(final int peeId) {
        final int accountId = dialogsOwnerId;

        appendDisposable(messagesInteractor.deleteDialog(accountId, peeId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onDialogRemovedSuccessfully(accountId, peeId), t -> showError(getView(), t)));
    }

    private void resolveRefreshingView() {
        // on resume only !!!
        if (isGuiResumed()) {
            getView().showRefreshing(cacheNowLoading || netLoadnigNow);
        }
    }

    private void loadCachedThenActualData() {
        cacheNowLoading = true;
        resolveRefreshingView();

        cacheLoadingDisposable.add(messagesInteractor.getCachedDialogs(dialogsOwnerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCachedDataReceived, ignored -> {
                    ignored.printStackTrace();
                    onCachedDataReceived(Collections.emptyList());
                }));
    }

    private void onCachedDataReceived(List<Dialog> data) {
        cacheNowLoading = false;

        dialogs.clear();
        dialogs.addAll(data);

        safeNotifyDataSetChanged();
        resolveRefreshingView();

        requestAtLast();
    }

    private void onPeerUpdate(List<PeerUpdate> updates) {
        for (PeerUpdate update : updates) {
            if (update.getAccountId() == dialogsOwnerId) {
                onDialogUpdate(update);
            }
        }
    }

    private void onDialogUpdate(PeerUpdate update) {
        if (dialogsOwnerId != update.getAccountId()) {
            return;
        }

        final int accountId = update.getAccountId();
        final int peerId = update.getPeerId();

        if (update.getLastMessage() != null) {
            List<Integer> id = Collections.singletonList(update.getLastMessage().getMessageId());
            appendDisposable(messagesInteractor.findCachedMessages(accountId, id)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(messages -> {
                        if (messages.isEmpty()) {
                            onDialogDeleted(accountId, peerId);
                        } else {
                            onActualMessagePeerMessageReceived(accountId, peerId, update, Optional.wrap(messages.get(0)));
                        }
                    }, ignore()));
        } else {
            onActualMessagePeerMessageReceived(accountId, peerId, update, Optional.empty());
        }
    }

    private void onActualMessagePeerMessageReceived(int accountId, int peerId, PeerUpdate update, Optional<Message> messageOptional) {
        if (accountId != dialogsOwnerId) {
            return;
        }

        int index = indexOf(dialogs, peerId);
        if (index != -1) {
            Dialog dialog = dialogs.get(index);

            if (update.getReadIn() != null) {
                dialog.setInRead(update.getReadIn().getMessageId());
            }

            if (update.getReadOut() != null) {
                dialog.setOutRead(update.getReadOut().getMessageId());
            }

            if (update.getUnread() != null) {
                dialog.setUnreadCount(update.getUnread().getCount());
            }

            if (messageOptional.nonEmpty()) {
                Message message = messageOptional.get();
                dialog.setLastMessageId(message.getId());
                dialog.setMessage(message);

                if (dialog.isChat()) {
                    dialog.setInterlocutor(message.getSender());
                }
            }

            if (update.getTitle() != null) {
                dialog.setTitle(update.getTitle().getTitle());
            }

            Collections.sort(dialogs, COMPARATOR);
        }

        safeNotifyDataSetChanged();
    }

    private void onDialogDeleted(int accountId, int peerId) {
        if (dialogsOwnerId != accountId) {
            return;
        }

        int index = indexOf(dialogs, peerId);
        if (index != -1) {
            dialogs.remove(index);
            safeNotifyDataSetChanged();
        }
    }

    private void safeNotifyDataSetChanged() {
        if (isGuiReady()) {
            getView().notifyDataSetChanged();
        }
    }

    private void safeScroll(int position) {
        if (isGuiReady()) {
            getView().scroll_pos(position);
        }
    }

    @Override
    public void onDestroyed() {
        cacheLoadingDisposable.dispose();
        netDisposable.dispose();
        super.onDestroyed();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
        checkLongpoll();
    }

    private void checkLongpoll() {
        if (isGuiResumed() && getAccountId() != ISettings.IAccountsSettings.INVALID_ID) {
            longpollManager.keepAlive(dialogsOwnerId);
        }
    }

    public void fireRefresh() {
        cacheLoadingDisposable.dispose();
        cacheNowLoading = false;

        netDisposable.clear();
        netLoadnigNow = false;

        requestAtLast();
    }

    public void fireSearchClick() {
        AssertUtils.assertPositive(dialogsOwnerId);
        getView().goToSearch(getAccountId());
    }

    public void fireDialogClick(Dialog dialog, int offset) {
        this.openChat(dialog, offset);
    }

    private void openChat(Dialog dialog, int offset) {
        getView().goToChat(getAccountId(),
                dialogsOwnerId,
                dialog.getPeerId(),
                dialog.getDisplayTitle(getApplicationContext()),
                dialog.getImageUrl(), offset);
    }

    public void fireDialogAvatarClick(Dialog dialog, int offset) {
        if (Peer.isUser(dialog.getPeerId()) || Peer.isGroup(dialog.getPeerId())) {
            getView().goToOwnerWall(super.getAccountId(), Peer.toOwnerId(dialog.getPeerId()), dialog.getInterlocutor());
        } else {
            this.openChat(dialog, offset);
        }
    }

    private boolean canLoadMore() {
        return !cacheNowLoading && !endOfContent && !netLoadnigNow && !dialogs.isEmpty();
    }

    public void fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext();
        }
    }

    private Integer getLastDialogMessageId() {
        try {
            return dialogs.get(dialogs.size() - 1).getLastMessageId();
        } catch (Exception e) {
            return null;
        }
    }

    public void fireNewGroupChatTitleEntered(List<User> users, String title) {
        final String targetTitle = safeIsEmpty(title) ? getTitleIfEmpty(users) : title;
        final int accountId = super.getAccountId();

        appendDisposable(messagesInteractor.createGroupChat(accountId, Utils.idsListOf(users), targetTitle)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(chatid -> onGroupChatCreated(chatid, targetTitle), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onGroupChatCreated(int chatId, String title) {
        callView(view -> view.goToChat(getAccountId(), dialogsOwnerId, Peer.fromChatId(chatId), title, null, 0));
    }

    public void fireUsersForChatSelected(@NonNull ArrayList<User> users) {
        if (users.size() == 1) {
            User user = users.get(0);
            // Post?
            getView().goToChat(getAccountId(), dialogsOwnerId, Peer.fromUserId(user.getId()), user.getFullName(), user.getMaxSquareAvatar(), 0);
        } else if (users.size() > 1) {
            getView().showEnterNewGroupChatTitle(users);
        }
    }

    public void fireRemoveDialogClick(Dialog dialog) {
        removeDialog(dialog.getPeerId());
    }

    public void fireCreateShortcutClick(Dialog dialog) {
        AssertUtils.assertPositive(dialogsOwnerId);

        final Context app = getApplicationContext();

        appendDisposable(ShortcutUtils
                .createChatShortcutRx(app, dialog.getImageUrl(), getAccountId(),
                        dialog.getPeerId(), dialog.getDisplayTitle(app))
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onShortcutCreated, throwable -> safeShowError(getView(), throwable.getMessage())));
    }

    private void onShortcutCreated() {
        if (isGuiReady()) {
            getView().showSnackbar(R.string.success, true);
        }
    }

    public void fireNotificationsSettingsClick(Dialog dialog) {
        AssertUtils.assertPositive(dialogsOwnerId);
        getView().showNotificationSettings(getAccountId(), dialog.getPeerId());
    }

    @Override
    protected void afterAccountChange(int oldAid, int newAid) {
        super.afterAccountChange(oldAid, newAid);

        // если на экране диалоги группы, то ничего не трогаем
        if (dialogsOwnerId < 0 && dialogsOwnerId != ISettings.IAccountsSettings.INVALID_ID) {
            return;
        }

        dialogsOwnerId = newAid;

        cacheLoadingDisposable.clear();
        cacheNowLoading = false;

        netDisposable.clear();
        netLoadnigNow = false;

        loadCachedThenActualData();

        longpollManager.forceDestroy(oldAid);
        checkLongpoll();
    }

    public void fireAddToLauncherShortcuts(Dialog dialog) {
        AssertUtils.assertPositive(dialogsOwnerId);

        Peer peer = new Peer(dialog.getId())
                .setAvaUrl(dialog.getImageUrl())
                .setTitle(dialog.getDisplayTitle(getApplicationContext()));

        Completable completable = ShortcutUtils.addDynamicShortcut(getApplicationContext(), dialogsOwnerId, peer);

        appendDisposable(completable
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> safeShowToast(getView(), R.string.success, false), Analytics::logUnexpectedError));
    }

    public void fireContextViewCreated(IDialogsView.IContextView contextView, final Dialog dialog) {
        boolean isHide = Settings.get().security().ContainsValueInSet(dialog.getId(), "hidden_dialogs");
        contextView.setCanDelete(true);
        contextView.setCanAddToHomescreen(dialogsOwnerId > 0 && !isHide);
        contextView.setCanAddToShortcuts(dialogsOwnerId > 0 && !isHide);
        contextView.setCanConfigNotifications(dialogsOwnerId > 0);
        contextView.setIsHidden(isHide);
    }

    public void fireOptionViewCreated(IDialogsView.IOptionView view) {
        view.setCanSearch(dialogsOwnerId > 0);
    }
}