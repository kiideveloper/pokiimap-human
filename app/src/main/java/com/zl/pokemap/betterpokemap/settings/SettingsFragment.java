package com.zl.pokemap.betterpokemap.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.zl.pokemap.betterpokemap.BuildConfig;
import com.zl.pokemap.betterpokemap.R;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener{

    SharedPreferences pref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen().findPreference("version_info").setSummary("version: "+ BuildConfig.VERSION_NAME);
        getPreferenceScreen().findPreference("version_info").setOnPreferenceClickListener(this);

        if(BuildConfig.DEBUG){
            try {
                getPreferenceScreen().removePreference(findPreference("community"));
                getPreferenceScreen().removePreference(findPreference("support"));
                getPreferenceScreen().removePreference(findPreference("developer"));
            }catch (Exception e){};
        }else{ //release
            try {
                getPreferenceScreen().removePreference(findPreference("debug"));
            }catch (Exception e){};
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if("version_info".equals(preference.getKey())){
            new ChangeLog(getActivity()).getLogDialog().show();
        }
        return false;
    }
}

