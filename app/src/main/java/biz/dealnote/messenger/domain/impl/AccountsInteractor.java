package biz.dealnote.messenger.domain.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.db.column.UserColumns;
import biz.dealnote.messenger.domain.IAccountsInteractor;
import biz.dealnote.messenger.domain.IBlacklistRepository;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.mappers.Dto2Model;
import biz.dealnote.messenger.model.Account;
import biz.dealnote.messenger.model.BannedPart;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.settings.ISettings;
import io.reactivex.Completable;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Utils.listEmptyIfNull;

public class AccountsInteractor implements IAccountsInteractor {

    private final INetworker networker;
    private final ISettings.IAccountsSettings settings;
    private final IOwnersRepository ownersRepository;
    private final IBlacklistRepository blacklistRepository;

    public AccountsInteractor(INetworker networker, ISettings.IAccountsSettings settings, IBlacklistRepository blacklistRepository, IOwnersRepository ownersRepository) {
        this.networker = networker;
        this.settings = settings;
        this.blacklistRepository = blacklistRepository;
        this.ownersRepository = ownersRepository;
    }

    @Override
    public Single<BannedPart> getBanned(int accountId, int count, int offset) {
        return networker.vkDefault(accountId)
                .account()
                .getBanned(count, offset, UserColumns.API_FIELDS)
                .map(items -> {
                    List<VKApiUser> dtos = listEmptyIfNull(items.profiles);
                    List<User> users = Dto2Model.transformUsers(dtos);
                    return new BannedPart(users);
                });
    }

    @Override
    public Completable banUsers(int accountId, Collection<User> users) {
        Completable completable = Completable.complete();

        for (User user : users) {
            completable = completable.andThen(networker.vkDefault(accountId)
                    .account()
                    .banUser(user.getId()))
                    .delay(1, TimeUnit.SECONDS) // чтобы не дергало UI
                    .ignoreElement()
                    .andThen(blacklistRepository.fireAdd(accountId, user));
        }

        return completable;
    }

    @Override
    public Completable unbanUser(int accountId, int userId) {
        return networker.vkDefault(accountId)
                .account()
                .unbanUser(userId)
                .delay(1, TimeUnit.SECONDS) // чтобы не дергало UI
                .ignoreElement()
                .andThen(blacklistRepository.fireRemove(accountId, userId));
    }

    @Override
    public Completable changeStatus(int accountId, String status) {
        return networker.vkDefault(accountId)
                .status()
                .set(status, null)
                .flatMapCompletable(ignored -> ownersRepository.handleStatusChange(accountId, accountId, status));
    }

    @Override
    public Single<List<Account>> getAll() {
        return Single.create(emitter -> {
            Collection<Integer> ids = settings.getRegistered();

            List<Account> accounts = new ArrayList<>(ids.size());

            for (int id : ids) {
                if (emitter.isDisposed()) {
                    break;
                }

                Owner owner = ownersRepository.getBaseOwnerInfo(id, id, IOwnersRepository.MODE_ANY)
                        .onErrorReturn(ignored -> id > 0 ? new User(id) : new Community(-id))
                        .blockingGet();

                Account account = new Account(id, owner);
                accounts.add(account);
            }

            emitter.onSuccess(accounts);
        });
    }
}