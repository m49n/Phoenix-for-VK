package biz.dealnote.messenger.mvp.view;

import biz.dealnote.messenger.model.AnswerVKOfficialList;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;

/**
 * Created by Ruslan Kolbasa on 11.09.2017.
 * phoenix
 */
public interface IAnswerVKOfficialView extends IAccountDependencyView, IMvpView, IErrorView {
    void displayData(AnswerVKOfficialList pages);

    void notifyDataSetChanged();

    void notifyUpdateCounter();

    void notifyDataAdded(int position, int count);

    void showRefreshing(boolean refreshing);
}
