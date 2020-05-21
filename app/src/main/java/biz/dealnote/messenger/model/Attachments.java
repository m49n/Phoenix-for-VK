package biz.dealnote.messenger.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import biz.dealnote.messenger.adapter.DocLink;
import biz.dealnote.messenger.adapter.PostImage;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.cloneListAsArrayList;
import static biz.dealnote.messenger.util.Utils.nonEmpty;
import static biz.dealnote.messenger.util.Utils.safeCountOf;
import static biz.dealnote.messenger.util.Utils.safeCountOfMultiple;
import static biz.dealnote.messenger.util.Utils.safeIsEmpty;

public class Attachments implements Parcelable, Cloneable {

    public static final Creator<Attachments> CREATOR = new Creator<Attachments>() {
        @Override
        public Attachments createFromParcel(Parcel in) {
            return new Attachments(in);
        }

        @Override
        public Attachments[] newArray(int size) {
            return new Attachments[size];
        }
    };

    private ArrayList<Audio> audios;
    private ArrayList<Sticker> stickers;
    private ArrayList<Photo> photos;
    private ArrayList<Document> docs;
    private ArrayList<Video> videos;
    private ArrayList<Post> posts;
    private ArrayList<Link> links;
    private ArrayList<Article> articles;
    private ArrayList<Poll> polls;
    private ArrayList<WikiPage> pages;
    private ArrayList<VoiceMessage> voiceMessages;
    private ArrayList<GiftItem> gifts;

    public Attachments() {
    }

    protected Attachments(Parcel in) {
        audios = in.createTypedArrayList(Audio.CREATOR);
        stickers = in.createTypedArrayList(Sticker.CREATOR);
        photos = in.createTypedArrayList(Photo.CREATOR);
        docs = in.createTypedArrayList(Document.CREATOR);
        videos = in.createTypedArrayList(Video.CREATOR);
        posts = in.createTypedArrayList(Post.CREATOR);
        links = in.createTypedArrayList(Link.CREATOR);
        articles = in.createTypedArrayList(Article.CREATOR);
        polls = in.createTypedArrayList(Poll.CREATOR);
        pages = in.createTypedArrayList(WikiPage.CREATOR);
        voiceMessages = in.createTypedArrayList(VoiceMessage.CREATOR);
        gifts = in.createTypedArrayList(GiftItem.CREATOR);
    }

    public ArrayList<VoiceMessage> getVoiceMessages() {
        return voiceMessages;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(audios);
        dest.writeTypedList(stickers);
        dest.writeTypedList(photos);
        dest.writeTypedList(docs);
        dest.writeTypedList(videos);
        dest.writeTypedList(posts);
        dest.writeTypedList(links);
        dest.writeTypedList(articles);
        dest.writeTypedList(polls);
        dest.writeTypedList(pages);
        dest.writeTypedList(voiceMessages);
        dest.writeTypedList(gifts);
    }

    public void add(AbsModel model) {
        if (model instanceof Audio) {
            prepareAudios().add((Audio) model);
            return;
        }

        if (model instanceof Sticker) {
            prepareStickers().add((Sticker) model);
            return;
        }

        if (model instanceof Photo) {
            preparePhotos().add((Photo) model);
            return;
        }

        if (model instanceof VoiceMessage) {
            prepareVoiceMessages().add((VoiceMessage) model);
            return;
        }

        if (model instanceof Document) {
            prepareDocs().add((Document) model);
            return;
        }

        if (model instanceof Video) {
            prepareVideos().add((Video) model);
            return;
        }

        if (model instanceof Post) {
            preparePosts().add((Post) model);
            return;
        }

        if (model instanceof Link) {
            prepareLinks().add((Link) model);
            return;
        }

        if (model instanceof Article) {
            prepareArticles().add((Article) model);
            return;
        }

        if (model instanceof Poll) {
            preparePolls().add((Poll) model);
            return;
        }

        if (model instanceof WikiPage) {
            prepareWikiPages().add((WikiPage) model);
        }

        if (model instanceof GiftItem) {
            prepareGifts().add((GiftItem) model);
        }
    }

