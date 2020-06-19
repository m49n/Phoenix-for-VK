package biz.dealnote.messenger.api.interfaces;

import java.util.Map;

import biz.dealnote.messenger.util.Optional;
import io.reactivex.Single;

public interface IOtherApi {
    Single<Optional<String>> rawRequest(String method, Map<String, String> postParams);
}