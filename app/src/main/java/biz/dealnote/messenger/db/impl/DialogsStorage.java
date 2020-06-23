package biz.dealnote.messenger.db.impl;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.dealnote.messenger.api.model.VKApiChat;
import biz.dealnote.messenger.db.MessengerContentProvider;
import biz.dealnote.messenger.db.PeerStateEntity;
import biz.dealnote.messenger.db.column.DialogsColumns;
import biz.dealnote.messenger.db.column.PeersColumns;
import biz.dealnote.messenger.db.interfaces.IDialogsStorage;
import biz.dealnote.messenger.db.model.PeerPatch;
import biz.dealnote.messenger.db.model.entity.DialogEntity;
import biz.dealnote.messenger.db.model.entity.MessageEntity;
import biz.dealnote.messenger.db.model.entity.SimpleDialogEntity;
import biz.dealnote.messenger.model.Chat;
import biz.dealnote.messenger.model.ChatAction;
import biz.dealnote.messenger.model.criteria.DialogsCriteria;
import biz.dealnote.messenger.util.Exestime;
import biz.dealnote.messenger.util.Optional;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.join;
import static biz.dealnote.messenger.util.Utils.nonEmpty;
import static biz.dealnote.messenger.util.Utils.safeCountOf;

class DialogsStorage extends AbsStorage implements IDialogsStorage {

    private PublishSubject<Pair<Integer, Integer>> unreadDialogsCounter;

    private SharedPreferences preferences;

    DialogsStorage(@NonNull AppStorages base) {
        super(base);
        preferences = base.getSharedPreferences("dialogs_prefs", Context.MODE_PRIVATE);
        unreadDialogsCounter = PublishSubject.create();
    }

    private static String unreadKeyFor(int accountId) {
        return "unread" + accountId;
    }

    @Override
    public int getUnreadDialogsCount(int accountId) {
        synchronized (this) {
            return preferences.getInt(unreadKeyFor(accountId), 0);
        }
    }

    @Override
    public Observable<Pair<Integer, Integer>> observeUnreadDialogsCount() {
        return unreadDialogsCounter;
    }

    @Override
    public Single<List<DialogEntity>> getDialogs(@NonNull DialogsCriteria criteria) {
        return Single.create(e -> {
            long start = System.currentTimeMillis();

            Uri uri = MessengerContentProvider.getDialogsContentUriFor(criteria.getAccountId());

            Cursor cursor = getContext().getContentResolver().query(uri, null, null,
                    null, DialogsColumns.LAST_MESSAGE_ID + " DESC");

            List<DialogEntity> dbos = new ArrayList<>(safeCountOf(cursor));

            if (nonNull(cursor)) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed()) {
                        break;
                    }

                    dbos.add(mapEntity(cursor));
                }

                cursor.close();
            }

