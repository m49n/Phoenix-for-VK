package biz.dealnote.messenger.adapter;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.SearchTabsFragment;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.DownloadUtil;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Objects.isNullOrEmptyString;

public class AudioRecyclerAdapter extends RecyclerView.Adapter<AudioRecyclerAdapter.AudioHolder>{

    private Context mContext;
    private List<Audio> mData;
    private IAudioInteractor mAudioInteractor;
    private CompositeDisposable audioListDisposable = new CompositeDisposable();

    public AudioRecyclerAdapter(Context context, List<Audio> data) {
        this.mAudioInteractor = InteractorFactory.createAudioInteractor();
        this.mContext = context;
        this.mData = data;
    }

    private void delete(final int accoutnId, Audio audio) {
        audioListDisposable.add(mAudioInteractor.delete(accoutnId, audio.getId(), audio.getOwnerId()).compose(RxUtils.applyCompletableIOToMainSchedulers()).subscribe(() -> {}, ignore -> {}));
    }

    private void add(int accountId, Audio audio) {
        audioListDisposable.add(mAudioInteractor.add(accountId, audio, null, null).compose(RxUtils.applySingleIOToMainSchedulers()).subscribe(t -> {}, ignore -> {}));
    }

    @Override
    public AudioHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AudioHolder(LayoutInflater.from(mContext).inflate(R.layout.item_audio, parent, false));
    }

    @Override
    public void onBindViewHolder(final AudioHolder holder, int position) {
        final Audio item = mData.get(position);

        holder.artist.setText(item.getArtist());
        holder.title.setText(item.getTitle());
        holder.time.setText(AppTextUtils.getDurationString(item.getDuration()));

        holder.lyric.setVisibility(item.getLyricsId() != 0 ? View.VISIBLE : View.INVISIBLE);

        holder.my.setVisibility(item.getOwnerId() == Settings.get().accounts().getCurrent() ? View.VISIBLE : View.INVISIBLE);

        holder.saved.setVisibility(DownloadUtil.TrackIsDownloaded(item) ? View.VISIBLE : View.INVISIBLE);

        holder.play.setImageResource(MusicUtils.isNowPlayingOrPreparing(item) ? (!Settings.get().other().isUse_stop_audio() ? R.drawable.pause : R.drawable.stop) : (isNullOrEmptyString(item.getUrl()) ? R.drawable.audio_died : R.drawable.play));

        holder.play.setOnClickListener(v -> {
            if (MusicUtils.isNowPlayingOrPreparing(item) || MusicUtils.isNowPaused(item)) {
                if(MusicUtils.isNowPlayingOrPreparing(item))
                    holder.play.setImageResource(isNullOrEmptyString(item.getUrl()) ? R.drawable.audio_died : R.drawable.play);
                else {
                    if(!Settings.get().other().isUse_stop_audio())
                        holder.play.setImageResource(R.drawable.pause);
                    else
                        holder.play.setImageResource(R.drawable.stop);
                }
                if(!Settings.get().other().isUse_stop_audio())
                    MusicUtils.playOrPause();
                else
                    MusicUtils.stop();
            }
            else {
                if (mClickListener != null) {
                    if(!Settings.get().other().isUse_stop_audio())
                        holder.play.setImageResource(R.drawable.pause);
                    else
                        holder.play.setImageResource(R.drawable.stop);
                    mClickListener.onClick(holder.getAdapterPosition(), item);
                }
            }
        });
        holder.Track.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(mContext, holder.Track);
            popup.inflate(R.menu.audio_item_menu);
            popup.setOnMenuItemClickListener(item1 -> {
                switch (item1.getItemId()) {
                    case R.id.search_by_artist:
                        PlaceFactory.getSearchPlace(Settings.get().accounts().getCurrent(), SearchTabsFragment.TAB_MUSIC, new AudioSearchCriteria(item.getArtist(), true)).tryOpenWith(mContext);
                        return true;
                    case R.id.get_album_cover:
                        DownloadUtil.downloadTrackCover(mContext, item);
                        return true;
                    case R.id.copy_url:
                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("response", item.getUrl());
                        clipboard.setPrimaryClip(clip);
                        PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.copied);
                        return true;
                    case R.id.add_item_audio:
                        boolean myAudio = item.getOwnerId() == Settings.get().accounts().getCurrent();
                        if(myAudio) {
                            delete(Settings.get().accounts().getCurrent(), item);
                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.deleted);
                        }
                        else {
                            add(Settings.get().accounts().getCurrent(), item);
                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.added);
                        }
                        return true;
                    case R.id.save_item_audio:
                        if(!AppPerms.hasReadStoragePermision(mContext)) {
                            AppPerms.requestReadExternalStoragePermission((Activity)mContext);
                            return true;
                        }
                        if(!AppPerms.hasWriteStoragePermision(mContext)) {
                            AppPerms.requestWriteStoragePermission((Activity)mContext);
                            return true;
                        }
                        int ret = DownloadUtil.downloadTrack(mContext, item);
                        if(ret == 0)
                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.saved_audio);
                        else if(ret == 1)
                            PhoenixToast.CreatePhoenixToast(mContext).showToastError(R.string.exist_audio);
                        else
                            PhoenixToast.CreatePhoenixToast(mContext).showToast(R.string.error_audio);
                        return true;
                    case R.id.bitrate_item_audio:
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(Audio.getMp3FromM3u8(item.getUrl()), new HashMap<>());
                        String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                        PhoenixToast.CreatePhoenixToast(mContext).showToast(mContext.getResources().getString(R.string.bitrate) + " " + (Long.parseLong(bitrate) / 1000) + " bit");
                        return true;
                    default:
                        return false;
                }
            });
            if(item.getOwnerId() == Settings.get().accounts().getCurrent())
                popup.getMenu().findItem(R.id.add_item_audio).setTitle(R.string.delete);
            else
                popup.getMenu().findItem(R.id.add_item_audio).setTitle(R.string.action_add);
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setData(List<Audio> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    class AudioHolder extends RecyclerView.ViewHolder {

        TextView artist;
        TextView title;
        ImageView play;
        TextView time;
        ImageView saved;
        ImageView lyric;
        ImageView my;
        LinearLayout Track;

        AudioHolder(View itemView) {
            super(itemView);
            artist = itemView.findViewById(R.id.dialog_title);
            title = itemView.findViewById(R.id.dialog_message);
            play = itemView.findViewById(R.id.item_audio_play);
            time = itemView.findViewById(R.id.item_audio_time);
            saved = itemView.findViewById(R.id.saved);
            lyric = itemView.findViewById(R.id.lyric);
            Track = itemView.findViewById(R.id.track_option);
            my = itemView.findViewById(R.id.my);
        }
    }

    private ClickListener mClickListener;

    public void setClickListener(ClickListener clickListener) {
        this.mClickListener = clickListener;
    }

    public interface ClickListener {
        void onClick(int position, Audio audio);
    }
}
