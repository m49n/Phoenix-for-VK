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

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.Settings;

public class AudiosTabsFragment extends BaseFragment {

    private int accountId;
    private int ownerId;

    public static final int PLAYLISTS = -3;
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
        setHasOptionsMenu(true);
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
        ViewPager2 viewPager = view.findViewById(R.id.fragment_audios_pager);
        viewPager.setOffscreenPageLimit(1);
        Adapter adapter = new Adapter(this);
        setupViewPager(viewPager, adapter);

        new TabLayoutMediator(view.findViewById(R.id.fragment_audios_tabs), viewPager, (tab, position) -> {
            Integer fid = adapter.mFragments.get(position);
            if(fid == MY_AUDIO)
                tab.setText(getString(R.string.my_saved));
            else if(fid == PLAYLISTS)
                tab.setText(getString(R.string.playlists));
            else if(fid == MY_RECOMENDATIONS)
                tab.setText(getString(R.string.recommendation));
            else if(fid == TOP_ALL)
                tab.setText(getString(R.string.top));
            else
                tab.setText(VKApiAudio.Genre.getTitleByGenre(requireActivity(), fid));
        }).attach();
    }

    public int getAccountId() {
        return accountId;
    }

    private Fragment CreateAudiosFragment(int option_menu)
    {
        if(option_menu == PLAYLISTS)
            return AudioPlaylistsFragment.newInstance(getAccountId(), ownerId);
        else {
            AudiosFragment fragment = AudiosFragment.newInstance(getAccountId(), ownerId, option_menu, 0, null);
            fragment.requireArguments().putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true);
            return fragment;
        }
    }

    private void setupViewPager(ViewPager2 viewPager, Adapter adapter) {
        adapter.addFragment(MY_AUDIO);
        adapter.addFragment(PLAYLISTS);
        if(ownerId >= 0)
            adapter.addFragment(MY_RECOMENDATIONS);
        if(getAccountId() == ownerId && Settings.get().other().isEnable_show_audio_top()) {
            adapter.addFragment(TOP_ALL);
            adapter.addFragment(VKApiAudio.Genre.ETHNIC);
            adapter.addFragment(VKApiAudio.Genre.INSTRUMENTAL);
            adapter.addFragment(VKApiAudio.Genre.ACOUSTIC_AND_VOCAL);
            adapter.addFragment(VKApiAudio.Genre.ALTERNATIVE);
            adapter.addFragment(VKApiAudio.Genre.CLASSICAL);
            adapter.addFragment(VKApiAudio.Genre.DANCE_AND_HOUSE);
            adapter.addFragment(VKApiAudio.Genre.DRUM_AND_BASS);
            adapter.addFragment(VKApiAudio.Genre.EASY_LISTENING);
            adapter.addFragment(VKApiAudio.Genre.ELECTROPOP_AND_DISCO);
            adapter.addFragment(VKApiAudio.Genre.INDIE_POP);
            adapter.addFragment(VKApiAudio.Genre.METAL);
            adapter.addFragment(VKApiAudio.Genre.OTHER);
            adapter.addFragment(VKApiAudio.Genre.POP);
            adapter.addFragment(VKApiAudio.Genre.REGGAE);
            adapter.addFragment(VKApiAudio.Genre.ROCK);
            adapter.addFragment(VKApiAudio.Genre.TRANCE);
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

    private class Adapter extends FragmentStateAdapter {
        private final List<Integer> mFragments = new ArrayList<>();

        public Adapter(@NonNull Fragment fragmentActivity) {
            super(fragmentActivity);
        }

        void addFragment(Integer fragment) {
            mFragments.add(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return CreateAudiosFragment(mFragments.get(position));
        }

        @Override
        public int getItemCount() {
            return mFragments.size();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                AudioSearchCriteria criteria = new AudioSearchCriteria("", false, true);
                PlaceFactory.getSingleTabSearchPlace(getAccountId(), SearchContentType.AUDIOS, criteria).tryOpenWith(requireActivity());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_audio_main, menu);
    }
}