            e.onSuccess(dbos);
            Exestime.log("getDialogs", start);
        });
    }

    @Override
    public Completable removePeerWithId(int accountId, int peerId) {
        return Completable.create(emitter -> {
            Uri uri = MessengerContentProvider.getDialogsContentUriFor(accountId);
            getContentResolver().delete(uri, DialogsColumns._ID + " = ?", new String[]{String.valueOf(peerId)});
            emitter.onComplete();
        });
    }

    @Override
    public Completable insertDialogs(int accountId, List<DialogEntity> entities, boolean clearBefore) {
        return Completable.create(emitter -> {
            final long start = System.currentTimeMillis();
            final Uri uri = MessengerContentProvider.getDialogsContentUriFor(accountId);
            final ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            if (clearBefore) {
                operations.add(ContentProviderOperation.newDelete(uri).build());
            }

            for (DialogEntity entity : entities) {
                SimpleDialogEntity simple = entity.simplify();

                operations.add(ContentProviderOperation.newInsert(uri).withValues(createCv(entity)).build());

                operations.add(ContentProviderOperation
                        .newInsert(MessengerContentProvider.getPeersContentUriFor(accountId))
                        .withValues(createPeerCv(simple))
                        .build());

                MessagesStorage.appendDboOperation(accountId, entity.getMessage(), operations, null, null);
            }

            getContentResolver().applyBatch(MessengerContentProvider.AUTHORITY, operations);
            emitter.onComplete();

            Exestime.log("DialogsStorage.insertDialogs", start, "count: " + entities.size() + ", clearBefore: " + clearBefore);
        });
    }

    private ContentValues createCv(DialogEntity entity) {
        ContentValues cv = new ContentValues();
        MessageEntity messageDbo = entity.getMessage();

        cv.put(DialogsColumns._ID, messageDbo.getPeerId());
        cv.put(DialogsColumns.UNREAD, entity.getUnreadCount());
        cv.put(DialogsColumns.TITLE, entity.getTitle());
        cv.put(DialogsColumns.IN_READ, entity.getInRead());
        cv.put(DialogsColumns.OUT_READ, entity.getOutRead());
        cv.put(DialogsColumns.PHOTO_50, entity.getPhoto50());
        cv.put(DialogsColumns.PHOTO_100, entity.getPhoto100());
        cv.put(DialogsColumns.PHOTO_200, entity.getPhoto200());
        cv.put(DialogsColumns.LAST_MESSAGE_ID, messageDbo.getId());
        cv.put(DialogsColumns.ACL, entity.getAcl());
        cv.put(DialogsColumns.IS_GROUP_CHANNEL, entity.isGroupChannel());
        return cv;
    }

    private ContentValues createPeerCv(SimpleDialogEntity entity) {
        ContentValues cv = new ContentValues();
        cv.put(PeersColumns._ID, entity.getPeerId());
        cv.put(PeersColumns.UNREAD, entity.getUnreadCount());
        cv.put(PeersColumns.TITLE, entity.getTitle());
        cv.put(PeersColumns.IN_READ, entity.getInRead());
        cv.put(PeersColumns.OUT_READ, entity.getOutRead());
        cv.put(PeersColumns.PHOTO_50, entity.getPhoto50());
        cv.put(PeersColumns.PHOTO_100, entity.getPhoto100());
        cv.put(PeersColumns.PHOTO_200, entity.getPhoto200());
        cv.put(PeersColumns.PINNED, serializeJson(entity.getPinned()));
        cv.put(PeersColumns.ACL, entity.getAcl());
        cv.put(PeersColumns.IS_GROUP_CHANNEL, entity.isGroupChannel());
        return cv;
    }

    @Override
    public Completable saveSimple(int accountId, @NonNull SimpleDialogEntity entity) {
        return Completable.create(emitter -> {
            final Uri uri = MessengerContentProvider.getPeersContentUriFor(accountId);
            final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            operations.add(ContentProviderOperation.newInsert(uri).withValues(createPeerCv(entity)).build());
            getContentResolver().applyBatch(MessengerContentProvider.AUTHORITY, operations);
            emitter.onComplete();
        });
    }

    @Override
    public Single<List<PeerStateEntity>> findPeerStates(int accountId, Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return Single.just(Collections.emptyList());
        }

        return Single.create(emitter -> {
            String[] projection = {
                    PeersColumns._ID,
                    PeersColumns.UNREAD,
                    PeersColumns.IN_READ,
                    PeersColumns.OUT_READ,
                    PeersColumns.LAST_MESSAGE_ID
            };

            Uri uri = MessengerContentProvider.getPeersContentUriFor(accountId);

            String where = PeersColumns._ID + " IN (" + join(",", ids) + ")";
            Cursor cursor = getContentResolver().query(uri, projection, where, null, null);

            List<PeerStateEntity> entities = new ArrayList<>(safeCountOf(cursor));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    PeerStateEntity entity = new PeerStateEntity(cursor.getInt(cursor.getColumnIndex(PeersColumns._ID)))
                            .setInRead(cursor.getInt(cursor.getColumnIndex(PeersColumns.IN_READ)))
                            .setOutRead(cursor.getInt(cursor.getColumnIndex(PeersColumns.OUT_READ)))
                            .setLastMessageId(cursor.getInt(cursor.getColumnIndex(PeersColumns.LAST_MESSAGE_ID)))
                            .setUnreadCount(cursor.getInt(cursor.getColumnIndex(PeersColumns.UNREAD)));
                    entities.add(entity);
                }
                cursor.close();
            }

            emitter.onSuccess(entities);
        });
    }

    @Override
    public Single<Optional<SimpleDialogEntity>> findSimple(int accountId, int peerId) {
        return Single.create(emitter -> {
            String[] projection = {
                    PeersColumns.UNREAD,
                    PeersColumns.TITLE,
                    PeersColumns.IN_READ,
                    PeersColumns.OUT_READ,
                    PeersColumns.PHOTO_50,
                    PeersColumns.PHOTO_100,
                    PeersColumns.PHOTO_200,
                    PeersColumns.PINNED,
                    PeersColumns.LAST_MESSAGE_ID,
                    PeersColumns.ACL,
                    PeersColumns.IS_GROUP_CHANNEL
            };

            Uri uri = MessengerContentProvider.getPeersContentUriFor(accountId);
            Cursor cursor = getContentResolver().query(uri, projection,
                    PeersColumns.FULL_ID + " = ?", new String[]{String.valueOf(peerId)}, null);

            SimpleDialogEntity entity = null;
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    entity = new SimpleDialogEntity(peerId)
                            .setUnreadCount(cursor.getInt(cursor.getColumnIndex(PeersColumns.UNREAD)))
                            .setTitle(cursor.getString(cursor.getColumnIndex(PeersColumns.TITLE)))
                            .setPhoto200(cursor.getString(cursor.getColumnIndex(PeersColumns.PHOTO_200)))
                            .setPhoto100(cursor.getString(cursor.getColumnIndex(PeersColumns.PHOTO_100)))
                            .setPhoto50(cursor.getString(cursor.getColumnIndex(PeersColumns.PHOTO_50)))
                            .setInRead(cursor.getInt(cursor.getColumnIndex(PeersColumns.IN_READ)))
                            .setOutRead(cursor.getInt(cursor.getColumnIndex(PeersColumns.OUT_READ)))
                            .setPinned(deserializeJson(cursor, PeersColumns.PINNED, MessageEntity.class))
                            .setLastMessageId(cursor.getInt(cursor.getColumnIndex(PeersColumns.LAST_MESSAGE_ID)))
                            .setAcl(cursor.getInt(cursor.getColumnIndex(PeersColumns.ACL)))
                            .setGroupChannel(cursor.getInt(cursor.getColumnIndex(PeersColumns.IS_GROUP_CHANNEL)) == 1);
                }

                cursor.close();
            }

            emitter.onSuccess(Optional.wrap(entity));
        });
    }

    @Override
    public void setUnreadDialogsCount(int accountId, int unreadCount) {
        synchronized (this) {
            preferences.edit()
                    .putInt(unreadKeyFor(accountId), unreadCount)
                    .apply();
        }

        unreadDialogsCounter.onNext(new Pair<>(accountId, unreadCount));
    }

    @Override
    public Single<Collection<Integer>> getMissingGroupChats(int accountId, @NonNull Collection<Integer> ids) {
        return Single.create(e -> {
            if (ids.isEmpty()) {
                e.onSuccess(Collections.emptyList());
                return;
            }

            Set<Integer> peerIds = new HashSet<>(ids);
            String[] projection = {DialogsColumns._ID};
            Uri uri = MessengerContentProvider.getDialogsContentUriFor(accountId);
            Cursor cursor = getContentResolver().query(uri, projection,
                    DialogsColumns.FULL_ID + " IN (" + TextUtils.join(",", peerIds) + ")", null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int peerId = cursor.getInt(cursor.getColumnIndex(DialogsColumns._ID));
                    peerIds.remove(peerId);
                }

                cursor.close();
            }

            e.onSuccess(peerIds);
        });
    }

    @Override
    public Completable insertChats(int accountId, List<VKApiChat> chats) {
        return Completable.fromAction(() -> {
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(chats.size());

            for (VKApiChat chat : chats) {
                operations.add(ContentProviderOperation
                        .newInsert(MessengerContentProvider.getDialogsContentUriFor(accountId))
                        .withValues(DialogsColumns.getCV(chat))
                        .build());
            }

            getContentResolver().applyBatch(MessengerContentProvider.AUTHORITY, operations);
        });
    }

    @Override
    public Completable applyPatches(int accountId, @NonNull List<PeerPatch> patches) {
        return Completable.create(emitter -> {
            Uri dialogsUri = MessengerContentProvider.getDialogsContentUriFor(accountId);
            Uri peersUri = MessengerContentProvider.getPeersContentUriFor(accountId);

            ArrayList<ContentProviderOperation> operations = new ArrayList<>(patches.size() * 2);

            for (PeerPatch patch : patches) {
                ContentValues dialogscv = new ContentValues();
                ContentValues peerscv = new ContentValues();

                if (nonNull(patch.getInRead())) {
                    dialogscv.put(DialogsColumns.IN_READ, patch.getInRead().getId());
                    peerscv.put(PeersColumns.IN_READ, patch.getInRead().getId());
                }

                if (nonNull(patch.getUnread())) {
                    dialogscv.put(DialogsColumns.UNREAD, patch.getUnread().getCount());
                    peerscv.put(PeersColumns.UNREAD, patch.getUnread().getCount());
                }

                if (nonNull(patch.getOutRead())) {
                    dialogscv.put(DialogsColumns.OUT_READ, patch.getOutRead().getId());
                    peerscv.put(PeersColumns.OUT_READ, patch.getOutRead().getId());
                }

                if (nonNull(patch.getLastMessage())) {
                    dialogscv.put(DialogsColumns.LAST_MESSAGE_ID, patch.getLastMessage().getId());
                    peerscv.put(PeersColumns.LAST_MESSAGE_ID, patch.getLastMessage().getId());
                }

                if (nonNull(patch.getPin())) {
                    peerscv.put(PeersColumns.PINNED, serializeJson(patch.getPin().getPinned()));
                }

                if (nonNull(patch.getTitle())) {
                    peerscv.put(PeersColumns.TITLE, patch.getTitle().getTitle());
                    dialogscv.put(DialogsColumns.TITLE, patch.getTitle().getTitle());
                }

                String[] args = {String.valueOf(patch.getId())};

                if (dialogscv.size() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(dialogsUri)
                            .withSelection(DialogsColumns._ID + " = ?", args)
                            .withValues(dialogscv)
                            .build());
                }

                if (peerscv.size() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(peersUri)
                            .withSelection(PeersColumns._ID + " = ?", args)
                            .withValues(peerscv)
                            .build());
                }
            }

            if (nonEmpty(operations)) {
                getContentResolver().applyBatch(MessengerContentProvider.AUTHORITY, operations);
            }

            emitter.onComplete();
        });
    }

    @Override
    public Single<Optional<Chat>> findChatById(int accountId, int peerId) {
        return Single.fromCallable(() -> {
            String[] projection = {
                    DialogsColumns.TITLE,
                    DialogsColumns.PHOTO_200,
                    DialogsColumns.PHOTO_100,
                    DialogsColumns.PHOTO_50,
            };

            Uri uri = MessengerContentProvider.getDialogsContentUriFor(accountId);
            Cursor cursor = getContentResolver().query(uri, projection,
                    DialogsColumns.FULL_ID + " = ?", new String[]{String.valueOf(peerId)}, null);

            Chat chat = null;
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    chat = new Chat(peerId);
                    chat.setTitle(cursor.getString(cursor.getColumnIndex(DialogsColumns.TITLE)))
                            .setPhoto200(cursor.getString(cursor.getColumnIndex(DialogsColumns.PHOTO_200)))
                            .setPhoto100(cursor.getString(cursor.getColumnIndex(DialogsColumns.PHOTO_100)))
                            .setPhoto50(cursor.getString(cursor.getColumnIndex(DialogsColumns.PHOTO_50)));
                }

                cursor.close();
            }

            return Optional.wrap(chat);
        });
    }

    private DialogEntity mapEntity(@NonNull Cursor cursor) {
        @ChatAction
        int action = cursor.getInt(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_ACTION));

        boolean encrypted = cursor.getInt(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_ENCRYPTED)) == 1;

        int messageId = cursor.getInt(cursor.getColumnIndex(DialogsColumns.LAST_MESSAGE_ID));
        int peerId = cursor.getInt(cursor.getColumnIndex(DialogsColumns._ID));
        int fromId = cursor.getInt(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_FROM_ID));

        MessageEntity message = new MessageEntity(messageId, peerId, fromId)
                .setBody(cursor.getString(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_BODY)))
                .setDate(cursor.getLong(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_DATE)))
                .setOut(cursor.getInt(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_OUT)) == 1)
                .setHasAttachmens(cursor.getInt(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_HAS_ATTACHMENTS)) == 1)
                .setForwardCount(cursor.getInt(cursor.getColumnIndex(DialogsColumns.FOREIGN_MESSAGE_FWD_COUNT)))
                .setAction(action)
                .setEncrypted(encrypted);

        return new DialogEntity(peerId)
                .setMessage(message)
                .setInRead(cursor.getInt(cursor.getColumnIndex(DialogsColumns.IN_READ)))
                .setOutRead(cursor.getInt(cursor.getColumnIndex(DialogsColumns.OUT_READ)))
                .setTitle(cursor.getString(cursor.getColumnIndex(DialogsColumns.TITLE)))
                .setPhoto50(cursor.getString(cursor.getColumnIndex(DialogsColumns.PHOTO_50)))
                .setPhoto100(cursor.getString(cursor.getColumnIndex(DialogsColumns.PHOTO_100)))
                .setPhoto200(cursor.getString(cursor.getColumnIndex(DialogsColumns.PHOTO_200)))
                .setUnreadCount(cursor.getInt(cursor.getColumnIndex(DialogsColumns.UNREAD)))
                .setLastMessageId(cursor.getInt(cursor.getColumnIndex(DialogsColumns.LAST_MESSAGE_ID)))
                .setAcl(cursor.getInt(cursor.getColumnIndex(DialogsColumns.ACL)))
                .setGroupChannel(cursor.getInt(cursor.getColumnIndex(DialogsColumns.LAST_MESSAGE_ID)) == 1);
    }
}