package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.AbsRecyclerViewAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.link.internal.LinkActionAdapter;
import biz.dealnote.messenger.link.internal.OwnerLinkSpanFactory;
import biz.dealnote.messenger.model.Comment;
import biz.dealnote.messenger.model.NewsfeedComment;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.model.PhotoWithOwner;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Topic;
import biz.dealnote.messenger.model.TopicWithOwner;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VideoWithOwner;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.AspectRatioImageView;
import biz.dealnote.messenger.view.VideoServiceIcons;
import biz.dealnote.messenger.view.emoji.EmojiconTextView;

import static biz.dealnote.messenger.util.AppTextUtils.getDateFromUnixTime;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.nonEmpty;
import static biz.dealnote.messenger.util.ViewUtils.displayAvatar;

public class NewsfeedCommentsAdapter extends AbsRecyclerViewAdapter<NewsfeedCommentsAdapter.AbsHolder> {

    private static final int VTYPE_POST = 1;
    private static final int VTYPE_VIDEO = 2;
    private static final int VTYPE_PHOTO = 3;
    private static final int VTYPE_TOPIC = 4;
    private final Context context;
    private final AttachmentsViewBinder attachmentsViewBinder;
    private final Transformation transformation;
    private final LinkActionAdapter linkActionAdapter;
    private final int colorTextSecondary;
    private final int iconColorActive;
    private List<NewsfeedComment> data;
    private ActionListener actionListener;

