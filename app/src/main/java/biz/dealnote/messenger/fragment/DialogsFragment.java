package biz.dealnote.messenger.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.EnterPinActivity;
import biz.dealnote.messenger.activity.SelectProfilesActivity;
import biz.dealnote.messenger.adapter.DialogsAdapter;
import biz.dealnote.messenger.dialog.DialogNotifOptionsDialog;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.DialogsSearchCriteria;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.OptionRequest;
import biz.dealnote.messenger.model.Dialog;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.DialogsPresenter;
import biz.dealnote.messenger.mvp.view.IDialogsView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.InputTextDialog;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;

public class DialogsFragment extends BaseMvpFragment<DialogsPresenter, IDialogsView>
        implements IDialogsView, DialogsAdapter.ClickListener {

    private static final int REQUEST_CODE_SELECT_USERS_FOR_CHAT = 114;
    private static final int REQUEST_SHOW_CHAT_FOR_SECURITY = 128;
    private RecyclerView mRecyclerView;
    private DialogsAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Toolbar toolbar;

    private FloatingActionButton mFab;
    private final RecyclerView.OnScrollListener mFabScrollListener = new RecyclerView.OnScrollListener() {
        int scrollMinOffset = 0;

        @Override
        public void onScrolled(@NotNull RecyclerView view, int dx, int dy) {
            if (scrollMinOffset == 0) {
                // one-time-init
                scrollMinOffset = (int) Utils.dpToPx(2, view.getContext());
            }

            if (dy > scrollMinOffset && mFab.isShown()) {
                mFab.hide();
            }

            if (dy < -scrollMinOffset && !mFab.isShown()) {
                mFab.show();
            }
        }
    };
    private LinearLayoutManager lnr;

    public static DialogsFragment newInstance(int accountId, int dialogsOwnerId, @Nullable String subtitle, int Offset) {
        DialogsFragment fragment = new DialogsFragment();
        Bundle args = new Bundle();
        args.putString(Extra.SUBTITLE, subtitle);
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.OWNER_ID, dialogsOwnerId);
        args.putInt(Extra.OFFSET, Offset);
        fragment.setArguments(args);
        return fragment;
    }

    private void onSecurityClick() {
        if (Settings.get().security().isUsePinForSecurity()) {
            startActivityForResult(new Intent(requireActivity(), EnterPinActivity.class), REQUEST_SHOW_CHAT_FOR_SECURITY);
        } else {
            Settings.get().security().setShowHiddenDialogs(true);
            ReconfigureOptionsHide();
            notifyDataSetChanged();
        }
    }

    private void ReconfigureOptionsHide() {
        boolean isShowHidden = Settings.get().security().getShowHiddenDialogs();
        if (Settings.get().security().getSetSize("hidden_dialogs") <= 0) {
            mFab.setImageResource(R.drawable.pencil);
            Settings.get().security().setShowHiddenDialogs(false);
            return;
        }

        mFab.setImageResource(isShowHidden ? R.drawable.offline : R.drawable.pencil);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dialogs, container, false);

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_dialogs);

        OptionView optionView = new OptionView();
        getPresenter().fireOptionViewCreated(optionView);
        toolbar.getMenu().findItem(R.id.action_search).setVisible(optionView.canSearch);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                getPresenter().fireSearchClick();
            }
            return true;
        });

        mFab = root.findViewById(R.id.fab);
        ReconfigureOptionsHide();
        mFab.setOnClickListener(v -> {
            if (Settings.get().security().getShowHiddenDialogs()) {
                Settings.get().security().setShowHiddenDialogs(false);
                ReconfigureOptionsHide();
                notifyDataSetChanged();
            } else
                createGroupChat();
        });

        mFab.setOnLongClickListener(v -> {
            if (!Settings.get().security().getShowHiddenDialogs() && Settings.get().security().getSetSize("hidden_dialogs") > 0) {
                onSecurityClick();
            }
            return true;
        });

        mRecyclerView = root.findViewById(R.id.recycleView);
        lnr = new LinearLayoutManager(requireActivity());
        mRecyclerView.setLayoutManager(lnr);
        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(DialogsAdapter.PICASSO_TAG));
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        mSwipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());

        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        mAdapter = new DialogsAdapter(requireActivity(), Collections.emptyList());
        mAdapter.setClickListener(this);

        mRecyclerView.setAdapter(mAdapter);
        return root;
    }

    @Override
    public void setCreateGroupChatButtonVisible(boolean visible) {
        if (nonNull(mFab) && nonNull(mRecyclerView)) {
            mFab.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                mRecyclerView.addOnScrollListener(mFabScrollListener);
            } else {
                mRecyclerView.removeOnScrollListener(mFabScrollListener);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_USERS_FOR_CHAT && resultCode == Activity.RESULT_OK) {
            ArrayList<User> users = data.getParcelableArrayListExtra(Extra.USERS);
            AssertUtils.requireNonNull(users);

            getPresenter().fireUsersForChatSelected(users);
        } else if (requestCode == REQUEST_SHOW_CHAT_FOR_SECURITY && resultCode == Activity.RESULT_OK) {
            Settings.get().security().setShowHiddenDialogs(true);
            ReconfigureOptionsHide();
            notifyDataSetChanged();
        }
    }

    @Override
    public void onDialogClick(Dialog dialog, int offset) {
        getPresenter().fireDialogClick(dialog, offset);
    }

    @Override
    public boolean onDialogLongClick(final Dialog dialog) {
        List<String> options = new ArrayList<>();

        ContextView contextView = new ContextView();
        getPresenter().fireContextViewCreated(contextView, dialog);

        final String delete = getString(R.string.delete);
        final String addToHomeScreen = getString(R.string.add_to_home_screen);
        final String notificationSettings = getString(R.string.peer_notification_settings);
        final String addToShortcuts = getString(R.string.add_to_launcer_shortcuts);

        final String setHide = getString(R.string.hide_dialog);
        final String setShow = getString(R.string.set_no_hide_dialog);

        if (contextView.canDelete) {
            options.add(delete);
        }

        if (contextView.canAddToHomescreen) {
            options.add(addToHomeScreen);
        }

        if (contextView.canConfigNotifications) {
            options.add(notificationSettings);
        }

        if (contextView.canAddToShortcuts && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            options.add(addToShortcuts);
        }

        if (!contextView.isHidden) {
            options.add(setHide);
        }

        if (contextView.isHidden && Settings.get().security().getShowHiddenDialogs()) {
            options.add(setShow);
        }

        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(contextView.isHidden && !Settings.get().security().getShowHiddenDialogs() ? getString(R.string.dialogs) : dialog.getDisplayTitle(requireActivity()))
                .setItems(options.toArray(new String[options.size()]), (dialogInterface, which) -> {
                    final String selected = options.get(which);
                    if (selected.equals(delete)) {
                        getPresenter().fireRemoveDialogClick(dialog);
                    } else if (selected.equals(addToHomeScreen)) {
                        getPresenter().fireCreateShortcutClick(dialog);
                    } else if (selected.equals(notificationSettings)) {
                        getPresenter().fireNotificationsSettingsClick(dialog);
                    } else if (selected.equals(addToShortcuts)) {
                        getPresenter().fireAddToLauncherShortcuts(dialog);
                    } else if (selected.equals(setHide)) {
                        if (!Settings.get().security().isUsePinForSecurity()) {
                            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(R.string.not_supported_hide);
                            PlaceFactory.getSecuritySettingsPlace().tryOpenWith(requireActivity());
                        } else {
                            Settings.get().security().AddValueToSet(dialog.getId(), "hidden_dialogs");
                            ReconfigureOptionsHide();
                            notifyDataSetChanged();
                        }
                    } else if (selected.equals(setShow)) {
                        Settings.get().security().RemoveValueFromSet(dialog.getId(), "hidden_dialogs");
                        ReconfigureOptionsHide();
                        notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();

        return !options.isEmpty();
    }

    @Override
    public void onAvatarClick(Dialog dialog, int offset) {
        getPresenter().fireDialogAvatarClick(dialog, offset);
    }

    private void createGroupChat() {
        SelectProfilesActivity.startFriendsSelection(this, REQUEST_CODE_SELECT_USERS_FOR_CHAT);
    }

    private void resolveToolbarNavigationIcon() {
        if (isNull(toolbar)) return;

        FragmentManager manager = requireActivity().getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 1) {
            Drawable tr = AppCompatResources.getDrawable(requireActivity(), R.drawable.arrow_left);
            Utils.setColorFilter(tr, CurrentTheme.getColorPrimary(requireActivity()));
            toolbar.setNavigationIcon(tr);
            toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        } else {
            Drawable tr = AppCompatResources.getDrawable(requireActivity(), R.drawable.phoenix_round);
            Utils.setColorFilter(tr, CurrentTheme.getColorPrimary(requireActivity()));
            toolbar.setNavigationIcon(tr);
            toolbar.setNavigationOnClickListener(v -> {
                ModalBottomSheetDialogFragment.Builder menus = new ModalBottomSheetDialogFragment.Builder();
                menus.add(new OptionRequest(R.id.button_ok, getString(R.string.set_offline), R.drawable.offline));
                menus.add(new OptionRequest(R.id.button_cancel, getString(R.string.open_clipboard_url), R.drawable.web));
                menus.show(getChildFragmentManager(), "left_options", option -> getPresenter().fireDialogOptions(requireActivity(), option));
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.DIALOGS);

        if (toolbar != null) {
            toolbar.setTitle(R.string.dialogs);
            toolbar.setSubtitle(requireArguments().getString(Extra.SUBTITLE));
            resolveToolbarNavigationIcon();
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_DIALOGS);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    /*
    @Override
    public void onDestroyView() {
        if (nonNull(mAdapter)) {
            mAdapter.cleanup();
        }

        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.destroyDrawingCache();
            mSwipeRefreshLayout.clearAnimation();
        }

        super.onDestroyView();
    }

     */

    @Override
    public void displayData(List<Dialog> data) {
        if (nonNull(mAdapter)) {
            mAdapter.setData(data);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.updateHidden(Settings.get().security().loadSet("hidden_dialogs"));
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void scroll_pos(int pos) {
        if (nonNull(mRecyclerView)) {
            lnr.scrollToPositionWithOffset(pos, 0);
        }
    }

    @Override
    public void notifyDataAdded(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.updateHidden(Settings.get().security().loadSet("hidden_dialogs"));
            mAdapter.notifyItemRangeInserted(position, count);
        }
    }

    @Override
    public void showRefreshing(boolean refreshing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    @Override
    public void goToChat(int accountId, int messagesOwnerId, int peerId, String title, String avaurl, int offset) {
        PlaceFactory.getChatDualPlace(accountId, messagesOwnerId, new Peer(peerId).setTitle(title).setAvaUrl(avaurl), offset).tryOpenWith(requireActivity());
    }

    @Override
    public void goToSearch(int accountId) {
        DialogsSearchCriteria criteria = new DialogsSearchCriteria("");

        PlaceFactory.getSingleTabSearchPlace(accountId, SearchContentType.DIALOGS, criteria)
                .tryOpenWith(requireActivity());
    }

    @Override
    public void showSnackbar(@StringRes int res, boolean isLong) {
        View view = super.getView();
        if (nonNull(view)) {
            Snackbar.make(view, res, isLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showEnterNewGroupChatTitle(List<User> users) {
        new InputTextDialog.Builder(requireActivity())
                .setTitleRes(R.string.set_groupchat_title)
                .setAllowEmpty(true)
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .setCallback(newValue -> getPresenter().fireNewGroupChatTitleEntered(users, newValue))
                .show();
    }

    @Override
    public void showNotificationSettings(int accountId, int peerId) {
        DialogNotifOptionsDialog dialog = DialogNotifOptionsDialog.newInstance(accountId, peerId);
        dialog.show(getParentFragmentManager(), "dialog-notif-options");
    }

    @Override
    public void goToOwnerWall(int accountId, int ownerId, @Nullable Owner owner) {
        PlaceFactory.getOwnerWallPlace(accountId, ownerId, owner).tryOpenWith(requireActivity());
    }

    @NotNull
    @Override
    public IPresenterFactory<DialogsPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new DialogsPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireArguments().getInt(Extra.OWNER_ID),
                requireArguments().getInt(Extra.OFFSET),
                saveInstanceState
        );
    }

    private static final class OptionView implements IOptionView {

        boolean canSearch;

        @Override
        public void setCanSearch(boolean can) {
            this.canSearch = can;
        }
    }

    private static final class ContextView implements IContextView {

        boolean canDelete;
        boolean canAddToHomescreen;
        boolean canConfigNotifications;
        boolean canAddToShortcuts;
        boolean isHidden;

        @Override
        public void setCanDelete(boolean can) {
            this.canDelete = can;
        }

        @Override
        public void setCanAddToHomescreen(boolean can) {
            this.canAddToHomescreen = can;
        }

        @Override
        public void setCanConfigNotifications(boolean can) {
            this.canConfigNotifications = can;
        }

        @Override
        public void setCanAddToShortcuts(boolean can) {
            this.canAddToShortcuts = can;
        }

        @Override
        public void setIsHidden(boolean can) {
            this.isHidden = can;
        }
    }
}