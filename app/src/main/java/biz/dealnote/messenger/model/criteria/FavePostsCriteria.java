package biz.dealnote.messenger.model.criteria;

public class FavePostsCriteria {

    private final int accountId;

    public FavePostsCriteria(int accountId) {
        this.accountId = accountId;
    }

    public int getAccountId() {
        return accountId;
    }
}