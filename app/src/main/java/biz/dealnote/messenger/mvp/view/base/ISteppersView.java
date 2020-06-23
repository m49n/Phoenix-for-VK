package biz.dealnote.messenger.mvp.view.base;

import androidx.annotation.NonNull;

import biz.dealnote.messenger.view.steppers.base.AbsStepsHost;
import biz.dealnote.mvp.core.IMvpView;

public interface ISteppersView<H extends AbsStepsHost> extends IMvpView {
    void updateStepView(int step);

    void moveSteppers(int from, int to);

    void goBack();

    void hideKeyboard();

    void updateStepButtonsAvailability(int step);

    void attachSteppersHost(@NonNull H mHost);
}
