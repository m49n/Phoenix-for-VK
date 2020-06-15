package biz.dealnote.messenger.adapter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.SendAttachmentsActivity;
import biz.dealnote.messenger.adapter.holder.IdentificableHolder;
import biz.dealnote.messenger.adapter.holder.SharedHolders;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.link.internal.LinkActionAdapter;
import biz.dealnote.messenger.link.internal.OwnerLinkSpanFactory;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.OptionRequest;
import biz.dealnote.messenger.model.Article;
import biz.dealnote.messenger.model.Attachments;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.CryptStatus;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.Types;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.model.WikiPage;
import biz.dealnote.messenger.model.menu.AudioItem;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.DownloadUtil;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.PolyTransformation;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.WaveFormView;
import biz.dealnote.messenger.view.WeakViewAnimatorAdapter;
import biz.dealnote.messenger.view.emoji.EmojiconTextView;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.dpToPx;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.safeIsEmpty;
import static biz.dealnote.messenger.util.Utils.safeLenghtOf;

public class AttachmentsViewBinder {

    private static final int PREFFERED_STICKER_SIZE = 120;
    private static final byte[] DEFAUL_WAVEFORM = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int sHolderIdCounter;
    private PhotosViewHelper photosViewHelper;
    private Transformation mAvatarTransformation;
    private int mActiveWaveFormColor;
    private int mNoactiveWaveFormColor;
    private SharedHolders<VoiceHolder> mVoiceSharedHolders;
    private VoiceActionListener mVoiceActionListener;
    private OnAttachmentsActionCallback mAttachmentsActionCallback;
    private EmojiconTextView.OnHashTagClickListener mOnHashTagClickListener;
    private Context mContext;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();
    private IAudioInteractor mAudioInteractor;

    public AttachmentsViewBinder(Context context, @NonNull OnAttachmentsActionCallback attachmentsActionCallback) {
        this.mContext = context;
        this.mVoiceSharedHolders = new SharedHolders<>(true);
        this.mAvatarTransformation = CurrentTheme.createTransformationForAvatar(context);
        this.photosViewHelper = new PhotosViewHelper(context, attachmentsActionCallback);
        this.mAttachmentsActionCallback = attachmentsActionCallback;
        this.mActiveWaveFormColor = CurrentTheme.getColorPrimary(context);
        this.mNoactiveWaveFormColor = Utils.adjustAlpha(mActiveWaveFormColor, 0.5f);
        this.mAudioInteractor = InteractorFactory.createAudioInteractor();
    }

    private static void safeSetVisibitity(@Nullable View view, int visibility) {
        if (view != null) view.setVisibility(visibility);
    }

    private static int generateHolderId() {
        sHolderIdCounter++;
        return sHolderIdCounter;
    }

    public void setOnHashTagClickListener(EmojiconTextView.OnHashTagClickListener onHashTagClickListener) {
        this.mOnHashTagClickListener = onHashTagClickListener;
    }

    public void displayAttachments(Attachments attachments, AttachmentsHolder containers, boolean postsAsLinks, Integer messageId) {
        if (attachments == null) {
            safeSetVisibitity(containers.getVgAudios(), View.GONE);
            safeSetVisibitity(containers.getVgVideos(), View.GONE);
            safeSetVisibitity(containers.getVgArticles(), View.GONE);
            safeSetVisibitity(containers.getVgDocs(), View.GONE);
            safeSetVisibitity(containers.getVgPhotos(), View.GONE);
            safeSetVisibitity(containers.getVgPosts(), View.GONE);
            safeSetVisibitity(containers.getVgStickers(), View.GONE);
            safeSetVisibitity(containers.getVoiceMessageRoot(), View.GONE);
            safeSetVisibitity(containers.getVgFriends(), View.GONE);
        } else {
            displayArticles(attachments.getArticles(), containers.getVgArticles());
            displayAudios(attachments.getAudios(), containers.getVgAudios());
            displayVoiceMessages(attachments.getVoiceMessages(), containers.getVoiceMessageRoot(), messageId);
            displayDocs(attachments.getDocLinks(postsAsLinks, true), containers.getVgDocs());

            if (containers.getVgStickers() != null) {
                displayStickers(attachments.getStickers(), containers.getVgStickers());
            }

            photosViewHelper.displayPhotos(attachments.getPostImages(), containers.getVgPhotos());
            photosViewHelper.displayVideos(attachments.getPostImagesVideos(), containers.getVgVideos());
        }
    }

    private void displayVoiceMessages(final ArrayList<VoiceMessage> voices, ViewGroup container, Integer messageId) {
        if (Objects.isNull(container)) return;

        boolean empty = safeIsEmpty(voices);
        container.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            return;
        }

        int i = voices.size() - container.getChildCount();
        for (int j = 0; j < i; j++) {
            container.addView(LayoutInflater.from(mContext).inflate(R.layout.item_voice_message, container, false));
        }

