package biz.dealnote.messenger.util;

import java.lang.ref.WeakReference;

import io.reactivex.functions.Consumer;

public class WeakConsumer<T> implements Consumer<T> {

    private final WeakReference<Consumer<T>> ref;

    public WeakConsumer(Consumer<T> orig) {
        this.ref = new WeakReference<>(orig);
    }

    @Override
    public void accept(T t) throws Exception {
        Consumer<T> orig = ref.get();
        if (orig != null) {
            orig.accept(t);
        }
    }
}
