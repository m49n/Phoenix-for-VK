package biz.dealnote.messenger.fragment.search;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.VideosAdapter;
import biz.dealnote.messenger.fragment.search.criteria.VideoSearchCriteria;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.mvp.presenter.search.VideosSearchPresenter;
import biz.dealnote.messenger.mvp.view.search.IVideosSearchView;
import biz.dealnote.mvp.core.IPresenterFactory;

public class VideoSearchFragment extends AbsSearchFragment<VideosSearchPresenter, IVideosSearchView, Video, VideosAdapter>
        implements VideosAdapter.VideoOnClickListener {

    public static VideoSearchFragment newInstance(int accountId, @Nullable VideoSearchCriteria initialCriteria) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelable(Extra.CRITERIA, initialCriteria);
        VideoSearchFragment fragment = new VideoSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    void setAdapterData(VideosAdapter adapter, List<Video> data) {
        adapter.setData(data);
    }

    @Override
    void postCreate(View root) {

    }

    @Override
    VideosAdapter createAdapter(List<Video> data) {
        VideosAdapter adapter = new VideosAdapter(requireActivity(), data);
        adapter.setVideoOnClickListener(this);
        return adapter;
    }

    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        int columns = getContext().getResources().getInteger(R.integer.videos_column_count);
        return new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL);
    }

    @Override
    public void onVideoClick(int position, Video video) {
        getPresenter().fireVideoClick(video);
    }

    @NotNull
    @Override
    public IPresenterFactory<VideosSearchPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new VideosSearchPresenter(
                getArguments().getInt(Extra.ACCOUNT_ID),
                getArguments().getParcelable(Extra.CRITERIA),
                saveInstanceState
        );
    }
}