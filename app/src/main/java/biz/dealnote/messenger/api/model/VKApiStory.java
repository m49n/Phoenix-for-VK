package biz.dealnote.messenger.api.model;

public class VKApiStory {

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
    
    public VKApiPhoto photo;
    
    public VKApiVideo video;
}