        for (int g = 0; g < container.getChildCount(); g++) {
            ViewGroup root = (ViewGroup) container.getChildAt(g);

            if (g < voices.size()) {
                VoiceHolder holder = (VoiceHolder) root.getTag();
                if (holder == null) {
                    holder = new VoiceHolder(root);
                    root.setTag(holder);
                }

                final VoiceMessage voice = voices.get(g);
                bindVoiceHolder(holder, voice, messageId);

                root.setVisibility(View.VISIBLE);
            } else {
                root.setVisibility(View.GONE);
            }
        }
    }

    public void bindVoiceHolderById(int holderId, boolean play, boolean paused, float progress, boolean amin) {
        VoiceHolder holder = mVoiceSharedHolders.findHolderByHolderId(holderId);
        if (nonNull(holder)) {
            bindVoiceHolderPlayState(holder, play, paused, progress, amin);
        }
    }

    private void bindVoiceHolderPlayState(VoiceHolder holder, boolean play, boolean paused, float progress, boolean anim) {
        @DrawableRes
        int icon = play && !paused ? R.drawable.pause : R.drawable.play;

        holder.mButtonPlay.setImageResource(icon);
        holder.mWaveFormView.setCurrentActiveProgress(play ? progress : 1.0f, anim);
    }

    public void configNowVoiceMessagePlaying(int voiceMessageId, float progress, boolean paused, boolean amin) {
        SparseArray<Set<WeakReference<VoiceHolder>>> holders = mVoiceSharedHolders.getCache();
        for (int i = 0; i < holders.size(); i++) {
            int key = holders.keyAt(i);

            boolean play = key == voiceMessageId;

            Set<WeakReference<VoiceHolder>> set = holders.get(key);
            for (WeakReference<VoiceHolder> reference : set) {
                VoiceHolder holder = reference.get();
                if (nonNull(holder)) {
                    bindVoiceHolderPlayState(holder, play, paused, progress, amin);
                }
            }
        }
    }

    public void setVoiceActionListener(VoiceActionListener voiceActionListener) {
        this.mVoiceActionListener = voiceActionListener;
    }

    public void disableVoiceMessagePlaying() {
        SparseArray<Set<WeakReference<VoiceHolder>>> holders = mVoiceSharedHolders.getCache();
        for (int i = 0; i < holders.size(); i++) {
            int key = holders.keyAt(i);
            Set<WeakReference<VoiceHolder>> set = holders.get(key);
            for (WeakReference<VoiceHolder> reference : set) {
                VoiceHolder holder = reference.get();
                if (nonNull(holder)) {
                    bindVoiceHolderPlayState(holder, false, false, 0f, false);
                }
            }
        }
    }

    private void bindVoiceHolder(@NonNull VoiceHolder holder, @NonNull VoiceMessage voice, Integer messageId) {
        int voiceMessageId = voice.getId();
        mVoiceSharedHolders.put(voiceMessageId, holder);

        holder.mDurationText.setText(AppTextUtils.getDurationString(voice.getDuration()));

        // can bee NULL/empty
        if (nonNull(voice.getWaveform()) && voice.getWaveform().length > 0) {
            holder.mWaveFormView.setWaveForm(voice.getWaveform());
        } else {
            holder.mWaveFormView.setWaveForm(DEFAUL_WAVEFORM);
        }

        if (isEmpty(voice.getTranscript()) && messageId != null) {
            holder.TranscriptBlock.setVisibility(View.GONE);
            holder.mDoTranscript.setVisibility(View.VISIBLE);
            holder.mDoTranscript.setOnClickListener(v -> {
                if (nonNull(mVoiceActionListener)) {
                    mVoiceActionListener.onTranscript(voice.getOwnerId() + "_" + voice.getId(), messageId);
                    holder.mDoTranscript.setVisibility(View.GONE);
                }
            });
        } else {
            holder.TranscriptBlock.setVisibility(View.VISIBLE);
            holder.TranscriptText.setText(voice.getTranscript());
            holder.mDoTranscript.setVisibility(View.GONE);
        }

        holder.mWaveFormView.setOnLongClickListener(v -> {
            if (!AppPerms.hasReadWriteStoragePermision(mContext)) {
                AppPerms.requestReadWriteStoragePermission((Activity) mContext);
                return true;
            }
            DownloadUtil.downloadVoice(mContext, voice, voice.getLinkMp3());

            return true;
        });

        holder.mButtonPlay.setOnClickListener(v -> {
            if (nonNull(mVoiceActionListener)) {
                mVoiceActionListener.onVoicePlayButtonClick(holder.getHolderId(), voiceMessageId, voice);
            }
        });

        if (nonNull(mVoiceActionListener)) {
            mVoiceActionListener.onVoiceHolderBinded(voiceMessageId, holder.getHolderId());
        }
    }

    private void displayStickers(List<Sticker> stickers, ViewGroup stickersContainer) {
        stickersContainer.setVisibility(safeIsEmpty(stickers) ? View.GONE : View.VISIBLE);
        if (isEmpty(stickers)) {
            return;
        }

        if (stickersContainer.getChildCount() == 0) {
            LottieAnimationView localView = new LottieAnimationView(mContext);
            stickersContainer.addView(localView);
        }

        LottieAnimationView imageView = (LottieAnimationView) stickersContainer.getChildAt(0);
        Sticker sticker = stickers.get(0);

        int prefferedStickerSize = (int) dpToPx(PREFFERED_STICKER_SIZE, mContext);
        Sticker.Image image = sticker.getImage(256, true);

        boolean horisontal = image.getHeight() < image.getWidth();
        double proporsion = (double) image.getWidth() / (double) image.getHeight();

        final float finalWidth;
        final float finalHeihgt;

        if (horisontal) {
            finalWidth = prefferedStickerSize;
            finalHeihgt = (float) (finalWidth / proporsion);
        } else {
            finalHeihgt = prefferedStickerSize;
            finalWidth = (float) (finalHeihgt * proporsion);
        }

        imageView.getLayoutParams().height = (int) finalHeihgt;
        imageView.getLayoutParams().width = (int) finalWidth;

        if (sticker.isAnimated()) {
            imageView.setAnimationFromUrl(sticker.getAnimationUrl());
            imageView.setRepeatCount(3);
            imageView.playAnimation();
            stickersContainer.setOnLongClickListener(e -> {
                imageView.playAnimation();
                return true;
            });
        } else {
            PicassoInstance.with()
                    .load(image.getUrl())
                    .tag(Constants.PICASSO_TAG)
                    .into(imageView);
        }
        stickersContainer.setOnClickListener(e -> openSticker(sticker));
    }

    public void displayCopyHistory(List<Post> posts, ViewGroup container, boolean reduce, int layout) {
        if (container != null) {
            container.setVisibility(safeIsEmpty(posts) ? View.GONE : View.VISIBLE);
        }

        if (safeIsEmpty(posts) || container == null) {
            return;
        }

        int i = posts.size() - container.getChildCount();
        for (int j = 0; j < i; j++) {
            View itemView = LayoutInflater.from(container.getContext()).inflate(layout, container, false);
            CopyHolder holder = new CopyHolder((ViewGroup) itemView, mAttachmentsActionCallback);
            itemView.setTag(holder);

            if (!reduce) {
                holder.bodyView.setAutoLinkMask(Linkify.WEB_URLS);
                holder.bodyView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            container.addView(itemView);
        }

        for (int g = 0; g < container.getChildCount(); g++) {
            ViewGroup postViewGroup = (ViewGroup) container.getChildAt(g);

            if (g < posts.size()) {
                final CopyHolder holder = (CopyHolder) postViewGroup.getTag();
                final Post copy = posts.get(g);

                if (isNull(copy)) {
                    postViewGroup.setVisibility(View.GONE);
                    return;
                }

                postViewGroup.setVisibility(View.VISIBLE);

                String text = reduce ? AppTextUtils.reduceStringForPost(copy.getText()) : copy.getText();

                holder.bodyView.setVisibility(isEmpty(copy.getText()) ? View.GONE : View.VISIBLE);
                holder.bodyView.setOnHashTagClickListener(mOnHashTagClickListener);
                holder.bodyView.setText(OwnerLinkSpanFactory.withSpans(text, true, false, new LinkActionAdapter() {
                    @Override
                    public void onOwnerClick(int ownerId) {
                        mAttachmentsActionCallback.onOpenOwner(ownerId);
                    }
                }));

                holder.ivAvatar.setOnClickListener(v -> mAttachmentsActionCallback.onOpenOwner(copy.getAuthorId()));
                ViewUtils.displayAvatar(holder.ivAvatar, mAvatarTransformation, copy.getAuthorPhoto(), Constants.PICASSO_TAG);

                holder.tvShowMore.setVisibility(reduce && safeLenghtOf(copy.getText()) > 400 ? View.VISIBLE : View.GONE);
                holder.ownerName.setText(copy.getAuthorName());
                holder.buttonDots.setTag(copy);

                displayAttachments(copy.getAttachments(), holder.attachmentsHolder, false, null);
            } else {
                postViewGroup.setVisibility(View.GONE);
            }
        }
    }

    public void displayForwards(List<Message> fwds, final ViewGroup fwdContainer, final Context context, boolean postsAsLinks) {
        fwdContainer.setVisibility(safeIsEmpty(fwds) ? View.GONE : View.VISIBLE);
        if (safeIsEmpty(fwds)) {
            return;
        }

        final int i = fwds.size() - fwdContainer.getChildCount();
        for (int j = 0; j < i; j++) {
            View localView = LayoutInflater.from(context).inflate(R.layout.item_forward_message, fwdContainer, false);
            fwdContainer.addView(localView);
        }

        for (int g = 0; g < fwdContainer.getChildCount(); g++) {
            final ViewGroup itemView = (ViewGroup) fwdContainer.getChildAt(g);
            if (g < fwds.size()) {
                final Message message = fwds.get(g);
                itemView.setVisibility(View.VISIBLE);
                itemView.setTag(message);

                TextView tvBody = itemView.findViewById(R.id.item_fwd_message_text);
                tvBody.setText(OwnerLinkSpanFactory.withSpans(message.getCryptStatus() == CryptStatus.DECRYPTED ? message.getDecryptedBody() : message.getBody(), true, false, new LinkActionAdapter() {
                    @Override
                    public void onOwnerClick(int ownerId) {
                        mAttachmentsActionCallback.onOpenOwner(ownerId);
                    }
                }));
                tvBody.setVisibility(message.getBody() == null || message.getBody().length() == 0 ? View.GONE : View.VISIBLE);

                ((TextView) itemView.findViewById(R.id.item_fwd_message_username)).setText(message.getSender().getFullName());
                ((TextView) itemView.findViewById(R.id.item_fwd_message_time)).setText(AppTextUtils.getDateFromUnixTime(message.getDate()));
                TextView tvFwds = itemView.findViewById(R.id.item_forward_message_fwds);
                tvFwds.setVisibility(message.getForwardMessagesCount() > 0 ? View.VISIBLE : View.GONE);

                tvFwds.setOnClickListener(v -> mAttachmentsActionCallback.onForwardMessagesOpen(message.getFwd()));

                final ImageView ivAvatar = itemView.findViewById(R.id.item_fwd_message_avatar);

                String senderPhotoUrl = message.getSender() == null ? null : message.getSender().getMaxSquareAvatar();
                ViewUtils.displayAvatar(ivAvatar, mAvatarTransformation, senderPhotoUrl, Constants.PICASSO_TAG);

                ivAvatar.setOnClickListener(v -> mAttachmentsActionCallback.onOpenOwner(message.getSenderId()));

                AttachmentsHolder attachmentContainers = new AttachmentsHolder();
                attachmentContainers.setVgAudios(itemView.findViewById(R.id.audio_attachments)).
                        setVgVideos(itemView.findViewById(R.id.video_attachments)).
                        setVgDocs(itemView.findViewById(R.id.docs_attachments)).
                        setVgArticles(itemView.findViewById(R.id.articles_attachments)).
                        setVgPhotos(itemView.findViewById(R.id.photo_attachments)).
                        setVgPosts(itemView.findViewById(R.id.posts_attachments)).
                        setVgStickers(itemView.findViewById(R.id.stickers_attachments)).
                        setVoiceMessageRoot(itemView.findViewById(R.id.voice_message_attachments));

                displayAttachments(message.getAttachments(), attachmentContainers, postsAsLinks, message.getId());
            } else {
                itemView.setVisibility(View.GONE);
                itemView.setTag(null);
            }
        }
    }

    private void displayDocs(List<DocLink> docs, ViewGroup root) {
        root.setVisibility(safeIsEmpty(docs) ? View.GONE : View.VISIBLE);
        if (safeIsEmpty(docs)) {
            return;
        }

        int i = docs.size() - root.getChildCount();
        for (int j = 0; j < i; j++) {
            root.addView(LayoutInflater.from(mContext).inflate(R.layout.item_document, root, false));
        }

        for (int g = 0; g < root.getChildCount(); g++) {
            ViewGroup itemView = (ViewGroup) root.getChildAt(g);
            if (g < docs.size()) {
                final DocLink doc = docs.get(g);
                itemView.setVisibility(View.VISIBLE);
                itemView.setTag(doc);

                MaterialCardView backCardT = itemView.findViewById(R.id.card_view);
                TextView tvTitle = itemView.findViewById(R.id.item_document_title);
                TextView tvDetails = itemView.findViewById(R.id.item_document_ext_size);
                EmojiconTextView tvPostText = itemView.findViewById(R.id.item_message_text);
                ImageView ivPhotoT = itemView.findViewById(R.id.item_document_image);
                ImageView ivPhoto_Post = itemView.findViewById(R.id.item_post_avatar_image);
                ImageView ivType = itemView.findViewById(R.id.item_document_type);

                String title = doc.getTitle(mContext);
                String details = doc.getSecondaryText(mContext);

                String imageUrl = doc.getImageUrl();
                String ext = doc.getExt() == null ? "" : doc.getExt() + ", ";

                String subtitle = ext + details;

                tvTitle.setText(title);
                if (doc.getType() == Types.POST) {
                    tvDetails.setVisibility(View.GONE);
                    tvPostText.setVisibility(View.VISIBLE);
                    tvPostText.setText(OwnerLinkSpanFactory.withSpans(subtitle, true, false, new LinkActionAdapter() {
                        @Override
                        public void onOwnerClick(int ownerId) {
                            mAttachmentsActionCallback.onOpenOwner(ownerId);
                        }
                    }));
                } else {
                    tvDetails.setVisibility(View.VISIBLE);
                    tvPostText.setVisibility(View.GONE);
                    tvDetails.setText(subtitle);
                }

                View attachmentsRoot = itemView.findViewById(R.id.item_message_attachment_container);
                AttachmentsHolder attachmentsHolder = new AttachmentsHolder();
                attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
                        .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
                        .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
                        .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
                        .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
                        .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
                        .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments));
                attachmentsRoot.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> openDocLink(doc));
                ivPhoto_Post.setVisibility(View.GONE);

                switch (doc.getType()) {
                    case Types.DOC:
                        if (imageUrl != null) {
                            ivType.setVisibility(View.GONE);
                            backCardT.setVisibility(View.VISIBLE);
                            ViewUtils.displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG);
                        } else {
                            ivType.setVisibility(View.VISIBLE);
                            backCardT.setVisibility(View.GONE);
                            Utils.setColorFilter(ivType.getBackground(), CurrentTheme.getColorPrimary(mContext));
                            ivType.setImageResource(R.drawable.file);
                        }
                        break;
                    case Types.POST:
                        backCardT.setVisibility(View.GONE);
                        ivType.setVisibility(View.GONE);
                        if (imageUrl != null) {
                            ivPhoto_Post.setVisibility(View.VISIBLE);
                            ViewUtils.displayAvatar(ivPhoto_Post, mAvatarTransformation, imageUrl, Constants.PICASSO_TAG);
                        } else {
                            ivPhoto_Post.setVisibility(View.GONE);
                        }
                        Post post = (Post) doc.attachment;
                        boolean hasAttachments = (nonNull(post.getAttachments()) && post.getAttachments().size() > 0);
                        attachmentsRoot.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);
                        if (hasAttachments)
                            displayAttachments(post.getAttachments(), attachmentsHolder, false, null);
                        break;
                    case Types.LINK:
                    case Types.WIKI_PAGE:
                        ivType.setVisibility(View.VISIBLE);
                        if (imageUrl != null) {
                            backCardT.setVisibility(View.VISIBLE);
                            ViewUtils.displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG);
                        } else {
                            backCardT.setVisibility(View.GONE);
                        }
                        Utils.setColorFilter(ivType.getBackground(), CurrentTheme.getColorPrimary(mContext));
                        ivType.setImageResource(R.drawable.attachment);
                        break;
                    case Types.POLL:
                        ivType.setVisibility(View.VISIBLE);
                        backCardT.setVisibility(View.GONE);
                        Utils.setColorFilter(ivType.getBackground(), CurrentTheme.getColorPrimary(mContext));
                        ivType.setImageResource(R.drawable.chart_bar);
                        break;
                    default:
                        ivType.setVisibility(View.GONE);
                        backCardT.setVisibility(View.GONE);
                        break;
                }

            } else {
                itemView.setVisibility(View.GONE);
                itemView.setTag(null);
            }
        }
    }

    private void displayArticles(List<Article> articles, ViewGroup root) {
        root.setVisibility(safeIsEmpty(articles) ? View.GONE : View.VISIBLE);
        if (safeIsEmpty(articles)) {
            return;
        }

        int i = articles.size() - root.getChildCount();
        for (int j = 0; j < i; j++) {
            root.addView(LayoutInflater.from(mContext).inflate(R.layout.item_article, root, false));
        }

        for (int g = 0; g < root.getChildCount(); g++) {
            ViewGroup itemView = (ViewGroup) root.getChildAt(g);
            if (g < articles.size()) {
                final Article article = articles.get(g);
                itemView.setVisibility(View.VISIBLE);
                itemView.setTag(article);

                ImageView ivPhoto = itemView.findViewById(R.id.item_article_image);
                TextView ivSubTitle = itemView.findViewById(R.id.item_article_subtitle);

                TextView ivTitle = itemView.findViewById(R.id.item_article_title);
                TextView ivName = itemView.findViewById(R.id.item_article_name);

                Button ivButton = itemView.findViewById(R.id.item_article_read);
                if (article.getURL() != null) {
                    ivButton.setVisibility(View.VISIBLE);
                    ivButton.setOnClickListener(v -> mAttachmentsActionCallback.onUrlOpen(article.getURL()));
                } else
                    ivButton.setVisibility(View.GONE);

                String photo_url = null;
                if (article.getPhoto() != null) {
                    photo_url = article.getPhoto().getUrlForSize(Settings.get().main().getPrefPreviewImageSize(), false);
                }

                if (photo_url != null) {
                    ivPhoto.setVisibility(View.VISIBLE);
                    ViewUtils.displayAvatar(ivPhoto, null, photo_url, Constants.PICASSO_TAG);
                    ivPhoto.setOnLongClickListener(v -> {
                        ArrayList<Photo> temp = new ArrayList<>(Collections.singletonList(article.getPhoto()));
                        mAttachmentsActionCallback.onPhotosOpen(temp, 0);
                        return true;
                    });
                } else
                    ivPhoto.setVisibility(View.GONE);

                if (article.getSubTitle() != null) {
                    ivSubTitle.setVisibility(View.VISIBLE);
                    ivSubTitle.setText(article.getSubTitle());
                } else
                    ivSubTitle.setVisibility(View.GONE);

                if (article.getTitle() != null) {
                    ivTitle.setVisibility(View.VISIBLE);
                    ivTitle.setText(article.getTitle());
                } else
                    ivTitle.setVisibility(View.GONE);

                if (article.getOwnerName() != null) {
                    ivName.setVisibility(View.VISIBLE);
                    ivName.setText(article.getOwnerName());
                } else
                    ivName.setVisibility(View.GONE);

            } else {
                itemView.setVisibility(View.GONE);
                itemView.setTag(null);
            }
        }
    }

    private void openSticker(Sticker sticker) {

    }

    private void openDocLink(DocLink link) {
        switch (link.getType()) {
            case Types.DOC:
                mAttachmentsActionCallback.onDocPreviewOpen((Document) link.attachment);
                break;
            case Types.POST:
                mAttachmentsActionCallback.onPostOpen((Post) link.attachment);
                break;
            case Types.LINK:
                mAttachmentsActionCallback.onLinkOpen((Link) link.attachment);
                break;
            case Types.POLL:
                mAttachmentsActionCallback.onPollOpen((Poll) link.attachment);
                break;
            case Types.WIKI_PAGE:
                mAttachmentsActionCallback.onWikiPageOpen((WikiPage) link.attachment);
                break;
        }
    }

    private void deleteTrack(final int accoutnId, Audio audio) {
        audioListDisposable.add(mAudioInteractor.delete(accoutnId, audio.getId(), audio.getOwnerId()).compose(RxUtils.applyCompletableIOToMainSchedulers()).subscribe(() -> {
        }, ignore -> {
        }));
    }

    private void addTrack(int accountId, Audio audio) {
        audioListDisposable.add(mAudioInteractor.add(accountId, audio, null, null).compose(RxUtils.applySingleIOToMainSchedulers()).subscribe(t -> {
        }, ignore -> {
        }));
    }

    private void get_lyrics(Audio audio) {
        audioListDisposable.add(mAudioInteractor.getLyrics(audio.getLyricsId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(t -> onAudioLyricsRecived(t, audio), t -> {/*TODO*/}));
    }

    private void onAudioLyricsRecived(String Text, Audio audio) {
        String title = audio.getArtistAndTitle();

        MaterialAlertDialogBuilder dlgAlert = new MaterialAlertDialogBuilder(mContext);
        dlgAlert.setIcon(R.drawable.dir_song);
        dlgAlert.setMessage(Text);
        dlgAlert.setTitle(title != null ? title : mContext.getString(R.string.get_lyrics));

        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("response", Text);
        clipboard.setPrimaryClip(clip);

        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.copied_to_clipboard);
        dlgAlert.create().show();
    }

    @DrawableRes
    private int getAudioCoverSimple() {
        return Settings.get().main().isAudio_round_icon() ? R.drawable.audio_button : R.drawable.audio_button_material;
    }

    private Transformation TransformCover() {
        return Settings.get().main().isAudio_round_icon() ? new RoundTransformation() : new PolyTransformation();
    }

    /**
     * Отображение аудиозаписей
     *
     * @param audios    аудиозаписи
     * @param container контейнер для аудиозаписей
     */

    private void displayAudios(final ArrayList<Audio> audios, ViewGroup container) {
        container.setVisibility(safeIsEmpty(audios) ? View.GONE : View.VISIBLE);
        if (safeIsEmpty(audios)) {
            return;
        }

        int i = audios.size() - container.getChildCount();
        for (int j = 0; j < i; j++) {
            container.addView(LayoutInflater.from(mContext).inflate(R.layout.item_audio, container, false));
        }

        for (int g = 0; g < container.getChildCount(); g++) {
            ViewGroup root = (ViewGroup) container.getChildAt(g);
            if (g < audios.size()) {
                final Audio audio = audios.get(g);

                AudioHolder holder = new AudioHolder(root);

                holder.tvTitle.setText(audio.getArtist());
                holder.tvSubtitle.setText(audio.getTitle());

                if (!audio.isHLS()) {
                    holder.quality.setVisibility(View.VISIBLE);
                    if (audio.getIsHq())
                        holder.quality.setImageResource(R.drawable.high_quality);
                    else
                        holder.quality.setImageResource(R.drawable.low_quality);
                } else
                    holder.quality.setVisibility(View.GONE);

                holder.play_icon.setImageResource(MusicUtils.isNowPlayingOrPreparing(audio) ? R.drawable.voice_state_animation : (MusicUtils.isNowPaused(audio) ? R.drawable.paused : (Utils.isEmpty(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song)));
                Utils.doAnimate(holder.play_icon.getDrawable(), true);
                int finalG = g;
                AtomicInteger PlayState = new AtomicInteger(MusicUtils.AudioStatus(audio));
                holder.play_icon.getViewTreeObserver().addOnPreDrawListener(() -> {
                    Integer PlayStateCurrent = MusicUtils.AudioStatus(audio);
                    if (PlayStateCurrent != PlayState.get()) {
                        PlayState.set(PlayStateCurrent);
                        holder.play_icon.setImageResource(PlayStateCurrent == 1 ? R.drawable.voice_state_animation : (PlayStateCurrent == 2 ? R.drawable.paused : (Utils.isEmpty(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song)));
                        Utils.doAnimate(holder.play_icon.getDrawable(), true);
                    }
                    return true;
                });

                if (Settings.get().other().isShow_audio_cover()) {
                    if (!Utils.isEmpty(audio.getThumb_image_little())) {
                        PicassoInstance.with()
                                .load(audio.getThumb_image_little())
                                .placeholder(java.util.Objects.requireNonNull(ResourcesCompat.getDrawable(mContext.getResources(), getAudioCoverSimple(), mContext.getTheme())))
                                .transform(TransformCover())
                                .tag(Constants.PICASSO_TAG)
                                .into(holder.play_cover);
                    } else {
                        PicassoInstance.with().cancelRequest(holder.play_cover);
                        holder.play_cover.setImageResource(getAudioCoverSimple());
                    }
                }

                holder.ibPlay.setOnClickListener(v -> {
                    if (MusicUtils.isNowPlayingOrPreparingOrPaused(audio)) {
                        if (!Settings.get().other().isUse_stop_audio()) {
                            if (!MusicUtils.isNowPaused(audio))
                                holder.play_icon.setImageResource(R.drawable.paused);
                            else {
                                holder.play_icon.setImageResource(R.drawable.voice_state_animation);
                                Utils.doAnimate(holder.play_icon.getDrawable(), true);
                            }
                            MusicUtils.playOrPause();
                        } else {
                            holder.play_icon.setImageResource(Utils.isEmpty(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song);
                            MusicUtils.stop();
                        }
                    } else {
                        holder.play_icon.setImageResource(R.drawable.voice_state_animation);
                        Utils.doAnimate(holder.play_icon.getDrawable(), true);
                        mAttachmentsActionCallback.onAudioPlay(finalG, audios);
                    }
                });
                if (audio.getDuration() <= 0)
                    holder.time.setVisibility(View.GONE);
                else {
                    holder.time.setVisibility(View.VISIBLE);
                    holder.time.setText(AppTextUtils.getDurationString(audio.getDuration()));
                }

                int Status = DownloadUtil.TrackIsDownloaded(audio);
                holder.saved.setVisibility(Status != 0 ? View.VISIBLE : View.GONE);
                if (Status == 1)
                    holder.saved.setImageResource(R.drawable.save);
                else if (Status == 2)
                    holder.saved.setImageResource(R.drawable.remote_cloud);
                holder.lyric.setVisibility(audio.getLyricsId() != 0 ? View.VISIBLE : View.GONE);
                holder.my.setVisibility(audio.getOwnerId() == Settings.get().accounts().getCurrent() ? View.VISIBLE : View.GONE);

                holder.Track.setOnLongClickListener(v -> {
                    if (!AppPerms.hasReadWriteStoragePermision(mContext)) {
                        AppPerms.requestReadWriteStoragePermission((Activity) mContext);
                        return false;
                    }
                    holder.saved.setVisibility(View.VISIBLE);
                    int ret = DownloadUtil.downloadTrack(mContext, audio, false);
                    if (ret == 0)
                        PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.saved_audio);
                    else if (ret == 1) {
                        Snackbar.make(v, R.string.audio_force_download, Snackbar.LENGTH_LONG).setAction(R.string.button_yes,
                                v1 -> DownloadUtil.downloadTrack(mContext, audio, true))
                                .setBackgroundTint(CurrentTheme.getColorPrimary(mContext)).setActionTextColor(Utils.isColorDark(CurrentTheme.getColorPrimary(mContext))
                                ? Color.parseColor("#ffffff") : Color.parseColor("#000000")).setTextColor(Utils.isColorDark(CurrentTheme.getColorPrimary(mContext))
                                ? Color.parseColor("#ffffff") : Color.parseColor("#000000")).show();

                    } else {
                        holder.saved.setVisibility(View.GONE);
                        PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.error_audio);
                    }
                    return true;
                });

                holder.Track.setOnClickListener(view -> {
                    holder.cancelSelectionAnimation();
                    holder.startSomeAnimation();

                    ModalBottomSheetDialogFragment.Builder menus = new ModalBottomSheetDialogFragment.Builder();

                    menus.add(new OptionRequest(AudioItem.play_item_audio, mContext.getString(R.string.play), R.drawable.play));
                    if (audio.getOwnerId() != Settings.get().accounts().getCurrent()) {
                        menus.add(new OptionRequest(AudioItem.add_item_audio, mContext.getString(R.string.action_add), R.drawable.list_add));
                        menus.add(new OptionRequest(AudioItem.add_and_download_button, mContext.getString(R.string.add_and_download_button), R.drawable.add_download));
                    } else
                        menus.add(new OptionRequest(AudioItem.add_item_audio, mContext.getString(R.string.delete), R.drawable.ic_outline_delete));
                    menus.add(new OptionRequest(AudioItem.share_button, mContext.getString(R.string.share), R.drawable.ic_outline_share));
                    menus.add(new OptionRequest(AudioItem.save_item_audio, mContext.getString(R.string.save), R.drawable.save));
                    if (audio.getAlbumId() != 0)
                        menus.add(new OptionRequest(AudioItem.open_album, mContext.getString(R.string.open_album), R.drawable.audio_album));
                    menus.add(new OptionRequest(AudioItem.get_recommendation_by_audio, mContext.getString(R.string.get_recommendation_by_audio), R.drawable.music_mic));

                    if (audio.getAlbumId() != 0)
                        menus.add(new OptionRequest(AudioItem.open_album, mContext.getString(R.string.open_album), R.drawable.audio_album));

                    if (audio.getLyricsId() != 0)
                        menus.add(new OptionRequest(AudioItem.get_lyrics_menu, mContext.getString(R.string.get_lyrics_menu), R.drawable.lyric));
                    menus.add(new OptionRequest(AudioItem.bitrate_item_audio, mContext.getString(R.string.get_bitrate), R.drawable.high_quality));
                    menus.add(new OptionRequest(AudioItem.search_by_artist, mContext.getString(R.string.search_by_artist), R.drawable.magnify));
                    menus.add(new OptionRequest(AudioItem.copy_url, mContext.getString(R.string.copy_url), R.drawable.content_copy));


                    menus.header(Utils.firstNonEmptyString(audio.getArtist(), " ") + " - " + audio.getTitle(), R.drawable.song, audio.getThumb_image_little());
                    menus.columns(2);
                    menus.show(((FragmentActivity) mContext).getSupportFragmentManager(), "audio_options", option -> {
                        switch (option.getId()) {
                            case AudioItem.play_item_audio:
                                mAttachmentsActionCallback.onAudioPlay(finalG, audios);
                                PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(mContext);
                                break;
                            case AudioItem.share_button:
                                SendAttachmentsActivity.startForSendAttachments(mContext, Settings.get().accounts().getCurrent(), audio);
                                break;
                            case AudioItem.search_by_artist:
                                PlaceFactory.getSingleTabSearchPlace(Settings.get().accounts().getCurrent(), SearchContentType.AUDIOS, new AudioSearchCriteria(audio.getArtist(), true, false)).tryOpenWith(mContext);
                                break;
                            case AudioItem.get_lyrics_menu:
                                get_lyrics(audio);
                                break;
                            case AudioItem.get_recommendation_by_audio:
                                PlaceFactory.SearchByAudioPlace(Settings.get().accounts().getCurrent(), audio.getOwnerId(), audio.getId()).tryOpenWith(mContext);
                                break;
                            case AudioItem.open_album:
                                PlaceFactory.getAudiosInAlbumPlace(Settings.get().accounts().getCurrent(), audio.getAlbum_owner_id(), audio.getAlbumId(), audio.getAlbum_access_key()).tryOpenWith(mContext);
                                break;
                            case AudioItem.copy_url:
                                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("response", audio.getUrl());
                                clipboard.setPrimaryClip(clip);
                                PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.copied);
                                break;
                            case AudioItem.add_item_audio:
                                boolean myAudio = audio.getOwnerId() == Settings.get().accounts().getCurrent();
                                if (myAudio) {
                                    deleteTrack(Settings.get().accounts().getCurrent(), audio);
                                    PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.deleted);
                                } else {
                                    addTrack(Settings.get().accounts().getCurrent(), audio);
                                    PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.added);
                                }
                                break;
                            case AudioItem.add_and_download_button:
                                addTrack(Settings.get().accounts().getCurrent(), audio);
                                PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.added);
                            case AudioItem.save_item_audio:
                                if (!AppPerms.hasReadWriteStoragePermision(mContext)) {
                                    AppPerms.requestReadWriteStoragePermission((Activity) mContext);
                                    break;
                                }
                                holder.saved.setVisibility(View.VISIBLE);
                                int ret = DownloadUtil.downloadTrack(mContext, audio, false);
                                if (ret == 0)
                                    PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.saved_audio);
                                else if (ret == 1) {
                                    Snackbar.make(view, R.string.audio_force_download, Snackbar.LENGTH_LONG).setAction(R.string.button_yes,
                                            v1 -> DownloadUtil.downloadTrack(mContext, audio, true))
                                            .setBackgroundTint(CurrentTheme.getColorPrimary(mContext)).setActionTextColor(Utils.isColorDark(CurrentTheme.getColorPrimary(mContext))
                                            ? Color.parseColor("#ffffff") : Color.parseColor("#000000")).setTextColor(Utils.isColorDark(CurrentTheme.getColorPrimary(mContext))
                                            ? Color.parseColor("#ffffff") : Color.parseColor("#000000")).show();
                                } else {
                                    holder.saved.setVisibility(View.GONE);
                                    PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.error_audio);
                                }
                                break;
                            case AudioItem.bitrate_item_audio:
                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                retriever.setDataSource(Audio.getMp3FromM3u8(audio.getUrl()), new HashMap<>());
                                String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                                PhoenixToast.CreatePhoenixToast(mContext).showToast(mContext.getResources().getString(R.string.bitrate) + " " + (Long.parseLong(bitrate) / 1000) + " bit");
                                break;

                            case AudioItem.goto_artist:
                                String[][] artists = Utils.getArrayFromHash(audio.getMain_artists());
                                if (audio.getMain_artists().keySet().size() > 1) {
                                    new MaterialAlertDialogBuilder(mContext)
                                            .setItems(artists[1], (dialog, which) -> PlaceFactory.getArtistPlace(Settings.get().accounts().getCurrent(), artists[0][which], false).tryOpenWith(mContext)).show();
                                } else {
                                    PlaceFactory.getArtistPlace(Settings.get().accounts().getCurrent(), artists[0][0], false).tryOpenWith(mContext);
                                }
                                break;
                        }
                    });
                });

                root.setVisibility(View.VISIBLE);
                root.setTag(audio);
            } else {
                root.setVisibility(View.GONE);
                root.setTag(null);
            }
        }
    }

    public interface VoiceActionListener extends EventListener {
        void onVoiceHolderBinded(int voiceMessageId, int voiceHolderId);

        void onVoicePlayButtonClick(int voiceHolderId, int voiceMessageId, @NonNull VoiceMessage voiceMessage);

        void onTranscript(String voiceMessageId, int messageId);
    }

    public interface OnAttachmentsActionCallback {
        void onPollOpen(@NonNull Poll poll);

        void onVideoPlay(@NonNull Video video);

        void onAudioPlay(int position, @NonNull ArrayList<Audio> audios);

        void onForwardMessagesOpen(@NonNull ArrayList<Message> messages);

        void onOpenOwner(int userId);

        void onDocPreviewOpen(@NonNull Document document);

        void onPostOpen(@NonNull Post post);

        void onLinkOpen(@NonNull Link link);

        void onUrlOpen(@NonNull String url);

        void onWikiPageOpen(@NonNull WikiPage page);

        void onStickerOpen(@NonNull Sticker sticker);

        void onPhotosOpen(@NonNull ArrayList<Photo> photos, int index);
    }

    private static final class CopyHolder {

        final ViewGroup itemView;
        final ImageView ivAvatar;
        final TextView ownerName;
        final EmojiconTextView bodyView;
        final View tvShowMore;
        final View buttonDots;
        final AttachmentsHolder attachmentsHolder;
        final OnAttachmentsActionCallback callback;

        CopyHolder(ViewGroup itemView, OnAttachmentsActionCallback callback) {
            this.itemView = itemView;
            this.bodyView = itemView.findViewById(R.id.item_post_copy_text);
            this.ivAvatar = itemView.findViewById(R.id.item_copy_history_post_avatar);
            this.tvShowMore = itemView.findViewById(R.id.item_post_copy_show_more);
            this.ownerName = itemView.findViewById(R.id.item_post_copy_owner_name);
            this.buttonDots = itemView.findViewById(R.id.item_copy_history_post_dots);
            this.attachmentsHolder = AttachmentsHolder.forCopyPost(itemView);
            this.callback = callback;

            this.buttonDots.setOnClickListener(v -> showDotsMenu());
        }

        void showDotsMenu() {
            PopupMenu menu = new PopupMenu(itemView.getContext(), buttonDots);
            menu.getMenu().add(R.string.open_post).setOnMenuItemClickListener(item -> {
                Post copy = (Post) buttonDots.getTag();
                callback.onPostOpen(copy);
                return true;
            });

            menu.show();
        }
    }

    private class AudioHolder {

        TextView tvTitle;
        TextView tvSubtitle;
        View ibPlay;
        ImageView play_icon;
        ImageView play_cover;
        TextView time;
        ImageView saved;
        ImageView lyric;
        ImageView my;
        ImageView quality;
        View Track;
        MaterialCardView selectionView;
        MaterialCardView isSelectedView;
        Animator.AnimatorListener animationAdapter;
        ObjectAnimator animator;

        AudioHolder(View root) {
            tvTitle = root.findViewById(R.id.dialog_title);
            tvSubtitle = root.findViewById(R.id.dialog_message);
            ibPlay = root.findViewById(R.id.item_audio_play);
            play_icon = root.findViewById(R.id.item_audio_play_icon);
            play_cover = root.findViewById(R.id.item_audio_play_cover);
            time = root.findViewById(R.id.item_audio_time);
            saved = root.findViewById(R.id.saved);
            lyric = root.findViewById(R.id.lyric);
            Track = root.findViewById(R.id.track_option);
            my = root.findViewById(R.id.my);
            selectionView = root.findViewById(R.id.item_audio_selection);
            isSelectedView = root.findViewById(R.id.item_audio_select_add);
            isSelectedView.setVisibility(View.GONE);
            quality = root.findViewById(R.id.quality);
            animationAdapter = new WeakViewAnimatorAdapter<View>(selectionView) {
                @Override
                public void onAnimationEnd(View view) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationStart(View view) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                protected void onAnimationCancel(View view) {
                    view.setVisibility(View.GONE);
                }
            };
        }

        void startSomeAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorSecondary(mContext));
            selectionView.setAlpha(0.5f);

            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f);
            animator.setDuration(500);
            animator.addListener(animationAdapter);
            animator.start();
        }

        void cancelSelectionAnimation() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }

            selectionView.setVisibility(View.INVISIBLE);
        }
    }

    private class VoiceHolder implements IdentificableHolder {

        WaveFormView mWaveFormView;
        ImageView mButtonPlay;
        TextView mDurationText;
        MaterialCardView TranscriptBlock;
        TextView TranscriptText;
        TextView mDoTranscript;

        VoiceHolder(View itemView) {
            mWaveFormView = itemView.findViewById(R.id.item_voice_wave_form_view);
            mWaveFormView.setActiveColor(mActiveWaveFormColor);
            mWaveFormView.setNoactiveColor(mNoactiveWaveFormColor);
            mWaveFormView.setSectionCount(Utils.isLandscape(itemView.getContext()) ? 128 : 64);
            mWaveFormView.setTag(generateHolderId());
            mButtonPlay = itemView.findViewById(R.id.item_voice_button_play);
            mDurationText = itemView.findViewById(R.id.item_voice_duration);

            TranscriptBlock = itemView.findViewById(R.id.transcription_block);
            TranscriptText = itemView.findViewById(R.id.transcription_text);
            mDoTranscript = itemView.findViewById(R.id.item_voice_translate);
        }

        @Override
        public int getHolderId() {
            return (int) mWaveFormView.getTag();
        }
    }
}
