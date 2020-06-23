package biz.dealnote.messenger.fragment.search;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.jetbrains.annotations.NotNull;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.fragment.search.criteria.BaseSearchCriteria;
import biz.dealnote.messenger.listener.AppStyleable;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.view.MySearchView;

public class SingleTabSearchFragment extends Fragment implements MySearchView.OnQueryTextListener, MySearchView.OnAdditionalButtonClickListener {

    @SearchContentType
    private int mContentType;
    private int mAccountId;
    private BaseSearchCriteria mInitialCriteria;
    private boolean attachedChild;
    private FragmentManager.FragmentLifecycleCallbacks mFragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentViewCreated(@NotNull FragmentManager fm, @NotNull Fragment f, @NotNull View v, Bundle savedInstanceState) {
            syncChildFragment();
        }
    };

    public static Bundle buildArgs(int accountId, @SearchContentType int contentType, @Nullable BaseSearchCriteria criteria) {
        Bundle args = new Bundle();
        args.putInt(Extra.TYPE, contentType);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.CRITERIA, criteria);
        return args;
    }

    public static SingleTabSearchFragment newInstance(Bundle args) {
        SingleTabSearchFragment fragment = new SingleTabSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static SingleTabSearchFragment newInstance(int accountId, @SearchContentType int contentType) {
        Bundle args = new Bundle();
        args.putInt(Extra.TYPE, contentType);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        SingleTabSearchFragment fragment = new SingleTabSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static SingleTabSearchFragment newInstance(int accountId, @SearchContentType int contentType, @Nullable BaseSearchCriteria criteria) {
        Bundle args = new Bundle();
        args.putInt(Extra.TYPE, contentType);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.CRITERIA, criteria);
        SingleTabSearchFragment fragment = new SingleTabSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContentType = getArguments().getInt(Extra.TYPE);
        this.mAccountId = getArguments().getInt(Extra.ACCOUNT_ID);
        this.mInitialCriteria = getArguments().getParcelable(Extra.CRITERIA);

        getChildFragmentManager().registerFragmentLifecycleCallbacks(mFragmentLifecycleCallbacks, false);

        if (Objects.nonNull(savedInstanceState)) {
            this.attachedChild = savedInstanceState.getBoolean("attachedChild");
        }
    }

    @Override
    public void onDestroy() {
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentLifecycleCallbacks);
        super.onDestroy();
    }

    private void resolveLeftButton(MySearchView searchView) {
        int count = requireActivity().getSupportFragmentManager().getBackStackEntryCount();
        if (searchView != null) {
            Drawable tr = AppCompatResources.getDrawable(requireActivity(), count == 1 && requireActivity() instanceof AppStyleable ?
                    R.drawable.magnify : R.drawable.arrow_left);
            Utils.setColorFilter(tr, CurrentTheme.getColorPrimary(requireActivity()));
            searchView.setLeftIcon(tr);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search_single, container, false);

        MySearchView searchView = root.findViewById(R.id.searchview);
        searchView.setOnQueryTextListener(this);
        searchView.setOnBackButtonClickListener(this::onBackButtonClick);
        searchView.setOnAdditionalButtonClickListener(this);
        searchView.setQuery(getInitialCriteriaText(), true);

        resolveLeftButton(searchView);

        if (!attachedChild) {
            attachChildFragment();
            this.attachedChild = true;
        }
        return root;
    }

    private void syncChildFragment() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.child_container);

        if (fragment instanceof AbsSearchFragment) {
            ((AbsSearchFragment) fragment).syncYourCriteriaWithParent();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("attachedChild", attachedChild);
    }

    private void fireNewQuery(String query) {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.child_container);

        // MVP
        if (fragment instanceof AbsSearchFragment) {
            ((AbsSearchFragment) fragment).fireTextQueryEdit(query);
        }
    }

    private void attachChildFragment() {
        Fragment fragment = SearchFragmentFactory.create(mContentType, mAccountId, mInitialCriteria);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.child_container, fragment)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onClearSelection();
        }
    }

    private String getInitialCriteriaText() {
        return Objects.isNull(mInitialCriteria) ? "" : mInitialCriteria.getQuery();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        fireNewQuery(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        fireNewQuery(newText);
        return false;
    }

    public void onBackButtonClick() {
        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() == 1
                && requireActivity() instanceof AppStyleable) {
            ((AppStyleable) requireActivity()).openMenu(true);
        } else {
            requireActivity().onBackPressed();
        }
    }

    @Override
    public void onAdditionalButtonClick() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.child_container);
        if (fragment instanceof AbsSearchFragment) {
            ((AbsSearchFragment) fragment).openSearchFilter();
        }
    }
}
