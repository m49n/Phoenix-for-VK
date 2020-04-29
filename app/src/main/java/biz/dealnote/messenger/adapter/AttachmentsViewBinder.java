package biz.dealnote.messenger.adapter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
import biz.dealnote.messenger.fragment.search.SearchTabsFragment;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.link.internal.LinkActionAdapter;
import biz.dealnote.messenger.link.internal.OwnerLinkSpanFactory;
import biz.dealnote.messenger.model.Attachments;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.Text;
import biz.dealnote.messenger.model.Types;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.model.WikiPage;
import biz.dealnote.messenger.model.menu.Item;
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
import static biz.dealnote.messenger.util.Objects.isNullOrEmptyString;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.dpToPx;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.safeIsEmpty;
import static biz.dealnote.messenger.util.Utils.safeLenghtOf;

public class AttachmentsViewBinder {

    private static final int PREFFERED_STICKER_SIZE = 120;
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

    private static final byte[] DEFAUL_WAVEFORM = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public void setOnHashTagClickListener(EmojiconTextView.OnHashTagClickListener onHashTagClickListener) {
        this.mOnHashTagClickListener = onHashTagClickListener;
    }

    private static int generateHolderId() {
        sHolderIdCounter++;
        return sHolderIdCounter;
    }

    public void displayAttachments(Attachments attachments, AttachmentsHolder containers, boolean postsAsLinks) {
        if (attachments == null) {
            safeSetVisibitity(containers.getVgAudios(), View.GONE);
            safeSetVisibitity(containers.getVgVideos(), View.GONE);
            safeSetVisibitity(containers.getVgDocs(), View.GONE);
            safeSetVisibitity(containers.getVgPhotos(), View.GONE);
            safeSetVisibitity(containers.getVgPosts(), View.GONE);
            safeSetVisibitity(containers.getVgStickers(), View.GONE);
            safeSetVisibitity(containers.getVoiceMessageRoot(), View.GONE);
            safeSetVisibitity(containers.getVgFriends(), View.GONE);
        } else {
            displayAudios(attachments.getAudios(), containers.getVgAudios());
            displayVoiceMessages(attachments.getVoiceMessages(), containers.getVoiceMessageRoot());
            displayDocs(attachments.getDocLinks(postsAsLinks, true), containers.getVgDocs());

            if (containers.getVgStickers() != null) {
                displayStickers(attachments.getStickers(), containers.getVgStickers());
            }

            photosViewHelper.displayPhotos(attachments.getPostImages(), containers.getVgPhotos());
            photosViewHelper.displayVideos(attachments.getPostImagesVideos(), containers.getVgVideos());
        }
    }