    public ArrayList<AbsModel> toList() {
        ArrayList<AbsModel> result = new ArrayList<>();
        if (nonEmpty(audios)) {
            result.addAll(audios);
        }

        if (nonEmpty(stickers)) {
            result.addAll(stickers);
        }

        if (nonEmpty(photos)) {
            result.addAll(photos);
        }

        if (nonEmpty(docs)) {
            result.addAll(docs);
        }

        if (nonEmpty(voiceMessages)) {
            result.addAll(voiceMessages);
        }

        if (nonEmpty(videos)) {
            result.addAll(videos);
        }

        if (nonEmpty(posts)) {
            result.addAll(posts);
        }

        if (nonEmpty(links)) {
            result.addAll(links);
        }

        if (nonEmpty(articles)) {
            result.addAll(articles);
        }

        if (nonEmpty(polls)) {
            result.addAll(polls);
        }

        if (nonEmpty(pages)) {
            result.addAll(pages);
        }

        if (nonEmpty(gifts)) {
            result.addAll(gifts);
        }

        return result;
    }

    public ArrayList<Audio> prepareAudios() {
        if (audios == null) {
            audios = new ArrayList<>(1);
        }

        return audios;
    }

    public ArrayList<WikiPage> prepareWikiPages() {
        if (pages == null) {
            pages = new ArrayList<>(1);
        }

        return pages;
    }

    public ArrayList<Photo> preparePhotos() {
        if (photos == null) {
            photos = new ArrayList<>(1);
        }

        return photos;
    }

    public ArrayList<Video> prepareVideos() {
        if (videos == null) {
            videos = new ArrayList<>(1);
        }

        return videos;
    }

    public ArrayList<Link> prepareLinks() {
        if (links == null) {
            links = new ArrayList<>(1);
        }

        return links;
    }

    public ArrayList<Article> prepareArticles() {
        if (articles == null) {
            articles = new ArrayList<>(1);
        }

        return articles;
    }

    public ArrayList<Document> prepareDocs() {
        if (docs == null) {
            docs = new ArrayList<>(1);
        }

        return docs;
    }

    public ArrayList<VoiceMessage> prepareVoiceMessages() {
        if (voiceMessages == null) {
            voiceMessages = new ArrayList<>(1);
        }

        return voiceMessages;
    }

    public ArrayList<Poll> preparePolls() {
        if (polls == null) {
            polls = new ArrayList<>(1);
        }

        return polls;
    }

    public ArrayList<Sticker> prepareStickers() {
        if (stickers == null) {
            stickers = new ArrayList<>(1);
        }

        return stickers;
    }

    public ArrayList<Post> preparePosts() {
        if (posts == null) {
            posts = new ArrayList<>(1);
        }

        return posts;
    }

    public ArrayList<GiftItem> prepareGifts() {
        if (gifts == null) {
            gifts = new ArrayList<>(1);
        }

        return gifts;
    }