    public NewsfeedCommentsAdapter(Context context, List<NewsfeedComment> data,
                                   AttachmentsViewBinder.OnAttachmentsActionCallback callback) {
        this.context = context;
        this.data = data;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
        this.attachmentsViewBinder = new AttachmentsViewBinder(context, callback);

        this.colorTextSecondary = CurrentTheme.getSecondaryTextColorCode(context);
        this.iconColorActive = CurrentTheme.getColorPrimary(context);

        this.linkActionAdapter = new LinkActionAdapter() {
            // do nothing
        };
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public AbsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VTYPE_POST:
                return new PostHolder(inflater.inflate(R.layout.item_newsfeed_comment_post, parent, false));
            case VTYPE_VIDEO:
                return new VideoHolder(inflater.inflate(R.layout.item_newsfeed_comment_video, parent, false));
            case VTYPE_PHOTO:
                return new PhotoHolder(inflater.inflate(R.layout.item_newsfeed_comment_photo, parent, false));
            case VTYPE_TOPIC:
                return new TopicHolder(inflater.inflate(R.layout.item_newsfeed_comment_topic, parent, false));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public void onBindViewHolder(@NonNull AbsHolder holder, int position) {
        bindBase(holder, position);

        switch (getItemViewType(position)) {
            case VTYPE_POST:
                bindPost((PostHolder) holder, position);
                break;
            case VTYPE_VIDEO:
                bindVideo((VideoHolder) holder, position);
                break;
            case VTYPE_PHOTO:
                bindPhoto((PhotoHolder) holder, position);
                break;
            case VTYPE_TOPIC:
                bindTopic((TopicHolder) holder, position);
                break;
        }
    }

    private void bindTopic(TopicHolder holder, int position) {
        TopicWithOwner wrapper = (TopicWithOwner) data.get(position).getModel();
        Topic topic = wrapper.getTopic();
        Owner owner = wrapper.getOwner();

        ViewUtils.displayAvatar(holder.ownerAvatar, transformation, owner.getMaxSquareAvatar(), Constants.PICASSO_TAG);

        if (nonNull(topic.getCreator())) {
            holder.creatorAvatar.setVisibility(View.VISIBLE);
            ViewUtils.displayAvatar(holder.creatorAvatar, transformation, topic.getCreator().get100photoOrSmaller(), Constants.PICASSO_TAG);
        } else {
            holder.creatorAvatar.setVisibility(View.GONE);
            PicassoInstance.with().cancelRequest(holder.creatorAvatar);
        }

        super.addOwnerAvatarClickHandling(holder.ownerAvatar, topic.getOwnerId());

        holder.ownerName.setText(owner.getFullName());
        holder.commentsCounter.setText(String.valueOf(topic.getCommentsCount()));
        holder.title.setText(topic.getTitle());
    }

    private void bindPhoto(PhotoHolder holder, int position) {
        PhotoWithOwner wrapper = (PhotoWithOwner) data.get(position).getModel();
        Photo photo = wrapper.getPhoto();
        Owner owner = wrapper.getOwner();

        ViewUtils.displayAvatar(holder.ownerAvatar, transformation, owner.getMaxSquareAvatar(), Constants.PICASSO_TAG);
        super.addOwnerAvatarClickHandling(holder.ownerAvatar, photo.getOwnerId());

        holder.ownerName.setText(owner.getFullName());
        holder.dateTime.setText(AppTextUtils.getDateFromUnixTime(context, photo.getDate()));

        holder.title.setVisibility(nonEmpty(photo.getText()) ? View.VISIBLE : View.GONE);
        holder.title.setText(photo.getText());

        if (photo.getWidth() > photo.getHeight()) {
            holder.image.setAspectRatio(photo.getWidth(), photo.getHeight());
            holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            holder.image.setAspectRatio(1, 1);
            holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        String photoUrl = photo.getUrlForSize(PhotoSize.X, true);

        if (nonEmpty(photoUrl)) {
            PicassoInstance.with()
                    .load(photoUrl)
                    .into(holder.image);
        }
    }

    private void bindVideo(VideoHolder holder, int position) {
        VideoWithOwner wrapper = (VideoWithOwner) data.get(position).getModel();
        Video video = wrapper.getVideo();
        Owner owner = wrapper.getOwner();

        holder.title.setText(video.getTitle());
        holder.viewsCounter.setText(String.valueOf(video.getViews()));
        holder.datitime.setText(getDateFromUnixTime(context, video.getDate()));

        Integer serviceIcon = VideoServiceIcons.getIconByType(video.getPlatform());
        if (nonNull(serviceIcon)) {
            holder.service.setVisibility(View.VISIBLE);
            holder.service.setImageResource(serviceIcon);
        } else {
            holder.service.setVisibility(View.GONE);
        }

        if (nonEmpty(video.getImage())) {
            PicassoInstance.with()
                    .load(video.getImage())
                    .into(holder.image);
        } else {
            PicassoInstance.with()
                    .cancelRequest(holder.image);
        }

        holder.duration.setText(AppTextUtils.getDurationString(video.getDuration()));
        holder.ownerName.setText(owner.getFullName());

        ViewUtils.displayAvatar(holder.avatar, transformation, owner.getMaxSquareAvatar(), Constants.PICASSO_TAG);
        super.addOwnerAvatarClickHandling(holder.avatar, video.getOwnerId());
    }

    private void bindBase(AbsHolder holder, int position) {
        NewsfeedComment newsfeedComment = data.get(position);
        Comment comment = newsfeedComment.getComment();

        if (isNull(comment)) {
            holder.commentRoot.setVisibility(View.GONE);
            return;
        }

        holder.commentRoot.setVisibility(View.VISIBLE);
        holder.commentRoot.setOnClickListener(v -> {
            if (nonNull(actionListener)) {
                actionListener.onCommentBodyClick(newsfeedComment);
            }
        });

        holder.commentAttachmentRoot.setVisibility(comment.hasAttachments() ? View.VISIBLE : View.GONE);
        attachmentsViewBinder.displayAttachments(comment.getAttachments(), holder.commentAttachmentHolder, true, null);

        displayAvatar(holder.commentAvatar, transformation, comment.getMaxAuthorAvaUrl(), Constants.PICASSO_TAG);

        holder.commentAuthorName.setText(comment.getFullAuthorName());
        holder.commentDatetime.setText(getDateFromUnixTime(context, comment.getDate()));

        Spannable text = OwnerLinkSpanFactory.withSpans(comment.getText(), true, true, linkActionAdapter);
        holder.commentText.setText(text, TextView.BufferType.SPANNABLE);
        holder.commentText.setVisibility(isEmpty(comment.getText()) ? View.GONE : View.VISIBLE);

        holder.commentLikeRoot.setVisibility(comment.getLikesCount() > 0 ? View.VISIBLE : View.GONE);
        holder.commentLikeCounter.setText(String.valueOf(comment.getLikesCount()));
        Utils.setColorFilter(holder.commentLikeIcon, comment.isUserLikes() ? iconColorActive : colorTextSecondary);

        super.addOwnerAvatarClickHandling(holder.commentAvatar, comment.getFromId());
    }

    private void bindPost(PostHolder holder, int position) {
        NewsfeedComment comment = data.get(position);
        Post post = (Post) comment.getModel();

        attachmentsViewBinder.displayAttachments(post.getAttachments(), holder.postAttachmentsHolder, false, null);
        attachmentsViewBinder.displayCopyHistory(post.getCopyHierarchy(), holder.postAttachmentsHolder.getVgPosts(), true, R.layout.item_copy_history_post);

        holder.ownerName.setText(post.getAuthorName());
        holder.postDatetime.setText(getDateFromUnixTime(context, post.getDate()));

        displayAvatar(holder.ownerAvatar, transformation, post.getAuthorPhoto(), Constants.PICASSO_TAG);
        super.addOwnerAvatarClickHandling(holder.ownerAvatar, post.getOwnerId());

        String reduced = AppTextUtils.reduceStringForPost(post.getText());
        holder.postText.setText(OwnerLinkSpanFactory.withSpans(reduced, true, false, linkActionAdapter));
        holder.buttonShowMore.setVisibility(post.hasText() && post.getText().length() > 400 ? View.VISIBLE : View.GONE);
        holder.postTextRoot.setVisibility(post.hasText() ? View.VISIBLE : View.GONE);

        holder.signerRoot.setVisibility(isNull(post.getCreator()) ? View.GONE : View.VISIBLE);
        super.addOwnerAvatarClickHandling(holder.signerRoot, post.getSignerId());

        if (nonNull(post.getCreator())) {
            holder.signerName.setText(post.getCreator().getFullName());
            displayAvatar(holder.signerAvatar, transformation, post.getCreator().getPhoto50(), Constants.PICASSO_TAG);
        }

        holder.viewsCounter.setText(String.valueOf(post.getViewCount()));
        holder.viewsCounterRoot.setVisibility(post.getViewCount() > 0 ? View.VISIBLE : View.GONE);

        holder.friendsOnlyIcon.setVisibility(post.isFriendsOnly() ? View.VISIBLE : View.GONE);

        holder.topDivider.setVisibility(WallAdapter.needToShowTopDivider(post) ? View.VISIBLE : View.GONE);

        View.OnClickListener postOpenClickListener = v -> {
            if (nonNull(actionListener)) {
                actionListener.onPostBodyClick(comment);
            }
        };

        holder.postRoot.setOnClickListener(postOpenClickListener);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        NewsfeedComment comment = data.get(position);
        if (comment.getModel() instanceof Post) {
            return VTYPE_POST;
        }

        if (comment.getModel() instanceof VideoWithOwner) {
            return VTYPE_VIDEO;
        }

        if (comment.getModel() instanceof PhotoWithOwner) {
            return VTYPE_PHOTO;
        }

        if (comment.getModel() instanceof TopicWithOwner) {
            return VTYPE_TOPIC;
        }

        throw new IllegalStateException("Unsupported view type");
    }

    public void setData(List<NewsfeedComment> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public interface ActionListener {
        void onPostBodyClick(NewsfeedComment comment);

        void onCommentBodyClick(NewsfeedComment comment);
    }

    private static final class TopicHolder extends AbsHolder {

        ImageView ownerAvatar;
        ImageView creatorAvatar;
        TextView commentsCounter;
        TextView ownerName;
        TextView title;

        TopicHolder(View itemView) {
            super(itemView);
            ownerAvatar = itemView.findViewById(R.id.owner_avatar);
            creatorAvatar = itemView.findViewById(R.id.creator_avatar);
            commentsCounter = itemView.findViewById(R.id.comments_counter);
            ownerName = itemView.findViewById(R.id.owner_name);
            title = itemView.findViewById(R.id.title);
        }
    }

    abstract static class AbsHolder extends RecyclerView.ViewHolder {

        View commentRoot;

        ImageView commentAvatar;
        TextView commentAuthorName;
        EmojiconTextView commentText;
        TextView commentDatetime;

        View commentLikeRoot;
        TextView commentLikeCounter;
        ImageView commentLikeIcon;

        ViewGroup commentAttachmentRoot;
        AttachmentsHolder commentAttachmentHolder;

        AbsHolder(View itemView) {
            super(itemView);

            this.commentRoot = itemView.findViewById(R.id.comment_root);
            this.commentAvatar = itemView.findViewById(R.id.item_comment_owner_avatar);
            this.commentAuthorName = itemView.findViewById(R.id.item_comment_owner_name);
            this.commentText = itemView.findViewById(R.id.item_comment_text);
            this.commentDatetime = itemView.findViewById(R.id.item_comment_time);

            this.commentLikeRoot = itemView.findViewById(R.id.item_comment_like_root);
            this.commentLikeCounter = itemView.findViewById(R.id.item_comment_like_counter);
            this.commentLikeIcon = itemView.findViewById(R.id.item_comment_like);

            this.commentAttachmentRoot = commentRoot.findViewById(R.id.item_comment_attachments_root);
            this.commentAttachmentHolder = AttachmentsHolder.forComment(commentAttachmentRoot);
        }
    }

    private static class PhotoHolder extends AbsHolder {

        ImageView ownerAvatar;
        TextView ownerName;
        TextView dateTime;
        TextView title;
        AspectRatioImageView image;
        View divider;

        PhotoHolder(View itemView) {
            super(itemView);
            this.ownerAvatar = itemView.findViewById(R.id.photo_owner_avatar);
            this.ownerName = itemView.findViewById(R.id.photo_owner_name);
            this.dateTime = itemView.findViewById(R.id.photo_datetime);
            this.image = itemView.findViewById(R.id.photo_image);
            this.title = itemView.findViewById(R.id.photo_title);
        }
    }

    private static class VideoHolder extends AbsHolder {

        TextView title;
        TextView datitime;
        TextView viewsCounter;

        ImageView service;
        ImageView image;
        TextView duration;

        ImageView avatar;
        TextView ownerName;

        VideoHolder(View itemView) {
            super(itemView);
            this.avatar = itemView.findViewById(R.id.video_owner_avatar);
            this.ownerName = itemView.findViewById(R.id.video_owner_name);

            this.title = itemView.findViewById(R.id.video_title);
            this.datitime = itemView.findViewById(R.id.video_datetime);
            this.viewsCounter = itemView.findViewById(R.id.video_views_counter);

            this.service = itemView.findViewById(R.id.video_service);
            this.image = itemView.findViewById(R.id.video_image);
            this.duration = itemView.findViewById(R.id.video_lenght);
        }
    }

    private static class PostHolder extends AbsHolder {

        ImageView ownerAvatar;
        TextView ownerName;
        TextView postDatetime;

        View postTextRoot;
        EmojiconTextView postText;
        View buttonShowMore;

        View signerRoot;
        ImageView signerAvatar;
        TextView signerName;

        AttachmentsHolder postAttachmentsHolder;

        View viewsCounterRoot;
        TextView viewsCounter;

        View friendsOnlyIcon;

        View topDivider;

        View postRoot;

        PostHolder(View itemView) {
            super(itemView);

            this.topDivider = itemView.findViewById(R.id.top_divider);

            this.ownerAvatar = itemView.findViewById(R.id.item_post_avatar);
            this.ownerName = itemView.findViewById(R.id.item_post_owner_name);
            this.postDatetime = itemView.findViewById(R.id.item_post_time);

            this.postTextRoot = itemView.findViewById(R.id.item_text_container);
            this.postText = itemView.findViewById(R.id.item_post_text);
            this.buttonShowMore = itemView.findViewById(R.id.item_post_show_more);

            ViewGroup postAttachmentRoot = itemView.findViewById(R.id.item_post_attachments);
            this.postAttachmentsHolder = AttachmentsHolder.forPost(postAttachmentRoot);

            this.signerRoot = itemView.findViewById(R.id.item_post_signer_root);
            this.signerAvatar = itemView.findViewById(R.id.item_post_signer_icon);
            this.signerName = itemView.findViewById(R.id.item_post_signer_name);

            this.viewsCounter = itemView.findViewById(R.id.post_views_counter);
            this.viewsCounterRoot = itemView.findViewById(R.id.post_views_counter_root);

            this.friendsOnlyIcon = itemView.findViewById(R.id.item_post_friedns_only);

            this.postRoot = itemView.findViewById(R.id.post_root);
        }
    }
}