    private void displayVoiceMessages(final ArrayList<VoiceMessage> voices, ViewGroup container) {
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
                bindVoiceHolder(holder, voice);

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

    private void bindVoiceHolder(@NonNull VoiceHolder holder, @NonNull VoiceMessage voice) {
        int voiceMessageId = voice.getId();
        mVoiceSharedHolders.put(voiceMessageId, holder);

        holder.mDurationText.setText(AppTextUtils.getDurationString(voice.getDuration()));

        // can bee NULL/empty
        if (nonNull(voice.getWaveform()) && voice.getWaveform().length > 0) {
            holder.mWaveFormView.setWaveForm(voice.getWaveform());
        } else {
            holder.mWaveFormView.setWaveForm(DEFAUL_WAVEFORM);
        }

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

                displayAttachments(copy.getAttachments(), holder.attachmentsHolder, false);
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
                tvBody.setText(message.getBody());
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
                        setVgPhotos(itemView.findViewById(R.id.photo_attachments)).
                        setVgPosts(itemView.findViewById(R.id.posts_attachments)).
                        setVgStickers(itemView.findViewById(R.id.stickers_attachments)).
                        setVoiceMessageRoot(itemView.findViewById(R.id.voice_message_attachments));

                displayAttachments(message.getAttachments(), attachmentContainers, postsAsLinks);
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

                TextView tvTitle = itemView.findViewById(R.id.item_document_title);
                TextView tvDetails = itemView.findViewById(R.id.item_document_ext_size);
                ImageView ivPhoto = itemView.findViewById(R.id.item_document_image);
                ImageView ivType = itemView.findViewById(R.id.item_document_type);

                String title = doc.getTitle(mContext);
                String details = doc.getSecondaryText(mContext);

                String imageUrl = doc.getImageUrl();
                String ext = doc.getExt() == null ? "" : doc.getExt() + ", ";

                String subtitle = ext + details;

                tvTitle.setText(title);
                tvDetails.setText(subtitle);

                itemView.setOnClickListener(v -> openDocLink(doc));

                switch (doc.getType()) {
                    case Types.DOC:
                        if (imageUrl != null) {
                            ivType.setVisibility(View.GONE);
                            ivPhoto.setVisibility(View.VISIBLE);
                            ViewUtils.displayAvatar(ivPhoto, null, imageUrl, Constants.PICASSO_TAG);
                        } else {
                            ivType.setVisibility(View.VISIBLE);
                            ivPhoto.setVisibility(View.GONE);
                            Utils.setColorFilter(ivType.getBackground(), CurrentTheme.getColorPrimary((mContext)));
                            ivType.setImageResource(R.drawable.file);
                        }
                        break;
                    case Types.POST:
                        if (imageUrl != null) {
                            ivType.setVisibility(View.GONE);
                            ivPhoto.setVisibility(View.VISIBLE);
                            ViewUtils.displayAvatar(ivPhoto, mAvatarTransformation, imageUrl, Constants.PICASSO_TAG);
                        } else {
                            ivPhoto.setVisibility(View.GONE);
                            ivType.setVisibility(View.GONE);
                        }
                        break;
                    case Types.LINK:
                    case Types.WIKI_PAGE:
                        ivType.setVisibility(View.VISIBLE);
                        if (imageUrl != null) {
                            ivPhoto.setVisibility(View.VISIBLE);
                            ViewUtils.displayAvatar(ivPhoto, null, imageUrl, Constants.PICASSO_TAG);
                        }
                        else
                            ivPhoto.setVisibility(View.GONE);
                        Utils.setColorFilter(ivType.getBackground(), Color.TRANSPARENT);
                        ivType.setImageResource(R.drawable.share_colored);
                        break;
                    case Types.POLL:
                        ivType.setVisibility(View.VISIBLE);
                        ivPhoto.setVisibility(View.GONE);
                        Utils.setColorFilter(ivType.getBackground(), CurrentTheme.getColorPrimary(mContext));
                        ivType.setImageResource(R.drawable.chart_bar);
                        break;
                    default:
                        ivType.setVisibility(View.GONE);
                        ivPhoto.setVisibility(View.GONE);
                        break;
                }

            } else {
                itemView.setVisibility(View.GONE);
                itemView.setTag(null);
            }
        }
    }

    private void openSticker(Sticker sticker) {

    }

    public interface VoiceActionListener extends EventListener {
        void onVoiceHolderBinded(int voiceMessageId, int voiceHolderId);

        void onVoicePlayButtonClick(int voiceHolderId, int voiceMessageId, @NonNull VoiceMessage voiceMessage);
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
        audioListDisposable.add(mAudioInteractor.delete(accoutnId, audio.getId(), audio.getOwnerId()).compose(RxUtils.applyCompletableIOToMainSchedulers()).subscribe(() -> {}, ignore -> {}));
    }

    private void addTrack(int accountId, Audio audio) {
        audioListDisposable.add(mAudioInteractor.add(accountId, audio, null, null).compose(RxUtils.applySingleIOToMainSchedulers()).subscribe(t -> {}, ignore -> {}));
    }

    private void get_lyrics(Audio audio) {
        audioListDisposable.add(mAudioInteractor.getLyrics(audio.getLyricsId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(t -> onAudioLyricsRecived(t, audio), t -> {/*TODO*/}));
    }

    private void onAudioLyricsRecived(String Text, Audio audio) {
        String title = audio.getArtistAndTitle();

        MaterialAlertDialogBuilder dlgAlert  = new MaterialAlertDialogBuilder(mContext);
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

                if(!audio.isHLS()) {
                    holder.quality.setVisibility(View.VISIBLE);
                    if(audio.getIsHq())
                        holder.quality.setImageResource(R.drawable.high_quality);
                    else
                        holder.quality.setImageResource(R.drawable.low_quality);
                }
                else
                    holder.quality.setVisibility(View.GONE);

                holder.ibPlay.setImageResource(MusicUtils.isNowPlayingOrPreparing(audio) ? R.drawable.voice_state_animation : (MusicUtils.isNowPaused(audio) ? R.drawable.paused : (isNullOrEmptyString(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song)));
                Utils.doAnimate(holder.ibPlay.getDrawable(), true);
                int finalG = g;
                AtomicInteger PlayState = new AtomicInteger(MusicUtils.AudioStatus(audio));
                holder.ibPlay.getViewTreeObserver().addOnPreDrawListener(() -> {
                    Integer PlayStateCurrent = MusicUtils.AudioStatus(audio);
                    if(PlayStateCurrent != PlayState.get()) {
                        PlayState.set(PlayStateCurrent);
                        holder.ibPlay.setImageResource(PlayStateCurrent == 1 ? R.drawable.voice_state_animation : (PlayStateCurrent == 2 ? R.drawable.paused : (isNullOrEmptyString(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song)));
                        Utils.doAnimate(holder.ibPlay.getDrawable(), true);
                    }
                    return true;
                });

                if(Settings.get().other().isShow_audio_cover())
                {
                    if(!isNullOrEmptyString(audio.getThumb_image_little()))
                    {
                        PicassoInstance.with()
                                .load(audio.getThumb_image_little())
                                .placeholder(R.drawable.background_gray)
                                .transform(TransformCover())
                                .tag("audio_" + audio.getOwnerId() + "_" + audio.getId())
                                .into(new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        holder.ibPlay.setBackground(new BitmapDrawable(mContext.getResources(), bitmap));
                                    }

                                    @Override
                                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                                    }
                                });
                    }
                    else {
                        PicassoInstance.with().cancelTag("audio_" + audio.getOwnerId() + "_" + audio.getId());
                        holder.ibPlay.setBackground(mContext.getResources().getDrawable(getAudioCoverSimple(), mContext.getTheme()));
                    }
                }

                holder.ibPlay.setOnClickListener(v -> {
                    if (MusicUtils.isNowPlayingOrPreparingOrPaused(audio)) {
                        if(!Settings.get().other().isUse_stop_audio()) {
                            if(!MusicUtils.isNowPaused(audio))
                                holder.ibPlay.setImageResource(R.drawable.paused);
                            else {
                                holder.ibPlay.setImageResource(R.drawable.voice_state_animation);
                                Utils.doAnimate(holder.ibPlay.getDrawable(), true);
                            }
                            MusicUtils.playOrPause();
                        }
                        else {
                            holder.ibPlay.setImageResource(isNullOrEmptyString(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song);
                            MusicUtils.stop();
                        }
                    }
                    else {
                        holder.ibPlay.setImageResource(R.drawable.voice_state_animation);
                        Utils.doAnimate(holder.ibPlay.getDrawable(), true);
                        mAttachmentsActionCallback.onAudioPlay(finalG, audios);
                    }
                });
                if(audio.getDuration() <= 0)
                    holder.time.setVisibility(View.GONE);
                else {
                    holder.time.setVisibility(View.VISIBLE);
                    holder.time.setText(AppTextUtils.getDurationString(audio.getDuration()));
                }

                holder.saved.setVisibility(DownloadUtil.TrackIsDownloaded(audio) ? View.VISIBLE : View.GONE);
                holder.saved.setImageResource(R.drawable.downloaded);
                holder.lyric.setVisibility(audio.getLyricsId() != 0 ? View.VISIBLE : View.GONE);
                holder.my.setVisibility(audio.getOwnerId() == Settings.get().accounts().getCurrent() ? View.VISIBLE : View.GONE);

                holder.Track.setOnLongClickListener(v -> {
                    if (!AppPerms.hasReadWriteStoragePermision(mContext)) {
                        AppPerms.requestReadWriteStoragePermission((Activity) mContext);
                        return false;
                    }
                    holder.saved.setVisibility(View.VISIBLE);
                    holder.saved.setImageResource(R.drawable.save);
                    int ret = DownloadUtil.downloadTrack(mContext, audio, false);
                    if (ret == 0)
                        PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.saved_audio);
                    else if (ret == 1) {
                        PhoenixToast.CreatePhoenixToast(mContext).showToastError(R.string.exist_audio);
                        new MaterialAlertDialogBuilder(mContext)
                                .setTitle(R.string.error)
                                .setMessage(R.string.audio_force_download)
                                .setPositiveButton(R.string.button_yes, (dialog_save, which_save) -> DownloadUtil.downloadTrack(mContext, audio, true))
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    } else {
                        holder.saved.setVisibility(View.GONE);
                        PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.error_audio);
                    }
                    return true;
                });

                holder.Track.setOnClickListener(view -> {
                    holder.cancelSelectionAnimation();
                    holder.startSomeAnimation();
                    final List<Item> items = new ArrayList<>();
                    items.add(new Item(R.id.btn_play_pause, new Text(R.string.play)).setIcon(R.drawable.play));
                    if (audio.getOwnerId() != Settings.get().accounts().getCurrent())
                        items.add(new Item(R.id.add_item_audio, new Text(R.string.action_add)).setIcon(R.drawable.list_add));
                    else
                        items.add(new Item(R.id.add_item_audio, new Text(R.string.delete)).setIcon(R.drawable.delete));
                    items.add(new Item(R.id.share_button, new Text(R.string.share)).setIcon(R.drawable.share_variant));
                    items.add(new Item(R.id.save_item_audio, new Text(R.string.save)).setIcon(R.drawable.save));
                    if(audio.getAlbumId() != 0)
                        items.add(new Item(R.id.open_album, new Text(R.string.open_album)).setIcon(R.drawable.audio_album));
                    items.add(new Item(R.id.get_recommendation_by_audio, new Text(R.string.get_recommendation_by_audio)).setIcon(R.drawable.music_mic));
                    if(audio.getLyricsId() != 0)
                        items.add(new Item(R.id.get_lyrics_menu, new Text(R.string.get_lyrics_menu)).setIcon(R.drawable.lyric));
                    items.add(new Item(R.id.bitrate_item_audio, new Text(R.string.get_bitrate)).setIcon(R.drawable.high_quality));
                    items.add(new Item(R.id.search_by_artist, new Text(R.string.search_by_artist)).setIcon(R.drawable.magnify));
                    items.add(new Item(R.id.copy_url, new Text(R.string.copy_url)).setIcon(R.drawable.content_copy));

                    MenuAdapter mAdapter = new MenuAdapter(mContext, items);
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                            .setIcon(holder.ibPlay.getBackground().mutate())
                            .setTitle(Utils.firstNonEmptyString(audio.getArtist(), " ") + " - " + audio.getTitle())
                            .setAdapter(mAdapter, (dialog, which) -> {
                                switch(items.get(which).getKey())
                                {
                                    case R.id.btn_play_pause:
                                        mAttachmentsActionCallback.onAudioPlay(finalG, audios);
                                        PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(mContext);
                                        break;
                                    case R.id.share_button:
                                        SendAttachmentsActivity.startForSendAttachments(mContext, Settings.get().accounts().getCurrent(), audio);
                                        break;
                                    case R.id.search_by_artist:
                                        PlaceFactory.getSearchPlace(Settings.get().accounts().getCurrent(), SearchTabsFragment.TAB_MUSIC, new AudioSearchCriteria(audio.getArtist(), true, false)).tryOpenWith(mContext);
                                        break;
                                    case R.id.get_lyrics_menu:
                                        get_lyrics(audio);
                                        break;
                                    case R.id.get_recommendation_by_audio:
                                        PlaceFactory.SearchByAudioPlace(Settings.get().accounts().getCurrent(), audio.getOwnerId(), audio.getId()).tryOpenWith(mContext);
                                        break;
                                    case R.id.open_album:
                                        PlaceFactory.getAudiosInAlbumPlace(Settings.get().accounts().getCurrent(), audio.getAlbum_owner_id(), audio.getAlbumId(), audio.getAlbum_access_key()).tryOpenWith(mContext);
                                        break;
                                    case R.id.copy_url:
                                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("response", audio.getUrl());
                                        clipboard.setPrimaryClip(clip);
                                        PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.copied);
                                        break;
                                    case R.id.add_item_audio:
                                        boolean myAudio = audio.getOwnerId() == Settings.get().accounts().getCurrent();
                                        if (myAudio) {
                                            deleteTrack(Settings.get().accounts().getCurrent(), audio);
                                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.deleted);
                                        } else {
                                            addTrack(Settings.get().accounts().getCurrent(), audio);
                                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.added);
                                        }
                                        break;
                                    case R.id.save_item_audio:
                                        if (!AppPerms.hasReadWriteStoragePermision(mContext)) {
                                            AppPerms.requestReadWriteStoragePermission((Activity) mContext);
                                            break;
                                        }
                                        holder.saved.setVisibility(View.VISIBLE);
                                        holder.saved.setImageResource(R.drawable.save);
                                        int ret = DownloadUtil.downloadTrack(mContext, audio, false);
                                        if (ret == 0)
                                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.saved_audio);
                                        else if (ret == 1) {
                                            PhoenixToast.CreatePhoenixToast(mContext).showToastError(R.string.exist_audio);
                                            new MaterialAlertDialogBuilder(mContext)
                                                    .setTitle(R.string.error)
                                                    .setMessage(R.string.audio_force_download)
                                                    .setPositiveButton(R.string.button_yes, (dialog_save, which_save) -> DownloadUtil.downloadTrack(mContext, audio, true))
                                                    .setNegativeButton(R.string.cancel, null)
                                                    .show();
                                        } else {
                                            holder.saved.setVisibility(View.GONE);
                                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.error_audio);
                                        }
                                        break;
                                    case R.id.bitrate_item_audio:
                                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                        retriever.setDataSource(Audio.getMp3FromM3u8(audio.getUrl()), new HashMap<>());
                                        String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                                        PhoenixToast.CreatePhoenixToast(mContext).showToast(mContext.getResources().getString(R.string.bitrate) + " " + (Long.parseLong(bitrate) / 1000) + " bit");
                                        break;
                                }
                            })
                            .setNegativeButton(R.string.button_cancel, null);
                    builder.show();
                });

                root.setVisibility(View.VISIBLE);
                root.setTag(audio);
            } else {
                root.setVisibility(View.GONE);
                root.setTag(null);
            }
        }
    }

    private static final class CopyHolder {

        final ViewGroup itemView;
        final ImageView ivAvatar;
        final TextView ownerName;
        final EmojiconTextView bodyView;
        final View tvShowMore;
        final View buttonDots;
        final AttachmentsHolder attachmentsHolder;

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

        final OnAttachmentsActionCallback callback;

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

    public interface OnAttachmentsActionCallback {
        void onPollOpen(@NonNull Poll poll);

        void onVideoPlay(@NonNull Video video);

        void onAudioPlay(int position, @NonNull ArrayList<Audio> audios);

        void onForwardMessagesOpen(@NonNull ArrayList<Message> messages);

        void onOpenOwner(int userId);

        void onDocPreviewOpen(@NonNull Document document);

        void onPostOpen(@NonNull Post post);

        void onLinkOpen(@NonNull Link link);

        void onWikiPageOpen(@NonNull WikiPage page);

        void onStickerOpen(@NonNull Sticker sticker);

        void onPhotosOpen(@NonNull ArrayList<Photo> photos, int index);
    }

    private class AudioHolder {

        TextView tvTitle;
        TextView tvSubtitle;
        ImageView ibPlay;
        TextView time;
        ImageView saved;
        ImageView lyric;
        ImageView my;
        ImageView quality;
        View Track;
        MaterialCardView selectionView;
        MaterialCardView isSelectedView;

        AudioHolder(View root) {
            tvTitle = root.findViewById(R.id.dialog_title);
            tvSubtitle = root.findViewById(R.id.dialog_message);
            ibPlay = root.findViewById(R.id.item_audio_play);
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

        Animator.AnimatorListener animationAdapter;
        ObjectAnimator animator;

        void startSomeAnimation(){
            selectionView.setCardBackgroundColor(CurrentTheme.getColorSecondary(mContext));
            selectionView.setAlpha(0.5f);

            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f);
            animator.setDuration(500);
            animator.addListener(animationAdapter);
            animator.start();
        }

        void cancelSelectionAnimation(){
            if(animator != null){
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

        VoiceHolder(View itemView) {
            mWaveFormView = itemView.findViewById(R.id.item_voice_wave_form_view);
            mWaveFormView.setActiveColor(mActiveWaveFormColor);
            mWaveFormView.setNoactiveColor(mNoactiveWaveFormColor);
            mWaveFormView.setSectionCount(Utils.isLandscape(itemView.getContext()) ? 128 : 64);
            mWaveFormView.setTag(generateHolderId());

            mButtonPlay = itemView.findViewById(R.id.item_voice_button_play);

            mDurationText = itemView.findViewById(R.id.item_voice_duration);
        }

        @Override
        public int getHolderId() {
            return (int) mWaveFormView.getTag();
        }
    }
}
