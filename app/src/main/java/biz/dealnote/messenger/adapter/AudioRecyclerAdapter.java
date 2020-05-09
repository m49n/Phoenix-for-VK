package biz.dealnote.messenger.adapter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.SendAttachmentsActivity;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.Text;
import biz.dealnote.messenger.model.menu.Item;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.DownloadUtil;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.PolyTransformation;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.view.WeakViewAnimatorAdapter;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Objects.isNullOrEmptyString;

public class AudioRecyclerAdapter extends RecyclerBindableAdapter<Audio, AudioRecyclerAdapter.AudioHolder> {

    private Context mContext;
    private IAudioInteractor mAudioInteractor;
    private boolean not_show_my;
    private boolean iSSelectMode;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();
    private ClickListener mClickListener;

    public AudioRecyclerAdapter(Context context, List<Audio> data, boolean not_show_my, boolean iSSelectMode) {
        super(data);
        this.mAudioInteractor = InteractorFactory.createAudioInteractor();
        this.mContext = context;
        this.not_show_my = not_show_my;
        this.iSSelectMode = iSSelectMode;
    }

    private void deleteTrack(final int accoutnId, Audio audio) {
        audioListDisposable.add(mAudioInteractor.delete(accoutnId, audio.getId(), audio.getOwnerId()).compose(RxUtils.applyCompletableIOToMainSchedulers()).subscribe(() -> {
        }, ignore -> {
        }));
    }

