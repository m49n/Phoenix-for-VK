package biz.dealnote.messenger.model.feedback;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import biz.dealnote.messenger.model.AbsModel;
import biz.dealnote.messenger.model.Comment;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.ParcelableModelWrapper;
import biz.dealnote.messenger.model.ParcelableOwnerWrapper;

public final class LikeCommentFeedback extends Feedback implements Parcelable {

    public static final Creator<LikeCommentFeedback> CREATOR = new Creator<LikeCommentFeedback>() {
        @Override
        public LikeCommentFeedback createFromParcel(Parcel in) {
            return new LikeCommentFeedback(in);
        }

        @Override
        public LikeCommentFeedback[] newArray(int size) {
            return new LikeCommentFeedback[size];
        }
    };
    private Comment liked;
    private AbsModel commented;
    private List<Owner> owners;

    // one of FeedbackType.LIKE_COMMENT, FeedbackType.LIKE_COMMENT_PHOTO, FeedbackType.LIKE_COMMENT_TOPIC, FeedbackType.LIKE_COMMENT_VIDEO
    public LikeCommentFeedback(@FeedbackType int type) {
        super(type);
    }

    private LikeCommentFeedback(Parcel in) {
        super(in);
        liked = in.readParcelable(Comment.class.getClassLoader());
        commented = ParcelableModelWrapper.readModel(in);
        owners = ParcelableOwnerWrapper.readOwners(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(liked, flags);
        ParcelableModelWrapper.writeModel(dest, flags, commented);
        ParcelableOwnerWrapper.writeOwners(dest, flags, owners);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public AbsModel getCommented() {
        return commented;
    }

    public LikeCommentFeedback setCommented(AbsModel commented) {
        this.commented = commented;
        return this;
    }

    public Comment getLiked() {
        return liked;
    }

    public LikeCommentFeedback setLiked(Comment liked) {
        this.liked = liked;
        return this;
    }

    public List<Owner> getOwners() {
        return owners;
    }

    public LikeCommentFeedback setOwners(List<Owner> owners) {
        this.owners = owners;
        return this;
    }
}