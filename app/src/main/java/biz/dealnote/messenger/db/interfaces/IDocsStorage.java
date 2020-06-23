package biz.dealnote.messenger.db.interfaces;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.List;

import biz.dealnote.messenger.db.model.entity.DocumentEntity;
import biz.dealnote.messenger.model.criteria.DocsCriteria;
import io.reactivex.Completable;
import io.reactivex.Single;


public interface IDocsStorage extends IStorage {

    @CheckResult
    Single<List<DocumentEntity>> get(@NonNull DocsCriteria criteria);

    @CheckResult
    Completable store(int accountId, int ownerId, List<DocumentEntity> entities, boolean clearBeforeInsert);

    @CheckResult
    Completable delete(int accountId, int docId, int ownerId);
}