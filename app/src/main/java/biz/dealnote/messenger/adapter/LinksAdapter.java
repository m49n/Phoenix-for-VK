package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.EventListener;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.PhotoSizes;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;

import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Created by admin on 25.12.2016.
 * phoenix
 */
public class LinksAdapter extends RecyclerBindableAdapter<Link, LinksAdapter.LinkViewHolder> {

    private ActionListener mActionListner;
    private Context context;

    public LinksAdapter(List<Link> data, Context context) {
        super(data);
        this.context = context;
    }

    public void setActionListner(ActionListener listner) {
        this.mActionListner = listner;
    }

    public String getImageUrl(Link link) {

        if (link.getPhoto() == null && link.getPreviewPhoto() != null)
            return link.getPreviewPhoto();

        if (Objects.nonNull(link.getPhoto()) && Objects.nonNull(link.getPhoto().getSizes())) {
            PhotoSizes sizes = link.getPhoto().getSizes();
            return sizes.getUrlForSize(Settings.get().main().getPrefPreviewImageSize(), true);
        }

        return null;
    }

    @Override
    protected void onBindItemViewHolder(LinkViewHolder holder, int position, int type) {
        Link item = getItem(position);

        if (Objects.isNullOrEmptyString(item.getTitle()))
            holder.tvTitle.setVisibility(View.GONE);
        else {
            holder.tvTitle.setVisibility(View.VISIBLE);
            holder.tvTitle.setText(item.getTitle());
        }
        if (Objects.isNullOrEmptyString(item.getDescription()))
            holder.tvDescription.setVisibility(View.GONE);
        else {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(item.getDescription());
        }
        if (Objects.isNullOrEmptyString(item.getUrl()))
            holder.tvURL.setVisibility(View.GONE);
        else {
            holder.tvURL.setVisibility(View.VISIBLE);
            holder.tvURL.setText(item.getUrl());
        }

        String imageUrl = getImageUrl(item);
        if (imageUrl != null) {
            holder.ivEmpty.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.VISIBLE);
            ViewUtils.displayAvatar(holder.ivImage, null, imageUrl, Constants.PICASSO_TAG);
        } else {
            PicassoInstance.with().cancelRequest(holder.ivImage);
            holder.ivImage.setVisibility(View.GONE);
            holder.ivEmpty.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (nonNull(mActionListner)) {
                mActionListner.onLinkClick(holder.getBindingAdapterPosition(), item);
            }
        });
    }

    @Override
    protected LinkViewHolder viewHolder(View view, int type) {
        return new LinkViewHolder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_document_list;
    }

    public interface ActionListener extends EventListener {
        void onLinkClick(int index, @NonNull Link doc);
    }

    static class LinkViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivEmpty;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvURL;

        private LinkViewHolder(View root) {
            super(root);
            ivImage = root.findViewById(R.id.item_document_image);
            ivEmpty = root.findViewById(R.id.item_document_empty);
            tvTitle = root.findViewById(R.id.item_document_title);
            tvDescription = root.findViewById(R.id.item_document_description);
            tvURL = root.findViewById(R.id.item_document_url);
        }
    }
}
