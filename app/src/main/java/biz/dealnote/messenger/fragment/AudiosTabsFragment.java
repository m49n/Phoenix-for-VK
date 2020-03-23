package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.settings.Settings;

public class AudiosTabsFragment extends BaseFragment {

    private int accountId;
    private int ownerId;

    public static final int MY_RECOMENDATIONS = -2;
    public static final int MY_AUDIO = -1;
    public static final int TOP_ALL = 0;

    public static Bundle buildArgs(int accountId, int ownerId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.OWNER_ID, ownerId);
        return args;
    }

    public static AudiosTabsFragment newInstance(int accountId, int ownerId) {
        return newInstance(buildArgs(accountId, ownerId));
    }

    public static AudiosTabsFragment newInstance(Bundle args) {
        AudiosTabsFragment fragment = new AudiosTabsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = requireArguments().getInt(Extra.ACCOUNT_ID);
        ownerId = requireArguments().getInt(Extra.OWNER_ID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_audios_tabs, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ViewPager viewPager = view.findViewById(R.id.fragment_audios_pager);
        viewPager.setOffscreenPageLimit(1);
        setupViewPager(viewPager);

        TabLayout tabLayout = view.findViewById(R.id.fragment_audios_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    public int getAccountId() {
        return accountId;
    }

    private AudiosFragment CreateAudiosFragment(int option_menu)
    {
        AudiosFragment fragment = AudiosFragment.newInstance(getAccountId(), ownerId, option_menu, false);
        fragment.requireArguments().putBoolean(VideosFragment.EXTRA_IN_TABS_CONTAINER, true);
        return fragment;
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getChildFragmentManager());
        adapter.addFragment(CreateAudiosFragment(MY_AUDIO), getString(R.string.my_saved));
        if(ownerId >= 0)
            adapter.addFragment(CreateAudiosFragment(MY_RECOMENDATIONS), getString(R.string.recommendation));
        adapter.addFragment(AudioPlaylistsFragment.newInstance(getAccountId(), ownerId), getString(R.string.playlists));
        if(getAccountId() == ownerId && Settings.get().other().isEnable_show_audio_top()) {
            adapter.addFragment(CreateAudiosFragment(TOP_ALL), getString(R.string.top));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.ETHNIC), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.ETHNIC));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.INSTRUMENTAL), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.INSTRUMENTAL));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.ACOUSTIC_AND_VOCAL), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.ACOUSTIC_AND_VOCAL));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.ALTERNATIVE), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.ALTERNATIVE));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.CLASSICAL), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.CLASSICAL));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.DANCE_AND_HOUSE), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.DANCE_AND_HOUSE));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.DRUM_AND_BASS), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.DRUM_AND_BASS));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.EASY_LISTENING), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.EASY_LISTENING));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.ELECTROPOP_AND_DISCO), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.ELECTROPOP_AND_DISCO));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.INDIE_POP), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.INDIE_POP));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.METAL), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.METAL));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.OTHER), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.OTHER));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.POP), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.POP));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.REGGAE), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.REGGAE));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.ROCK), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.ROCK));
            adapter.addFragment(CreateAudiosFragment(VKApiAudio.Genre.TRANCE), VKApiAudio.Genre.getTitleByGenre(requireActivity(), VKApiAudio.Genre.TRANCE));
        }
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.AUDIOS);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.music);
            actionBar.setSubtitle(null);
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_AUDIOS);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    static class Adapter extends FragmentStatePagerAdapter {

        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
