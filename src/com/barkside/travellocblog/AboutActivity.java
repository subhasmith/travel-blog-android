package com.barkside.travellocblog;

import com.google.android.gms.common.GooglePlayServicesUtil;

import android.os.Bundle;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

/*
 * Activity to display About information, including Legal Notices
 */

public class AboutActivity extends Activity
{
   private static final String TAG = "AboutActivity";

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about_activity);
      
      String version = "0.0.0"; // to denote unknown version
      try
      {
         PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
         version = pInfo.versionName;
      } catch (NameNotFoundException e)
      {
         Log.d(TAG, "Failed to get version string " + e);
      }
      
      // App name and version number
      TextView tv = (TextView) findViewById(R.id.app_name);
      tv.setText(R.string.app_name);
      tv = (TextView) findViewById(R.id.app_version);
      tv.setText(version);

      // Our main about message with links to Travel Blog code, Play Store, Mailing List, etc
      tv = (TextView) findViewById(R.id.about_message);
      tv.setMovementMethod(LinkMovementMethod.getInstance());

      // All other copyright messages of software used in this package
      tv = (TextView) findViewById(R.id.about_notices);
      tv.setText(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this));
   }

}
