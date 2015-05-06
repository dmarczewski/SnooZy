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

package ca.mudar.snoozy.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import ca.mudar.snoozy.Const;
import ca.mudar.snoozy.R;
import ca.mudar.snoozy.util.ComponentHelper;
import ca.mudar.snoozy.util.LogUtils;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = LogUtils.makeLogTag(SettingsFragment.class);

    private SharedPreferences mSharedPrefs;
    private Preference mHasNotifications;
    private Preference mHasVibration;
    private Preference mHasSound;
    private Preference mScreenLockStatus;
    private Preference mPowerConnectionStatus;
    private Preference mPowerConnectionType;
    private Preference mDelayToLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager pm = this.getPreferenceManager();
        pm.setSharedPreferencesName(Const.APP_PREFS_NAME);
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);

        addPreferencesFromResource(R.xml.preferences);
        mSharedPrefs = pm.getSharedPreferences();

        setupPreferences();

        final PreferenceScreen deviceAdminPrefScreen = (PreferenceScreen) findPreference(Const.PrefsNames.DEVICE_ADMIN);
        deviceAdminPrefScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                launchDeviceAdminSettings();

                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");


        /**
         * Set up a listener whenever a key changes
         */
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);

        /**
         * Toggle prefs based on masterSwitch status
         */
        final boolean isEnabled = mSharedPrefs.getBoolean(Const.PrefsNames.IS_ENABLED, false);
        updateMasterSwitchDependencies(isEnabled);

        /**
         * Update summaries
         */
        mScreenLockStatus.setSummary(getLockStatusSummary());
        mPowerConnectionStatus.setSummary(getConnectionStatusSummary());
        mPowerConnectionType.setSummary(getConnectionTypeSummary());
        mDelayToLock.setSummary(getLockDelaySummary());
        findPreference(Const.PrefsNames.CACHE_AGE).setSummary(getCacheAgeSummary());
    }

    @Override
    public void onPause() {
        super.onPause();

        /**
         * Remove the listener onPause
         */
        if (mSharedPrefs != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(TAG, "onSharedPreferenceChanged "
                + String.format("key = %s", key));

        /**
         * onChanged, new preferences values are sent to the AppHelper.
         */
        if (key.equals(Const.PrefsNames.IS_ENABLED)) {
            updateMasterSwitchDependencies(sharedPreferences.getBoolean(key, false));
        } else if (key.equals(Const.PrefsNames.DELAY_TO_LOCK)) {
            findPreference(Const.PrefsNames.DELAY_TO_LOCK)
                    .setSummary(getLockDelaySummary());
        } else if (key.equals(Const.PrefsNames.CACHE_AGE)) {
            findPreference(Const.PrefsNames.CACHE_AGE)
                    .setSummary(getCacheAgeSummary());
        } else if (key.equals(Const.PrefsNames.SCREEN_LOCK_STATUS)) {
            findPreference(Const.PrefsNames.SCREEN_LOCK_STATUS)
                    .setSummary(getLockStatusSummary());
        } else if (key.equals(Const.PrefsNames.POWER_CONNECTION_STATUS)) {
            findPreference(Const.PrefsNames.POWER_CONNECTION_STATUS)
                    .setSummary(getConnectionStatusSummary());
        } else if (key.equals(Const.PrefsNames.POWER_CONNECTION_TYPE)) {
            findPreference(Const.PrefsNames.POWER_CONNECTION_TYPE)
                    .setSummary(getConnectionTypeSummary());
        }
    }

    private void setupPreferences() {
        final Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (!v.hasVibrator()) {
            getPreferenceScreen().removePreference(findPreference(Const.PrefsNames.HAS_VIBRATION));
        }

        mHasNotifications = findPreference(Const.PrefsNames.HAS_NOTIFICATIONS);
        mHasVibration = findPreference(Const.PrefsNames.HAS_VIBRATION);
        mHasSound = findPreference(Const.PrefsNames.HAS_SOUND);
        mScreenLockStatus = findPreference(Const.PrefsNames.SCREEN_LOCK_STATUS);
        mPowerConnectionStatus = findPreference(Const.PrefsNames.POWER_CONNECTION_STATUS);
        mPowerConnectionType = findPreference(Const.PrefsNames.POWER_CONNECTION_TYPE);
        mDelayToLock = findPreference(Const.PrefsNames.DELAY_TO_LOCK);
    }

    private void updateMasterSwitchDependencies(boolean isChecked) {
        Log.v(TAG, "updateMasterDependencies "
                + String.format("isChecked = %s", isChecked));
        mHasNotifications.setEnabled(isChecked);
        mHasVibration.setEnabled(isChecked);
        mHasSound.setEnabled(isChecked);
        if (isChecked) {
            final boolean isDeviceAdmin = ComponentHelper.isDeviceAdmin(getActivity());
            Log.v(TAG, "isDeviceAdmin = " + isDeviceAdmin );
            updateDeviceAdminDependencies(isDeviceAdmin);
        } else {
            updateDeviceAdminDependencies(false);
        }
    }

    private void updateDeviceAdminDependencies(boolean isDeviceAdmin) {
        Log.v(TAG, "updateDeviceAdminDependencies "
                + String.format("isDeviceAdmin = %s", isDeviceAdmin));

        mScreenLockStatus.setEnabled(isDeviceAdmin);
        mPowerConnectionStatus.setEnabled(isDeviceAdmin);
        mPowerConnectionType.setEnabled(isDeviceAdmin);
        mDelayToLock.setEnabled(isDeviceAdmin);
    }

    private void launchDeviceAdminSettings() {
        try {
            Intent intentDeviceAdmin = new Intent(Settings.ACTION_SETTINGS);
            intentDeviceAdmin.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intentDeviceAdmin.setClassName(Const.IntentActions.ANDROID_SETTINGS, Const.IntentActions.ANDROID_DEVICE_ADMIN);
            startActivity(intentDeviceAdmin);
        } catch (Exception e) {
            e.printStackTrace();
            Intent intentSecuritySettings = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            intentSecuritySettings.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intentSecuritySettings);
        }
    }

    private int getLockDelaySummary() {
        final String value = mSharedPrefs.getString(Const.PrefsNames.DELAY_TO_LOCK, Const.PrefsValues.DELAY_FAST);
        int res;
        if (Const.PrefsValues.DELAY_SLOW.equals(value)) {
            res = R.string.prefs_delay_slow;
        } else if (Const.PrefsValues.DELAY_MODERATE.equals(value)) {
            res = R.string.prefs_delay_moderate;
        } else {
            res = R.string.prefs_delay_fast;
        }

        return res;
    }

    private int getCacheAgeSummary() {
        final String value = mSharedPrefs.getString(Const.PrefsNames.CACHE_AGE, Const.PrefsValues.CACHE_ALL);
        int res;
        if (Const.PrefsValues.CACHE_NONE.equals(value)) {
            res = R.string.prefs_cache_age_none;
        } else if (Const.PrefsValues.CACHE_SMALL.equals(value)) {
            res = R.string.prefs_cache_age_small;
        } else if (Const.PrefsValues.CACHE_MEDIUM.equals(value)) {
            res = R.string.prefs_cache_age_medium;
        } else if (Const.PrefsValues.CACHE_LARGE.equals(value)) {
            res = R.string.prefs_cache_age_large;
        } else {
            res = R.string.prefs_cache_age_all;
        }

        return res;
    }

    private int getLockStatusSummary() {
        final String value = mSharedPrefs.getString(Const.PrefsNames.SCREEN_LOCK_STATUS, Const.PrefsValues.SCREEN_LOCKED);
        int res;
        if (Const.PrefsValues.SCREEN_LOCKED.equals(value)) {
            res = R.string.prefs_screen_lock_locked;
        } else if (Const.PrefsValues.SCREEN_UNLOCKED.equals(value)) {
            res = R.string.prefs_screen_lock_unlocked;
        } else {
            res = R.string.prefs_screen_lock_both;
        }

        return res;
    }

    private int getConnectionStatusSummary() {
        final String value = mSharedPrefs.getString(Const.PrefsNames.POWER_CONNECTION_STATUS, Const.PrefsValues.IGNORE);
        int res;
        if (Const.PrefsValues.CONNECTION_ON.equals(value)) {
            res = R.string.prefs_power_connection_on;
        } else if (Const.PrefsValues.CONNECTION_OFF.equals(value)) {
            res = R.string.prefs_power_connection_off;
        } else {
            res = R.string.prefs_power_connection_both;
        }

        return res;
    }

    private int getConnectionTypeSummary() {
        final String value = mSharedPrefs.getString(Const.PrefsNames.POWER_CONNECTION_TYPE, Const.PrefsValues.IGNORE);
        int res;
        if (Const.PrefsValues.CONNECTION_AC.equals(value)) {
            res = R.string.prefs_power_connection_ac;
        } else if (Const.PrefsValues.CONNECTION_USB.equals(value)) {
            res = R.string.prefs_power_connection_usb;
        } else if (Const.PrefsValues.CONNECTION_WIRELESS.equals(value)) {
            res = R.string.prefs_power_connection_wireless;
        } else {
            res = R.string.prefs_power_connection_all;
        }

        return res;
    }
}