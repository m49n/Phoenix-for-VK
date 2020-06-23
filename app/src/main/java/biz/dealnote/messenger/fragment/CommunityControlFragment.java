package biz.dealnote.messenger.fragment;

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

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.GroupSettings;

public class CommunityControlFragment extends Fragment {

    private Community mCommunity;
    //private GroupSettings mSettings;
    private int mAccountId;

    public static CommunityControlFragment newInstance(int accountId, Community community, GroupSettings settings) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.SETTINGS, settings);
        args.putParcelable(Extra.OWNER, community);
        CommunityControlFragment fragment = new CommunityControlFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.mSettings = getArguments().getParcelable(Extra.SETTINGS);
        this.mAccountId = getArguments().getInt(Extra.ACCOUNT_ID);
        this.mCommunity = getArguments().getParcelable(Extra.OWNER);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_community_control, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        ViewPager2 pager = root.findViewById(R.id.view_pager);
        pager.setOffscreenPageLimit(1);

        List<ITab> tabs = new ArrayList<>();
        if (mCommunity.getAdminLevel() > 0)
            tabs.add(new Tab(getString(R.string.community_blacklist_tab_title), () -> CommunityBlacklistFragment.newInstance(mAccountId, mCommunity.getId())));
        tabs.add(new Tab(getString(R.string.community_links_tab_title), () -> CommunityLinksFragment.newInstance(mAccountId, mCommunity.getId())));
        tabs.add(new Tab(mCommunity.getAdminLevel() == 0 ? getString(R.string.community_managers_contacts) : getString(R.string.community_managers_tab_title), () -> CommunityManagersFragment.newInstance(mAccountId, mCommunity)));

        Adapter tab_set = new Adapter(tabs, this);
        pager.setAdapter(tab_set);

        new TabLayoutMediator(root.findViewById(R.id.tablayout), pager, (tab, position) ->
                tab.setText(tab_set.tabs.get(position).getTabTitle())).attach();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityUtils.setToolbarTitle(this, R.string.community_control);
        ActivityUtils.setToolbarSubtitle(this, mCommunity.getFullName());

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onClearSelection();
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(true)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    private interface ITab {
        String getTabTitle();

        IFragmentCreator getFragmentCreator();
    }

    private interface IFragmentCreator {
        Fragment create();
    }

    private static class Tab implements ITab {

        final String title;
        final IFragmentCreator creator;

        private Tab(String title, IFragmentCreator creator) {
            this.title = title;
            this.creator = creator;
        }

        @Override
        public String getTabTitle() {
            return title;
        }

        @Override
        public IFragmentCreator getFragmentCreator() {
            return creator;
        }
    }

    private static class Adapter extends FragmentStateAdapter {
        private final List<ITab> tabs;

        public Adapter(List<ITab> tabs, @NonNull Fragment fragmentActivity) {
            super(fragmentActivity);
            this.tabs = tabs;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return tabs.get(position).getFragmentCreator().create();
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }
    }
}