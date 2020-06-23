package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;

import biz.dealnote.mvp.core.IMvpView;

public interface IEnterPinView extends IMvpView, IErrorView, IToastView {
    void displayPin(int[] value, int noValue);

    void sendSuccessAndClose();

    void displayErrorAnimation();

    void displayAvatarFromUrl(@NonNull String url);

    void displayDefaultAvatar();
}
