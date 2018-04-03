package sq.rogue.rosettadrone.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.util.Patterns;

import java.util.Map;
import java.util.regex.Pattern;

import sq.rogue.rosettadrone.R;

// Display value of preference in summary field

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    SharedPreferences sharedPreferences;

    /**
     * Sets up the fragment upon creation. Sets shared preferences.
     * @param savedInstanceState Saved state from prior application runs.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getPreferenceManager().getSharedPreferences();
//        for (Map.Entry<String, ?> preferenceEntry : sharedPreferences.getAll().entrySet()) {
//            Preference preference = (Preference) preferenceEntry.getValue();
//            if (preference instanceof EditTextPreference) {
//                addValidator(preference);
//            } else {
//
//            }
//        }
//        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Fires when preferences are to be loaded into the fragment. Reads preferences from the
     * preferences XML and sets the validation listeners for each shared preference.
     * @param savedInstanceState A bundle containing any saved state from previous runs.
     * @param rootKey Key to root the preference fragment to the PreferenceScreen if not-null.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setListeners();
    }

    /**
     *
     */
    public void setListeners() {
        findPreference("pref_gcs_ip").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return validate_ip((String) newValue);
            }
        });

        findPreference("pref_video_ip").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return validate_ip((String) newValue);
            }
        });

        findPreference("pref_telem_port").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return validate_port((Integer) newValue);
            }
        });

        findPreference("pref_video_port").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return validate_port((Integer) newValue);
            }
        });
    }

    /**
     * Validates an IP address against the built-in RegEx. Does NOT validate correctly with
     * short ip addresses. E.g. "127.0.1" and "127.1" will return false even though they are valid.
     * @param ip A string representation of an IP address
     * @return True if the IP address is valid, false otherwise.
     */
    public boolean validate_ip(String ip) {
        return Patterns.IP_ADDRESS.matcher(ip).matches();
    }

    /**
     * Validates a port. A valid port is between (inclusive) 1 and 65535. A 16-bit integer. Although 0
     * is technically a valid port it is not used and reserved for TCP/IP.
     * @param port A port number to validate.
     * @return True if the port is between (inclusive) 1 and 65535. False otherwise.
     */
    public boolean validate_port(int port) {
        return (port >= 1 && port <= 65535);
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateAllPreferences();
    }

    /**
     * Helper method to be called when all preferences need updates such as when the fragment
     * is resumed.
     */
    public void updateAllPreferences() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    Preference singlePref = preferenceGroup.getPreference(j);
                    updatePreference(singlePref);
                }
            } else {
                updatePreference(preference);
            }
        }
    }

    /**
     *
     */
    @Override
    public void onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Listener that triggers when a shared preference changes. Validation does not occur within this
     * listener.
     * @param sharedPreferences The list of shared preferences.
     * @param key The key for the corresponding shared preference that changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreference(findPreference(key));
    }

    /**
     * Method which updates a preferences and sets the summary to the new value is applicable.
     * @param preference The preference to update
     */
    public void updatePreference(Preference preference) {
        if (preference == null) return;
        if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) preference;
            preference.setSummary(editTextPref.getText());
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
            return;
        } else {
            return;
        }
        SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
        preference.setSummary(sharedPrefs.getString(preference.getKey(), "Default"));
    }
}