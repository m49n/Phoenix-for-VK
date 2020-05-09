package biz.dealnote.messenger.adapter.horizontal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.ImageHelper;
import biz.dealnote.messenger.util.PolyTransformation;
import biz.dealnote.messenger.util.ViewUtils;

import static biz.dealnote.messenger.util.Objects.isNullOrEmptyString;

public class HorizontalPlaylistAdapter extends RecyclerBindableAdapter<VKApiAudioPlaylist, HorizontalPlaylistAdapter.Holder> {

    private Listener listener;

    public HorizontalPlaylistAdapter(List<VKApiAudioPlaylist> data) {
        super(data);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindItemViewHolder(Holder holder, int position, int type) {
        final VKApiAudioPlaylist playlist = getItem(position);

        Context context = holder.itemView.getContext();

        if (!isNullOrEmptyString(playlist.thumb_image))
            ViewUtils.displayAvatar(holder.thumb, new PolyTransformation(), playlist.thumb_image, Constants.PICASSO_TAG);
        else
            holder.thumb.setImageBitmap(ImageHelper.getPolyBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.generic_audio_nowplaying)));
        holder.count.setText(playlist.count + " " + context.getString(R.string.audios_pattern_count));
        holder.name.setText(playlist.title);
        if (isNullOrEmptyString(playlist.description))
            holder.description.setVisibility(View.GONE);
        else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(playlist.description);
        }
        if (isNullOrEmptyString(playlist.artist_name))
            holder.artist.setVisibility(View.GONE);
        else {
            holder.artist.setVisibility(View.VISIBLE);
            holder.artist.setText(playlist.artist_name);
        }
        if (playlist.Year == 0)
            holder.year.setVisibility(View.GONE);
        else {
            holder.year.setVisibility(View.VISIBLE);
            holder.year.setText(String.valueOf(playlist.Year));
        }
        if (isNullOrEmptyString(playlist.genre))
            holder.genre.setVisibility(View.GONE);
        else {
            holder.genre.setVisibility(View.VISIBLE);
            holder.genre.setText(playlist.genre);
        }
        holder.update.setText(AppTextUtils.getDateFromUnixTime(context, playlist.update_time));
        holder.add.setOnClickListener(v -> listener.onPlayListClick(playlist, position));
        if (playlist.owner_id == Settings.get().accounts().getCurrent())
            holder.add.setImageResource(R.drawable.delete);
        else
            holder.add.setImageResource(R.drawable.plus);
    }

    @Override
    protected Holder viewHolder(View view, int type) {
        return new Holder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_internal_audio_playlist;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onPlayListClick(VKApiAudioPlaylist item, int pos);
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView thumb;
        TextView name;
        TextView description;
        TextView count;
        TextView year;
        TextView artist;
        TextView genre;
        TextView update;
        View playlist_container;
        FloatingActionButton add;

        public Holder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.item_thumb);
            name = itemView.findViewById(R.id.item_name);
            count = itemView.findViewById(R.id.item_count);
            playlist_container = itemView.findViewById(R.id.playlist_container);
            description = itemView.findViewById(R.id.item_description);
            update = itemView.findViewById(R.id.item_time);
            year = itemView.findViewById(R.id.item_year);
            artist = itemView.findViewById(R.id.item_artist);
            genre = itemView.findViewById(R.id.item_genre);
            add = itemView.findViewById(R.id.add_playlist);
        }
    }
}
