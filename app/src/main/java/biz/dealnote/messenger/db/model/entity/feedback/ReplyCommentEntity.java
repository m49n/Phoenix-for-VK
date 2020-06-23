package biz.dealnote.messenger.db.model.entity.feedback;

import biz.dealnote.messenger.db.model.entity.CommentEntity;
import biz.dealnote.messenger.db.model.entity.Entity;
import biz.dealnote.messenger.db.model.entity.EntityWrapper;

public class ReplyCommentEntity extends FeedbackEntity {

    private EntityWrapper commented = EntityWrapper.empty();

    private CommentEntity ownComment;

    private CommentEntity feedbackComment;

    public ReplyCommentEntity(int type) {
        super(type);
    }

    public Entity getCommented() {
        return commented.get();
    }

    public ReplyCommentEntity setCommented(Entity commented) {
        this.commented = new EntityWrapper(commented);
        return this;
    }

    public CommentEntity getFeedbackComment() {
        return feedbackComment;
    }

    public ReplyCommentEntity setFeedbackComment(CommentEntity feedbackComment) {
        this.feedbackComment = feedbackComment;
        return this;
    }

    public CommentEntity getOwnComment() {
        return ownComment;
    }

    public ReplyCommentEntity setOwnComment(CommentEntity ownComment) {
        this.ownComment = ownComment;
        return this;
    }
}