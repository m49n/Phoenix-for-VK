package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.IRelationshipInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.FriendsCounters;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IFriendsTabsView;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;


public class FriendsTabsPresenter extends AccountDependencyPresenter<IFriendsTabsView> {

    private static final String SAVE_COUNTERS = "save_counters";

    private final int userId;
    private final IRelationshipInteractor relationshipInteractor;
    private final IOwnersRepository ownersRepository;
    private FriendsCounters counters;
    private Owner owner;

    public FriendsTabsPresenter(int accountId, int userId, @Nullable FriendsCounters counters, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.userId = userId;
        this.relationshipInteractor = InteractorFactory.createRelationshipInteractor();
        this.ownersRepository = Repository.INSTANCE.getOwners();

        if (Objects.nonNull(savedInstanceState)) {
            this.counters = savedInstanceState.getParcelable(SAVE_COUNTERS);
        } else {
            this.counters = counters;
        }

        if (this.counters == null) {
            this.counters = new FriendsCounters(0, 0, 0, 0);
            requestCounters();
        }

        if (Objects.isNull(owner) && userId != accountId) {
            requestOwnerInfo();
        }
    }

    private void requestOwnerInfo() {
        final int accountId = super.getAccountId();
        appendDisposable(ownersRepository.getBaseOwnerInfo(accountId, userId, IOwnersRepository.MODE_ANY)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onOwnerInfoReceived, t -> {/*ignore*/}));
    }

    private void onOwnerInfoReceived(Owner owner) {
        this.owner = owner;
        callView(view -> view.displayUserNameAtToolbar(owner.getFullName()));
    }

    private void requestCounters() {
        final int accountId = super.getAccountId();
        appendDisposable(relationshipInteractor.getFriendsCounters(accountId, userId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCountersReceived, this::onCountersGetError));
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        getView().setDrawerFriendsSectionSelected(this.userId == super.getAccountId());
    }

    private void onCountersGetError(Throwable t) {
        callView(view -> view.displayConters(counters));
        showError(getView(), t);
    }

    private void onCountersReceived(FriendsCounters counters) {
        this.counters = counters;
        callView(view -> view.displayConters(counters));
    }

    @Override
    public void onGuiCreated(@NonNull IFriendsTabsView view) {
        super.onGuiCreated(view);
        view.configTabs(getAccountId(), userId, userId != getAccountId());
        view.displayConters(this.counters);
    }
}