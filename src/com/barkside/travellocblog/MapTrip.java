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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
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
   
   // This activity manages the blog data, and passes this object to the
   // fragment activity to display the map data
   private BlogDataManager mBlogMgr = new BlogDataManager();
   
   private static String mDistanceUnits = ""; // Trip info distance in km or miles
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.map_trip);

      // Turn on the parent navigation button in action bar
      ActionBar actionBar = getSupportActionBar();
      actionBar.setDisplayHomeAsUpEnabled(true);

      Uri blogUri = null;
      // Display the Map as a fragment
      MapTripFragment mapFrag;
      if (savedInstanceState == null) {
         Intent intent = getIntent();
         
         // Get the filename from the Uri
         blogUri = intent.getData();
         Log.d(TAG, "onCreate intent uri " + blogUri);

         // First-time init; create fragment to embed in activity.
         FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         mapFrag = MapTripFragment.newInstance();
         ft.add(R.id.map_trip_fragment, mapFrag);
         ft.commit();
      } else {
         mapFrag = (MapTripFragment)
               getSupportFragmentManager().findFragmentById(R.id.map_trip_fragment);

         mBlogMgr.onRestoreInstanceState(savedInstanceState);
         blogUri = mBlogMgr.uri();
         Log.d(TAG, "onCreate savedInstanceState uri " + blogUri);
      }
      
      mBlogMgr.openBlog(this, blogUri, R.string.open_failed_one_file);
      mapFrag.useBlogMgr(mBlogMgr);
   }
   
   // Note that fragment onResume is called after the parent activity onResume
   // Use onResumeFragments instead to guarantee order.
   @Override
   protected void onResumeFragments() {
      // Fragment will load the file into mBlogMgr and display the map data.
      super.onResumeFragments();
      
      // Whether to show trip info distance in miles or km
      // May change after onCreate, so check this in onResume
      SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
      mDistanceUnits = sharedPref.getString(SettingsActivity.DISTANCE_UNITS_KEY,
            getString(R.string.distance_units_default));

      // MapFragment has opened the file and displayed it. If it failed to open,
      // then mBlogMgr.uri() == null.
      updateTitles();
   }

   /**
    * We need to survive a device orientation change. Android will completely destroy
    * and recreate this activity.
    * If we don't remember Blog uri for example, and parent activity called openTrip
    * the onCreate activity will use the original intent uri, which
    * is not correct. Note that the parent activity should ask this fragment for current
    * trip name (best to do that in an onResume).
    */
   
   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      mBlogMgr.onSaveInstanceState(savedInstanceState);
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
      switch (item.getItemId()) {
      case R.id.open_trip:
         openTripUI();
         return true;
      case R.id.settings:
         startActivity(new Intent(this, SettingsActivity.class));
         return true;
      case R.id.send_feedback:
         Utils.sendFeedback(this, TAG);
         return true;
      case R.id.help:
         Utils.showHelp(getSupportFragmentManager());
         return true;
         // Respond to the action bar's Up/Home button
      case android.R.id.home:
         Utils.handleUpNavigation(this, mBlogMgr.uri());
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }

   void openTripUI()
   {
      BlogDataManager blogMgr = new BlogDataManager();
      final CharSequence[] fileList = blogMgr.getBlogsList();
      final Context context = this;
      
      final MapTripFragment mapFrag = (MapTripFragment)
            getSupportFragmentManager().findFragmentById(R.id.map_trip_fragment);

      TravelLocBlogMain.selectTripDialog(this, fileList,
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item)
         {
            dialog.dismiss();
            String newname = fileList[item].toString();
            Uri uri = Utils.blognameToUri(newname);

            if (mBlogMgr.openBlog(context, uri, R.string.open_failed_one_file)) {
               // Do not save blog name to preferences. That makes for confusing UI,
               // only Main activity saves last opened trip and all other activities should
               // avoid saving last opened file.
               // Utils.setPreferencesLastOpenedTrip(context, newname);

               // Tell the Map Fragment to display this new blog
               mapFrag.displayTrip();
               // Update Action Bar title.
               updateTitles();
            }
         }
      });
   }
   
   // Update ActionBar title and subtitle.
   private void updateTitles() {
      String title = "";

      BlogDataManager blogMgr = mBlogMgr;
      
      String blogname = blogMgr.blogname();
      if (!blogname.equals(""))
         title = Utils.blogToDisplayname(blogname);
      
      // update ActionBar title with blog name
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();
      actionBar.setTitle(title);
      
      // Update ActionBar subtitle with Trip Info
      String subtitle = "";
      if (!blogname.equals(""))   
         subtitle = getTripInfo(blogMgr, mDistanceUnits);
      else
         subtitle = getString(R.string.open_failed_title);
      actionBar.setSubtitle(subtitle);
   }

   // Summarize trip in a short string.
   // Ex: 11 places, 12.3 miles.
   private String getTripInfo(BlogDataManager blogMgr, String distanceUnits) {
      String units = distanceUnits;
      float fdistkm = blogMgr.getTotalDistance() / 1000.0f;
      float fdist;
      if (units.equals("miles")) fdist = fdistkm * 0.6213711f;
      else if (units.equals("km")) fdist = fdistkm;
      else {
         // Unknown distance units. Default to km.
         Log.e(TAG, "getTripInfo: unknown distance units specified: " + units);
         units = "km";
         fdist = fdistkm;
      }
      int count = blogMgr.getMaxBlogElements();
      Resources res = getResources();
      String str = res.getQuantityString(R.plurals.trip_info_title,
            count, count, fdist, units);
      return str;
   }

}
