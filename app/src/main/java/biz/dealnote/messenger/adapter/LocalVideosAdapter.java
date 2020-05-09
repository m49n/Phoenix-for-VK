package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.MemoryPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.LocalVideo;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.view.AspectRatioImageView;

public class LocalVideosAdapter extends RecyclerView.Adapter<LocalVideosAdapter.ViewHolder> {

    public static final String TAG = LocalVideosAdapter.class.getSimpleName();

    private Context context;
    private List<LocalVideo> data;
    private Set<ViewHolder> holders;
    private ClickListener clickListener;

    public LocalVideosAdapter(Context context, List<LocalVideo> data) {
        this.context = context;
        this.data = data;
        this.holders = new HashSet<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_local_video, parent, false));
        holders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final LocalVideo video = data.get(position);
        holder.attachPhoto(video);

        PicassoInstance.with()
                .load(LocalVideo.buildUriForPicasso(video.getId()))
                .tag(TAG)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView);

        resolveSelectionVisibility(video, holder);
        resolveIndexText(video, holder);

        View.OnClickListener listener = v -> {
            if (clickListener != null) {
                clickListener.onVideoClick(holder, video);
            }
        };

        holder.photoImageView.setOnClickListener(listener);
        holder.selectedRoot.setOnClickListener(listener);
    }

    public void updateHoldersSelectionAndIndexes() {
        for (ViewHolder holder : holders) {
            LocalVideo video = (LocalVideo) holder.itemView.getTag();

            if (video == null) {
                // TODO: 13.12.2017 Photo can bee null !!!! WTF?
                continue;
            }

            resolveSelectionVisibility(video, holder);
            resolveIndexText(video, holder);
        }
    }

    private void resolveSelectionVisibility(LocalVideo video, ViewHolder holder) {
        holder.selectedRoot.setVisibility(video.isSelected() ? View.VISIBLE : View.GONE);
    }

    private void resolveIndexText(LocalVideo video, ViewHolder holder) {
        holder.tvTitle.setText(video.getTitle());
        holder.tvDuration.setText(video.getDuration() == 0 ? "" : AppTextUtils.getDurationStringMS(video.getDuration()));
        holder.tvIndex.setText(video.getIndex() == 0 ? "" : String.valueOf(video.getIndex()));
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface ClickListener {
        void onVideoClick(ViewHolder holder, LocalVideo video);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        AspectRatioImageView photoImageView;
        View selectedRoot;
        TextView tvIndex;
        TextView tvTitle;
        TextView tvDuration;

        public ViewHolder(final View itemView) {
            super(itemView);
            this.photoImageView = itemView.findViewById(R.id.item_video_album_image);
            this.selectedRoot = itemView.findViewById(R.id.selected);
            this.tvIndex = itemView.findViewById(R.id.item_video_index);
            this.tvTitle = itemView.findViewById(R.id.item_video_album_title);
            this.tvDuration = itemView.findViewById(R.id.item_video_album_count);
        }

        private void attachPhoto(LocalVideo video) {
            itemView.setTag(video);
        }
    }
}
