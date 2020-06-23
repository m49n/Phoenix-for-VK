package biz.dealnote.messenger.fragment.attachments;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.dialog.ImageSizeAlertDialog;
import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.ModelsBundle;
import biz.dealnote.messenger.model.WallEditorAttrs;
import biz.dealnote.messenger.mvp.presenter.PostCreatePresenter;
import biz.dealnote.messenger.mvp.view.IPostCreateView;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

public class PostCreateFragment extends AbsPostEditFragment<PostCreatePresenter, IPostCreateView>
        implements IPostCreateView {

    private static final String EXTRA_EDITING_TYPE = "editing_type";
    private static final String EXTRA_STREAMS = "streams";

    public static PostCreateFragment newInstance(Bundle args) {
        PostCreateFragment fragment = new PostCreateFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle buildArgs(int accountId, int ownerId, @EditingPostType int editingType,
                                   ModelsBundle bundle, @NonNull WallEditorAttrs attrs,
                                   @Nullable ArrayList<Uri> streams, @Nullable String body) {
        Bundle args = new Bundle();
        args.putInt(EXTRA_EDITING_TYPE, editingType);
        args.putParcelableArrayList(EXTRA_STREAMS, streams);

        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putString(Extra.BODY, body);
        args.putParcelable(Extra.BUNDLE, bundle);
        args.putParcelable(Extra.ATTRS, attrs);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @NotNull
    @Override
    public IPresenterFactory<PostCreatePresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> {
            int accountId = requireArguments().getInt(Extra.ACCOUNT_ID);
            int ownerId = requireArguments().getInt(Extra.OWNER_ID);

            @EditingPostType
            int type = requireArguments().getInt(EXTRA_EDITING_TYPE);

            ModelsBundle bundle = requireArguments().getParcelable(Extra.BUNDLE);

            WallEditorAttrs attrs = requireArguments().getParcelable(Extra.ATTRS);
            AssertUtils.requireNonNull(attrs);

            String links = requireArguments().getString(Extra.BODY);

            ArrayList<Uri> streams = requireArguments().getParcelableArrayList(EXTRA_STREAMS);
            requireArguments().remove(EXTRA_STREAMS); // only first start
            requireArguments().remove(Extra.BODY);
            return new PostCreatePresenter(accountId, ownerId, type, bundle, attrs, streams, links, saveInstanceState);
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(true)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void displayUploadUriSizeDialog(@NonNull List<Uri> uris) {
        new ImageSizeAlertDialog.Builder(requireActivity())
                .setOnSelectedCallback(size -> getPresenter().fireUriUploadSizeSelected(uris, size))
                .setOnCancelCallback(() -> getPresenter().fireUriUploadCancelClick())
                .show();
    }

    @Override
    public void goBack() {
        requireActivity().onBackPressed();
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

    @Override
    public boolean onBackPressed() {
        return getPresenter().onBackPresed();
    }
}