package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

import biz.dealnote.messenger.api.model.VKApiArticle;
import biz.dealnote.messenger.api.model.VKApiAttachment;
import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.api.model.VKApiGiftItem;
import biz.dealnote.messenger.api.model.VKApiLink;
import biz.dealnote.messenger.api.model.VKApiPhoto;
import biz.dealnote.messenger.api.model.VKApiPoll;
import biz.dealnote.messenger.api.model.VKApiPost;
import biz.dealnote.messenger.api.model.VKApiSticker;
import biz.dealnote.messenger.api.model.VKApiVideo;
import biz.dealnote.messenger.api.model.VKApiWikiPage;
import biz.dealnote.messenger.api.model.VkApiAttachments;
import biz.dealnote.messenger.api.model.VkApiAudioMessage;
import biz.dealnote.messenger.api.model.VkApiDoc;
import biz.dealnote.messenger.util.Objects;

/**
 * Created by admin on 27.12.2016.
 * phoenix
 */
public class AttachmentsDtoAdapter extends AbsAdapter implements JsonDeserializer<VkApiAttachments> {

    @Override
    public VkApiAttachments deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        VkApiAttachments dto = new VkApiAttachments();

        dto.entries = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject o = array.get(i).getAsJsonObject();

            String type = optString(o, "type");
            VKApiAttachment attachment = parse(type, o, context);

            if (Objects.nonNull(attachment)) {
                dto.entries.add(new VkApiAttachments.Entry(type, attachment));
            }
        }

        return dto;
    }

    private VKApiAttachment parse(String type, JsonObject root, JsonDeserializationContext context) {
        JsonElement o = root.get(type);

        //{"type":"photos_list","photos_list":["406536042_456239026"]}

        if (VkApiAttachments.TYPE_PHOTO.equals(type)) {
            return context.deserialize(o, VKApiPhoto.class);
        } else if (VkApiAttachments.TYPE_VIDEO.equals(type)) {
            return context.deserialize(o, VKApiVideo.class);
        } else if (VkApiAttachments.TYPE_AUDIO.equals(type)) {
            return context.deserialize(o, VKApiAudio.class);
        } else if (VkApiAttachments.TYPE_DOC.equals(type)) {
            return context.deserialize(o, VkApiDoc.class);
        } else if (VkApiAttachments.TYPE_POST.equals(type) || VkApiAttachments.TYPE_FAVE_POST.equals(type)) {
            return context.deserialize(o, VKApiPost.class);
            //} else if (VkApiAttachments.TYPE_POSTED_PHOTO.equals(type)) {
            //    return context.deserialize(o, VKApiPostedPhoto.class);
        } else if (VkApiAttachments.TYPE_LINK.equals(type)) {
            return context.deserialize(o, VKApiLink.class);
            //} else if (VkApiAttachments.TYPE_NOTE.equals(type)) {
            //    return context.deserialize(o, VKApiNote.class);
            //} else if (VkApiAttachments.TYPE_APP.equals(type)) {
            //    return context.deserialize(o, VKApiApplicationContent.class);
        } else if (VkApiAttachments.TYPE_ARTICLE.equals(type)) {
            return context.deserialize(o, VKApiArticle.class);
        } else if (VkApiAttachments.TYPE_POLL.equals(type)) {
            return context.deserialize(o, VKApiPoll.class);
        } else if (VkApiAttachments.TYPE_WIKI_PAGE.equals(type)) {
            return context.deserialize(o, VKApiWikiPage.class);
            //} else if (VkApiAttachments.TYPE_ALBUM.equals(type)) {
            //    return context.deserialize(o, VKApiPhotoAlbum.class); // not supported yet
        } else if (VkApiAttachments.TYPE_STICKER.equals(type)) {
            return context.deserialize(o, VKApiSticker.class);
        } else if (VKApiAttachment.TYPE_AUDIO_MESSAGE.equals(type)) {
            return context.deserialize(o, VkApiAudioMessage.class);
        } else if (VKApiAttachment.TYPE_GIFT.equals(type)) {
            return context.deserialize(o, VKApiGiftItem.class);
        } else if (VKApiAttachment.TYPE_GRAFFITY.equals(type)) {
            VKApiSticker graph = new VKApiSticker();
            graph.sticker_id = optInt(o.getAsJsonObject(), "id");
            graph.images = new ArrayList<>();
            VKApiSticker.Image img = new VKApiSticker.Image();
            img.url = optString(o.getAsJsonObject(), "url");
            img.height = optInt(o.getAsJsonObject(), "height");
            img.width = optInt(o.getAsJsonObject(), "width");
            graph.images.add(img);
            graph.images_with_background = new ArrayList<>();
            graph.images_with_background.add(img);
            return graph;

        } else if (VKApiAttachment.TYPE_HISTORY.equals(type)) {
            if (o.getAsJsonObject().has("photo"))
                return context.deserialize(o.getAsJsonObject().get("photo"), VKApiPhoto.class);
            else if (o.getAsJsonObject().has("video"))
                return context.deserialize(o.getAsJsonObject().get("video"), VKApiVideo.class);
        } else if (VKApiAttachment.TYPE_AUDIO_PLAYLIST.equals(type)) {
            VKApiLink ret = new VKApiLink();
            VKApiAudioPlaylist pl = context.deserialize(o, VKApiAudioPlaylist.class);
            ret.url = "https://vk.com/music/album/" + pl.owner_id + "_" + pl.id;
            if (pl.access_key != null)
                ret.url += "_" + pl.access_key;
            ret.caption = pl.title;
            ret.description = pl.description;
            ret.title = pl.title;
            ret.preview_photo = pl.thumb_image;
            return ret;
        }

        return null;
    }
}
