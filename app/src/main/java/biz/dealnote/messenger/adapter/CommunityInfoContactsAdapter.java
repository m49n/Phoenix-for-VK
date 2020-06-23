package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.Manager;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.OnlineView;

public class CommunityInfoContactsAdapter extends RecyclerView.Adapter<CommunityInfoContactsAdapter.Holder> {

    private List<Manager> users;
    private Transformation transformation;
    private ActionListener actionListener;

    public CommunityInfoContactsAdapter(Context context, List<Manager> users) {
        this.users = users;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_manager, parent, false));
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        Manager manager = users.get(position);
        User user = manager.getUser();

        holder.name.setText(user.getFullName());

        ViewUtils.displayAvatar(holder.avatar, transformation, user.getMaxSquareAvatar(), Constants.PICASSO_TAG);

        Integer onlineRes = ViewUtils.getOnlineIcon(user.isOnline(), user.isOnlineMobile(), user.getPlatform(), user.getOnlineApp());
        if (Objects.nonNull(onlineRes)) {
            holder.onlineView.setIcon(onlineRes);
            holder.onlineView.setVisibility(View.VISIBLE);
        } else {
            holder.onlineView.setVisibility(View.GONE);
        }

        @StringRes
        Integer roleTextRes;

        if (manager.getContactInfo() != null && manager.getContactInfo().getDescriprion() != null)
            holder.role.setText(manager.getContactInfo().getDescriprion());
        else {
            roleTextRes = R.string.role_unknown;
            holder.role.setText(roleTextRes);
        }
        holder.itemView.setOnClickListener(v -> {
            if (Objects.nonNull(actionListener)) {
                actionListener.onManagerClick(user);
            }
        });

    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setData(List<Manager> data) {
        this.users = data;
        notifyDataSetChanged();
    }

    public interface ActionListener {
        void onManagerClick(User manager);
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView avatar;
        OnlineView onlineView;
        TextView name;
        TextView role;

        Holder(View itemView) {
            super(itemView);
            this.avatar = itemView.findViewById(R.id.avatar);
            this.onlineView = itemView.findViewById(R.id.online);
            this.name = itemView.findViewById(R.id.name);
            this.role = itemView.findViewById(R.id.role);
        }
    }
}