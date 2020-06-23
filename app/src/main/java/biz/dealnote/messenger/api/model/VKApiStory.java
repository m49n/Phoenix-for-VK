package biz.dealnote.messenger.api.model;

public class VKApiStory implements VKApiAttachment {

    /**
     * Note ID, positive number
     */
    public int id;

    /**
     * Note owner ID.
     */
    public int owner_id;

    /**
     * Date (in Unix time) when the note was created.
     */
    public long date;

    public long expires_at;

    public boolean is_expired;

    public String access_key;

    public VKApiPhoto photo;

    public VKApiVideo video;

    @Override
    public String getType() {
        return TYPE_STORY;
    }
}