    public int size() {
        return Utils.safeCountOfMultiple(
                audios,
                stickers,
                photos,
                docs,
                videos,
                posts,
                links,
                articles,
                polls,
                pages,
                voiceMessages,
                gifts
        );
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isPhotosVideosGifsOnly() {
        boolean hasGifWithPreview = false;

        if (nonEmpty(docs)) {
            for (Document document : docs) {
                if (document.isGif() && nonNull(document.getPhotoPreview())) {
                    hasGifWithPreview = true;
                } else {
                    return false;
                }
            }
        }

        if (safeIsEmpty(photos) && safeIsEmpty(videos) && !hasGifWithPreview) {
            return false;
        }

        return safeIsEmpty(audios) &&
                safeIsEmpty(stickers) &&
                safeIsEmpty(posts) &&
                safeIsEmpty(links) &&
                safeIsEmpty(articles) &&
                safeIsEmpty(pages) &&
                safeIsEmpty(polls) &&
                safeIsEmpty(voiceMessages) &&
                safeIsEmpty(gifts);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ArrayList<PostImage> getPostImagesVideos() {
        ArrayList<PostImage> result = new ArrayList<>(safeCountOf(videos));
        if (nonNull(videos)) {
            for (Video video : videos) {
                result.add(new PostImage(video, PostImage.TYPE_VIDEO));
            }
        }
        return result;
    }

    public ArrayList<PostImage> getPostImages() {
        ArrayList<PostImage> result = new ArrayList<>(safeCountOfMultiple(photos, videos));
        if (nonNull(photos)) {
            for (Photo photo : photos) {
                result.add(new PostImage(photo, PostImage.TYPE_IMAGE));
            }
        }

        if (nonNull(docs)) {
            for (Document document : docs) {
                if (document.isGif() && nonNull(document.getPhotoPreview())) {
                    result.add(new PostImage(document, PostImage.TYPE_GIF));
                }
            }
        }

        return result;
    }

    public ArrayList<DocLink> getDocLinks(boolean postsAsLink, boolean excludeGifWithImages) {
        ArrayList<DocLink> result = new ArrayList<>();
        if (docs != null) {
            for (Document doc : docs) {
                if (excludeGifWithImages && doc.isGif() && nonNull(doc.getPhotoPreview())) {
                    continue;
                }

                result.add(new DocLink(doc));
            }
        }

        if (postsAsLink && posts != null) {
            for (Post post : posts) {
                if (post != null) {
                    result.add(new DocLink(post));
                }
            }
        }

        if (links != null) {
            for (Link link : links) {
                result.add(new DocLink(link));
            }
        }

        if (polls != null) {
            for (Poll poll : polls) {
                result.add(new DocLink(poll));
            }
        }

        if (pages != null) {
            for (WikiPage page : pages) {
                result.add(new DocLink(page));
            }
        }

        return result;
    }

    @Override
    public Attachments clone() throws CloneNotSupportedException {
        Attachments clone = (Attachments) super.clone();
        clone.audios = cloneListAsArrayList(this.audios);
        clone.stickers = cloneListAsArrayList(this.stickers);
        clone.photos = cloneListAsArrayList(this.photos);
        clone.docs = cloneListAsArrayList(this.docs);
        clone.videos = cloneListAsArrayList(this.videos);
        clone.posts = cloneListAsArrayList(this.posts);
        clone.links = cloneListAsArrayList(this.links);
        clone.articles = cloneListAsArrayList(this.articles);
        clone.polls = cloneListAsArrayList(this.polls);
        clone.pages = cloneListAsArrayList(this.pages);
        clone.voiceMessages = cloneListAsArrayList(this.voiceMessages);
        return clone;
    }

    @Override
    public String toString() {
        String line = "";
        if (nonNull(audios)) {
            line = line + " audios=" + safeCountOf(audios);
        }

        if (nonNull(stickers)) {
            line = line + " stickers=" + safeCountOf(stickers);
        }

        if (nonNull(photos)) {
            line = line + " photos=" + safeCountOf(photos);
        }

        if (nonNull(docs)) {
            line = line + " docs=" + safeCountOf(docs);
        }

        if (nonNull(videos)) {
            line = line + " videos=" + safeCountOf(videos);
        }

        if (nonNull(posts)) {
            line = line + " posts=" + safeCountOf(posts);
        }

        if (nonNull(links)) {
            line = line + " links=" + safeCountOf(links);
        }

        if (nonNull(articles)) {
            line = line + " articles=" + safeCountOf(articles);
        }

        if (nonNull(polls)) {
            line = line + " polls=" + safeCountOf(polls);
        }

        if (nonNull(pages)) {
            line = line + " pages=" + safeCountOf(pages);
        }

        if (nonNull(voiceMessages)) {
            line = line + " voiceMessages=" + safeCountOf(voiceMessages);
        }

        if (nonNull(gifts)) {
            line = line + " gifts=" + safeCountOf(gifts);
        }

        return line.trim();
    }

    public ArrayList<Post> getPosts() {
        return posts;
    }

    public void setPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public ArrayList<Audio> getAudios() {
        return audios;
    }

    public ArrayList<Sticker> getStickers() {
        return stickers;
    }

    public ArrayList<Photo> getPhotos() {
        return photos;
    }

    public ArrayList<Document> getDocs() {
        return docs;
    }

    public ArrayList<Video> getVideos() {
        return videos;
    }

    public ArrayList<Link> getLinks() {
        return links;
    }

    public ArrayList<Article> getArticles() {
        return articles;
    }

    public ArrayList<Poll> getPolls() {
        return polls;
    }

    public ArrayList<WikiPage> getPages() {
        return pages;
    }

    public ArrayList<GiftItem> getGifts() {
        return gifts;
    }
}
