package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.db.OwnerHelper;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.VideoSearchCriteria;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.Settings;

public class VideosTabsFragment extends BaseFragment {

    private int accountId;
    private int ownerId;
    private String action;

    public static Bundle buildArgs(int accountId, int ownerId, String action) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putString(Extra.ACTION, action);
        return args;
    }

    public static VideosTabsFragment newInstance(int accountId, int ownerId, String action) {
        return newInstance(buildArgs(accountId, ownerId, action));
    }

    public static VideosTabsFragment newInstance(Bundle args) {
        VideosTabsFragment fragment = new VideosTabsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        accountId = requireArguments().getInt(Extra.ACCOUNT_ID);
        ownerId = requireArguments().getInt(Extra.OWNER_ID);
        action = requireArguments().getString(Extra.ACTION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_videos_tabs, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ViewPager2 viewPager = view.findViewById(R.id.fragment_videos_pager);
        viewPager.setOffscreenPageLimit(1);
        Adapter adapter = new Adapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(view.findViewById(R.id.fragment_videos_tabs), viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.videos_my);
                    break;
                case 1:
                    tab.setText(R.string.videos_albums);
                    break;
            }
        }).attach();
    }

    public int getAccountId() {
        return accountId;
    }

    private boolean isMy() {
        return getAccountId() == ownerId;
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.VIDEOS);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.videos);
            actionBar.setSubtitle(isMy() ? null : OwnerHelper.loadOwnerFullName(requireActivity(), getAccountId(), ownerId));
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            if (isMy()) {
                ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_VIDEOS);
            } else {
                ((OnSectionResumeCallback) requireActivity()).onClearSelection();
            }
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            VideoSearchCriteria criteria = new VideoSearchCriteria("", true);
            PlaceFactory.getSingleTabSearchPlace(getAccountId(), SearchContentType.VIDEOS, criteria).tryOpenWith(requireActivity());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_video_main, menu);
    }

    class Adapter extends FragmentStateAdapter {
        public Adapter(@NonNull Fragment fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    VideosFragment fragment = VideosFragment.newInstance(getAccountId(), ownerId, 0, action, null);
                    fragment.requireArguments().putBoolean(VideosFragment.EXTRA_IN_TABS_CONTAINER, true);
                    return fragment;
                case 1:
                    return VideoAlbumsFragment.newInstance(getAccountId(), ownerId, action);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}