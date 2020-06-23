package biz.dealnote.messenger.mvp.view;

import androidx.annotation.StringRes;

import biz.dealnote.messenger.media.gif.IGifPlayer;

public interface IGifPagerView extends IBasicDocumentView, IErrorView {

    void displayData(int pageCount, int selectedIndex);

    void setAspectRatioAt(int position, int w, int h);

    void setPreparingProgressVisible(int position, boolean preparing);

    void setupAddRemoveButton(boolean addEnable);

    void attachDisplayToPlayer(int adapterPosition, IGifPlayer gifPlayer);

    void setToolbarTitle(@StringRes int titleRes, Object... params);

    void setToolbarSubtitle(@StringRes int titleRes, Object... params);

    void configHolder(int adapterPosition, boolean progress, int aspectRatioW, int aspectRatioH);
}