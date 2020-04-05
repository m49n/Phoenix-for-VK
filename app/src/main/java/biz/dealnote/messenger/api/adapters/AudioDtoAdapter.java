package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import biz.dealnote.messenger.api.model.VKApiAudio;

/**
 * Created by ruslan.kolbasa on 28.12.2016.
 * phoenix
 */
public class AudioDtoAdapter extends AbsAdapter implements JsonDeserializer<VKApiAudio> {

    @Override
    public VKApiAudio deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        VKApiAudio dto = new VKApiAudio();
        dto.id = root.has("id") ? root.get("id").getAsInt() : 0;
        dto.owner_id = root.has("owner_id") ? root.get("owner_id").getAsInt() : 0;
        dto.artist = root.has("artist") ? root.get("artist").getAsString() : null;
        dto.title =  root.has("title") ? root.get("title").getAsString() : null;
        dto.duration = root.has("duration") ? root.get("duration").getAsInt() : 0;
        dto.url = root.has("url") ? root.get("url").getAsString() : null;
        dto.lyrics_id = root.has("lyrics_id") ? root.get("lyrics_id").getAsInt() : 0;
        dto.album_id = root.has("album_id") ? root.get("album_id").getAsInt() : 0;
        dto.genre_id = root.has("genre_id") ? root.get("genre_id").getAsInt() : 0;
        dto.access_key = root.has("access_key") ? root.get("access_key").getAsString() : null;

        if(root.has("album"))
        {
            JsonObject thmb = root.getAsJsonObject("album");
            if(root.has("title"))
                dto.album_title = thmb.get("title").getAsString();
            if(thmb.has("thumb")) {
                thmb = thmb.getAsJsonObject("thumb");
                if (thmb.has("photo_68"))
                    dto.thumb_image_little = thmb.get("photo_68").getAsString();
                else if (thmb.has("photo_34"))
                    dto.thumb_image_little = thmb.get("photo_34").getAsString();

                if (thmb.has("photo_1200")) {
                    dto.thumb_image_very_big = thmb.get("photo_1200").getAsString();
                }
                if (thmb.has("photo_600")) {
                    dto.thumb_image_big = thmb.get("photo_600").getAsString();
                    if(dto.thumb_image_very_big == null)
                        dto.thumb_image_very_big = thmb.get("photo_600").getAsString();
                }
                else if (thmb.has("photo_300")) {
                    dto.thumb_image_big = thmb.get("photo_300").getAsString();
                    if(dto.thumb_image_very_big == null)
                        dto.thumb_image_very_big = thmb.get("photo_300").getAsString();
                }
            }
        }

        return dto;
    }
}
