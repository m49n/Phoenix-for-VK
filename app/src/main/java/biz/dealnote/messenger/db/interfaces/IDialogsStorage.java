package biz.dealnote.messenger.db.interfaces;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;

import biz.dealnote.messenger.api.model.VKApiChat;
import biz.dealnote.messenger.db.PeerStateEntity;
import biz.dealnote.messenger.db.model.PeerPatch;
import biz.dealnote.messenger.db.model.entity.DialogEntity;
import biz.dealnote.messenger.db.model.entity.SimpleDialogEntity;
import biz.dealnote.messenger.model.Chat;
import biz.dealnote.messenger.model.criteria.DialogsCriteria;
import biz.dealnote.messenger.util.Optional;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface IDialogsStorage extends IStorage {

    int getUnreadDialogsCount(int accountId);

    Observable<Pair<Integer, Integer>> observeUnreadDialogsCount();

    Single<List<PeerStateEntity>> findPeerStates(int accountId, Collection<Integer> ids);

    void setUnreadDialogsCount(int accountId, int unreadCount);

    Single<Optional<SimpleDialogEntity>> findSimple(int accountId, int peerId);

    Completable saveSimple(int accountId, @NonNull SimpleDialogEntity entity);

    Single<List<DialogEntity>> getDialogs(@NonNull DialogsCriteria criteria);

    Completable removePeerWithId(int accountId, int peerId);

    Completable insertDialogs(int accountId, List<DialogEntity> dbos, boolean clearBefore);

    /**
     * Получение списка идентификаторов диалогов, информация о которых отсутствует в базе данных
     *
     * @param ids список входящих идентификаторов
     * @return отсутствующие
     */
    Single<Collection<Integer>> getMissingGroupChats(int accountId, Collection<Integer> ids);

    Completable insertChats(int accountId, List<VKApiChat> chats);

    Completable applyPatches(int accountId, @NonNull List<PeerPatch> patches);

    Single<Optional<Chat>> findChatById(int accountId, int peerId);
}