package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

import biz.dealnote.messenger.model.AnswerVKOfficial;
import biz.dealnote.messenger.model.AnswerVKOfficialList;

/**
 * Created by admin on 27.12.2016.
 * phoenix
 */
public class AnswerVKOfficialDtoAdapter extends AbsAdapter implements JsonDeserializer<AnswerVKOfficialList> {

    @Override
    public AnswerVKOfficialList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        AnswerVKOfficialList dtolist = new AnswerVKOfficialList();
        dtolist.items = new ArrayList<>();
        dtolist.fields = new ArrayList<>();

        if(root.has("profiles"))
        {
            JsonArray temp = root.getAsJsonArray("profiles");
            for(JsonElement i : temp) {
                JsonObject obj = i.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                if(obj.has("photo_200")) {
                    String url = obj.get("photo_200").getAsString();
                    dtolist.fields.add(new AnswerVKOfficialList.AnswerField(id, url));
                }
                else if(obj.has("photo_200_orig")) {
                    String url = obj.get("photo_200_orig").getAsString();
                    dtolist.fields.add(new AnswerVKOfficialList.AnswerField(id, url));
                }
            }
        }
        if(root.has("groups"))
        {
            JsonArray temp = root.getAsJsonArray("groups");
            for(JsonElement i : temp) {
                JsonObject obj = i.getAsJsonObject();
                int id = (obj.get("id").getAsInt() * -1);
                if(obj.has("photo_200")) {
                    String url = obj.get("photo_200").getAsString();
                    dtolist.fields.add(new AnswerVKOfficialList.AnswerField(id, url));
                }
                else if(obj.has("photo_200_orig")) {
                    String url = obj.get("photo_200_orig").getAsString();
                    dtolist.fields.add(new AnswerVKOfficialList.AnswerField(id, url));
                }
            }
        }

        if(!root.has("items") || root.getAsJsonArray("items").size() <= 0)
            return dtolist;

        for(JsonElement i : root.getAsJsonArray("items")) {
            JsonObject root_item = i.getAsJsonObject();
            AnswerVKOfficial dto = new AnswerVKOfficial();
            dto.iconType = optString(root_item, "icon_type");
            dto.header = optString(root_item, "header");
            if (dto.header != null) {
                dto.header = dto.header.replace("{date}", "").replaceAll("'''(((?!''').)*)'''", "<b>$1</b>").replaceAll("\\[vk(ontakte)?:\\/\\/[A-Za-z0-9\\/\\?=]+\\|([^\\]]+)\\]", "$2");
            }
            dto.text = optString(root_item, "text");
            if (dto.text != null)
                dto.text = dto.text.replace("{date}", "").replaceAll("'''(((?!''').)*)'''", "<b>$1</b>").replaceAll("\\[vk(ontakte)?:\\/\\/[A-Za-z0-9\\/\\?=]+\\|([^\\]]+)\\]", "$2");
            ;
            dto.time = optInt(root_item, "date");
            dto.iconURL = optString(root_item, "icon_url");

            if (root_item.has("main_item")) {
                root_item = root_item.get("main_item").getAsJsonObject();
                if (root_item.has("image_object")) {
                    JsonArray jsonPhotos2 = root_item.get("image_object").getAsJsonArray();
                    if (jsonPhotos2.size() > 0) {
                        dto.iconURL = jsonPhotos2.get(jsonPhotos2.size() - 1).getAsJsonObject().get("url").getAsString();
                    }
                }
            }
            dtolist.items.add(dto);
        }
        return dtolist;
    }
}
