package biz.dealnote.messenger.view.pager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import java.lang.ref.WeakReference;

public class WeakGoBackAnimationAdapter extends AnimatorListenerAdapter {

    private WeakReference<GoBackCallback> mReference;

    public WeakGoBackAnimationAdapter(GoBackCallback holder) {
        this.mReference = new WeakReference<>(holder);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        GoBackCallback callback = mReference.get();
        if (callback != null) {
            callback.goBack();
        }
    }
}
