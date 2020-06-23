package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import biz.dealnote.messenger.api.model.FaveLinkDto;

public class FaveLinkDtoAdapter extends AbsAdapter implements JsonDeserializer<FaveLinkDto> {

    @Override
    public FaveLinkDto deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        FaveLinkDto link = new FaveLinkDto();
        if (!root.has("link"))
            return link;
        root = root.get("link").getAsJsonObject();
        link.id = optString(root, "id");
        link.description = optString(root, "description");
        link.photo_50 = optString(root, "photo_50");
        link.photo_100 = optString(root, "photo_100");
        link.title = optString(root, "title");
        link.url = optString(root, "url");

        return link;
    }
}
