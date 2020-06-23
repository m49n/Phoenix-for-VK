package biz.dealnote.messenger.fragment.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.fragment.AdditionalNavigationFragment;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Accounts;
import biz.dealnote.messenger.view.ViewPagerTransformers;

public class SearchTabsFragment extends Fragment {

    public static final int TAB_PEOPLE = 0;
    public static final int TAB_COMMUNITIES = 1;
    public static final int TAB_NEWS = 2;
    public static final int TAB_MUSIC = 3;
    public static final int TAB_VIDEOS = 4;
    public static final int TAB_MESSAGES = 5;
    public static final int TAB_DOCUMENTS = 6;
    private static final String TAG = SearchTabsFragment.class.getSimpleName();
    private static final String SAVE_CURRENT_TAB = "save_current_tab";
    private int mCurrentTab;

    public static Bundle buildArgs(int accountId, int tab) {
        Bundle args = new Bundle();
        args.putInt(Extra.TAB, tab);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        return args;
    }

    public static SearchTabsFragment newInstance(Bundle args) {
        SearchTabsFragment fragment = new SearchTabsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentTab = savedInstanceState.getInt(SAVE_CURRENT_TAB);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search_tabs, container, false);
        ViewPager2 mViewPager = root.findViewById(R.id.viewpager);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        Adapter mAdapter = new Adapter(this);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setPageTransformer(ViewPagerTransformers.ZOOM_OUT);
        mViewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(root.findViewById(R.id.tablayout), mViewPager, (tab, position) -> {
            switch (position) {
                case TAB_PEOPLE:
                    tab.setText(R.string.people);
                    break;
                case TAB_COMMUNITIES:
                    tab.setText(R.string.communities);
                    break;
                case TAB_MUSIC:
                    tab.setText(R.string.music);
                    break;
                case TAB_VIDEOS:
                    tab.setText(R.string.videos);
                    break;
                case TAB_DOCUMENTS:
                    tab.setText(R.string.documents);
                    break;
                case TAB_NEWS:
                    tab.setText(R.string.feed);
                    break;
                case TAB_MESSAGES:
                    tab.setText(R.string.messages);
                    break;
            }
        }).attach();

        if (getArguments().containsKey(Extra.TAB)) {
            mCurrentTab = getArguments().getInt(Extra.TAB);

            getArguments().remove(Extra.TAB);
            mViewPager.setCurrentItem(mCurrentTab);
        }
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_CURRENT_TAB, mCurrentTab);
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.SEARCH);

        ActivityUtils.setToolbarTitle(this, R.string.search);
        ActivityUtils.setToolbarSubtitle(this, null); //

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_SEARCH);
        }
    }

    private class Adapter extends FragmentStateAdapter {

        public Adapter(@NonNull Fragment fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            int accountId = Accounts.fromArgs(getArguments());

            Fragment fragment;

            switch (position) {
                case TAB_PEOPLE:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.PEOPLE);
                    break;

                case TAB_COMMUNITIES:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.COMMUNITIES);
                    break;

                case TAB_MUSIC:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.AUDIOS);
                    break;

                case TAB_VIDEOS:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.VIDEOS);
                    break;

                case TAB_DOCUMENTS:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.DOCUMENTS);
                    break;

                case TAB_NEWS:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.NEWS);
                    break;

                case TAB_MESSAGES:
                    fragment = SingleTabSearchFragment.newInstance(accountId, SearchContentType.MESSAGES);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            fragment.getArguments().putInt(Extra.POSITION, position);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 7;
        }
    }
}
