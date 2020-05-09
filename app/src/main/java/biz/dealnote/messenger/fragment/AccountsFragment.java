package biz.dealnote.messenger.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.activity.EnterPinActivity;
import biz.dealnote.messenger.activity.LoginActivity;
import biz.dealnote.messenger.activity.ProxyManagerActivity;
import biz.dealnote.messenger.adapter.AccountAdapter;
import biz.dealnote.messenger.api.Auth;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.db.DBHelper;
import biz.dealnote.messenger.dialog.DirectAuthDialog;
import biz.dealnote.messenger.domain.IAccountsInteractor;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.longpoll.LongpollInstance;
import biz.dealnote.messenger.model.Account;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.ShortcutUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.firstNonEmptyString;

public class AccountsFragment extends BaseFragment implements View.OnClickListener, AccountAdapter.Callback {

    private static final int REQUEST_PIN_FOR_SECURITY = 120;
    private static final String SAVE_DATA = "save_data";
    private static final int REQUEST_LOGIN = 107;
    private static final int REQEUST_DIRECT_LOGIN = 108;
    private TextView empty;
    private RecyclerView mRecyclerView;
    private AccountAdapter mAdapter;
    private ArrayList<Account> mData;
    private IOwnersRepository mOwnersInteractor;
    private IAccountsInteractor accountsInteractor;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mOwnersInteractor = Repository.INSTANCE.getOwners();
        accountsInteractor = InteractorFactory.createAccountInteractor();

