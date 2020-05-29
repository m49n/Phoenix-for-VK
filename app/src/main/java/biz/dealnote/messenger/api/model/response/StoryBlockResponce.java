package biz.dealnote.messenger.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import biz.dealnote.messenger.api.model.VKApiStory;

public class StoryBlockResponce {

    @SerializedName("promo_stories")
    public List<VKApiStory> promo_stories;

    @SerializedName("stories")
    public List<VKApiStory> stories;

    @SerializedName("live_active")
    public List<VKApiStory> live_active;

    @SerializedName("live_finished")
    public List<VKApiStory> live_finished;

    @SerializedName("community_grouped_stories")
    public List<VKApiStory> community_grouped_stories;

    @SerializedName("app_grouped_stories")
    public List<VKApiStory> app_grouped_stories;
}
