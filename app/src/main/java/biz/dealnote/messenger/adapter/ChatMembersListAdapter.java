package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.AppChatUser;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.model.UserPlatform;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.OnlineView;

import static biz.dealnote.messenger.util.Utils.isEmpty;

public class ChatMembersListAdapter extends RecyclerView.Adapter<ChatMembersListAdapter.ViewHolder> {

    private List<AppChatUser> data;
    private Transformation transformation;
    private int paddingForFirstLast;
    private ActionListener actionListener;

    public ChatMembersListAdapter(Context context, List<AppChatUser> users) {
        this.data = users;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
        this.paddingForFirstLast = Utils.is600dp(context) ? (int) Utils.dpToPx(16, context) : 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_chat_user_list, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Context context = holder.itemView.getContext();

        final AppChatUser item = data.get(position);
        final Owner user = item.getMember();

        boolean online = false;
        boolean onlineMobile = false;

        @UserPlatform
        int platform = UserPlatform.UNKNOWN;
        int app = 0;

        if (user instanceof User) {
            User interlocuter = (User) user;
            online = interlocuter.isOnline();
            onlineMobile = interlocuter.isOnlineMobile();
            platform = interlocuter.getPlatform();
            app = interlocuter.getOnlineApp();
        }

        Integer iconRes = ViewUtils.getOnlineIcon(online, onlineMobile, platform, app);
        holder.vOnline.setIcon(iconRes != null ? iconRes : 0);
        holder.vOnline.setVisibility(online ? View.VISIBLE : View.GONE);

        String userAvatarUrl = user.getMaxSquareAvatar();

        if (isEmpty(userAvatarUrl)) {
            PicassoInstance.with()
                    .load(R.drawable.ic_avatar_unknown)
                    .transform(transformation)
                    .into(holder.ivAvatar);
        } else {
            PicassoInstance.with()
                    .load(userAvatarUrl)
                    .transform(transformation)
                    .into(holder.ivAvatar);
        }

        holder.tvName.setText(user.getFullName());
        boolean isCreator = user.getOwnerId() == item.getInvitedBy();

        if (isCreator) {
            holder.tvSubline.setText(R.string.creator_of_conversation);
        } else {
            holder.tvSubline.setText(context.getString(R.string.invited_by, item.getInviter().getFullName()));
        }

        holder.itemView.setOnClickListener(view -> {
            if (Objects.nonNull(actionListener)) {
                actionListener.onUserClick(item);
            }
        });

        holder.vRemove.setVisibility(item.isCanRemove() ? View.VISIBLE : View.GONE);
        holder.vRemove.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRemoveClick(item);
            }
        });

        View view = holder.itemView;

        view.setPadding(view.getPaddingLeft(),
                position == 0 ? paddingForFirstLast : 0,
                view.getPaddingRight(),
                position == getItemCount() - 1 ? paddingForFirstLast : 0);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<AppChatUser> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public interface ActionListener {
        void onRemoveClick(AppChatUser user);

        void onUserClick(AppChatUser user);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        OnlineView vOnline;
        ImageView ivAvatar;
        TextView tvName;
        TextView tvSubline;
        View vRemove;

        ViewHolder(View root) {
            super(root);
            vOnline = root.findViewById(R.id.item_user_online);
            ivAvatar = root.findViewById(R.id.item_user_avatar);
            tvName = root.findViewById(R.id.item_user_name);
            tvSubline = root.findViewById(R.id.item_user_invited_by);
            vRemove = root.findViewById(R.id.item_user_remove);
        }
    }
}