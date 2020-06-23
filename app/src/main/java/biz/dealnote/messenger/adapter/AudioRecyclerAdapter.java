package biz.dealnote.messenger.adapter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Transformation;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.SendAttachmentsActivity;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.OptionRequest;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.menu.AudioItem;
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

public class AudioRecyclerAdapter extends RecyclerBindableAdapter<Audio, AudioRecyclerAdapter.AudioHolder> {

    private Context mContext;
    private IAudioInteractor mAudioInteractor;
    private boolean not_show_my;
    private boolean iSSelectMode;
    private int iCatalogBlock;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();
    private ClickListener mClickListener;

    public AudioRecyclerAdapter(Context context, List<Audio> data, boolean not_show_my, boolean iSSelectMode, int iCatalogBlock) {
        super(data);
        this.mAudioInteractor = InteractorFactory.createAudioInteractor();
        this.mContext = context;
        this.not_show_my = not_show_my;
        this.iSSelectMode = iSSelectMode;
        this.iCatalogBlock = iCatalogBlock;
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
        final Audio audio = getItem(position);

        holder.cancelSelectionAnimation();
        if (audio.isAnimationNow()) {
            holder.startSelectionAnimation();
            audio.setAnimationNow(false);
        }

        holder.artist.setText(audio.getArtist());
        if (!audio.isHLS()) {
            holder.quality.setVisibility(View.VISIBLE);
            if (audio.getIsHq())
                holder.quality.setImageResource(R.drawable.high_quality);
            else
                holder.quality.setImageResource(R.drawable.low_quality);
        } else
            holder.quality.setVisibility(View.GONE);

        holder.title.setText(audio.getTitle());
        if (audio.getDuration() <= 0)
            holder.time.setVisibility(View.INVISIBLE);
        else {
            holder.time.setVisibility(View.VISIBLE);
            holder.time.setText(AppTextUtils.getDurationString(audio.getDuration()));
        }

        holder.lyric.setVisibility(audio.getLyricsId() != 0 ? View.VISIBLE : View.GONE);
        holder.isSelectedView.setVisibility(audio.isSelected() ? View.VISIBLE : View.GONE);

        if (not_show_my)
            holder.my.setVisibility(View.GONE);
        else
            holder.my.setVisibility(audio.getOwnerId() == Settings.get().accounts().getCurrent() ? View.VISIBLE : View.GONE);

        int Status = DownloadUtil.TrackIsDownloaded(audio);
        holder.saved.setVisibility(Status != 0 ? View.VISIBLE : View.GONE);
        if (Status == 1)
            holder.saved.setImageResource(R.drawable.save);
        else if (Status == 2)
            holder.saved.setImageResource(R.drawable.remote_cloud);

        holder.play_icon.setImageResource(MusicUtils.isNowPlayingOrPreparing(audio) ? R.drawable.voice_state_animation : (MusicUtils.isNowPaused(audio) ? R.drawable.paused : (Utils.isEmpty(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song)));
        Utils.doAnimate(holder.play_icon.getDrawable(), true);

        if (Settings.get().other().isShow_audio_cover()) {
            if (!Utils.isEmpty(audio.getThumb_image_little())) {
                PicassoInstance.with()
                        .load(audio.getThumb_image_little())
                        .placeholder(Objects.requireNonNull(ResourcesCompat.getDrawable(mContext.getResources(), getAudioCoverSimple(), mContext.getTheme())))
                        .transform(TransformCover())
                        .tag(Constants.PICASSO_TAG)
                        .into(holder.play_cover);
            } else {
                PicassoInstance.with().cancelRequest(holder.play_cover);
                holder.play_cover.setImageResource(getAudioCoverSimple());
            }
        } else {
            PicassoInstance.with().cancelRequest(holder.play_cover);
            holder.play_cover.setImageResource(getAudioCoverSimple());
        }

        holder.play.setOnClickListener(v -> {
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
                if (mClickListener != null) {
                    holder.play_icon.setImageResource(R.drawable.voice_state_animation);
                    Utils.doAnimate(holder.play_icon.getDrawable(), true);
                    mClickListener.onClick(position, iCatalogBlock, audio);
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

                if (!Utils.isEmpty(audio.getMain_artists()))
                    menus.add(new OptionRequest(AudioItem.goto_artist, mContext.getString(R.string.audio_goto_artist), R.drawable.account_circle));

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
                            if (mClickListener != null) {
                                mClickListener.onClick(position, iCatalogBlock, audio);
                                if (Settings.get().other().isShow_mini_player())
                                    PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(mContext);
                            }
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
        } else {
            holder.Track.setOnClickListener(view -> {
                audio.setIsSelected(!audio.isSelected());
                holder.isSelectedView.setVisibility(audio.isSelected() ? View.VISIBLE : View.GONE);
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
        void onClick(int position, int catalog, Audio audio);
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
