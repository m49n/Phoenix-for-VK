package biz.dealnote.messenger.fragment.attachments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.mvp.presenter.CommentCreatePresenter;
import biz.dealnote.messenger.mvp.view.ICreateCommentView;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class CommentCreateFragment extends AbsAttachmentsEditFragment<CommentCreatePresenter, ICreateCommentView>
        implements ICreateCommentView {

    public static CommentCreateFragment newInstance(int accountId, int commentDbid, int sourceOwnerId, String body) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.COMMENT_ID, commentDbid);
        args.putInt(Extra.OWNER_ID, sourceOwnerId);
        args.putString(Extra.BODY, body);
        CommentCreateFragment fragment = new CommentCreateFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_attchments, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ready:
                getPresenter().fireReadyClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NotNull
    @Override
    public IPresenterFactory<CommentCreatePresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            int accountId = requireArguments().getInt(Extra.ACCOUNT_ID);
            int commentDbid = requireArguments().getInt(Extra.COMMENT_ID);
            int sourceOwnerId = requireArguments().getInt(Extra.COMMENT_ID);
            String body = requireArguments().getString(Extra.BODY);
            return new CommentCreatePresenter(accountId, commentDbid, sourceOwnerId, body, saveInstanceState);
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        ActivityUtils.setToolbarTitle(this, R.string.new_comment);
        ActivityUtils.setToolbarSubtitle(this, null);

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(true)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onClearSelection();
        }
    }

    @Override
    public boolean onBackPressed() {
        return getPresenter().onBackPressed();
    }

    @Override
    public void returnDataToParent(String textBody) {
        Intent data = new Intent();
        data.putExtra(Extra.BODY, textBody);

        if (nonNull(getTargetFragment())) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
        }
    }

    @Override
    public void goBack() {
        requireActivity().onBackPressed();
    }
}