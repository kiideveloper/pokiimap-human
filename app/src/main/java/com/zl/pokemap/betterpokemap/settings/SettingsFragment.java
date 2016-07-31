package com.zl.pokemap.betterpokemap.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.zl.pokemap.betterpokemap.BuildConfig;
import com.zl.pokemap.betterpokemap.R;
import com.zl.pokemap.betterpokemap.hack.MapHelper;
import com.zl.pokemap.betterpokemap.hack.settings.PokemapSharedPreferences;

import org.joda.time.Duration;

import java.text.MessageFormat;
import java.util.List;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
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
        mSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
                final SettingsFragment fragment = SettingsFragment.this;
                final Context context = fragment.getActivity();
                if (context == null || fragment.getPreferenceScreen() == null) {
                    final String tag = fragment.getClass().getSimpleName();
                    // TODO: Introduce a static function to register this class and ensure that
                    // onCreate must be called before "onSharedPreferenceChanged" is called.
                    Log.w(tag, "onSharedPreferenceChanged called before activity starts.");
                    return;
                }
                fragment.onSharedPreferenceChanged(prefs, key);
            }
        };
        pref.registerOnSharedPreferenceChangeListener(
                mSharedPreferenceChangeListener);

    }

    @Override
    public void onDestroy() {
        pref.unregisterOnSharedPreferenceChangeListener(
                mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if("version_info".equals(preference.getKey())){
            new ChangeLog(getActivity()).getLogDialog().show();
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateListPreferenceSummaryToCurrentValue("show_pokestop", getPreferenceScreen());
        updateListPreferenceSummaryToCurrentValue("pokeball", getPreferenceScreen());
        updateListPreferenceSummaryToCurrentValue("step_delay", getPreferenceScreen());
        updateSearchRangeTime();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateListPreferenceSummaryToCurrentValue("show_pokestop", getPreferenceScreen());
        updateListPreferenceSummaryToCurrentValue("pokeball", getPreferenceScreen());
        updateListPreferenceSummaryToCurrentValue("step_delay", getPreferenceScreen());
        updateSearchRangeTime();
    }

    private void updateSearchRangeTime(){
        try {
            Preference p = getPreferenceScreen().findPreference("search_steps");
            int steps = Integer.parseInt(pref.getString(PokemapSharedPreferences.STEPS, "5"));
            int delay = Integer.parseInt(pref.getString("step_delay", "5"));
            int each = delay*1000;
            List<LatLng> areas = MapHelper.getSearchArea(steps,new LatLng(0.0,0.0));
            Duration d = new Duration(each*areas.size());
            p.setSummary(MessageFormat.format(
                    getString(R.string.scan_info),
                    d.getStandardMinutes(),
                    d.getStandardSeconds() - 60*d.getStandardMinutes()));
        }catch (Exception e){}

    }


    static void updateListPreferenceSummaryToCurrentValue(final String prefKey,
                                                          final PreferenceScreen screen) {

        try {
            // Because the "%s" summary trick of {@link ListPreference} doesn't work properly before
            // KitKat, we need to update the summary programmatically.
            final ListPreference listPreference = (ListPreference)screen.findPreference(prefKey);
            if (listPreference == null) {
                return;
            }
            final CharSequence entries[] = listPreference.getEntries();
            final int entryIndex = listPreference.findIndexOfValue(listPreference.getValue());
            listPreference.setSummary(entryIndex < 0 ? null : entries[entryIndex]);
        }catch (Exception e){}


    }
}

