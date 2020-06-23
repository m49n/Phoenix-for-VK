package biz.dealnote.messenger.mvp.view;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.Message;

public interface IMessagesLookView extends IBasicMessageListView, IErrorView {

    void focusTo(int index);

    void setupHeaders(@LoadMoreState int upHeaderState, @LoadMoreState int downHeaderState);

    void forwardMessages(int accountId, @NonNull ArrayList<Message> messages);

    void showDeleteForAllDialog(ArrayList<Integer> ids);
}
