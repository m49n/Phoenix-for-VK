package biz.dealnote.messenger.db.interfaces;

import androidx.annotation.NonNull;

import java.util.List;

import biz.dealnote.messenger.db.model.entity.OwnerEntities;
import biz.dealnote.messenger.db.model.entity.feedback.FeedbackEntity;
import biz.dealnote.messenger.model.criteria.NotificationsCriteria;
import io.reactivex.Single;

public interface IFeedbackStorage extends IStorage {
    Single<int[]> insert(int accountId, List<FeedbackEntity> dbos, OwnerEntities owners, boolean clearBefore);

    Single<List<FeedbackEntity>> findByCriteria(@NonNull NotificationsCriteria criteria);
}