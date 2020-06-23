package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.text.TextUtils;

import java.util.Calendar;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.AbsModel;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.model.Call;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.Graffiti;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.PhotoSizes;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.model.Types;
import biz.dealnote.messenger.model.WikiPage;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;

public class DocLink {

    private static final String URL = "URL";
    private static final String W = "WIKI";
    public AbsModel attachment;
    private int type;

    public DocLink(AbsModel attachment) {
        this.attachment = attachment;
        this.type = typeOf(attachment);
    }

    private static int typeOf(AbsModel model) {
        if (model instanceof Document) {
            return Types.DOC;
        }

        if (model instanceof Post) {
            return Types.POST;
        }

        if (model instanceof Link) {
            return Types.LINK;
        }

        if (model instanceof Poll) {
            return Types.POLL;
        }

        if (model instanceof WikiPage) {
            return Types.WIKI_PAGE;
        }

        if (model instanceof Story) {
            return Types.STORY;
        }

        if (model instanceof Call) {
            return Types.CALL;
        }

        if (model instanceof AudioPlaylist) {
            return Types.AUDIO_PLAYLIST;
        }

        if (model instanceof Graffiti) {
            return Types.GRAFFITY;
        }

        if (model instanceof PhotoAlbum) {
            return Types.ALBUM;
        }

        throw new IllegalArgumentException();
    }

    public int getType() {
        return type;
    }

    public String getImageUrl() {
        switch (type) {
            case Types.DOC:
                Document doc = (Document) attachment;
                return doc.getPreviewWithSize(Settings.get().main().getPrefPreviewImageSize(), true);

            case Types.POST:
                return ((Post) attachment).getAuthorPhoto();

            case Types.GRAFFITY:
                return ((Graffiti) attachment).getUrl();

            case Types.STORY:
                return ((Story) attachment).getOwner().getMaxSquareAvatar();

            case Types.ALBUM:
                PhotoAlbum album = (PhotoAlbum) attachment;
                if (Objects.nonNull(album.getSizes())) {
                    PhotoSizes sizes = album.getSizes();
                    return sizes.getUrlForSize(Settings.get().main().getPrefPreviewImageSize(), true);
                }
                return null;

            case Types.AUDIO_PLAYLIST:
                return ((AudioPlaylist) attachment).getThumb_image();

            case Types.LINK:
                Link link = (Link) attachment;

                if (link.getPhoto() == null && link.getPreviewPhoto() != null)
                    return link.getPreviewPhoto();

                if (Objects.nonNull(link.getPhoto()) && Objects.nonNull(link.getPhoto().getSizes())) {
                    PhotoSizes sizes = link.getPhoto().getSizes();
                    return sizes.getUrlForSize(Settings.get().main().getPrefPreviewImageSize(), true);
                }

                return null;
        }

        return null;
    }

    public String getTitle(Context context) {
        String title;
        switch (type) {
            case Types.DOC:
                return ((Document) attachment).getTitle();

            case Types.POST:
                return ((Post) attachment).getAuthorName();

            case Types.AUDIO_PLAYLIST:
                return ((AudioPlaylist) attachment).getTitle();

            case Types.ALBUM:
                return ((PhotoAlbum) attachment).getTitle();

            case Types.LINK:
                title = ((Link) attachment).getTitle();
                if (TextUtils.isEmpty(title)) {
                    title = "[" + context.getString(R.string.attachment_link).toLowerCase() + "]";
                }
                return title;

            case Types.POLL:
                Poll poll = (Poll) attachment;
                return context.getString(poll.isAnonymous() ? R.string.anonymous_poll : R.string.open_poll);

            case Types.STORY:
                return ((Story) attachment).getOwner().getFullName();

            case Types.WIKI_PAGE:
                return context.getString(R.string.wiki_page);

            case Types.CALL:
                int initiator = ((Call) attachment).getInitiator_id();
                return initiator == Settings.get().accounts().getCurrent() ? context.getString(R.string.input_call) : context.getString(R.string.output_call);
        }
        return null;
    }

    public String getExt(Context context) {
        switch (type) {
            case Types.DOC:
                return ((Document) attachment).getExt();
            case Types.POST:
                return null;
            case Types.LINK:
                return URL;
            case Types.WIKI_PAGE:
                return W;
            case Types.STORY:
                return context.getString(R.string.story);
            case Types.AUDIO_PLAYLIST:
                return context.getString(R.string.playlist);
        }
        return null;
    }

    public String getSecondaryText(Context context) {
        switch (type) {
            case Types.DOC:
                return AppTextUtils.getSizeString((int) ((Document) attachment).getSize());

            case Types.POST:
                Post post = (Post) attachment;
                return post.hasText() ? post.getText() : (post.hasAttachments() ? "" : "[" + context.getString(R.string.wall_post) + "]");

            case Types.LINK:
                return ((Link) attachment).getUrl();

            case Types.ALBUM:
                return Utils.firstNonEmptyString(((PhotoAlbum) attachment).getDescription(), " ") +
                        " " + context.getString(R.string.photos_count, ((PhotoAlbum) attachment).getSize());

            case Types.POLL:
                return ((Poll) attachment).getQuestion();

            case Types.WIKI_PAGE:
                return ((WikiPage) attachment).getTitle();

            case Types.CALL:
                return ((Call) attachment).getState();

            case Types.AUDIO_PLAYLIST:
                return Utils.firstNonEmptyString(((AudioPlaylist) attachment).getArtist_name(), " ") + " " +
                        ((AudioPlaylist) attachment).getCount() + " " + context.getString(R.string.audios_pattern_count);

            case Types.STORY: {
                Story item = ((Story) attachment);
                if (item.getExpires() <= 0)
                    return null;
                else {
                    if (item.isIs_expired()) {
                        return context.getString(R.string.is_expired);
                    } else {
                        Long exp = (item.getExpires() - Calendar.getInstance().getTime().getTime() / 1000) / 3600;
                        return (context.getString(R.string.expires, String.valueOf(exp), context.getString(Utils.declOfNum(exp, new int[]{R.string.hour, R.string.hour_sec, R.string.hours}))));
                    }
                }
            }
        }
        return null;
    }
}
