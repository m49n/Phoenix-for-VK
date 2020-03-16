package biz.dealnote.messenger.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.DownloadUtil;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.disposables.CompositeDisposable;

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

        if(item.CacheAudioIcon != null) {
            holder.play.setBackground(item.CacheAudioIcon);
        }
        else
            holder.play.setBackgroundResource(R.drawable.audio);

        if(item.getThumb_image_little() != null && item.CacheAudioIcon == null)
        {
            PicassoInstance.with()
                    .load(item.getThumb_image_little())
                    .transform(CurrentTheme.createTransformationForAvatar(Injection.provideApplicationContext()))
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            item.CacheAudioIcon = new BitmapDrawable(Injection.provideApplicationContext().getResources(), bitmap);
                            holder.play.setBackground(item.CacheAudioIcon);
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                        }
                    });
        }

        holder.artist.setText(item.getArtist());
        holder.title.setText(item.getTitle());
        holder.time.setText(AppTextUtils.getDurationString(item.getDuration()));

        holder.hq.setVisibility(item.isHq() ? View.VISIBLE : View.INVISIBLE);

        holder.my.setVisibility(item.getOwnerId() == Settings.get().accounts().getCurrent() ? View.VISIBLE : View.INVISIBLE);

        holder.saved.setVisibility(DownloadUtil.TrackIsDownloaded(item) ? View.VISIBLE : View.INVISIBLE);

        holder.play.setImageResource(MusicUtils.isNowPlayingOrPreparing(item) ? R.drawable.pause_button : R.drawable.play_button);

        holder.play.setOnClickListener(v -> {
            if (MusicUtils.isNowPlayingOrPreparing(item) || MusicUtils.isNowPaused(item)) {
                if(MusicUtils.isNowPlayingOrPreparing(item))
                    holder.play.setImageResource(R.drawable.play_button);
                else
                    holder.play.setImageResource(R.drawable.pause_button);
                MusicUtils.playOrPause();
            }
            else {
                if (mClickListener != null) {
                    mClickListener.onClick(holder.getAdapterPosition(), item);
                }
            }
        });
        holder.Track.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(mContext, holder.Track);
            popup.inflate(R.menu.audio_item_menu);
            popup.setOnMenuItemClickListener(item1 -> {
                switch (item1.getItemId()) {
                    case R.id.add_item_audio:
                        boolean myAudio = item.getOwnerId() == Settings.get().accounts().getCurrent();
                        if(myAudio)
                            delete(Settings.get().accounts().getCurrent(), item);
                        else
                            add(Settings.get().accounts().getCurrent(), item);
                        return true;
                    case R.id.save_item_audio:
                        if(!AppPerms.hasWriteStoragePermision(mContext)) {
                            AppPerms.requestWriteStoragePermission((Activity)mContext);
                        }
                        if(!AppPerms.hasReadStoragePermision(mContext)) {
                            AppPerms.requestReadExternalStoragePermission((Activity)mContext);
                        }
                        int ret = DownloadUtil.downloadTrack(mContext, item);
                        if(ret == 0)
                            PhoenixToast.showToast(mContext, R.string.saved_audio);
                        else if(ret == 1)
                            PhoenixToast.showToastSuccess(mContext, R.string.exist_audio);
                        else
                            PhoenixToast.showToast(mContext, R.string.error_audio);
                        return true;
                    case R.id.bitrate_item_audio:
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(Audio.getMp3FromM3u8(item.getUrl()), new HashMap<>());
                        String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                        PhoenixToast.showToast(mContext, mContext.getResources().getString(R.string.bitrate) + " " + (Long.parseLong(bitrate) / 1000) + " bit");
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
        ImageView hq;
        ImageView my;
        LinearLayout Track;

        AudioHolder(View itemView) {
            super(itemView);
            artist = itemView.findViewById(R.id.dialog_title);
            title = itemView.findViewById(R.id.dialog_message);
            play = itemView.findViewById(R.id.item_audio_play);
            time = itemView.findViewById(R.id.item_audio_time);
            saved = itemView.findViewById(R.id.saved);
            hq = itemView.findViewById(R.id.hq);
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
