package biz.dealnote.messenger.domain;

import java.util.List;

import biz.dealnote.messenger.model.Owner;
import io.reactivex.Single;

public interface ILikesInteractor {
    String FILTER_LIKES = "likes";
    String FILTER_COPIES = "copies";

    Single<List<Owner>> getLikes(int accountId, String type, int ownerId, int itemId, String filter, int count, int offset);
}