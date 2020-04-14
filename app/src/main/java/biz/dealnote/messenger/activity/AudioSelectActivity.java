package biz.dealnote.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.fragment.AudiosFragment;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceProvider;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;

/**
 * Created by Ruslan Kolbasa on 17.08.2017.
 * phoenix
 */
public class AudioSelectActivity extends NoMainActivity implements PlaceProvider {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.get().ui().getMainTheme());

        if (Objects.isNull(savedInstanceState)) {
            int accountId = super.getIntent().getExtras().getInt(Extra.ACCOUNT_ID);
            int ownerId = super.getIntent().getExtras().getInt(Extra.OWNER_ID);
            attachInitialFragment(accountId, ownerId);
        }
    }

    /**
     * @param context
     * @param accountId От чьего имени получать
     * @param ownerId   Чьи получать
     * @return
     */
    public static Intent createIntent(Context context, int accountId, int ownerId) {
        return new Intent(context, AudioSelectActivity.class)
                .putExtra(Extra.ACCOUNT_ID, accountId)
                .putExtra(Extra.OWNER_ID, ownerId);
    }

    private void attachInitialFragment(int accountId, int ownerId) {
        AudiosFragment fragment = AudiosFragment.newInstanceSelect(accountId, ownerId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(getMainContainerViewId(), fragment)
                .addToBackStack("audio-select")
                .commit();
    }

    @Override
    public void openPlace(Place place) {

    }
}
