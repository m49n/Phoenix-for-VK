package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;

/**
 * Created by ruslan.kolbasa on 27.12.2016.
 * phoenix
 */
public class AudioPlaylistDtoAdapter extends AbsAdapter implements JsonDeserializer<VKApiAudioPlaylist> {

    @Override
    public VKApiAudioPlaylist deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        VKApiAudioPlaylist album = new VKApiAudioPlaylist();

        album.id = optInt(root, "id");
        album.count = optInt(root, "count");
        album.owner_id = optInt(root, "owner_id");
        album.title = optString(root, "title");
        album.description = optString(root, "description");
        album.update_time = optInt(root, "update_time");
        if(root.getAsJsonObject().has("photo"))
        {
            JsonObject thmb = root.getAsJsonObject("photo");

            if(thmb.has("photo_600"))
                album.thumb_image = thmb.get("photo_600").getAsString();
            else if(thmb.has("photo_300"))
                album.thumb_image = thmb.get("photo_300").getAsString();
        }
        else if(root.getAsJsonObject().has("thumbs"))
        {
            JsonArray thmb = root.getAsJsonArray("thumbs");
            if(thmb.size() > 0)
            {
                JsonObject thmbc = thmb.get(0).getAsJsonObject();
                if(thmbc.has("photo_600"))
                    album.thumb_image = thmbc.get("photo_600").getAsString();
                else if(thmbc.has("photo_300"))
                    album.thumb_image = thmbc.get("photo_300").getAsString();
            }
        }
        return album;
    }
}
