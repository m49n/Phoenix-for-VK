package biz.dealnote.messenger.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.vkdatabase.CountriesAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpDialogFragment;
import biz.dealnote.messenger.listener.TextWatcherAdapter;
import biz.dealnote.messenger.model.database.Country;
import biz.dealnote.messenger.mvp.presenter.CountriesPresenter;
import biz.dealnote.messenger.mvp.view.ICountriesView;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class SelectCountryDialog extends BaseMvpDialogFragment<CountriesPresenter, ICountriesView>
        implements CountriesAdapter.Listener, ICountriesView {

    private CountriesAdapter mAdapter;
    private View mLoadingView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(requireActivity(), R.layout.dialog_countries, null);

        Dialog dialog = new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.countries_title)
                .setView(view)
                .setNegativeButton(R.string.button_cancel, null)
                .create();

        EditText filterView = view.findViewById(R.id.input);
        filterView.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getPresenter().fireFilterEdit(s);
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        mAdapter = new CountriesAdapter(requireActivity(), Collections.emptyList());
        mAdapter.setListener(this);

        recyclerView.setAdapter(mAdapter);

        mLoadingView = view.findViewById(R.id.progress_root);

        fireViewCreated();
        return dialog;
    }

    @Override
    public void onClick(Country country) {
        getPresenter().fireCountryClick(country);
    }

    @Override
    public void displayData(List<Country> countries) {
        if (nonNull(mAdapter)) {
            mAdapter.setData(countries);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayLoading(boolean loading) {
        if (nonNull(mLoadingView)) {
            mLoadingView.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void returnSelection(Country country) {
        Intent intent = new Intent();
        intent.putExtra(Extra.COUNTRY, country);
        intent.putExtra(Extra.ID, country.getId());
        intent.putExtra(Extra.TITLE, country.getTitle());

        if (getArguments() != null) {
            intent.putExtras(getArguments());
        }

        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        dismiss();
    }

    @NotNull
    @Override
    public IPresenterFactory<CountriesPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new CountriesPresenter(
                getArguments().getInt(Extra.ACCOUNT_ID),
                saveInstanceState
        );
    }
}