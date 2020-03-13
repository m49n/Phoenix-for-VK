package biz.dealnote.messenger.model;

import java.util.List;

/**
 * Created by admin on 09.07.2017.
 * phoenix
 */
public class BannedPart {
    private final List<User> users;

    public BannedPart(List<User> users) {
        this.users = users;
    }

    public int getTotalCount() {
        return users.size();
    }

    public List<User> getUsers() {
        return users;
    }
}