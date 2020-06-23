package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.squareup.picasso.Transformation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.link.internal.LinkActionAdapter;
import biz.dealnote.messenger.link.internal.OwnerLinkSpanFactory;
import biz.dealnote.messenger.model.CryptStatus;
import biz.dealnote.messenger.model.GiftItem;
import biz.dealnote.messenger.model.LastReadId;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.MessageStatus;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.BubbleLinearLayout;
import biz.dealnote.messenger.view.OnlineView;
import biz.dealnote.messenger.view.emoji.EmojiconTextView;

import static biz.dealnote.messenger.util.AppTextUtils.getDateFromUnixTime;
import static biz.dealnote.messenger.util.Objects.nonNull;

public class MessagesAdapter extends RecyclerBindableAdapter<Message, RecyclerView.ViewHolder> {

    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_FRIEND_MESSAGE = 2;
    private static final int TYPE_SERVICE = 3;
    private static final int TYPE_STICKER_MY = 4;
    private static final int TYPE_STICKER_FRIEND = 5;
    private static final int TYPE_GIFT_MY = 6;
    private static final int TYPE_GIFT_FRIEND = 7;
    private static final int TYPE_GRAFFITY_MY = 8;
    private static final int TYPE_GRAFFITY_FRIEND = 9;
    private static final Date DATE = new Date();
    private SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private Context context;
    private AttachmentsViewBinder attachmentsViewBinder;
    private Transformation avatarTransformation;
    private ShapeDrawable selectedDrawable;
    private int unreadColor;
    private EmojiconTextView.OnHashTagClickListener onHashTagClickListener;
    private OnMessageActionListener onMessageActionListener;
    private AttachmentsViewBinder.OnAttachmentsActionCallback attachmentsActionCallback;
    private LastReadId lastReadId;
    private OwnerLinkSpanFactory.ActionListener ownerLinkAdapter = new LinkActionAdapter() {
        @Override
        public void onOwnerClick(int ownerId) {
            if (nonNull(attachmentsActionCallback)) {
                attachmentsActionCallback.onOpenOwner(ownerId);
            }
        }
    };

    public MessagesAdapter(Context context, List<Message> items, AttachmentsViewBinder.OnAttachmentsActionCallback callback) {
        this(context, items, new LastReadId(0, 0), callback);
    }

    public MessagesAdapter(Context context, List<Message> items, LastReadId lastReadId, AttachmentsViewBinder.OnAttachmentsActionCallback callback) {
        super(items);
        this.context = context;
        this.lastReadId = lastReadId;
        this.attachmentsActionCallback = callback;
        this.attachmentsViewBinder = new AttachmentsViewBinder(context, callback);
        this.avatarTransformation = CurrentTheme.createTransformationForAvatar(context);
        this.selectedDrawable = new ShapeDrawable(new OvalShape());
        this.selectedDrawable.getPaint().setColor(CurrentTheme.getColorPrimary(context));
        this.unreadColor = CurrentTheme.getMessageUnreadColor(context);
    }

    @Override
    protected void onBindItemViewHolder(RecyclerView.ViewHolder viewHolder, int position, int type) {
        Message message = getItem(position);
        switch (type) {
            case TYPE_SERVICE:
                bindServiceHolder((ServiceMessageHolder) viewHolder, message);
                break;
            case TYPE_GRAFFITY_FRIEND:
            case TYPE_GRAFFITY_MY:
            case TYPE_MY_MESSAGE:
            case TYPE_FRIEND_MESSAGE:
                bindNormalMessage((MessageHolder) viewHolder, message);
                break;
            case TYPE_STICKER_FRIEND:
            case TYPE_STICKER_MY:
                bindStickerHolder((StickerMessageHolder) viewHolder, message);
                break;
            case TYPE_GIFT_FRIEND:
            case TYPE_GIFT_MY:
                bindGiftHolder((GiftMessageHolder) viewHolder, message);
                break;
        }
    }

