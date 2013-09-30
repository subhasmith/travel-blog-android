package com.barkside.travellocblog;

import android.os.Bundle;
import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/*
 * Activity to display Help information
 */

public class HelpActivity extends Activity
{
   // private static final String TAG = "HelpActivity";

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.help_activity);
      
      TextView tv;
      
      // Intro message with links to Travel Blog User Guide
      tv = (TextView) findViewById(R.id.help_intro);
      tv.setMovementMethod(LinkMovementMethod.getInstance());

      // The full help message
      tv = (TextView) findViewById(R.id.help_message);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
   }
}
