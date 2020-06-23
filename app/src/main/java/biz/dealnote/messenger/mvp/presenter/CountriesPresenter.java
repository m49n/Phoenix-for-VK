package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IDatabaseInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.database.Country;
import biz.dealnote.messenger.mvp.presenter.base.RxSupportPresenter;
import biz.dealnote.messenger.mvp.view.ICountriesView;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.isEmpty;


public class CountriesPresenter extends RxSupportPresenter<ICountriesView> {

    private final int accountId;

    private final IDatabaseInteractor databaseInteractor;
    private final List<Country> filtered;
    private List<Country> countries;
    private String filter;
    private boolean loadingNow;

    public CountriesPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(savedInstanceState);
        this.accountId = accountId;
        this.countries = new ArrayList<>();
        this.filtered = new ArrayList<>();
        this.databaseInteractor = InteractorFactory.createDatabaseInteractor();

        requestData();
    }

    @Override
    public void onGuiCreated(@NonNull ICountriesView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayData(this.filtered);
    }

    private void setLoadingNow(boolean loadingNow) {
        this.loadingNow = loadingNow;
        resolveLoadingView();
    }

    @OnGuiCreated
    private void resolveLoadingView() {
        if (isGuiReady()) {
            getView().displayLoading(loadingNow);
        }
    }

    private void onDataReceived(List<Country> countries) {
        setLoadingNow(false);

        this.countries = countries;

        reFillFilteredData();
        callView(ICountriesView::notifyDataSetChanged);
    }

    public void fireFilterEdit(CharSequence text) {
        if (Objects.safeEquals(text.toString(), this.filter)) {
            return;
        }

        this.filter = text.toString();

        reFillFilteredData();
        callView(ICountriesView::notifyDataSetChanged);
    }

    private void reFillFilteredData() {
        filtered.clear();

        if (isEmpty(filter)) {
            filtered.addAll(countries);
            return;
        }

        String lowerFilter = filter.toLowerCase();

        for (Country country : countries) {
            if (country.getTitle().toLowerCase().contains(lowerFilter)) {
                filtered.add(country);
            }
        }
    }

    private void onDataGetError(Throwable t) {
        setLoadingNow(false);
        showError(getView(), getCauseIfRuntime(t));
    }

    private void requestData() {
        setLoadingNow(true);
        appendDisposable(databaseInteractor.getCountries(accountId, false)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onDataReceived, this::onDataGetError));
    }

    public void fireCountryClick(Country country) {
        getView().returnSelection(country);
    }
}