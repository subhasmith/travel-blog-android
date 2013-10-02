package com.barkside.travellocblog;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
   
   public final static String DEFAULT_DESC_ON_KEY = "default_desc_on";
   public final static String LOCATION_DURATION_KEY = "location_duration";
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
