package biz.dealnote.messenger.link.internal;

public class OtherLink extends AbsInternalLink {
    public String Link;

    public OtherLink(int start, int end, String link, String name) {
        this.start = start;
        this.end = end;
        this.targetLine = name;
        this.Link = link;
    }
}
