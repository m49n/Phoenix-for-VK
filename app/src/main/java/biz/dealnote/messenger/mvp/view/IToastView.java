package biz.dealnote.messenger.mvp.view;

import androidx.annotation.StringRes;

import biz.dealnote.messenger.util.PhoenixToast;

/**
 * Created by admin on 14.04.2017.
 * phoenix
 */
public interface IToastView {
    void showToast(@StringRes int titleTes, boolean isLong, Object... params);
    PhoenixToast getPhoenixToast();
}
