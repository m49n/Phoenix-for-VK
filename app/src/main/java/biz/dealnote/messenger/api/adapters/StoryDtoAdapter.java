package biz.dealnote.messenger.api.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import biz.dealnote.messenger.api.model.VKApiPhoto;
import biz.dealnote.messenger.api.model.VKApiStory;
import biz.dealnote.messenger.api.model.VKApiVideo;

/**
 * Created by ruslan.kolbasa on 27.12.2016.
 * phoenix
 */
public class StoryDtoAdapter extends AbsAdapter implements JsonDeserializer<VKApiStory> {

    @Override
    public VKApiStory deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        VKApiStory story = new VKApiStory();

        story.id = optInt(root, "id");
        story.owner_id = optInt(root, "owner_id");
        story.date = optInt(root, "owner_id");
        story.expires_at = optInt(root, "expires_at");
        if(root.has("photo"))
            story.photo = context.deserialize(root.get("photo"), VKApiPhoto.class);
        if(root.has("video"))
            story.video = context.deserialize(root.get("video"), VKApiVideo.class);
        return story;
    }
}