        if (savedInstanceState != null) {
            mData = savedInstanceState.getParcelableArrayList(SAVE_DATA);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        empty = root.findViewById(R.id.empty);
        mRecyclerView = root.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false));

        root.findViewById(R.id.fab).setOnClickListener(this);
        root.findViewById(R.id.kate_acc).setOnClickListener(this);
        root.findViewById(R.id.dav_acc).setOnClickListener(this);
        root.findViewById(R.id.dav_acc).setVisibility(View.GONE);
        return root;
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean firstRun = false;
        if (mData == null) {
            mData = new ArrayList<>();
            firstRun = true;
        }

        mAdapter = new AccountAdapter(requireActivity(), mData, this);
        mRecyclerView.setAdapter(mAdapter);

        if (firstRun) {
            load();
        }

        resolveEmptyText();
    }

    private void resolveEmptyText() {
        if (!isAdded() || empty == null) return;
        empty.setVisibility(Utils.safeIsEmpty(mData) ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(null);
            actionBar.setSubtitle(null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_DATA, mData);
    }

    @Override
    public void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    private void load() {
        mCompositeDisposable.add(accountsInteractor
                .getAll()
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(appAccounts -> {
                    mData.clear();
                    mData.addAll(appAccounts);

                    if (Objects.nonNull(mAdapter)) {
                        mAdapter.notifyDataSetChanged();
                    }

                    resolveEmptyText();
                    if (isAdded() && Utils.safeIsEmpty(mData)) {
                        requireActivity().invalidateOptionsMenu();
                        startDirectLogin();
                    }
                }));
    }

    private void startExportAccounts() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.error_dir = Environment.getExternalStorageDirectory();
        properties.offset = Environment.getExternalStorageDirectory();
        ;
        properties.extensions = null;
        properties.show_hidden_files = true;
        FilePickerDialog dialog = new FilePickerDialog(requireActivity(), properties);
        dialog.setTitle(R.string.export_accounts);
        dialog.setDialogSelectionListener(files -> {
            File file = new File(files[0], "phoenix_accounts_backup.json");

            appendDisposable(Injection.provideNetworkInterfaces().vkDefault(Settings.get().accounts().getCurrent()).users().get(Settings.get().accounts().getRegistered(), null, "photo_max_orig,first_name,last_name", null)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(userInfo ->
                            SaveAccounts(file, userInfo), throwable -> SaveAccounts(file, null)));
        });
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PIN_FOR_SECURITY:
                if (resultCode == Activity.RESULT_OK) {
                    startExportAccounts();
                }
                break;
            case REQUEST_LOGIN:
                if (resultCode == Activity.RESULT_OK) {
                    int uid = data.getExtras().getInt(Extra.USER_ID);
                    String token = data.getStringExtra(Extra.TOKEN);
                    String Login = data.getStringExtra(Extra.LOGIN);
                    String Password = data.getStringExtra(Extra.PASSWORD);
                    String TwoFA = data.getStringExtra(Extra.TWOFA);
                    processNewAccount(uid, token, "vkofficial", Login != null ? Login : "", Password != null ? Password : "", TwoFA != null ? TwoFA : "none", true, true);
                }
                break;

            case REQEUST_DIRECT_LOGIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (DirectAuthDialog.ACTION_LOGIN_VIA_WEB.equals(data.getAction())) {
                        startLoginViaWeb();
                    } else if (DirectAuthDialog.ACTION_VALIDATE_VIA_WEB.equals(data.getAction())) {
                        String url = data.getStringExtra(Extra.URL);
                        String Login = data.getStringExtra(Extra.LOGIN);
                        String Password = data.getStringExtra(Extra.PASSWORD);
                        String TwoFA = data.getStringExtra(Extra.TWOFA);
                        startValidateViaWeb(url, Login, Password, TwoFA);
                    } else if (DirectAuthDialog.ACTION_LOGIN_COMPLETE.equals(data.getAction())) {
                        int uid = data.getExtras().getInt(Extra.USER_ID);
                        String token = data.getStringExtra(Extra.TOKEN);
                        String Login = data.getStringExtra(Extra.LOGIN);
                        String Password = data.getStringExtra(Extra.PASSWORD);
                        String TwoFA = data.getStringExtra(Extra.TWOFA);
                        processNewAccount(uid, token, "vkofficial", Login, Password, TwoFA, true, true);
                    }
                }
                break;
        }
    }

    private int indexOf(int uid) {
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).getId() == uid) {
                return i;
            }
        }

        return -1;
    }

    private void merge(Account account) {
        int index = indexOf(account.getId());

        if (index != -1) {
            mData.set(index, account);
        } else {
            mData.add(account);
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        resolveEmptyText();
    }

    private void processNewAccount(final int uid, final String token, final String type, final String Login, final String Password, final String TwoFA, boolean IsSend, boolean isCurrent) {
        //Accounts account = new Accounts(token, uid);

        // важно!! Если мы получили новый токен, то необходимо удалить запись
        // о регистрации push-уведомлений
        //PushSettings.unregisterFor(getContext(), account);

        Settings.get()
                .accounts()
                .storeAccessToken(uid, token);

        Settings.get()
                .accounts().storeTokenType(uid, type);

        Settings.get()
                .accounts()
                .registerAccountId(uid, isCurrent);

        merge(new Account(uid, null));

        mCompositeDisposable.add(mOwnersInteractor.getBaseOwnerInfo(uid, uid, IOwnersRepository.MODE_ANY)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(owner -> merge(new Account(uid, owner)), t -> {/*ignored*/}));
    }

    private void startLoginViaWeb() {
        Intent intent = LoginActivity.createIntent(requireActivity(), String.valueOf(Constants.API_ID), Auth.getScope());
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void startValidateViaWeb(String url, String Login, String Password, String TwoFa) {
        Intent intent = LoginActivity.createIntent(requireActivity(), url, Login, Password, TwoFa);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void startDirectLogin() {
        DirectAuthDialog.newInstance()
                .targetTo(this, REQEUST_DIRECT_LOGIN)
                .show(getParentFragmentManager(), "direct-login");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                startDirectLogin();
                break;
            case R.id.kate_acc:
                onKate();
                break;
        }
    }

    private boolean canRunRootCommands() {
        boolean retval = false;
        Process suProcess;
        PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.get_root);
        try {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            os.writeBytes("id\n");
            os.flush();

            BufferedReader d = new BufferedReader(new InputStreamReader(osRes));
            while (!d.ready())
                Thread.sleep(10);
            String currUid = d.readLine();
            boolean exitSu = false;
            if (null == currUid)
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError("Can't get root access or denied by user");
            else if (currUid.contains("uid=0")) {
                retval = true;
                exitSu = true;
            } else {
                exitSu = true;
                PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError("Root access rejected: \" + currUid");
            }
            if (exitSu) {
                os.writeBytes("exit\n");
                os.flush();
            }
        } catch (Exception e) {
            retval = false;
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError("Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }

    private void onKate() {
        if (!canRunRootCommands())
            return;
        StringBuilder JSDT = new StringBuilder();
        try {
            Process suProcess;
            suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());
            BufferedReader d = new BufferedReader(new InputStreamReader(osRes));
            os.writeBytes("cat /data/data/com.perm.kate_new_6/shared_prefs/com.perm.kate_new_6_preferences.xml\n");
            os.flush();
            while (!d.ready())
                Thread.sleep(10);

            String Temp;
            while (d.ready()) {
                Temp = d.readLine();
                if (Temp != null) {
                    JSDT.append(d.readLine());
                } else
                    break;
            }
            os.writeBytes("exit\n");
            os.flush();
            suProcess.waitFor();
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(JSDT.toString())));
            NodeList elements = doc.getElementsByTagName("map").item(0).getChildNodes();
            for (int i = 0; i < elements.getLength(); i++) {
                NamedNodeMap attributes = elements.item(i).getAttributes();
                if (attributes == null || attributes.getNamedItem("name") == null)
                    continue;
                String name = attributes.getNamedItem("name").getNodeValue();
                if (name.equals("accounts")) {
                    JSONArray jsonRoot = new JSONArray(elements.item(i).getTextContent());
                    List<Integer> accounts = Settings.get().accounts().getRegistered();
                    for (int s = 0; s < jsonRoot.length(); s++) {
                        JSONObject mJsonObject = jsonRoot.getJSONObject(s);
                        if (accounts != null && accounts.size() > 0) {
                            if (accounts.contains(mJsonObject.getInt("mid")))
                                continue;
                        }
                        processNewAccount(mJsonObject.getInt("mid"), mJsonObject.getString("access_token"), "kate", "", "", "kate_app", true, false);
                    }
                    break;
                }

            }
        } catch (Exception e) {
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError("Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }
    }

    private void delete(Account account) {
        Settings.get()
                .accounts()
                .removeAccessToken(account.getId());

        Settings.get()
                .accounts()
                .removeType(account.getId());

        Settings.get()
                .accounts()
                .remove(account.getId());

        DBHelper.removeDatabaseFor(requireActivity(), account.getId());

        LongpollInstance.get().forceDestroy(account.getId());

        mData.remove(account);
        mAdapter.notifyDataSetChanged();
        resolveEmptyText();
    }

    private void setAsActive(Account account) {
        Settings.get()
                .accounts()
                .setCurrent(account.getId());

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(final Account account) {
        boolean idCurrent = account.getId() == Settings.get()
                .accounts()
                .getCurrent();

        String[] items;

        if (account.getId() > 0) {
            if (idCurrent) {
                items = new String[]{getString(R.string.delete), getString(R.string.add_to_home_screen)};
            } else {
                items = new String[]{getString(R.string.delete), getString(R.string.add_to_home_screen), getString(R.string.set_as_active)};
            }
        } else {
            items = new String[]{getString(R.string.delete)};
        }

        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(account.getDisplayName())
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            delete(account);
                            break;
                        case 1:
                            createShortcut(account);
                            break;
                        case 2:
                            setAsActive(account);
                            break;
                    }
                })
                .show();
    }

    private VKApiUser getByID(List<VKApiUser> Users, int user_id) {
        for (VKApiUser i : Users) {
            if (i.id == user_id)
                return i;
        }
        return null;
    }

    private void AddUserInfo(final JsonObject temp, List<VKApiUser> Users, int user_id) {
        if (Users == null) {
            temp.addProperty("user_name", "error");
            temp.addProperty("avatar", "error");
            return;
        }
        VKApiUser usr = getByID(Users, user_id);
        if (usr == null) {
            temp.addProperty("user_name", "error");
            temp.addProperty("avatar", "error");
            return;
        }
        temp.addProperty("user_name", firstNonEmptyString(usr.last_name, " ") + " " + firstNonEmptyString(usr.first_name, " "));
        temp.addProperty("avatar", firstNonEmptyString(usr.photo_max_orig, " "));
    }

    private void SaveAccounts(File file, List<VKApiUser> Users) {
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
            Injection.provideApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.saved_to_param_file_name, file.getAbsolutePath());
        } catch (Exception e) {
            PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(e.getLocalizedMessage());
        } finally {
            Utils.safelyClose(out);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_ok) {
            requireActivity().finish();
            return true;
        }

        if (item.getItemId() == R.id.privacy_policy) {
            showPrivacyPolicy();
            return true;
        }

        if (item.getItemId() == R.id.action_proxy) {
            startProxySettings();
            return true;
        }

        if (item.getItemId() == R.id.export_accounts) {
            if (!AppPerms.hasReadStoragePermision(getActivity())) {
                AppPerms.requestReadExternalStoragePermission(getActivity());
                return true;
            }
            if (Settings.get().accounts() == null || Settings.get().accounts().getRegistered() == null || Settings.get().accounts().getRegistered().size() <= 0)
                return true;
            if (Settings.get().security().isUsePinForSecurity()) {
                startActivityForResult(new Intent(requireActivity(), EnterPinActivity.class), REQUEST_PIN_FOR_SECURITY);
            } else
                startExportAccounts();
            return true;
        }

        if (item.getItemId() == R.id.import_accounts) {
            if (!AppPerms.hasReadStoragePermision(getActivity())) {
                AppPerms.requestReadExternalStoragePermission(getActivity());
                return true;
            }
            DialogProperties properties = new DialogProperties();
            properties.selection_mode = DialogConfigs.SINGLE_MODE;
            properties.selection_type = DialogConfigs.FILE_SELECT;
            properties.root = Environment.getExternalStorageDirectory();
            properties.error_dir = Environment.getExternalStorageDirectory();
            properties.offset = Environment.getExternalStorageDirectory();
            ;
            properties.extensions = new String[]{"json"};
            properties.show_hidden_files = true;
            FilePickerDialog dialog = new FilePickerDialog(requireActivity(), properties);
            dialog.setTitle(R.string.import_accounts);
            dialog.setDialogSelectionListener(files -> {
                try {
                    StringBuilder jbld = new StringBuilder();
                    final File file = new File(files[0]);
                    if (file.exists()) {
                        FileInputStream dataFromServerStream = new FileInputStream(file);
                        BufferedReader d = new BufferedReader(new InputStreamReader(dataFromServerStream));
                        while (d.ready())
                            jbld.append(d.readLine());
                        d.close();
                        JsonArray reader = JsonParser.parseString(jbld.toString()).getAsJsonObject().getAsJsonArray("phoenix_accounts");
                        for (JsonElement i : reader) {
                            JsonObject elem = i.getAsJsonObject();
                            int id = elem.get("user_id").getAsInt();
                            if (Settings.get().accounts().getRegistered().contains(id))
                                continue;
                            String token = elem.get("access_token").getAsString();
                            String Type = elem.get("type").getAsString();
                            processNewAccount(id, token, Type, "", "", "phoenix_app", true, false);
                        }
                    }
                    PhoenixToast.CreatePhoenixToast(requireActivity()).showToast(R.string.accounts_restored, file.getAbsolutePath());
                } catch (Exception e) {
                    PhoenixToast.CreatePhoenixToast(requireActivity()).showToastError(e.getLocalizedMessage());
                }
            });
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startProxySettings() {
        startActivity(new Intent(requireActivity(), ProxyManagerActivity.class));
    }

    private void showPrivacyPolicy() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY_LINK));
        startActivity(browserIntent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_accounts, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.export_accounts).setVisible(mData.size() > 0);
    }

    private void createShortcut(final Account account) {
        if (account.getId() < 0) {
            return; // this is comminity
        }

        User user = (User) account.getOwner();

        final Context app = requireContext().getApplicationContext();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String avaUrl = user == null ? null : user.getMaxSquareAvatar();
                try {
                    ShortcutUtils.createAccountShurtcut(app, account.getId(), account.getDisplayName(), avaUrl);
                } catch (IOException e) {
                    return e.getMessage();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (Utils.nonEmpty(s)) {
                    Toast.makeText(app, s, Toast.LENGTH_LONG).show();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}