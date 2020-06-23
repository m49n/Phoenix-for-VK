package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.link.LinkHelper;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioCatalog;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;

public class AudioCatalogAdapter extends RecyclerView.Adapter<AudioCatalogAdapter.ViewHolder> implements AudioPlaylistsCatalogAdapter.ClickListener,
        AudioRecyclerAdapter.ClickListener, VideosAdapter.VideoOnClickListener, CatalogLinksAdapter.ActionListener {

    private List<AudioCatalog> data;
    private Context mContext;
    private int account_id;
    private ClickListener clickListener;

    public AudioCatalogAdapter(List<AudioCatalog> data, int account_id, Context context) {
        this.data = data;
        this.mContext = context;
        this.account_id = account_id;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        if (viewType == 0)
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_catalog, parent, false));
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_catalog_artist, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position).getArtist() == null)
            return 0;
        return 1;
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        final AudioCatalog category = data.get(position);

        if (category.getArtist() != null) {
            if (Utils.isEmpty(category.getArtist().getName()))
                holder.title.setVisibility(View.GONE);
            else {
                holder.title.setVisibility(View.VISIBLE);
                holder.title.setText(category.getArtist().getName());
            }
            if (holder.Image != null) {
                if (Utils.isEmpty(category.getArtist().getPhoto()))
                    PicassoInstance.with().cancelRequest(holder.Image);
                else
                    ViewUtils.displayAvatar(holder.Image, null, category.getArtist().getPhoto(), Constants.PICASSO_TAG);
            }
            return;
        }

        holder.catalog.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(position, category);
            }
        });

        if (Utils.isEmpty(category.getTitle()))
            holder.title.setVisibility(View.GONE);
        else {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(category.getTitle());
        }
        if (Utils.isEmpty(category.getSubtitle())) {
            holder.subtitle.setVisibility(View.GONE);
        } else {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(category.getSubtitle());
        }

        if (!Utils.isEmpty(category.getPlaylists())) {
            AudioPlaylistsCatalogAdapter adapter = new AudioPlaylistsCatalogAdapter(category.getPlaylists(), mContext);
            adapter.setClickListener(this);
            holder.list.setVisibility(View.VISIBLE);
            holder.list.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
            holder.list.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
            holder.list.setAdapter(adapter);
        } else if (!Utils.isEmpty(category.getAudios())) {
            Audio current = MusicUtils.getCurrentAudio();
            int scroll_to = category.getAudios().indexOf(current);
            AudioRecyclerAdapter adapter = new AudioRecyclerAdapter(mContext, category.getAudios(), false, false, position);
            adapter.setClickListener(this);
            holder.list.setVisibility(View.VISIBLE);
            holder.list.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.HORIZONTAL));
            holder.list.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
            holder.list.setAdapter(adapter);
            if (scroll_to >= 0)
                holder.list.scrollToPosition(scroll_to);
        } else if (!Utils.isEmpty(category.getVideos())) {
            VideosAdapter adapter = new VideosAdapter(mContext, category.getVideos());
            adapter.setVideoOnClickListener(this);
            holder.list.setVisibility(View.VISIBLE);
            holder.list.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
            holder.list.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
            holder.list.setAdapter(adapter);
        } else if (!Utils.isEmpty(category.getLinks())) {
            CatalogLinksAdapter adapter = new CatalogLinksAdapter(category.getLinks(), mContext);
            adapter.setActionListner(this);
            holder.list.setVisibility(View.VISIBLE);
            holder.list.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
            holder.list.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
            holder.list.setAdapter(adapter);
        } else
            holder.list.setVisibility(View.GONE);
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<AudioCatalog> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public void onAlbumClick(int index, AudioPlaylist album) {
        if (Utils.isEmpty(album.getOriginal_access_key()) || album.getOriginal_id() == 0 || album.getOriginal_owner_id() == 0)
            PlaceFactory.getAudiosInAlbumPlace(account_id, album.getOwnerId(), album.getId(), album.getAccess_key()).tryOpenWith(mContext);
        else
            PlaceFactory.getAudiosInAlbumPlace(account_id, album.getOriginal_owner_id(), album.getOriginal_id(), album.getOriginal_access_key()).tryOpenWith(mContext);
    }

    @Override
    public void onDelete(int index, AudioPlaylist album) {

    }

    @Override
    public void onAdd(int index, AudioPlaylist album) {
        if (clickListener != null) {
            clickListener.onAddPlayList(index, album);
        }
    }

    @Override
    public void onClick(int position, int catalog, Audio audio) {
        MusicPlaybackService.startForPlayList(mContext, new ArrayList<>(data.get(catalog).getAudios()), position, false);
        if (!Settings.get().other().isShow_mini_player())
            PlaceFactory.getPlayerPlace(account_id).tryOpenWith(mContext);
    }

    @Override
    public void onVideoClick(int position, Video video) {
        PlaceFactory.getVideoPreviewPlace(account_id, video).tryOpenWith(mContext);
    }

    @Override
    public void onLinkClick(int index, @NonNull Link doc) {
        LinkHelper.openLinkInBrowser(mContext, doc.getUrl());
    }

    public interface ClickListener {
        void onClick(int index, AudioCatalog value);

        void onAddPlayList(int index, AudioPlaylist album);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;
        RecyclerView list;
        ImageView Image;
        View catalog;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            list = itemView.findViewById(R.id.list);
            Image = itemView.findViewById(R.id.item_image);
            catalog = itemView.findViewById(R.id.item_catalog_block);
        }
    }
}
