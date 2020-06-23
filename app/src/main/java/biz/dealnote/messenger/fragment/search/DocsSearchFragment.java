package biz.dealnote.messenger.fragment.search;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.adapter.DocsAdapter;
import biz.dealnote.messenger.fragment.search.criteria.DocumentSearchCriteria;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.mvp.presenter.search.DocsSearchPresenter;
import biz.dealnote.messenger.mvp.view.search.IDocSearchView;
import biz.dealnote.mvp.core.IPresenterFactory;

public class DocsSearchFragment extends AbsSearchFragment<DocsSearchPresenter, IDocSearchView, Document, DocsAdapter>
        implements DocsAdapter.ActionListener, IDocSearchView {

    public static DocsSearchFragment newInstance(int accountId, @Nullable DocumentSearchCriteria initialCriteria) {
        Bundle args = new Bundle();
        args.putParcelable(Extra.CRITERIA, initialCriteria);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        DocsSearchFragment fragment = new DocsSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    void setAdapterData(DocsAdapter adapter, List<Document> data) {
        adapter.setItems(data);
    }

    @Override
    void postCreate(View root) {

    }

    @Override
    DocsAdapter createAdapter(List<Document> data) {
        DocsAdapter adapter = new DocsAdapter(data);
        adapter.setActionListner(this);
        return adapter;
    }

    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        return new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false);
    }

    @Override
    public void onDocClick(int index, @NonNull Document doc) {
        getPresenter().fireDocClick(doc);
    }

    @Override
    public boolean onDocLongClick(int index, @NonNull Document doc) {
        return false;
    }

    @NotNull
    @Override
    public IPresenterFactory<DocsSearchPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new DocsSearchPresenter(
                getArguments().getInt(Extra.ACCOUNT_ID),
                getArguments().getParcelable(Extra.CRITERIA),
                saveInstanceState
        );
    }
}