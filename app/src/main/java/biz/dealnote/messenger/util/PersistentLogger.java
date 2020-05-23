package biz.dealnote.messenger.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.db.interfaces.ILogsStorage;
import biz.dealnote.messenger.model.LogEvent;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ruslan Kolbasa on 26.04.2017.
 * phoenix
 */
public class PersistentLogger {

    public static void logThrowable(String tag, Throwable throwable) {
        ILogsStorage store = Injection.provideLogsStore();
        Throwable cause = Utils.getCauseIfRuntime(throwable);

        getStackTrace(cause)
                .flatMapCompletable(s -> store.add(LogEvent.Type.ERROR, tag, s)
                        .ignoreElement())
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {
                }, ignore -> {
                });
    }

    private static Single<String> getStackTrace(final Throwable throwable) {
        return Single.fromCallable(() -> {
            try (StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw)) {
                throwable.printStackTrace(pw);
                return sw.toString();
            }
        });
    }
}