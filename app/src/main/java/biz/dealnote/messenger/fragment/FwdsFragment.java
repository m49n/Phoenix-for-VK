package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.adapter.AttachmentsViewBinder;
import biz.dealnote.messenger.adapter.MessagesAdapter;
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.model.LastReadId;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.mvp.presenter.FwdsPresenter;
import biz.dealnote.messenger.mvp.view.IFwdsView;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class FwdsFragment extends PlaceSupportMvpFragment<FwdsPresenter, IFwdsView>
        implements MessagesAdapter.OnMessageActionListener, IFwdsView, AttachmentsViewBinder.VoiceActionListener {

    private MessagesAdapter mAdapter;

    public static Bundle buildArgs(int accountId, ArrayList<Message> messages) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putParcelableArrayList(Extra.MESSAGES, messages);
        return args;
    }

    public static FwdsFragment newInstance(Bundle args) {
        FwdsFragment fwdsFragment = new FwdsFragment();
        fwdsFragment.setArguments(args);
        return fwdsFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_fwds, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        mAdapter = new MessagesAdapter(requireActivity(), Collections.emptyList(), this);
        mAdapter.setOnMessageActionListener(this);
        mAdapter.setVoiceActionListener(this);
        recyclerView.setAdapter(mAdapter);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onClearSelection();
        }

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setSubtitle(null);
            actionBar.setTitle(R.string.title_mssages);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void onAvatarClick(@NonNull Message message, int userId) {
        super.onOpenOwner(userId);
    }

    @Override
    public void onLongAvatarClick(@NonNull Message message, int userId) {
        super.onOpenOwner(userId);
    }

    @Override
    public void onRestoreClick(@NonNull Message message, int position) {
        // not supported
    }

    @Override
    public boolean onMessageLongClick(@NonNull Message message) {
        // not supported
        return false;
    }

    @Override
    public void onMessageClicked(@NonNull Message message) {
        // not supported
    }

    @Override
    public void onMessageDelete(@NonNull Message message) {

    }

    @Override
    public void displayMessages(@NonNull List<Message> messages, @NonNull LastReadId lastReadId) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.setItems(messages, lastReadId);
        }
    }

    @Override
    public void notifyMessagesUpAdded(int position, int count) {
        // not supported
    }

    @Override
    public void notifyDataChanged() {
        if (Objects.nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyMessagesDownAdded(int count) {
        // not supported
    }

    @Override
    public void configNowVoiceMessagePlaying(int voiceId, float progress, boolean paused, boolean amin) {
        if (nonNull(mAdapter)) {
            mAdapter.configNowVoiceMessagePlaying(voiceId, progress, paused, amin);
        }
    }

    @Override
    public void bindVoiceHolderById(int holderId, boolean play, boolean paused, float progress, boolean amin) {
        if (nonNull(mAdapter)) {
            mAdapter.bindVoiceHolderById(holderId, play, paused, progress, amin);
        }
    }

    @Override
    public void disableVoicePlaying() {
        if (nonNull(mAdapter)) {
            mAdapter.disableVoiceMessagePlaying();
        }
    }

    @Override
    public void showActionMode(String title, Boolean canEdit, Boolean canPin) {
        // not supported
    }

    @Override
    public void finishActionMode() {
        // not supported
    }

    @Override
    public IPresenterFactory<FwdsPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            ArrayList<Message> messages = requireArguments().getParcelableArrayList(Extra.MESSAGES);
            int accountId = requireArguments().getInt(Extra.ACCOUNT_ID);
            return new FwdsPresenter(accountId, messages, saveInstanceState);
        };
    }

    @Override
    public void onVoiceHolderBinded(int voiceMessageId, int voiceHolderId) {
        getPresenter().fireVoiceHolderCreated(voiceMessageId, voiceHolderId);
    }

    @Override
    public void onVoicePlayButtonClick(int voiceHolderId, int voiceMessageId, @NonNull VoiceMessage voiceMessage) {
        getPresenter().fireVoicePlayButtonClick(voiceHolderId, voiceMessageId, voiceMessage);
    }
}