    private void bindStickerHolder(StickerMessageHolder holder, final Message message) {
        bindBaseMessageHolder(holder, message);

        if (message.isDeleted()) {
            holder.root.setAlpha(0.6f);
            holder.Restore.setVisibility(View.VISIBLE);
            holder.Restore.setOnClickListener(v -> {
                if (onMessageActionListener != null) {
                    onMessageActionListener.onRestoreClick(message, holder.getBindingAdapterPosition());
                }
            });
        } else {
            holder.root.setAlpha(1);
            holder.Restore.setVisibility(View.GONE);
        }

        Sticker sticker = message.getAttachments().getStickers().get(0);
        if (sticker.isAnimated()) {
            holder.sticker.setAnimationFromUrl(sticker.getAnimationUrl());
            holder.sticker.playAnimation();
        } else {
            Sticker.Image image = sticker.getImage(256, true);

            PicassoInstance.with()
                    .load(image.getUrl())
                    .into(holder.sticker);
        }

        boolean hasAttachments = Utils.nonEmpty(message.getFwd()) || (nonNull(message.getAttachments()) && message.getAttachments().size_no_stickers() > 0);
        holder.attachmentsRoot.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);

        if (hasAttachments) {
            attachmentsViewBinder.displayAttachments(message.getAttachments(), holder.attachmentsHolder, true, message.getId());
            attachmentsViewBinder.displayForwards(message.getFwd(), holder.forwardMessagesRoot, context, true);
        }
    }

    public void setItems(List<Message> messages, LastReadId lastReadId) {
        this.lastReadId = lastReadId;
        setItems(messages);
    }

    private void bindGiftHolder(GiftMessageHolder holder, final Message message) {
        bindBaseMessageHolder(holder, message);

        if (message.isDeleted()) {
            holder.root.setAlpha(0.6f);
            holder.Restore.setVisibility(View.VISIBLE);
            holder.Restore.setOnClickListener(v -> {
                if (onMessageActionListener != null) {
                    onMessageActionListener.onRestoreClick(message, holder.getBindingAdapterPosition());
                }
            });
        } else {
            holder.root.setAlpha(1);
            holder.Restore.setVisibility(View.GONE);
        }

        holder.message.setVisibility(TextUtils.isEmpty(message.getBody()) ? View.GONE : View.VISIBLE);
        holder.message.setText(OwnerLinkSpanFactory.withSpans(message.getBody(), true, false, ownerLinkAdapter));
        GiftItem giftItem = message.getAttachments().getGifts().get(0);

        PicassoInstance.with()
                .load(giftItem.getThumb256())
                .into(holder.gift);
    }

    private void bindStatusText(TextView textView, int status, long time, long updateTime) {
        switch (status) {
            case MessageStatus.SENDING:
                textView.setText(context.getString(R.string.sending));
                textView.setTextColor(ContextCompat.getColor(context, R.color.default_message_status));
                break;
            case MessageStatus.QUEUE:
                textView.setText(context.getString(R.string.in_order));
                textView.setTextColor(ContextCompat.getColor(context, R.color.default_message_status));
                break;
            case MessageStatus.ERROR:
                textView.setText(context.getString(R.string.error));
                textView.setTextColor(Color.RED);
                break;
            case MessageStatus.WAITING_FOR_UPLOAD:
                textView.setText(R.string.waiting_for_upload);
                textView.setTextColor(ContextCompat.getColor(context, R.color.default_message_status));
                break;
            default:
                String text = getDateFromUnixTime(time);

                if (updateTime != 0) {
                    DATE.setTime(updateTime * 1000);
                    text = text + " " + context.getString(R.string.message_edited_at, df.format(DATE));
                }

                textView.setText(text);
                textView.setTextColor(CurrentTheme.getSecondaryTextColorCode(context));
                break;
        }
    }

    private void bindReadState(View root, boolean read) {
        root.setBackgroundColor(read ? Color.TRANSPARENT : unreadColor);
        root.getBackground().setAlpha(60);
    }

    private void bindBaseMessageHolder(BaseMessageHolder holder, Message message) {
        holder.important.setVisibility(message.isImportant() ? View.VISIBLE : View.GONE);

        bindStatusText(holder.status, message.getStatus(), message.getDate(), message.getUpdateTime());

        boolean read = message.isOut() ? lastReadId.getOutgoing() >= message.getId() : lastReadId.getIncoming() >= message.getId();

        bindReadState(holder.itemView, message.getStatus() == MessageStatus.SENT && read);

        if (message.isSelected()) {
            holder.itemView.setBackgroundColor(CurrentTheme.getColorSecondary(context));
            holder.itemView.getBackground().setAlpha(80);
            holder.avatar.setBackground(selectedDrawable);
            holder.avatar.setImageResource(R.drawable.ic_message_check_vector);
        } else {
            String avaurl = message.getSender() != null ? message.getSender().getMaxSquareAvatar() : null;
            ViewUtils.displayAvatar(holder.avatar, avatarTransformation, avaurl, Constants.PICASSO_TAG);

            holder.avatar.setBackgroundColor(Color.TRANSPARENT);
        }
        if (holder.user != null)
            holder.user.setText(message.getSender().getFullName());

        holder.avatar.setOnClickListener(v -> {
            if (nonNull(onMessageActionListener)) {
                onMessageActionListener.onAvatarClick(message, message.getSenderId());
            }
        });

        holder.avatar.setOnLongClickListener(v -> {
            if (nonNull(onMessageActionListener)) {
                onMessageActionListener.onLongAvatarClick(message, message.getSenderId());
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (nonNull(onMessageActionListener)) {
                onMessageActionListener.onMessageClicked(message);
            }
        });

        holder.itemView.setOnLongClickListener(v -> nonNull(onMessageActionListener)
                && onMessageActionListener.onMessageLongClick(message));
    }

    private void bindNormalMessage(final MessageHolder holder, final Message message) {
        bindBaseMessageHolder(holder, message);

        if (message.isDeleted()) {
            holder.root.setAlpha(0.6f);
            holder.Restore.setVisibility(View.VISIBLE);
            holder.Restore.setOnClickListener(v -> {
                if (onMessageActionListener != null) {
                    onMessageActionListener.onRestoreClick(message, holder.getBindingAdapterPosition());
                }
            });
        } else {
            holder.root.setAlpha(1);
            holder.Restore.setVisibility(View.GONE);
        }

        holder.body.setVisibility(TextUtils.isEmpty(message.getBody()) ? View.GONE : View.VISIBLE);
        String displayedBody = null;

        switch (message.getCryptStatus()) {
            case CryptStatus.NO_ENCRYPTION:
            case CryptStatus.ENCRYPTED:
            case CryptStatus.DECRYPT_FAILED:
                displayedBody = message.getBody();
                break;
            case CryptStatus.DECRYPTED:
                displayedBody = message.getDecryptedBody();
                break;
        }

        if (!message.isGraffity()) {
            switch (message.getCryptStatus()) {
                case CryptStatus.ENCRYPTED:
                case CryptStatus.DECRYPT_FAILED:
                    holder.bubble.setBubbleColor(Color.parseColor("#D4ff0000"));
                    break;
                case CryptStatus.NO_ENCRYPTION:
                case CryptStatus.DECRYPTED:
                    if (message.isOut()) {
                        if (Settings.get().other().isCustom_MyMessage())
                            holder.bubble.setBubbleColor(Settings.get().other().getColorMyMessage());
                        else {
                            if (Settings.get().main().isMy_message_no_color())
                                holder.bubble.setBubbleColor(CurrentTheme.getColorFromAttrs(R.attr.message_bubble_color, context, "#D4ff0000"));
                            else
                                holder.bubble.setBubbleColor(CurrentTheme.getColorFromAttrs(R.attr.my_messages_bubble_color, context, "#D4ff0000"));
                        }
                    } else
                        holder.bubble.setBubbleColor(CurrentTheme.getColorFromAttrs(R.attr.message_bubble_color, context, "#D4ff0000"));
                    break;
            }
        }

        holder.body.setText(OwnerLinkSpanFactory.withSpans(displayedBody, true, false, ownerLinkAdapter));
        holder.encryptedView.setVisibility(message.getCryptStatus() == CryptStatus.NO_ENCRYPTION ? View.GONE : View.VISIBLE);

        boolean hasAttachments = Utils.nonEmpty(message.getFwd()) || (nonNull(message.getAttachments()) && message.getAttachments().size() > 0);
        holder.attachmentsRoot.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);

        if (hasAttachments) {
            attachmentsViewBinder.displayAttachments(message.getAttachments(), holder.attachmentsHolder, true, message.getId());
            attachmentsViewBinder.displayForwards(message.getFwd(), holder.forwardMessagesRoot, context, true);
        }
    }

    private void bindServiceHolder(ServiceMessageHolder holder, Message message) {
        if (message.isDeleted()) {
            holder.root.setAlpha(0.6f);
            holder.Restore.setVisibility(View.VISIBLE);
            holder.Restore.setOnClickListener(v -> {
                if (onMessageActionListener != null) {
                    onMessageActionListener.onRestoreClick(message, holder.getBindingAdapterPosition());
                }
            });
        } else {
            holder.root.setAlpha(1);
            holder.Restore.setVisibility(View.GONE);
        }

        boolean read = message.isOut() ? lastReadId.getOutgoing() >= message.getId() : lastReadId.getIncoming() >= message.getId();
        bindReadState(holder.itemView, message.getStatus() == MessageStatus.SENT && read);
        holder.tvAction.setText(message.getServiceText(context));
        holder.itemView.setOnClickListener(v -> {
            if (nonNull(onMessageActionListener)) {
                onMessageActionListener.onMessageClicked(message);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (nonNull(onMessageActionListener)) {
                onMessageActionListener.onMessageDelete(message);
            }
            return true;
        });
        attachmentsViewBinder.displayAttachments(message.getAttachments(), holder.mAttachmentsHolder, true, message.getId());
    }

    @Override
    protected RecyclerView.ViewHolder viewHolder(View view, int type) {
        switch (type) {
            case TYPE_GRAFFITY_FRIEND:
            case TYPE_GRAFFITY_MY:
            case TYPE_MY_MESSAGE:
            case TYPE_FRIEND_MESSAGE:
                return new MessageHolder(view);
            case TYPE_SERVICE:
                return new ServiceMessageHolder(view);
            case TYPE_STICKER_FRIEND:
            case TYPE_STICKER_MY:
                return new StickerMessageHolder(view);
            case TYPE_GIFT_FRIEND:
            case TYPE_GIFT_MY:
                return new GiftMessageHolder(view);
        }

        return null;
    }

    @Override
    protected int layoutId(int type) {
        switch (type) {
            case TYPE_MY_MESSAGE:
                return R.layout.item_message_my;
            case TYPE_FRIEND_MESSAGE:
                return R.layout.item_message_friend;
            case TYPE_GRAFFITY_MY:
                return R.layout.item_message_graffity_my;
            case TYPE_GRAFFITY_FRIEND:
                return R.layout.item_message_graffity_friend;
            case TYPE_SERVICE:
                return R.layout.item_service_message;
            case TYPE_STICKER_FRIEND:
                return R.layout.item_message_friend_sticker;
            case TYPE_STICKER_MY:
                return R.layout.item_message_my_sticker;
            case TYPE_GIFT_FRIEND:
                return R.layout.item_message_friend_gift;
            case TYPE_GIFT_MY:
                return R.layout.item_message_my_gift;
        }

        throw new IllegalArgumentException();
    }

    @Override
    protected int getItemType(int position) {
        Message m = getItem(position - getHeadersCount());
        if (m.isServiseMessage()) {
            return TYPE_SERVICE;
        }

        if (m.isSticker()) {
            return m.isOut() ? TYPE_STICKER_MY : TYPE_STICKER_FRIEND;
        }

        if (m.isGraffity()) {
            return m.isOut() ? TYPE_GRAFFITY_MY : TYPE_GRAFFITY_FRIEND;
        }

        if (m.isGift()) {
            return m.isOut() ? TYPE_GIFT_MY : TYPE_GIFT_FRIEND;
        }

        return m.isOut() ? TYPE_MY_MESSAGE : TYPE_FRIEND_MESSAGE;
    }

    public void setVoiceActionListener(AttachmentsViewBinder.VoiceActionListener voiceActionListener) {
        this.attachmentsViewBinder.setVoiceActionListener(voiceActionListener);
    }

    public void configNowVoiceMessagePlaying(int voiceId, float progress, boolean paused, boolean amin) {
        attachmentsViewBinder.configNowVoiceMessagePlaying(voiceId, progress, paused, amin);
    }

    public void bindVoiceHolderById(int holderId, boolean play, boolean paused, float progress, boolean amin) {
        attachmentsViewBinder.bindVoiceHolderById(holderId, play, paused, progress, amin);
    }

    public void disableVoiceMessagePlaying() {
        attachmentsViewBinder.disableVoiceMessagePlaying();
    }

    public void setOnHashTagClickListener(EmojiconTextView.OnHashTagClickListener onHashTagClickListener) {
        this.onHashTagClickListener = onHashTagClickListener;
    }

    public void setOnMessageActionListener(OnMessageActionListener onMessageActionListener) {
        this.onMessageActionListener = onMessageActionListener;
    }

    public interface OnMessageActionListener {
        void onAvatarClick(@NonNull Message message, int userId);

        void onLongAvatarClick(@NonNull Message message, int userId);

        void onRestoreClick(@NonNull Message message, int position);

        boolean onMessageLongClick(@NonNull Message message);

        void onMessageClicked(@NonNull Message message);

        void onMessageDelete(@NonNull Message message);
    }

    private static class ServiceMessageHolder extends RecyclerView.ViewHolder {

        View root;
        TextView tvAction;
        AttachmentsHolder mAttachmentsHolder;
        Button Restore;

        ServiceMessageHolder(View itemView) {
            super(itemView);
            tvAction = itemView.findViewById(R.id.item_service_message_text);
            root = itemView.findViewById(R.id.message_container);
            Restore = itemView.findViewById(R.id.item_message_restore);

            mAttachmentsHolder = new AttachmentsHolder();
            mAttachmentsHolder.setVgAudios(itemView.findViewById(R.id.audio_attachments)).
                    setVgVideos(itemView.findViewById(R.id.video_attachments)).
                    setVgDocs(itemView.findViewById(R.id.docs_attachments)).
                    setVgArticles(itemView.findViewById(R.id.articles_attachments)).
                    setVgPhotos(itemView.findViewById(R.id.photo_attachments)).
                    setVgPosts(itemView.findViewById(R.id.posts_attachments)).
                    setVgStickers(itemView.findViewById(R.id.stickers_attachments));
        }
    }

    private static class StickerMessageHolder extends BaseMessageHolder {

        LottieAnimationView sticker;

        View attachmentsRoot;
        AttachmentsHolder attachmentsHolder;
        ViewGroup forwardMessagesRoot;

        StickerMessageHolder(View itemView) {
            super(itemView);
            this.sticker = itemView.findViewById(R.id.sticker);
            forwardMessagesRoot = itemView.findViewById(R.id.forward_messages);
            attachmentsRoot = itemView.findViewById(R.id.item_message_attachment_container);
            attachmentsHolder = new AttachmentsHolder();
            attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
                    .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
                    .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
                    .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
                    .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
                    .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
                    .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments));
        }
    }

    private abstract static class BaseMessageHolder extends RecyclerView.ViewHolder {

        View root;
        EmojiconTextView user;
        TextView status;
        ImageView avatar;
        OnlineView important;
        Button Restore;

        BaseMessageHolder(View itemView) {
            super(itemView);
            this.root = itemView.findViewById(R.id.message_container);
            this.Restore = itemView.findViewById(R.id.item_message_restore);
            this.user = itemView.findViewById(R.id.item_message_user);
            this.status = itemView.findViewById(R.id.item_message_status_text);
            this.important = itemView.findViewById(R.id.item_message_important);
            this.avatar = itemView.findViewById(R.id.item_message_avatar);
        }
    }

    private class GiftMessageHolder extends BaseMessageHolder {
        ImageView gift;
        EmojiconTextView message;

        GiftMessageHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.item_message_text);
            message.setMovementMethod(LinkMovementMethod.getInstance());
            message.setOnHashTagClickListener(onHashTagClickListener);
            message.setOnLongClickListener(v -> this.itemView.performLongClick());
            message.setOnClickListener(v -> this.itemView.performClick());
            this.gift = itemView.findViewById(R.id.gift);
        }
    }

    private class MessageHolder extends BaseMessageHolder {
        EmojiconTextView body;
        ViewGroup forwardMessagesRoot;
        BubbleLinearLayout bubble;
        View attachmentsRoot;
        AttachmentsHolder attachmentsHolder;
        View encryptedView;

        MessageHolder(View itemView) {
            super(itemView);
            encryptedView = itemView.findViewById(R.id.item_message_encrypted);

            body = itemView.findViewById(R.id.item_message_text);
            body.setMovementMethod(LinkMovementMethod.getInstance());
            body.setOnHashTagClickListener(onHashTagClickListener);
            body.setOnLongClickListener(v -> this.itemView.performLongClick());
            body.setOnClickListener(v -> this.itemView.performClick());

            forwardMessagesRoot = itemView.findViewById(R.id.forward_messages);
            bubble = itemView.findViewById(R.id.item_message_bubble);

            attachmentsRoot = itemView.findViewById(R.id.item_message_attachment_container);
            attachmentsHolder = new AttachmentsHolder();
            attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
                    .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
                    .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
                    .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
                    .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
                    .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
                    .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments));
        }
    }
}
