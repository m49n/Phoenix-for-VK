package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.TextWatcherAdapter;
import biz.dealnote.messenger.mvp.presenter.AddProxyPresenter;
import biz.dealnote.messenger.mvp.view.IAddProxyView;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class AddProxyFragment extends BaseMvpFragment<AddProxyPresenter, IAddProxyView> implements IAddProxyView {

    private EditText mAddress;
    private EditText mPort;
    private CheckBox mAuth;
    private EditText mUsername;
    private EditText mPassword;
    private View mAuthFieldsRoot;

    public static AddProxyFragment newInstance() {
        Bundle args = new Bundle();
        AddProxyFragment fragment = new AddProxyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_proxy_add, container, false);

        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mAuthFieldsRoot = root.findViewById(R.id.auth_fields_root);

        mAddress = root.findViewById(R.id.address);
        mAddress.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getPresenter().fireAddressEdit(s);
            }
        });

        mPort = root.findViewById(R.id.port);
        mPort.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getPresenter().firePortEdit(s);
            }
        });

        mAuth = root.findViewById(R.id.authorization);
        mAuth.setOnCheckedChangeListener((buttonView, isChecked) -> getPresenter().fireAuthChecked(isChecked));

        mUsername = root.findViewById(R.id.username);
        mUsername.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getPresenter().fireUsernameEdit(s);
            }
        });

        mPassword = root.findViewById(R.id.password);
        mPassword.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getPresenter().firePassEdit(s);
            }
        });

        root.findViewById(R.id.button_save).setOnClickListener(v -> getPresenter().fireSaveClick());
        return root;
    }

    @Override
    public void setAuthFieldsEnabled(boolean enabled) {
        if (nonNull(mAuthFieldsRoot)) {
            mAuthFieldsRoot.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setAuthChecked(boolean checked) {
        if (nonNull(mAuth)) {
            mAuth.setChecked(checked);
        }
    }

    @Override
    public void goBack() {
        requireActivity().onBackPressed();
    }

    @NotNull
    @Override
    public IPresenterFactory<AddProxyPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new AddProxyPresenter(saveInstanceState);
    }
}