package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.Nullable;

import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.ICommunityMembersView;


public class CommunityMembersPresenter extends AccountDependencyPresenter<ICommunityMembersView> {

    private final int groupId;

    public CommunityMembersPresenter(int accountId, int groupId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.groupId = groupId;
    }
}