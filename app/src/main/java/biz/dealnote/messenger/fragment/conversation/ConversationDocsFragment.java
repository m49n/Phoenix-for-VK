package biz.dealnote.messenger.fragment.conversation;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.adapter.DocsAdapter;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.mvp.presenter.conversations.ChatAttachmentDocsPresenter;
import biz.dealnote.messenger.mvp.view.conversations.IChatAttachmentDocsView;
import biz.dealnote.mvp.core.IPresenterFactory;

public class ConversationDocsFragment extends AbsChatAttachmentsFragment<Document, ChatAttachmentDocsPresenter, IChatAttachmentDocsView>
        implements DocsAdapter.ActionListener, IChatAttachmentDocsView {

    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        return new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false);
    }

    @Override
    public RecyclerView.Adapter createAdapter() {
        DocsAdapter simpleDocRecycleAdapter = new DocsAdapter(Collections.emptyList());
        simpleDocRecycleAdapter.setActionListner(this);
        return simpleDocRecycleAdapter;
    }

    @Override
    public void displayAttachments(List<Document> data) {
        if (getAdapter() instanceof DocsAdapter) {
            ((DocsAdapter) getAdapter()).setItems(data);
        }
    }

    @NotNull
    @Override
    public IPresenterFactory<ChatAttachmentDocsPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new ChatAttachmentDocsPresenter(
                getArguments().getInt(Extra.PEER_ID),
                getArguments().getInt(Extra.ACCOUNT_ID),
                saveInstanceState
        );
    }

    @Override
    public void onDocClick(int index, @NonNull Document doc) {
        getPresenter().fireDocClick(doc);
    }

    @Override
    public boolean onDocLongClick(int index, @NonNull Document doc) {
        return false;
    }
}
