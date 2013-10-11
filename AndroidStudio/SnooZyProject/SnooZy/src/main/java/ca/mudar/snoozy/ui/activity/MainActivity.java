/*
    SnooZy Charger
    Power Connection manager. Turn the screen off on power connection
    or disconnection, to save battery consumption by the phone's display.

    Copyright (C) 2013 Mudar Noufal <mn@mudar.ca>

    This file is part of SnooZy Charger.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.mudar.snoozy.ui.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.widget.Toast;

import ca.mudar.snoozy.Const;
import ca.mudar.snoozy.R;
import ca.mudar.snoozy.ui.fragment.HistoryFragment;
import ca.mudar.snoozy.util.ComponentHelper;

import static ca.mudar.snoozy.util.LogUtils.makeLogTag;

public class MainActivity extends BaseActivity {
    private static final String TAG = makeLogTag(MainActivity.class);
    private boolean mHasDeviceAdminIntent = false;
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, Const.APP_PREFS_NAME, Context.MODE_PRIVATE, R.xml.preferences, false);
        PreferenceManager.setDefaultValues(this, Const.APP_PREFS_NAME, Context.MODE_PRIVATE, R.xml.preferences_beta, false);

        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null) {
            updateNotifyPrefs(getIntent());
            getIntent().removeExtra(Const.IntentExtras.RESET_NOTIFY_NUMBER);
            getIntent().removeExtra(Const.IntentExtras.INCREMENT_NOTIFY_GROUP);
        }

        FragmentManager fm = getFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            HistoryFragment fragment = new HistoryFragment();
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }

        if (ComponentHelper.isDeviceAdmin(this)) {
            // Start the background service which will register the BroadcastReceiver
            togglePowerConnectionReceiver(true);
        } else {
            togglePowerConnectionReceiver(false);

            Intent intent = ComponentHelper.getDeviceAdminAddIntent(this);
            startActivityForResult(intent, Const.RequestCodes.ENABLE_ADMIN);
            mHasDeviceAdminIntent = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SharedPreferences sharedPrefs = getSharedPreferences(Const.APP_PREFS_NAME, Context.MODE_PRIVATE);
        final boolean isEnabledPrefs = sharedPrefs.getBoolean(Const.PrefsNames.IS_ENABLED, false);

        final boolean isBetaUser = sharedPrefs.getBoolean(Const.PrefsNames.IS_BETA_USER, false);

        if (ComponentHelper.isDeviceAdmin(this)) {
            if (!isEnabledPrefs) {
                mToast = mToast.makeText(this, R.string.toast_service_disabled, Toast.LENGTH_SHORT);
                mToast.show();
            }
        } else {
            if (isEnabledPrefs && !mHasDeviceAdminIntent) {
                mToast = mToast.makeText(this, R.string.toast_running_no_admin, Toast.LENGTH_LONG);
                mToast.show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Const.RequestCodes.ENABLE_ADMIN) {
            mHasDeviceAdminIntent = false;
            togglePowerConnectionReceiver(resultCode == RESULT_OK);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getExtras() != null) {
            updateNotifyPrefs(intent);
        }

        intent.removeExtra(Const.IntentExtras.RESET_NOTIFY_NUMBER);
        intent.removeExtra(Const.IntentExtras.INCREMENT_NOTIFY_GROUP);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mToast != null) {
            mToast.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void togglePowerConnectionReceiver(boolean isDeviceAdmin) {

        final SharedPreferences sharedPrefs = getSharedPreferences(Const.APP_PREFS_NAME, Context.MODE_PRIVATE);
        final boolean isEnabledPrefs = sharedPrefs.getBoolean(Const.PrefsNames.IS_ENABLED, false);

        if (isDeviceAdmin && isEnabledPrefs) {
            ComponentHelper.togglePowerConnectionReceiver(getApplicationContext(),
                    true);
        } else {
            ComponentHelper.togglePowerConnectionReceiver(getApplicationContext(),
                    isEnabledPrefs);
        }
    }

    private void updateNotifyPrefs(Intent intent) {
        final SharedPreferences sharedPrefs = getSharedPreferences(Const.APP_PREFS_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();

        if (intent != null) {
            final boolean hasResetNotifyNumber = intent
                    .getBooleanExtra(Const.IntentExtras.RESET_NOTIFY_NUMBER, false);
            final boolean hasIncrementNotifyGroup = intent
                    .getBooleanExtra(Const.IntentExtras.INCREMENT_NOTIFY_GROUP, false);

            if (hasResetNotifyNumber) {
                sharedPrefsEditor.putInt(Const.PrefsNames.NOTIFY_COUNT, 1);
            }

            if (hasIncrementNotifyGroup) {
                final int notifyGroup = sharedPrefs.getInt(Const.PrefsNames.NOTIFY_GROUP, 1);
                sharedPrefsEditor.putInt(Const.PrefsNames.NOTIFY_GROUP, notifyGroup + 1);
            }

            sharedPrefsEditor.apply();
        }
    }
}
