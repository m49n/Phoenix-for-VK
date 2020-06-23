package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.LongSparseArray;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import java.lang.ref.WeakReference;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.BackPressCallback;
import biz.dealnote.messenger.model.selection.AbsSelectableSource;
import biz.dealnote.messenger.model.selection.FileManagerSelectableSource;
import biz.dealnote.messenger.model.selection.LocalGallerySelectableSource;
import biz.dealnote.messenger.model.selection.LocalPhotosSelectableSource;
import biz.dealnote.messenger.model.selection.LocalVideosSelectableSource;
import biz.dealnote.messenger.model.selection.Sources;
import biz.dealnote.messenger.model.selection.Types;
import biz.dealnote.messenger.model.selection.VkPhotosSelectableSource;
import biz.dealnote.messenger.util.Objects;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class DualTabPhotosFragment extends BaseFragment implements BackPressCallback {

    private Sources mSources;
    private Adapter mPagerAdapter;
    private int mCurrentTab;

    public static DualTabPhotosFragment newInstance(Sources sources) {
        Bundle args = new Bundle();
        args.putParcelable(Extra.SOURCES, sources);

        DualTabPhotosFragment fragment = new DualTabPhotosFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSources = requireArguments().getParcelable(Extra.SOURCES);

        if (nonNull(savedInstanceState)) {
            this.mCurrentTab = savedInstanceState.getInt("mCurrentTab");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mCurrentTab", mCurrentTab);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_dual_tab_photos, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        ViewPager2 viewPager = root.findViewById(R.id.view_pager);

        mPagerAdapter = new Adapter(this, mSources);
        viewPager.setAdapter(mPagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mCurrentTab = position;
            }
        });

        new TabLayoutMediator(root.findViewById(R.id.tablayout), viewPager, (tab, position) ->
                tab.setText(mPagerAdapter.getPageTitle(position))).attach();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (nonNull(actionBar)) {
            actionBar.setTitle(R.string.multiply_poll);
            actionBar.setSubtitle(null);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (nonNull(mPagerAdapter)) {
            Fragment fragment = mPagerAdapter.findFragmentByPosition(mCurrentTab);

            return !(fragment instanceof BackPressCallback) || ((BackPressCallback) fragment).onBackPressed();
        }

        return true;
    }

    private class Adapter extends FragmentStateAdapter {

        private final Sources mSources;
        private LongSparseArray<WeakReference<Fragment>> fragments;

        public Adapter(@NonNull Fragment fm, Sources mSources) {
            super(fm);
            this.mSources = mSources;
            this.fragments = new LongSparseArray<>();
        }

        public CharSequence getPageTitle(int position) {
            @Types
            int tabtype = mSources.get(position).getType();

            switch (tabtype) {
                case Types.LOCAL_PHOTOS:
                    return getString(R.string.local_photos_tab_title);

                case Types.LOCAL_GALLERY:
                    return getString(R.string.local_gallery_tab_title);

                case Types.VIDEOS:
                    return getString(R.string.videos);

                case Types.VK_PHOTOS:
                    return getString(R.string.vk_photos_tab_title);

                case Types.FILES:
                    return getString(R.string.files_tab_title);
            }

            throw new UnsupportedOperationException();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            AbsSelectableSource source = mSources.get(position);

            if (source instanceof LocalPhotosSelectableSource) {
                Bundle args = new Bundle();
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
                LocalImageAlbumsFragment fragment = new LocalImageAlbumsFragment();
                fragment.setArguments(args);
                fragments.put(position, new WeakReference<>(fragment));
                return fragment;
            }

            if (source instanceof LocalGallerySelectableSource) {
                LocalPhotosFragment fragment = LocalPhotosFragment.newInstance(10, null, true);
                fragments.put(position, new WeakReference<>(fragment));
                return fragment;
            }

            if (source instanceof LocalVideosSelectableSource) {
                Bundle args = new Bundle();
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
                LocalVideosFragment fragment = LocalVideosFragment.newInstance();
                fragment.setArguments(args);
                fragments.put(position, new WeakReference<>(fragment));
                return fragment;
            }

            if (source instanceof VkPhotosSelectableSource) {
                final VkPhotosSelectableSource vksource = (VkPhotosSelectableSource) source;
                VKPhotoAlbumsFragment fragment = VKPhotoAlbumsFragment.newInstance(vksource.getAccountId(), vksource.getOwnerId(), null, null, true);
                fragments.put(position, new WeakReference<>(fragment));
                return fragment;
            }

            if (source instanceof FileManagerSelectableSource) {
                Bundle args = new Bundle();
                args.putInt(Extra.ACTION, FileManagerFragment.SELECT_FILE);
                args.putBoolean(FileManagerFragment.EXTRA_SHOW_CANNOT_READ, true);

                FileManagerFragment fileManagerFragment = new FileManagerFragment();
                fileManagerFragment.setArguments(args);
                fragments.put(position, new WeakReference<>(fileManagerFragment));
                return fileManagerFragment;
            }

            throw new UnsupportedOperationException();
        }

        @Override
        public int getItemCount() {
            return mSources.count();
        }

        public Fragment findFragmentByPosition(int position) {
            WeakReference<Fragment> weak = fragments.get(position);
            return Objects.isNull(weak) ? null : weak.get();
        }
    }
}