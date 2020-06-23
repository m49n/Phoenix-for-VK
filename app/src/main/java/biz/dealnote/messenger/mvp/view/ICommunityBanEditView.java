package biz.dealnote.messenger.mvp.view;

import java.util.List;

import biz.dealnote.messenger.model.IdOption;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;


public interface ICommunityBanEditView extends IMvpView, IAccountDependencyView, IErrorView, IProgressView, IToastView {
    void displayUserInfo(Owner user);

    void displayBanStatus(int adminId, String adminName, long endDate);

    void displayBlockFor(String blockFor);

    void displayReason(String reason);

    void diplayComment(String comment);

    void setShowCommentChecked(boolean checked);

    void goBack();

    void displaySelectOptionDialog(int requestCode, List<IdOption> options);

    void openProfile(int accountId, Owner owner);
}