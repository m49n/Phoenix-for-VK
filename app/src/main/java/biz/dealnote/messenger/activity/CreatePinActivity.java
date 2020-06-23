package biz.dealnote.messenger.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.CreatePinFragment;

public class CreatePinActivity extends NoMainActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, CreatePinFragment.newInstance())
                    .commit();
        }
    }
}