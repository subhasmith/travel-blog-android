package com.barkside.travellocblog;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;

/*
 * Activity to display About information, including Legal Notices
 */

public class AboutActivity extends ActionBarActivity
{
   // private static final String TAG = "AboutActivity";

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about_activity);
      
      String version = Utils.getAppVersion(this);
      
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
