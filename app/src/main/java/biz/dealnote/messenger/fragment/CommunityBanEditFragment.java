package biz.dealnote.messenger.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.TextWatcherAdapter;
import biz.dealnote.messenger.model.Banned;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.IdOption;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.mvp.presenter.CommunityBanEditPresenter;
import biz.dealnote.messenger.mvp.view.ICommunityBanEditView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.util.FormatUtil;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.MySpinnerView;
import biz.dealnote.messenger.view.OnlineView;
import biz.dealnote.mvp.core.IPresenterFactory;

public class CommunityBanEditFragment extends BaseMvpFragment<CommunityBanEditPresenter, ICommunityBanEditView>
        implements ICommunityBanEditView {

    private ImageView mAvatar;
    private OnlineView mOnlineView;
    private TextView mName;
    private TextView mDomain;
    private TextView mBanStatus;
    private MySpinnerView mBlockFor;
    private MySpinnerView mReason;
    private EditText mComment;
    private CheckBox mShowComment;

    public static CommunityBanEditFragment newInstance(int accountId, int groupId, Banned banned) {
        return newInstance(accountId, groupId, banned, null);
    }

    public static CommunityBanEditFragment newInstance(int accountId, int groupId, ArrayList<User> users) {
        return newInstance(accountId, groupId, null, users);
    }

    private static CommunityBanEditFragment newInstance(int accountId, int groupId, Banned banned, ArrayList<User> users) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.GROUP_ID, groupId);
        args.putParcelableArrayList(Extra.USERS, users);
        args.putParcelable(Extra.BANNED, banned);
        CommunityBanEditFragment fragment = new CommunityBanEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_community_ban_edit, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mAvatar = root.findViewById(R.id.avatar);
        mAvatar.setOnClickListener(v -> getPresenter().fireAvatarClick());

        mOnlineView = root.findViewById(R.id.online);
        mName = root.findViewById(R.id.name);
        mDomain = root.findViewById(R.id.domain);

        mBanStatus = root.findViewById(R.id.status);

        mBlockFor = root.findViewById(R.id.spinner_block_for);
        mBlockFor.setIconOnClickListener(v -> getPresenter().fireBlockForClick());

        mReason = root.findViewById(R.id.spinner_reason);
        mReason.setIconOnClickListener(v -> getPresenter().fireResonClick());

        mComment = root.findViewById(R.id.community_ban_comment);
        mComment.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getPresenter().fireCommentEdit(s);
            }
        });

        mShowComment = root.findViewById(R.id.community_ban_show_comment_to_user);
        mShowComment.setOnCheckedChangeListener((buttonView, checked) -> getPresenter().fireShowCommentCheck(checked));
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.community_ban_edit, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            getPresenter().fireButtonSaveClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityUtils.setToolbarTitle(this, R.string.block_user);
        ActivityUtils.setToolbarSubtitle(this, null);

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(true)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @NonNull
    @Override
    public IPresenterFactory<CommunityBanEditPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            int accountId = requireArguments().getInt(Extra.ACCOUNT_ID);
            int groupId = requireArguments().getInt(Extra.GROUP_ID);
            Banned banned = requireArguments().getParcelable(Extra.BANNED);

            if (banned != null) {
                return new CommunityBanEditPresenter(accountId, groupId, banned, saveInstanceState);
            }

            ArrayList<User> users = requireArguments().getParcelableArrayList(Extra.USERS);
            ArrayList<Owner> owners = new ArrayList<>();
            if (Utils.nonEmpty(users)) {
                owners.addAll(users);
            }

            return new CommunityBanEditPresenter(accountId, groupId, owners, saveInstanceState);
        };
    }

    @Override
    public void displayUserInfo(Owner owner) {
        if (Objects.nonNull(mAvatar)) {
            ViewUtils.displayAvatar(mAvatar, new RoundTransformation(), owner.getMaxSquareAvatar(), null);
        }

        safelySetText(mName, owner.getFullName());

        Integer iconRes = null;
        if (owner instanceof User) {
            User user = (User) owner;
            iconRes = ViewUtils.getOnlineIcon(user.isOnline(), user.isOnlineMobile(), user.getPlatform(), user.getOnlineApp());
        }

        if (Objects.nonNull(mOnlineView)) {
            mOnlineView.setVisibility(Objects.nonNull(iconRes) ? View.VISIBLE : View.INVISIBLE);
            if (Objects.nonNull(iconRes)) {
                mOnlineView.setIcon(iconRes);
            }
        }

        if (Utils.nonEmpty(owner.getDomain())) {
            safelySetText(mDomain, "@" + owner.getDomain());
        } else if (owner instanceof User) {
            safelySetText(mDomain, "@id" + ((User) owner).getId());
        } else if (owner instanceof Community) {
            safelySetText(mDomain, "@club" + ((Community) owner).getId());
        }
    }

    @Override
    public void displayBanStatus(int adminId, String adminName, long endDate) {
        if (Objects.nonNull(mBanStatus)) {
            try {
                Context context = mBanStatus.getContext();
                Spannable spannable = FormatUtil.formatCommunityBanInfo(context, adminId, adminName, endDate, null);
                mBanStatus.setText(spannable, TextView.BufferType.SPANNABLE);
                mBanStatus.setMovementMethod(LinkMovementMethod.getInstance());
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    @Override
    public void displayBlockFor(String blockFor) {
        if (Objects.nonNull(mBlockFor)) {
            mBlockFor.setValue(blockFor);
        }
    }

    @Override
    public void displayReason(String reason) {
        if (Objects.nonNull(mReason)) {
            mReason.setValue(reason);
        }
    }

    @Override
    public void diplayComment(String comment) {
        safelySetText(mComment, comment);
    }

    @Override
    public void setShowCommentChecked(boolean checked) {
        safelySetCheched(mShowComment, checked);
    }

    @Override
    public void goBack() {
        requireActivity().onBackPressed();
    }

    @Override
    public void displaySelectOptionDialog(int requestCode, List<IdOption> options) {
        String[] strings = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            strings[i] = options.get(i).getTitle();
        }

        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.select_from_list_title)
                .setItems(strings, (dialog, which) -> getPresenter().fireOptionSelected(requestCode, options.get(which)))
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    @Override
    public void openProfile(int accountId, Owner owner) {
        PlaceFactory.getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity());
    }
}