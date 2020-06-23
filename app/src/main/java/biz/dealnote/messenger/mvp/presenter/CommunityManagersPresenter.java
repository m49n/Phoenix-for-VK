package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.model.VKApiCommunity;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.db.column.UserColumns;
import biz.dealnote.messenger.domain.IGroupSettingsInteractor;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.domain.impl.GroupSettingsInteractor;
import biz.dealnote.messenger.domain.mappers.Dto2Model;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.ContactInfo;
import biz.dealnote.messenger.model.Manager;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.ICommunityManagersView;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.listEmptyIfNull;


public class CommunityManagersPresenter extends AccountDependencyPresenter<ICommunityManagersView> {

    private final Community groupId;

    private final List<Manager> data;

    private final IGroupSettingsInteractor interactor;
    private boolean loadingNow;

    public CommunityManagersPresenter(int accountId, Community groupId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.interactor = new GroupSettingsInteractor(Injection.provideNetworkInterfaces(), Injection.provideStores().owners(), Repository.INSTANCE.getOwners());
        this.groupId = groupId;
        this.data = new ArrayList<>();

        appendDisposable(Injection.provideStores()
                .owners()
                .observeManagementChanges()
                .filter(pair -> pair.getFirst() == groupId.getId())
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(pair -> onManagerActionReceived(pair.getSecond()), Analytics::logUnexpectedError));

        requestData();
    }

    private void onManagerActionReceived(Manager manager) {
        int index = Utils.findIndexByPredicate(data, m -> m.getUser().getId() == manager.getUser().getId());
        boolean removing = Utils.isEmpty(manager.getRole());

        if (index != -1) {
            if (removing) {
                data.remove(index);
                callView(view -> view.notifyItemRemoved(index));
            } else {
                data.set(index, manager);
                callView(view -> view.notifyItemChanged(index));
            }
        } else {
            if (!removing) {
                data.add(0, manager);
                callView(view -> view.notifyItemAdded(0));
            }
        }
    }

    private ContactInfo findByIdU(List<ContactInfo> contacts, int user_id) {
        for (ContactInfo element : contacts) {
            if (element.getUserId() == user_id) {
                return element;
            }
        }
        return null;
    }

    private void onContactsReceived(List<ContactInfo> contacts) {
        final int accountId = super.getAccountId();
        List<Integer> Ids = new ArrayList<>(contacts.size());
        for (ContactInfo it : contacts)
            Ids.add(it.getUserId());
        appendDisposable(Injection.provideNetworkInterfaces().vkDefault(accountId).users().get(Ids, null, UserColumns.API_FIELDS, null)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(t -> {
                    List<VKApiUser> users = listEmptyIfNull(t);
                    List<Manager> managers = new ArrayList<>(users.size());
                    for (VKApiUser user : users) {
                        ContactInfo contact = findByIdU(contacts, user.id);
                        Manager manager = new Manager(Dto2Model.transformUser(user), user.role);
                        if (nonNull(contact)) {
                            manager.setDisplayAsContact(true).setContactInfo(contact);
                        }
                        managers.add(manager);
                        onDataReceived(managers);
                    }
                }, this::onRequestError));
    }

    private void requestContacts() {
        final int accountId = super.getAccountId();
        appendDisposable(interactor.getContacts(accountId, groupId.getId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onContactsReceived, this::onRequestError));
    }

    private void requestData() {
        final int accountId = super.getAccountId();

        setLoadingNow(true);
        if (groupId.getAdminLevel() < VKApiCommunity.AdminLevel.ADMIN) {
            requestContacts();
            return;
        }
        appendDisposable(interactor.getManagers(accountId, groupId.getId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onDataReceived, this::onRequestError));
    }

    @Override
    public void onGuiCreated(@NonNull ICommunityManagersView view) {
        super.onGuiCreated(view);
        view.displayData(data);
    }

    private void setLoadingNow(boolean loadingNow) {
        this.loadingNow = loadingNow;
        resolveRefreshingView();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    private void resolveRefreshingView() {
        if (isGuiResumed()) {
            getView().displayRefreshing(loadingNow);
        }
    }

    private void onRequestError(Throwable throwable) {
        setLoadingNow(false);
        showError(getView(), throwable);
    }

    private void onDataReceived(List<Manager> managers) {
        setLoadingNow(false);

        this.data.clear();
        this.data.addAll(managers);

        callView(ICommunityManagersView::notifyDataSetChanged);
    }

    public void fireRefresh() {
        requestData();
    }

    public void fireManagerClick(Manager manager) {
        getView().goToManagerEditing(getAccountId(), groupId.getId(), manager);
    }

    public void fireRemoveClick(Manager manager) {
        final int accountId = super.getAccountId();
        final User user = manager.getUser();

        appendDisposable(interactor.editManager(accountId, groupId.getId(), user, null, false, null, null, null)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onRemoveComplete, throwable -> onRemoveError(Utils.getCauseIfRuntime(throwable))));
    }

    private void onRemoveError(Throwable throwable) {
        throwable.printStackTrace();
        showError(getView(), throwable);
    }

    private void onRemoveComplete() {
        safeShowToast(getView(), R.string.deleted, false);
    }

    public void fireButtonAddClick() {
        getView().startSelectProfilesActivity(getAccountId(), groupId.getId());
    }

    public void fireProfilesSelected(ArrayList<User> users) {
        getView().startAddingUsersToManagers(getAccountId(), groupId.getId(), users);
    }
}