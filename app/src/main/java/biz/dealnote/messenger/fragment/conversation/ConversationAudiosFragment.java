package biz.dealnote.messenger.fragment.conversation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.adapter.AudioRecyclerAdapter;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.conversations.ChatAttachmentAudioPresenter;
import biz.dealnote.messenger.mvp.view.conversations.IChatAttachmentAudiosView;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;

public class ConversationAudiosFragment extends AbsChatAttachmentsFragment<Audio, ChatAttachmentAudioPresenter, IChatAttachmentAudiosView>
        implements AudioRecyclerAdapter.ClickListener, IChatAttachmentAudiosView {

    private PlaybackStatus mPlaybackStatus;

    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        return new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false);
    }

    @Override
    public RecyclerView.Adapter createAdapter() {
        AudioRecyclerAdapter audioRecyclerAdapter = new AudioRecyclerAdapter(requireActivity(), Collections.emptyList(), false, false, 0);
        audioRecyclerAdapter.setClickListener(this);
        return audioRecyclerAdapter;
    }

    @Override
    public void onClick(int position, int catalog, Audio audio) {
        getPresenter().fireAudioPlayClick(position, audio);
    }

    @Override
    public void displayAttachments(List<Audio> data) {
        ((AudioRecyclerAdapter) getAdapter()).setData(data);
    }

    @NotNull
    @Override
    public IPresenterFactory<ChatAttachmentAudioPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new ChatAttachmentAudioPresenter(
                getArguments().getInt(Extra.PEER_ID),
                getArguments().getInt(Extra.ACCOUNT_ID),
                saveInstanceState
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mPlaybackStatus = new PlaybackStatus();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        filter.addAction(MusicPlaybackService.META_CHANGED);
        filter.addAction(MusicPlaybackService.PREPARED);
        filter.addAction(MusicPlaybackService.REFRESH);
        requireActivity().registerReceiver(mPlaybackStatus, filter);
    }

    @Override
    public void onPause() {
        try {
            requireActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable ignored) {
        }
        super.onPause();
    }

    private final class PlaybackStatus extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (isNull(action)) return;

            if (MusicPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                getAdapter().notifyDataSetChanged();
            }
        }
    }
}
