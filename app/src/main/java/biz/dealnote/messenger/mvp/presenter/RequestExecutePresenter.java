package biz.dealnote.messenger.mvp.presenter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.Apis;
import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IRequestExecuteView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Pair;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Utils.firstNonEmptyString;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.join;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

/**
 * Created by Ruslan Kolbasa on 05.07.2017.
 * phoenix
 */
public class RequestExecutePresenter extends AccountDependencyPresenter<IRequestExecuteView> {

    private String body;
    private String method;
    private final INetworker networker;

    public RequestExecutePresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.networker = Apis.get();
    }

    private void executeRequest() {
        final String trimmedMethod = nonEmpty(method) ? method.trim() : null;
        final String trimmedBody = nonEmpty(body) ? body.trim() : null;

        if (isEmpty(trimmedMethod)) {
            showError(getView(), new Exception("Method can't be empty"));
            return;
        }

        final int accountId = super.getAccountId();

        Map<String, String> params = new HashMap<>();

        if (nonEmpty(trimmedBody)) {
            try {
                String lines[] = trimmedBody.split("\\r?\\n");

                for (String line : lines) {
                    String[] parts = line.split("=");
                    String name = parts[0].toLowerCase();
                    String value = parts[1];
                    value = value.replaceAll("\"", "");

                    params.put(name, value);
                }
            } catch (Exception e) {
                showError(getView(), e);
                return;
            }
        }

        setLoadinNow(true);

        appendDisposable(executeSingle(accountId, trimmedMethod, params)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onRequestResponse, throwable -> onRequestError(Utils.getCauseIfRuntime(throwable))));
    }

    private boolean hasWritePermission() {
        return AppPerms.hasWriteStoragePermision(getApplicationContext());
    }

    private final static char[]	ILLEGAL_FILENAME_CHARS	= {'/', '\\', ':', '*', '?', '"', '<', '>', '|', ',', '=', ';', '\n', '\t', '\r' };
    static private String makeLegalFilenameNTV(String filename) {
        for(int i = 0; i < ILLEGAL_FILENAME_CHARS.length; i++) {
            filename = filename.replace(ILLEGAL_FILENAME_CHARS[i], '_');
        }
        return filename;
    }

    private void saveToFile() {
        if (!hasWritePermission()) {
            getView().requestWriteExternalStoragePermission();
            return;
        }

        FileOutputStream out = null;

        try {
            final String filename = makeLegalFilenameNTV(this.method) + ".json";

            final File file = new File(Environment.getExternalStorageDirectory(), filename);
            file.delete();

            byte[] bytes = fullResponseBody.getBytes(StandardCharsets.UTF_8);

            out = new FileOutputStream(file);
            out.write(bytes);
            out.flush();

            getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

            getView().getPhoenixToast().showToast(R.string.saved_to_param_file_name, file.getAbsolutePath());
        } catch (Exception e) {
            showError(getView(), e);
        } finally {
            Utils.safelyClose(out);
        }
    }

    @Override
    public void onGuiCreated(@NonNull IRequestExecuteView view) {
        super.onGuiCreated(view);
        view.displayBody(trimmedReposenBody);
    }

    private String fullResponseBody;
    private String trimmedReposenBody;

    private void onRequestResponse(Pair<String, String> body) {
        setLoadinNow(false);

        this.fullResponseBody = body.getFirst();
        this.trimmedReposenBody = body.getSecond();

        callView(view -> view.displayBody(trimmedReposenBody));
    }

    private void onRequestError(Throwable throwable) {
        setLoadinNow(false);
        showError(getView(), throwable);
    }

    private boolean loadinNow;

    private void setLoadinNow(boolean loadinNow) {
        this.loadinNow = loadinNow;
        resolveProgresDialog();
    }

    @OnGuiCreated
    private void resolveProgresDialog() {
        if (isGuiReady()) {
            if (loadinNow) {
                getView().displayProgressDialog(R.string.please_wait, R.string.waiting_for_response_message, false);
            } else {
                getView().dismissProgressDialog();
            }
        }
    }

    /**
     * Convert a JSON string to pretty print version
     */
    private static String toPrettyFormat(String jsonString) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonString).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }

    private Single<Pair<String, String>> executeSingle(final int accountId, final String method, final Map<String, String> params) {
        return networker.vkDefault(accountId)
                .other()
                .rawRequest(method, params)
                .map(optional -> {
                    String responseString = optional.get();

                    String fullJson = Objects.isNull(responseString) ? null : toPrettyFormat(responseString);

                    String trimmedJson = null;

                    if (nonEmpty(fullJson)) {
                        String lines[] = fullJson.split("\\r?\\n");

                        List<String> trimmed = new ArrayList<>();

                        for (String line : lines) {
                            if (trimmed.size() > 200) {
                                trimmed.add("\n");
                                trimmed.add("... and more " + (lines.length - 200) + " lines");
                                break;
                            }

                            trimmed.add(line);
                        }

                        trimmedJson = join("\n", trimmed);
                    }

                    return Pair.Companion.create(fullJson, trimmedJson);
                });
    }

    public void fireSaveClick() {
        saveToFile();
    }

    public void fireWritePermissionResolved() {
        if (hasWritePermission()) {
            saveToFile();
        }
    }

    public void fireExecuteClick() {
        getView().hideKeyboard();

        executeRequest();
    }

    public void fireMethodEdit(CharSequence s) {
        method = s.toString();
    }

    public void fireBodyEdit(CharSequence s) {
        body = s.toString();
    }

    public void fireCopyClick() {
        ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("response", fullResponseBody);
        clipboard.setPrimaryClip(clip);

        getView().getPhoenixToast().showToast(R.string.copied_to_clipboard);
    }

    private VKApiUser getByID(List<VKApiUser> Users, int user_id)
    {
        for(VKApiUser i : Users)
        {
            if(i.id == user_id)
                return i;
        }
        return null;
    }

    private void AddUserInfo(final JsonObject temp, List<VKApiUser> Users, int user_id)
    {
        if(Users == null)
        {
            temp.addProperty("user_name", "error");
            temp.addProperty("avatar", "error");
            return;
        }
        VKApiUser usr = getByID(Users, user_id);
        if(usr == null)
        {
            temp.addProperty("user_name", "error");
            temp.addProperty("avatar", "error");
            return;
        }
        temp.addProperty("user_name", firstNonEmptyString(usr.last_name, " ") + " " + firstNonEmptyString(usr.first_name, " "));
        temp.addProperty("avatar", firstNonEmptyString(usr.photo_max_orig, " "));
    }

    private void SaveAccounts(File file, List<VKApiUser> Users)
    {
        FileOutputStream out = null;
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (int i : Settings.get().accounts().getRegistered()) {
                final JsonObject temp = new JsonObject();

                AddUserInfo(temp, Users, i);
                temp.addProperty("user_id", i);
                temp.addProperty("access_token", Settings.get().accounts().getAccessToken(i));
                temp.addProperty("type", Settings.get().accounts().getType(i));
                arr.add(temp);
            }
            root.add("phoenix_accounts", arr);

            byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);

            out = new FileOutputStream(file);
            out.write(bytes);
            out.flush();

            getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

            getView().getPhoenixToast().showToast(R.string.saved_to_param_file_name, file.getAbsolutePath());
        }
        catch (Exception e) {
            showError(getView(), e);
        } finally {
            Utils.safelyClose(out);
        }
    }

    public void fireAccountClick() {
        if (!hasWritePermission()) {
            getView().requestWriteExternalStoragePermission();
            return;
        }

        try {

            StringBuilder jbld = new StringBuilder();
            final File file = new File(Environment.getExternalStorageDirectory(), "phoenix_accounts_backup.json");
            if(file.exists()) {
                FileInputStream dataFromServerStream = new FileInputStream(file);
                BufferedReader d = new BufferedReader(new InputStreamReader(dataFromServerStream));
                while(d.ready())
                    jbld.append(d.readLine());
                d.close();
                JsonArray reader = JsonParser.parseString(jbld.toString()).getAsJsonObject().getAsJsonArray("phoenix_accounts");
                for(JsonElement i : reader)
                {
                    JsonObject elem = i.getAsJsonObject();
                    int id = elem.get("user_id").getAsInt();
                    if(Settings.get().accounts().getRegistered().contains(id))
                        continue;
                    String token = elem.get("access_token").getAsString();
                    String Type = elem.get("type").getAsString();
                    Settings.get()
                            .accounts()
                            .storeAccessToken(id, token);

                    Settings.get()
                            .accounts().storeTokenType(id, Type);

                    Settings.get()
                            .accounts()
                            .registerAccountId(id, false);
                }
                getView().getPhoenixToast().showToast(R.string.accounts_restored, file.getAbsolutePath());
                return;
            }

            if(Settings.get().accounts() == null || Settings.get().accounts().getRegistered() == null || Settings.get().accounts().getRegistered().size() <= 0)
                return;

            appendDisposable(networker.vkDefault(getAccountId()).users().get(Settings.get().accounts().getRegistered(), null, "photo_max_orig,first_name,last_name", null)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(userInfo ->
                    {
                        SaveAccounts(file, userInfo);
                    }, throwable -> {
                        SaveAccounts(file, null);
                    }));
        } catch (Exception e) {
            showError(getView(), e);
        }
    }
}