package biz.dealnote.messenger.place;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.AbsModel;
import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.WallEditorAttrs;
import biz.dealnote.messenger.spots.SpotsDialog;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.Disposable;

public class PlaceUtil {

    public static void goToPostEditor(@NonNull Activity activity, final int accountId, final Post post) {
        AlertDialog dialog = createProgressDialog(activity);
        WeakReference<Dialog> dialogWeakReference = new WeakReference<>(dialog);
        WeakReference<Activity> reference = new WeakReference<>(activity);

        final int ownerId = post.getOwnerId();

        Set<Integer> ids = new HashSet<>();
        ids.add(accountId);
        ids.add(ownerId);

        Disposable disposable = Repository.INSTANCE.getOwners()
                .findBaseOwnersDataAsBundle(accountId, ids, IOwnersRepository.MODE_NET)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(owners -> {
                    WallEditorAttrs attrs = new WallEditorAttrs(owners.getById(ownerId), owners.getById(accountId));

                    Dialog d = dialogWeakReference.get();
                    if (d != null) {
                        d.dismiss();
                    }

                    Activity a = reference.get();

                    if (a != null) {
                        PlaceFactory.getEditPostPlace(accountId, post, attrs).tryOpenWith(a);
                    }
                }, throwable -> safelyShowError(reference, throwable));

        dialog.setOnCancelListener(d -> disposable.dispose());
    }

    private static void safelyShowError(WeakReference<Activity> reference, Throwable throwable) {
        Activity a = reference.get();
        if (a != null) {
            new MaterialAlertDialogBuilder(a)
                    .setTitle(R.string.error)
                    .setMessage(Utils.getCauseIfRuntime(throwable).getMessage())
                    .setPositiveButton(R.string.button_ok, null)
                    .show();
        }
    }

    public static void goToPostCreation(@NonNull Activity activity, int accountId, int ownerId,
                                        @EditingPostType int editingType, @Nullable List<AbsModel> input) {
        goToPostCreation(activity, accountId, ownerId, editingType, input, null, null);
    }

    public static void goToPostCreation(@NonNull Activity activity, int accountId, int ownerId,
                                        @EditingPostType int editingType, @Nullable List<AbsModel> input, @Nullable ArrayList<Uri> streams, @Nullable String body) {

        AlertDialog dialog = createProgressDialog(activity);
        WeakReference<Dialog> dialogWeakReference = new WeakReference<>(dialog);
        WeakReference<Activity> reference = new WeakReference<>(activity);

        Set<Integer> ids = new HashSet<>();
        ids.add(accountId);
        ids.add(ownerId);

        Disposable disposable = Repository.INSTANCE.getOwners()
                .findBaseOwnersDataAsBundle(accountId, ids, IOwnersRepository.MODE_NET)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(owners -> {
                    WallEditorAttrs attrs = new WallEditorAttrs(owners.getById(ownerId), owners.getById(accountId));

                    Dialog d = dialogWeakReference.get();
                    if (d != null) {
                        d.dismiss();
                    }

                    Activity a = reference.get();
                    if (a != null) {
                        PlaceFactory.getCreatePostPlace(accountId, ownerId, editingType, input, attrs, streams, body).tryOpenWith(a);
                    }
                }, throwable -> safelyShowError(reference, throwable));

        dialog.setOnCancelListener(d -> disposable.dispose());
    }

    private static AlertDialog createProgressDialog(Activity activity) {
        AlertDialog dialog = new SpotsDialog.Builder().setContext(activity).setMessage(activity.getString(R.string.message_obtaining_owner_information)).setCancelable(true).build();
        dialog.show();
        return dialog;
    }
}