package biz.dealnote.messenger.db.model.entity.feedback;

import biz.dealnote.messenger.db.model.entity.PostEntity;

/**
 * Created by ruslan.kolbasa on 09.12.2016.
 * phoenix
 */
public class PostFeedbackEntity extends FeedbackEntity {

    private PostEntity post;

    public PostFeedbackEntity(int type) {
        super(type);
    }

    public PostEntity getPost() {
        return post;
    }

    public PostFeedbackEntity setPost(PostEntity post) {
        this.post = post;
        return this;
    }
}