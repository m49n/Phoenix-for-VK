package biz.dealnote.messenger.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class AudioCatalogDto {

    @SerializedName("id")
    public String id;

    @SerializedName("source")
    public String source;

    @SerializedName("next_from")
    public String next_from;

    @SerializedName("subtitle")
    public String subtitle;

    @SerializedName("title")
    public String title;

    @SerializedName("type")
    public String type;

    @SerializedName("count")
    public int count;

    @SerializedName("audios")
    public List<VKApiAudio> audios;

    @SerializedName("playlists")
    public List<VKApiAudioPlaylist> playlists;
}
