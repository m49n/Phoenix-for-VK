package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import biz.dealnote.messenger.model.AnswerVKOfficial;

/**
 * Created by admin on 27.12.2016.
 * phoenix
 */
public class AnswerVKOfficialDtoAdapter extends AbsAdapter implements JsonDeserializer<AnswerVKOfficial> {

    @Override
    public AnswerVKOfficial deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        AnswerVKOfficial dto = new AnswerVKOfficial();
        dto.header = optString(root, "header");
        if(dto.header != null) {
            dto.header = dto.header.replace("{date}", "").replaceAll("'''(((?!''').)*)'''", "<b>$1</b>").replaceAll("\\[vk(ontakte)?:\\/\\/[A-Za-z0-9\\/\\?=]+\\|([^\\]]+)\\]", "$2");;
        }
        dto.text = optString(root, "text");
        if(dto.text != null)
            dto.text = dto.text.replace("{date}", "").replaceAll("'''(((?!''').)*)'''", "<b>$1</b>").replaceAll("\\[vk(ontakte)?:\\/\\/[A-Za-z0-9\\/\\?=]+\\|([^\\]]+)\\]", "$2");;
        dto.time = optInt(root, "date");
        dto.iconURL = optString(root, "icon_url");

        if(root.has("main_item")) {
            root = root.get("main_item").getAsJsonObject();
            if (root.has("image_object")) {
                JsonArray jsonPhotos2 = root.get("image_object").getAsJsonArray();
                if (jsonPhotos2.size() > 0) {
                    dto.iconURL = jsonPhotos2.get(jsonPhotos2.size() - 1).getAsJsonObject().get("url").getAsString();
                }
            }
        }
        return dto;
    }
}
