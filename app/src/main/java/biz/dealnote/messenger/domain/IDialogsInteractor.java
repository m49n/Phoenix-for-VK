package biz.dealnote.messenger.domain;

import biz.dealnote.messenger.model.Chat;
import io.reactivex.Single;

public interface IDialogsInteractor {
    Single<Chat> getChatById(int accountId, int peerId);
}