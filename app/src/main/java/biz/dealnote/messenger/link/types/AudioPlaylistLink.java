package biz.dealnote.messenger.link.types;

public class AudioPlaylistLink extends AbsLink {

    public int ownerId;
    public int playlistId;
    public String access_key;

    public AudioPlaylistLink(int ownerId, int playlistId, String access_key) {
        super(PLAYLIST);
        this.playlistId = playlistId;
        this.ownerId = ownerId;
        this.access_key = access_key;
    }

    @Override
    public String toString() {
        return "AudioPlaylistLink{" +
                "ownerId=" + ownerId +
                ", playlistId=" + playlistId +
                ", access_key=" + access_key +
                '}';
    }
}
