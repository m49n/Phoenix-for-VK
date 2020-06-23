package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;

import java.util.List;

import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.Topic;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;


public interface ITopicsView extends IAccountDependencyView, IMvpView, IErrorView {
    void displayData(@NonNull List<Topic> topics);

    void notifyDataSetChanged();

    void notifyDataAdd(int position, int count);

    void showRefreshing(boolean refreshing);

    void setupLoadMore(@LoadMoreState int state);

    void goToComments(int accountId, @NonNull Topic topic);

    void setButtonCreateVisible(boolean visible);
}