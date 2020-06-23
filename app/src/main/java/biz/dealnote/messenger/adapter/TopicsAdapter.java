package biz.dealnote.messenger.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.util.EventListener;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.Topic;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.util.Utils.isEmpty;

public class TopicsAdapter extends RecyclerBindableAdapter<Topic, TopicsAdapter.ViewHolder> {

    private Transformation transformation;
    private ActionListener mActionListener;
    private int firstLastPadding = 0;

    public TopicsAdapter(Context context, List<Topic> topics, @NonNull ActionListener actionListener) {
        super(topics);
        this.mActionListener = actionListener;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);

        if (Utils.is600dp(context)) {
            firstLastPadding = (int) Utils.dpToPx(16, context);
        }
    }

    @Override
    protected void onBindItemViewHolder(ViewHolder holder, int position, int type) {
        final Topic item = getItem(position - getHeadersCount());
        Context context = holder.itemView.getContext();

        holder.title.setText(item.getTitle());
        holder.subtitle.setText(context.getString(R.string.topic_comments_counter,
                AppTextUtils.getDateFromUnixTime(item.getLastUpdateTime()), item.getCommentsCount()));

        holder.itemView.setOnLongClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.itemView);
            popup.inflate(R.menu.topics_item_menu);
            popup.setOnMenuItemClickListener(item1 -> {
                if (item1.getItemId() == R.id.copy_url) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(context.getString(R.string.link), "vk.com/topic" + item.getOwnerId() + "_" + item.getId());
                    clipboard.setPrimaryClip(clip);

                    PhoenixToast.CreatePhoenixToast(context).showToast(R.string.copied);
                    return true;
                }
                return false;
            });
            popup.show();
            return false;
        });

        String avaUrl = Objects.isNull(item.getUpdater()) ? null : item.getUpdater().getMaxSquareAvatar();

        if (isEmpty(avaUrl)) {
            PicassoInstance.with()
                    .load(R.drawable.ic_avatar_unknown)
                    .transform(transformation)
                    .into(holder.creator);
        } else {
            PicassoInstance.with()
                    .load(avaUrl)
                    .transform(transformation)
                    .into(holder.creator);
        }

        holder.itemView.setPadding(holder.itemView.getPaddingLeft(),
                position == 0 ? firstLastPadding : 0,
                holder.itemView.getPaddingRight(),
                position == getItemCount() - 1 ? firstLastPadding : 0);

        holder.itemView.setOnClickListener(view -> mActionListener.onTopicClick(item));
    }

    @Override
    protected ViewHolder viewHolder(View view, int type) {
        return new ViewHolder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_topic;
    }

    public interface ActionListener extends EventListener {
        void onTopicClick(@NonNull Topic topic);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView subtitle;
        private ImageView creator;

        private ViewHolder(View root) {
            super(root);
            title = root.findViewById(R.id.item_topic_title);
            subtitle = root.findViewById(R.id.item_topic_subtitle);
            creator = root.findViewById(R.id.item_topicstarter_avatar);
        }
    }
}
