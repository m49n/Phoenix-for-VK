package biz.dealnote.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.AudiosFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.SingleTabSearchFragment;
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceProvider;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;

/**
 * Created by Ruslan Kolbasa on 17.08.2017.
 * phoenix
 */
public class AudioSelectActivity extends NoMainActivity implements PlaceProvider {

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Objects.isNull(savedInstanceState)) {
            int accountId = super.getIntent().getExtras().getInt(Extra.ACCOUNT_ID);
            int ownerId = super.getIntent().getExtras().getInt(Extra.OWNER_ID);
            attachInitialFragment(accountId, ownerId);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_audio_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            AudioSearchCriteria criteria = new AudioSearchCriteria("", false, true);
            SingleTabSearchFragment singleTabSearchFragment = SingleTabSearchFragment.newInstance(Settings.get().accounts().getCurrent(), SearchContentType.AUDIOS_SELECT, criteria);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(getMainContainerViewId(), singleTabSearchFragment)
                    .addToBackStack("audio-search-select")
                    .commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
