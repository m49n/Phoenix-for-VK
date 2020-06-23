package biz.dealnote.messenger.realtime;

import java.util.List;

import biz.dealnote.messenger.api.model.longpoll.AddMessageUpdate;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.Observable;

public interface IRealtimeMessagesProcessor {

    Observable<TmpResult> observeResults();

    int process(int accountId, List<AddMessageUpdate> updates);

    int process(int accountId, int messageId, boolean ignoreIfExists) throws QueueContainsException;

    void registerNotificationsInterceptor(int interceptorId, Pair<Integer, Integer> aidPeerPair);

    void unregisterNotificationsInterceptor(int interceptorId);
}