    public void addTrack(int accountId, Audio audio) {
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

    @Override
    protected void onBindItemViewHolder(AudioHolder holder, int position, int type) {
        final Audio item = getItem(position);

        holder.cancelSelectionAnimation();
        if (item.isAnimationNow()) {
            holder.startSelectionAnimation();
            item.setAnimationNow(false);
        }

        holder.artist.setText(item.getArtist());
        if (!item.isHLS()) {
            holder.quality.setVisibility(View.VISIBLE);
            if (item.getIsHq())
                holder.quality.setImageResource(R.drawable.high_quality);
            else
                holder.quality.setImageResource(R.drawable.low_quality);
        } else
            holder.quality.setVisibility(View.GONE);

        holder.title.setText(item.getTitle());
        if (item.getDuration() <= 0)
            holder.time.setVisibility(View.GONE);
        else {
            holder.time.setVisibility(View.VISIBLE);
            holder.time.setText(AppTextUtils.getDurationString(item.getDuration()));
        }

        holder.lyric.setVisibility(item.getLyricsId() != 0 ? View.VISIBLE : View.GONE);
        holder.isSelectedView.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);

        if (not_show_my)
            holder.my.setVisibility(View.GONE);
        else
            holder.my.setVisibility(item.getOwnerId() == Settings.get().accounts().getCurrent() ? View.VISIBLE : View.GONE);

        holder.saved.setVisibility(DownloadUtil.TrackIsDownloaded(item) ? View.VISIBLE : View.GONE);
        holder.saved.setImageResource(R.drawable.downloaded);

        holder.play_icon.setImageResource(MusicUtils.isNowPlayingOrPreparing(item) ? R.drawable.voice_state_animation : (MusicUtils.isNowPaused(item) ? R.drawable.paused : (isNullOrEmptyString(item.getUrl()) ? R.drawable.audio_died : R.drawable.song)));
        Utils.doAnimate(holder.play_icon.getDrawable(), true);

        if (Settings.get().other().isShow_audio_cover()) {
            if (!isNullOrEmptyString(item.getThumb_image_little())) {
                PicassoInstance.with()
                        .load(item.getThumb_image_little())
                        .placeholder(mContext.getResources().getDrawable(getAudioCoverSimple(), mContext.getTheme()))
                        .transform(TransformCover())
                        .tag(Constants.PICASSO_TAG)
                        .into(holder.play_cover);
            } else {
                PicassoInstance.with().cancelRequest(holder.play_cover);
                holder.play_cover.setImageDrawable(mContext.getResources().getDrawable(getAudioCoverSimple(), mContext.getTheme()));
            }
        }

        holder.play.setOnClickListener(v -> {
            if (MusicUtils.isNowPlayingOrPreparingOrPaused(item)) {
                if (!Settings.get().other().isUse_stop_audio()) {
                    if (!MusicUtils.isNowPaused(item))
                        holder.play_icon.setImageResource(R.drawable.paused);
                    else {
                        holder.play_icon.setImageResource(R.drawable.voice_state_animation);
                        Utils.doAnimate(holder.play_icon.getDrawable(), true);
                    }
                    MusicUtils.playOrPause();
                } else {
                    holder.play_icon.setImageResource(isNullOrEmptyString(item.getUrl()) ? R.drawable.audio_died : R.drawable.song);
                    MusicUtils.stop();
                }
            } else {
                if (mClickListener != null) {
                    holder.play_icon.setImageResource(R.drawable.voice_state_animation);
                    Utils.doAnimate(holder.play_icon.getDrawable(), true);
                    mClickListener.onClick(position, item);
                }
            }
        });

        if (!iSSelectMode) {
            holder.Track.setOnLongClickListener(v -> {
                if (!AppPerms.hasReadWriteStoragePermision(mContext)) {
                    AppPerms.requestReadWriteStoragePermission((Activity) mContext);
                    return false;
                }
                holder.saved.setVisibility(View.VISIBLE);
                holder.saved.setImageResource(R.drawable.save);
                int ret = DownloadUtil.downloadTrack(mContext, item, false);
                if (ret == 0)
                    PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.saved_audio);
                else if (ret == 1) {
                    PhoenixToast.CreatePhoenixToast(mContext).showToastError(R.string.exist_audio);
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.error)
                            .setMessage(R.string.audio_force_download)
                            .setPositiveButton(R.string.button_yes, (dialog_save, which_save) -> DownloadUtil.downloadTrack(mContext, item, true))
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    holder.saved.setVisibility(View.GONE);
                    PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.error_audio);
                }
                return true;
            });
            holder.Track.setOnClickListener(view -> {
                holder.cancelSelectionAnimation();
                holder.startSomeAnimation();
                final List<Item> items = new ArrayList<>();
                items.add(new Item(R.id.play_item_audio, new Text(R.string.play)).setIcon(R.drawable.play));
                if (item.getOwnerId() != Settings.get().accounts().getCurrent())
                    items.add(new Item(R.id.add_item_audio, new Text(R.string.action_add)).setIcon(R.drawable.list_add));
                else
                    items.add(new Item(R.id.add_item_audio, new Text(R.string.delete)).setIcon(R.drawable.delete));
                items.add(new Item(R.id.share_button, new Text(R.string.share)).setIcon(R.drawable.share_variant));
                items.add(new Item(R.id.save_item_audio, new Text(R.string.save)).setIcon(R.drawable.save));
                if (item.getAlbumId() != 0)
                    items.add(new Item(R.id.open_album, new Text(R.string.open_album)).setIcon(R.drawable.audio_album));
                items.add(new Item(R.id.get_recommendation_by_audio, new Text(R.string.get_recommendation_by_audio)).setIcon(R.drawable.music_mic));
                if (item.getLyricsId() != 0)
                    items.add(new Item(R.id.get_lyrics_menu, new Text(R.string.get_lyrics_menu)).setIcon(R.drawable.lyric));
                items.add(new Item(R.id.bitrate_item_audio, new Text(R.string.get_bitrate)).setIcon(R.drawable.high_quality));
                items.add(new Item(R.id.search_by_artist, new Text(R.string.search_by_artist)).setIcon(R.drawable.magnify));
                items.add(new Item(R.id.copy_url, new Text(R.string.copy_url)).setIcon(R.drawable.content_copy));

                MenuAdapter mAdapter = new MenuAdapter(mContext, items);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                        .setIcon(R.drawable.dir_song)
                        .setTitle(Utils.firstNonEmptyString(item.getArtist(), " ") + " - " + item.getTitle())
                        .setAdapter(mAdapter, (dialog, which) -> {
                            switch (items.get(which).getKey()) {
                                case R.id.play_item_audio:
                                    if (mClickListener != null) {
                                        mClickListener.onClick(position, item);
                                        if (Settings.get().other().isShow_mini_player())
                                            PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(mContext);
                                    }
                                    break;
                                case R.id.share_button:
                                    SendAttachmentsActivity.startForSendAttachments(mContext, Settings.get().accounts().getCurrent(), item);
                                    break;
                                case R.id.search_by_artist:
                                    PlaceFactory.getSingleTabSearchPlace(Settings.get().accounts().getCurrent(), SearchContentType.AUDIOS, new AudioSearchCriteria(item.getArtist(), true, false)).tryOpenWith(mContext);
                                    break;
                                case R.id.get_lyrics_menu:
                                    get_lyrics(item);
                                    break;
                                case R.id.get_recommendation_by_audio:
                                    PlaceFactory.SearchByAudioPlace(Settings.get().accounts().getCurrent(), item.getOwnerId(), item.getId()).tryOpenWith(mContext);
                                    break;
                                case R.id.open_album:
                                    PlaceFactory.getAudiosInAlbumPlace(Settings.get().accounts().getCurrent(), item.getAlbum_owner_id(), item.getAlbumId(), item.getAlbum_access_key()).tryOpenWith(mContext);
                                    break;
                                case R.id.copy_url:
                                    ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText("response", item.getUrl());
                                    clipboard.setPrimaryClip(clip);
                                    PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.copied);
                                    break;
                                case R.id.add_item_audio:
                                    boolean myAudio = item.getOwnerId() == Settings.get().accounts().getCurrent();
                                    if (myAudio) {
                                        deleteTrack(Settings.get().accounts().getCurrent(), item);
                                        PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.deleted);
                                    } else {
                                        addTrack(Settings.get().accounts().getCurrent(), item);
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
                                    int ret = DownloadUtil.downloadTrack(mContext, item, false);
                                    if (ret == 0)
                                        PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.saved_audio);
                                    else if (ret == 1) {
                                        PhoenixToast.CreatePhoenixToast(mContext).showToastError(R.string.exist_audio);
                                        new MaterialAlertDialogBuilder(mContext)
                                                .setTitle(R.string.error)
                                                .setMessage(R.string.audio_force_download)
                                                .setPositiveButton(R.string.button_yes, (dialog_save, which_save) -> DownloadUtil.downloadTrack(mContext, item, true))
                                                .setNegativeButton(R.string.cancel, null)
                                                .show();
                                    } else {
                                        holder.saved.setVisibility(View.GONE);
                                        PhoenixToast.CreatePhoenixToast(mContext).showToastBottom(R.string.error_audio);
                                    }
                                    break;
                                case R.id.bitrate_item_audio:
                                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                    retriever.setDataSource(Audio.getMp3FromM3u8(item.getUrl()), new HashMap<>());
                                    String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                                    PhoenixToast.CreatePhoenixToast(mContext).showToast(mContext.getResources().getString(R.string.bitrate) + " " + (Long.parseLong(bitrate) / 1000) + " bit");
                                    break;
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null);
                builder.show();
            });
        } else {
            holder.Track.setOnClickListener(view -> {
                item.setIsSelected(!item.isSelected());
                holder.isSelectedView.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
            });
        }
    }

    @Override
    protected AudioHolder viewHolder(View view, int type) {
        return new AudioHolder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_audio;
    }

    public void setData(List<Audio> data) {
        setItems(data);
    }

    public void setClickListener(ClickListener clickListener) {
        this.mClickListener = clickListener;
    }

    public interface ClickListener {
        void onClick(int position, Audio audio);
    }

    class AudioHolder extends RecyclerView.ViewHolder {

        TextView artist;
        TextView title;
        View play;
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
        AudioHolder(View itemView) {
            super(itemView);
            artist = itemView.findViewById(R.id.dialog_title);
            title = itemView.findViewById(R.id.dialog_message);
            play = itemView.findViewById(R.id.item_audio_play);
            play_icon = itemView.findViewById(R.id.item_audio_play_icon);
            play_cover = itemView.findViewById(R.id.item_audio_play_cover);
            time = itemView.findViewById(R.id.item_audio_time);
            saved = itemView.findViewById(R.id.saved);
            lyric = itemView.findViewById(R.id.lyric);
            Track = itemView.findViewById(R.id.track_option);
            my = itemView.findViewById(R.id.my);
            quality = itemView.findViewById(R.id.quality);
            isSelectedView = itemView.findViewById(R.id.item_audio_select_add);
            selectionView = itemView.findViewById(R.id.item_audio_selection);
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

        void startSelectionAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorPrimary(mContext));
            selectionView.setAlpha(0.5f);

            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f);
            animator.setDuration(1500);
            animator.addListener(animationAdapter);
            animator.start();
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
}
