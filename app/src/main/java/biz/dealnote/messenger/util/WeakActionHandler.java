package biz.dealnote.messenger.util;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class WeakActionHandler<T> extends Handler {

    private final WeakReference<T> ref;
    private Action<T> action;

    public WeakActionHandler(T object) {
        this.ref = new WeakReference<>(object);
    }

    @Override
    public final void handleMessage(Message msg) {
        T object = ref.get();
        if (Objects.nonNull(action)) {
            action.doAction(msg.what, object);
        }
    }

    public WeakActionHandler setAction(Action<T> action) {
        this.action = action;
        return this;
    }

    public interface Action<T> {
        void doAction(int what, T object);
    }
}
