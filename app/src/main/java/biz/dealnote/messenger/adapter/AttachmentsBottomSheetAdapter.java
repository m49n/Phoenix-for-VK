package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.EventListener;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.holder.IdentificableHolder;
import biz.dealnote.messenger.adapter.holder.SharedHolders;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.AbsModel;
import biz.dealnote.messenger.model.Article;
import biz.dealnote.messenger.model.AttachmenEntry;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.model.Call;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.FwdMessages;
import biz.dealnote.messenger.model.Graffiti;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.view.CircleRoadProgress;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.isEmpty;

public class AttachmentsBottomSheetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ERROR_COLOR = Color.parseColor("#ff0000");
    private static final int VTYPE_BUTTON = 0;
    private static final int VTYPE_ENTRY = 1;

    private final List<AttachmenEntry> data;
    private final ActionListener actionListener;
    private SharedHolders<EntryHolder> holders;
    private int nextHolderId;
    private Context context;

    public AttachmentsBottomSheetAdapter(Context context, List<AttachmenEntry> data, ActionListener actionListener) {
        this.data = data;
        this.actionListener = actionListener;
        this.holders = new SharedHolders<>(false);
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VTYPE_BUTTON) {
            return new ImagesButtonHolder(inflater.inflate(R.layout.button_add_photo, parent, false));
        }

        EntryHolder holder = new EntryHolder(inflater.inflate(R.layout.message_attachments_entry, parent, false));
        holder.attachId(generateHolderId());
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VTYPE_BUTTON:
                bindAddPhotoButton((ImagesButtonHolder) holder);
                break;
            case VTYPE_ENTRY:
                bindEntryHolder((EntryHolder) holder, position);
                break;
        }
    }

    private void bindEntryHolder(EntryHolder holder, int position) {
        int dataPosition = position - 1;
        holders.put(dataPosition, holder);

        final AttachmenEntry entry = data.get(dataPosition);
        final AbsModel model = entry.getAttachment();

        if (model instanceof Photo) {
            bindImageHolder(holder, (Photo) model);
        } else if (model instanceof Upload) {
            bindUploading(holder, (Upload) model);
        } else if (model instanceof Post) {
            bindPost(holder, (Post) model);
        } else if (model instanceof Video) {
            bindVideo(holder, (Video) model);
        } else if (model instanceof FwdMessages) {
            bindMessages(holder, (FwdMessages) model);
        } else if (model instanceof Document) {
            bindDoc(holder, (Document) model);
        } else if (model instanceof Audio) {
            bindAudio(holder, (Audio) model);
        } else if (model instanceof Article) {
            bindArticle(holder, (Article) model);
        } else if (model instanceof Story) {
            bindStory(holder, (Story) model);
        } else if (model instanceof Call) {
            bindCall(holder, (Call) model);
        } else if (model instanceof AudioPlaylist) {
            bindAudioPlaylist(holder, (AudioPlaylist) model);
        } else if (model instanceof Graffiti) {
            bindGraffiti(holder, (Graffiti) model);
        } else if (model instanceof PhotoAlbum) {
            bindPhotoAlbum(holder, (PhotoAlbum) model);
        }

        holder.buttomRemove.setOnClickListener(v -> actionListener.onButtonRemoveClick(entry));
        holder.Retry.setOnClickListener(v -> actionListener.onButtonRetryClick(entry));
    }

    @SuppressWarnings("unused")
    private void bindMessages(EntryHolder holder, FwdMessages messages) {
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);

        holder.title.setText(R.string.messages);

        bindImageView(holder, null);
    }

    private void bindImageView(EntryHolder holder, String url) {
        if (isEmpty(url)) {
            PicassoInstance.with().cancelRequest(holder.image);
            holder.image.setImageResource(R.drawable.background_gray);
        } else {
            PicassoInstance.with()
                    .load(url)
                    .placeholder(R.drawable.background_gray)
                    .into(holder.image);
        }
    }

    private void bindImageAudioView(EntryHolder holder, String url) {
        if (isEmpty(url)) {
            PicassoInstance.with().cancelRequest(holder.image);
            holder.image.setImageResource(Settings.get().ui().isDarkModeEnabled(context) ? R.drawable.generic_audio_nowplaying_dark : R.drawable.generic_audio_nowplaying_light);
        } else {
            PicassoInstance.with()
                    .load(url)
                    .placeholder(R.drawable.background_gray)
                    .into(holder.image);
        }
    }

    private void bindPhotoAlbum(EntryHolder holder, PhotoAlbum album) {
        holder.title.setText(R.string.photo_album);
        String photoLink = nonNull(album.getSizes()) ? album.getSizes().getUrlForSize(PhotoSize.X, false) : null;
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        bindImageView(holder, photoLink);
    }

    private void bindGraffiti(EntryHolder holder, Graffiti graffiti) {
        holder.title.setText(R.string.graffity);
        String photoLink = graffiti.getUrl();
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        bindImageView(holder, photoLink);
    }

    private void bindArticle(EntryHolder holder, Article link) {
        holder.title.setText(R.string.article);
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        String photoLink = nonNull(link.getPhoto()) ? link.getPhoto().getUrlForSize(PhotoSize.X, false) : null;
        bindImageView(holder, photoLink);
    }

    private void bindAudioPlaylist(EntryHolder holder, AudioPlaylist link) {
        holder.title.setText(link.getTitle());
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        String photoLink = link.getThumb_image();
        bindImageView(holder, photoLink);
    }

    private void bindStory(EntryHolder holder, Story story) {
        holder.title.setText(R.string.story);
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        String photoLink = nonNull(story.getOwner()) ? story.getOwner().getMaxSquareAvatar() : null;
        bindImageView(holder, photoLink);
    }

    private void bindCall(EntryHolder holder, Call call) {
        holder.title.setText(R.string.call);
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        PicassoInstance.with().cancelRequest(holder.image);
        holder.image.setImageResource(R.drawable.phone_call_color);
    }

    private void bindAudio(EntryHolder holder, Audio audio) {
        String audiostr = audio.getArtist() + " - " + audio.getTitle();
        holder.title.setText(audiostr);
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        bindImageAudioView(holder, audio.getThumb_image_big());
    }

    private void bindVideo(EntryHolder holder, Video video) {
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        holder.title.setText(video.getTitle());

        bindImageView(holder, video.getImage());
    }

    private void bindDoc(EntryHolder holder, Document doc) {
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);
        holder.title.setText(doc.getTitle());

        String imgUrl = doc.getPreviewWithSize(PhotoSize.Q, false);

        bindImageView(holder, imgUrl);
    }

    private void bindPost(EntryHolder holder, Post post) {
        holder.progress.setVisibility(View.INVISIBLE);
        holder.Retry.setVisibility(View.GONE);
        holder.tintView.setVisibility(View.GONE);

        String title = post.getTextCopiesInclude();
        if (isEmpty(title)) {
            holder.title.setText(R.string.attachment_wall_post);
        } else {
            holder.title.setText(title);
        }

        String imgUrl = post.findFirstImageCopiesInclude(PhotoSize.Q, false);
        bindImageView(holder, imgUrl);
    }

    private void bindUploading(EntryHolder holder, Upload upload) {
        holder.tintView.setVisibility(View.VISIBLE);

        boolean inProgress = upload.getStatus() == Upload.STATUS_UPLOADING;
        holder.progress.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
        if (inProgress) {
            holder.progress.changePercentage(upload.getProgress());
        } else {
            holder.progress.changePercentage(0);
        }

        @ColorInt
        int titleColor = holder.title.getTextColors().getDefaultColor();

        holder.Retry.setVisibility(View.GONE);
        switch (upload.getStatus()) {
            case Upload.STATUS_UPLOADING:
                String precentText = upload.getProgress() + "%";
                holder.title.setText(precentText);
                break;
            case Upload.STATUS_CANCELLING:
                holder.title.setText(R.string.cancelling);
                break;
            case Upload.STATUS_QUEUE:
                holder.title.setText(R.string.in_order);
                break;
            case Upload.STATUS_ERROR:
                holder.title.setText(R.string.error);
                titleColor = ERROR_COLOR;
                holder.Retry.setVisibility(View.VISIBLE);
                break;
        }

        holder.title.setTextColor(titleColor);

        if (upload.hasThumbnail()) {
            PicassoInstance.with()
                    .load(upload.buildThumnailUri())
                    .placeholder(R.drawable.background_gray)
                    .into(holder.image);
        } else {
            PicassoInstance.with().cancelRequest(holder.image);
            holder.image.setImageResource(R.drawable.background_gray);
        }
    }

    public void changeUploadProgress(int dataPosition, int progress, boolean smoothly) {
        EntryHolder holder = holders.findOneByEntityId(dataPosition);
        if (nonNull(holder)) {
            String precentText = progress + "%";
            holder.title.setText(precentText);

            if (smoothly) {
                holder.progress.changePercentageSmoothly(progress);
            } else {
                holder.progress.changePercentage(progress);
            }
        }
    }

    private void bindImageHolder(EntryHolder holder, Photo photo) {
        String url = photo.getUrlForSize(PhotoSize.Q, false);

        holder.Retry.setVisibility(View.GONE);
        holder.progress.setVisibility(View.INVISIBLE);
        holder.tintView.setVisibility(View.GONE);
        holder.title.setText(R.string.photo);

        bindImageView(holder, url);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VTYPE_BUTTON : VTYPE_ENTRY;
    }

    private void bindAddPhotoButton(ImagesButtonHolder holder) {
        holder.button.setOnClickListener(v -> actionListener.onAddPhotoButtonClick());
    }

    @Override
    public int getItemCount() {
        return data.size() + 1;
    }

    private int generateHolderId() {
        nextHolderId++;
        return nextHolderId;
    }

    public interface ActionListener extends EventListener {
        void onAddPhotoButtonClick();

        void onButtonRemoveClick(AttachmenEntry entry);

        void onButtonRetryClick(AttachmenEntry entry);
    }

    private static class ImagesButtonHolder extends RecyclerView.ViewHolder {

        View button;

        ImagesButtonHolder(View itemView) {
            super(itemView);
            this.button = itemView.findViewById(R.id.add);
        }
    }

    private static class EntryHolder extends RecyclerView.ViewHolder implements IdentificableHolder {

        ImageView image;
        TextView title;
        View buttomRemove;
        CircleRoadProgress progress;
        ImageView Retry;
        View tintView;

        EntryHolder(View itemView) {
            super(itemView);
            this.image = itemView.findViewById(R.id.image);
            this.title = itemView.findViewById(R.id.title);
            this.buttomRemove = itemView.findViewById(R.id.progress_root);
            this.progress = itemView.findViewById(R.id.progress_view);
            this.tintView = itemView.findViewById(R.id.tint_view);
            this.Retry = itemView.findViewById(R.id.retry_upload);
        }

        @Override
        public int getHolderId() {
            return (int) tintView.getTag();
        }

        void attachId(int id) {
            this.tintView.setTag(id);
        }
    }
}