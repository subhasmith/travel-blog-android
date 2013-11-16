package com.barkside.travellocblog;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
   
    private final static String TAG = "SettingsActivity";
   
    public final static String DEFAULT_DESC_ON_KEY = "default_desc_on";
    public final static String LOCATION_DURATION_KEY = "location_duration";
    public final static String LAST_OPENED_TRIP_KEY = "last_opened_trip";
    // To display a What's new message once on first start after update of the app,
    // we keep track of the app version used each time we start the app.
    public final static String LAST_VERSION_USED_KEY = "last_version";
    // Whether to display Map Trip info in km or miles
    public final static String DISTANCE_UNITS_KEY = "distance_units";

    @SuppressWarnings("deprecation") // addPreferencesFromResource is used below
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // http://developer.android.com/guide/topics/ui/settings.html
        // We follow the standard usage as outlined in the Settings Android Developer
        // guide. Therefore, use the default getDefaultSharedPreferences and don't
        // use a named prefs files. While setSharedPreferencesName can be called here,
        // it is a deprecated call, and will make things harder in the future when
        // we have increased number of preferences. So sticking with recommended usage.
        // Don't: getPreferenceManager().setSharedPreferencesName(TravelLocBlogMain.PREFS_NAME);
        
        addPreferencesFromResource(R.xml.preferences);
        
        // Display changes to the Location Duration setting
        initializeListUpdates(LOCATION_DURATION_KEY);

        // Display changes to the Distance Units settings
        initializeListUpdates(DISTANCE_UNITS_KEY);
    }
    
    /**
     * Work around Android bug that does not support automatic updates to
     * ListPreference summary element to display the value chosen by user.
     * http://stackoverflow.com/questions/531427/how-do-i-display-the-current-value-of-an-android-preference-in-the-preference-su/11210367#11210367
     */
    private void updateListSummary(ListPreference updateList, String newValue) {
       Log.d(TAG, "update list value " + newValue);
       if (newValue != null) {
          int index = updateList.findIndexOfValue(newValue);
          CharSequence entry = updateList.getEntries()[index];
          updateList.setSummary(entry);          
       }
    }

    // Add listener to the given ListPreference setting so that it automatically
    // updates the summary text when user changes the setting.
    @SuppressWarnings("deprecation") // findPreference is used below
    private void initializeListUpdates(String listKey) {
       final ListPreference listPrefs = (ListPreference) findPreference(listKey);

       updateListSummary(listPrefs, listPrefs.getValue());

       listPrefs.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference, Object newValue) {
             updateListSummary(listPrefs, newValue.toString());
             return true;
          }       
       });          

    }
    /**
     * Can't really use this to start a dialog message to display about, so have
     * to implement an AboutActivity class. In future when we don't need the
     * support library and only care about Android 3.0 then can use PreferenceFragment
     * and call getSupportFragmentManager as is done to display Help and What's New
     * in the main class. TODO: when we don't need to support anything older than Android 3.0.
     * http://stackoverflow.com/questions/5501431/was-preferencefragment-intentionally-excluded-from-the-compatibility-package
     */
   /*
    * implements OnSharedPreferenceChangeListener
   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      // Start the About message screen
      if (key.equals("AboutKey")) {
         FragmentManager fm = getSupportFragmentManager();
         MessagesDialog aboutDialog = new MessagesDialog();
         Bundle args = new Bundle();
         args.putInt("MESSAGES_TYPE", 100);
         aboutDialog.setArguments(args);
         aboutDialog.show(fm, "about_message");
      }
   }
   */
    
}
