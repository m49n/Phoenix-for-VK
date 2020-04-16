package biz.dealnote.messenger.domain;

import java.util.List;

import biz.dealnote.messenger.model.NewsfeedComment;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.Single;

/**
 * Created by admin on 08.05.2017.
 * phoenix
 */
public interface INewsfeedInteractor {

    Single<Pair<List<NewsfeedComment>, String>> getNewsfeedComments(int accountId, int count, String startFrom, String filter);

    Single<Pair<List<NewsfeedComment>, String>> getMentions(int accountId, Integer owner_id, Integer count, Integer offset, Long startTime, Long endTime);

}
