package biz.dealnote.messenger.push;

import java.util.concurrent.Executors;

import biz.dealnote.messenger.Injection;
import io.reactivex.Scheduler;
import io.reactivex.SingleTransformer;
import io.reactivex.schedulers.Schedulers;

public class NotificationScheduler {

    public static final Scheduler INSTANCE = Schedulers.from(Executors.newFixedThreadPool(1));

    public static <T> SingleTransformer<T, T> fromNotificationThreadToMain() {
        return single -> single
                .subscribeOn(INSTANCE)
                .observeOn(Injection.provideMainThreadScheduler());
    }
}