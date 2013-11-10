/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.barkside.travellocblog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Activity to display all the blog entry locations on a map.
 * This uses a fragment to display the blog data.
 */
public class MapTrip extends ActionBarActivity {
   // For logging and debugging purposes
   private static final String TAG = "MapTrip";

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.map_trip);

      Intent intent = getIntent();
      
      Log.d(TAG, "intent data " + intent.getDataString());
      
      /* Get the filename from the Uri */
      Uri uri = intent.getData();
      Log.d(TAG, "got uri " + uri);
      String filename = Utils.uriToBlogname(uri);
      Log.d(TAG, "uri to blogname " + filename);

      // update ActionBar title with blog name
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();
      actionBar.setTitle(Utils.blogToDisplayname(filename));
      actionBar.setDisplayHomeAsUpEnabled(true);
      
      // Display the Map as a fragment
      if (savedInstanceState == null) {
         // First-time init; create fragment to embed in activity.
         FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         MapTripFragment newFragment = MapTripFragment.newInstance(uri);
         Log.d(TAG, "Got MapTripFragment " + newFragment);
         ft.add(R.id.map_trip_fragment, newFragment);
         ft.commit();
     }

   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.map_trip, menu);
      
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId())
      {
         case R.id.send_feedback:
            Utils.sendFeedback(this, TAG);
            return true;
       case R.id.help:
            Utils.showHelp(getSupportFragmentManager());
            return true;
            // Respond to the action bar's Up/Home button
       case android.R.id.home:
          Intent upIntent = NavUtils.getParentActivityIntent(this);
          if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
              // This activity is NOT part of this app's task, so create a new task
              // when navigating up, with a synthesized back stack.
              TaskStackBuilder.create(this)
                      // Add all of this activity's parents to the back stack
                      .addNextIntentWithParentStack(upIntent)
                      // Navigate up to the closest parent
                      .startActivities();
          } else {
              // This activity is part of this app's task, so simply
              // navigate up to the logical parent activity.
              NavUtils.navigateUpTo(this, upIntent);
          }
          return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

}
