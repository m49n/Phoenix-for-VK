package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.api.model.VKApiCommunity;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.Objects;

import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class CommunityLinksAdapter extends RecyclerView.Adapter<CommunityLinksAdapter.Holder> {

    private List<VKApiCommunity.Link> links;
    private ActionListener actionListener;

    public CommunityLinksAdapter(List<VKApiCommunity.Link> links) {
        this.links = links;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_link, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Context context = holder.itemView.getContext();

        VKApiCommunity.Link link = links.get(position);

        holder.title.setText(link.name);
        holder.subtitle.setText(link.desc);

        holder.itemView.setOnClickListener(v -> {
            if (Objects.nonNull(actionListener)) {
                actionListener.onClick(link);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (Objects.nonNull(actionListener)) {
                actionListener.onLongClick(link);
            }
            return true;
        });

        String photoUrl = link.photo_50;

        if (nonEmpty(photoUrl)) {
            holder.icon.setVisibility(View.VISIBLE);
            PicassoInstance.with()
                    .load(photoUrl)
                    .resize(50, 50)
                    .centerCrop()
                    .transform(CurrentTheme.createTransformationForAvatar(context))
                    .into(holder.icon);
        } else {
            PicassoInstance.with()
                    .cancelRequest(holder.icon);
            holder.icon.setVisibility(View.GONE);
        }
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public int getItemCount() {
        return links.size();
    }

    public void setData(List<VKApiCommunity.Link> data) {
        this.links = data;
        notifyDataSetChanged();
    }

    public interface ActionListener {
        void onClick(VKApiCommunity.Link link);

        void onLongClick(VKApiCommunity.Link link);
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView title;
        TextView subtitle;
        ImageView icon;

        Holder(View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.title);
            this.subtitle = itemView.findViewById(R.id.subtitle);
            this.icon = itemView.findViewById(R.id.icon);
        }
    }
}