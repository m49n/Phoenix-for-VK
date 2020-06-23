package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;


public interface ICreatePollView extends IAccountDependencyView, IMvpView, IProgressView, IErrorView {
    void displayQuestion(String question);

    void setAnonymous(boolean anomymous);

    void displayOptions(String[] options);

    void showQuestionError(@StringRes int message);

    void showOptionError(int index, @StringRes int message);

    void sendResultAndGoBack(@NonNull Poll poll);

    void setMultiply(boolean multiply);
}
