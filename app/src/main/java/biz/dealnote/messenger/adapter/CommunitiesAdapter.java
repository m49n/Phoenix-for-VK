package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.multidata.MultyDataAdapter;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.DataWrapper;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;

public class CommunitiesAdapter extends MultyDataAdapter<Community, CommunitiesAdapter.Holder> {

    private static final ItemInfo<Community> INFO = new ItemInfo<>();
    private final Transformation transformation;
    private ActionListener actionListener;

    public CommunitiesAdapter(Context context, List<DataWrapper<Community>> dataWrappers, int[] titles) {
        super(dataWrappers, titles);
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
    }

    @NotNull
    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return new Holder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_community, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        get(position, INFO);

        Community community = INFO.item;
        holder.headerRoot.setVisibility(INFO.internalPosition == 0 ? View.VISIBLE : View.GONE);
        holder.headerTitle.setText(INFO.sectionTitleRes);

        ViewUtils.displayAvatar(holder.ivAvatar, transformation, community.getMaxSquareAvatar(), Constants.PICASSO_TAG);

        holder.tvName.setText(community.getFullName());
        holder.subtitle.setText(R.string.community);

        holder.contentRoot.setOnClickListener(view -> {
            if (Objects.nonNull(actionListener)) {
                actionListener.onCommunityClick(community);
            }
        });
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public interface ActionListener {
        void onCommunityClick(Community community);
    }

    static class Holder extends RecyclerView.ViewHolder {

        View headerRoot;
        TextView headerTitle;

        View contentRoot;
        TextView tvName;
        ImageView ivAvatar;
        TextView subtitle;

        Holder(View root) {
            super(root);
            this.headerRoot = root.findViewById(R.id.header_root);
            this.headerTitle = root.findViewById(R.id.header_title);
            this.contentRoot = root.findViewById(R.id.content_root);
            this.tvName = root.findViewById(R.id.name);
            this.ivAvatar = root.findViewById(R.id.avatar);
            this.subtitle = root.findViewById(R.id.subtitle);
        }
    }
}