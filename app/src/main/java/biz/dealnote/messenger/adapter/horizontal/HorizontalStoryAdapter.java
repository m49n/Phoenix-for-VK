package biz.dealnote.messenger.adapter.horizontal;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;

public class HorizontalStoryAdapter extends RecyclerBindableAdapter<Story, HorizontalStoryAdapter.Holder> {

    private Listener listener;

    public HorizontalStoryAdapter(List<Story> data) {
        super(data);
    }

    @Override
    protected void onBindItemViewHolder(Holder holder, int position, int type) {
        final Story item = getItem(position);

        Context context = holder.itemView.getContext();
        holder.name.setText(item.getOwner().getFullName());
        if (item.getExpires() <= 0)
            holder.expires.setVisibility(View.INVISIBLE);
        else {
            holder.expires.setVisibility(View.VISIBLE);
            if (item.isIs_expired()) {
                holder.expires.setText(R.string.is_expired);
            } else {
                Long exp = (item.getExpires() - Calendar.getInstance().getTime().getTime() / 1000) / 3600;
                holder.expires.setText(context.getString(R.string.expires, String.valueOf(exp), context.getString(Utils.declOfNum(exp, new int[]{R.string.hour, R.string.hour_sec, R.string.hours}))));
            }
        }

        if (Objects.isNull(item.getOwner())) {
            ViewUtils.displayAvatar(holder.story_image, new RoundTransformation(), null, Constants.PICASSO_TAG);
        } else {
            ViewUtils.displayAvatar(holder.story_image, new RoundTransformation(), item.getOwner().getMaxSquareAvatar(), Constants.PICASSO_TAG);
        }

        holder.itemView.setOnClickListener(v -> listener.onOptionClick(item, position));
    }

    @Override
    protected Holder viewHolder(View view, int type) {
        return new Holder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_story;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onOptionClick(Story item, int pos);
    }

    static class Holder extends RecyclerView.ViewHolder {

        MaterialCardView background;
        ImageView story_image;
        TextView name;
        TextView expires;

        Holder(View itemView) {
            super(itemView);
            background = itemView.findViewById(R.id.card_view);
            name = itemView.findViewById(R.id.item_story_name);
            expires = itemView.findViewById(R.id.item_story_expires);
            story_image = itemView.findViewById(R.id.item_story_pic);
        }
    }
}
