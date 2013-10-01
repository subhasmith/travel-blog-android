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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * Activity to display all the blog note entry locations on a map.
 * 
 * Map code taken from
 * android-sdks/extras/google/google_play_services/samples/maps/
 * src/com/example/mapdemo/MarkerDemo java file.
 * 
 */
public class TripMapView extends FragmentActivity {
   // For logging and debugging purposes
   private static final String TAG       = "TripMapView";

   private GoogleMap           mMap;
   BlogData                    mBlogData = null;

   private BitmapDescriptor    mIcon     = null;
   private BitmapDescriptor    mIconFirst= null;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.map_trip);

      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      /* Get the filename from an extra */
      String filename = extras.getString("TRIP");
      mBlogData = new BlogData();
      mBlogData.openBlog(filename);

      /* use one pointer for the first location, and another for the rest */
      mIconFirst = BitmapDescriptorFactory.fromResource(R.drawable.marker_green_go);
      mIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_blue_circle);

      setUpMapIfNeeded();
   }

   @Override
   public void onResume() {
      super.onResume();
      setUpMapIfNeeded();
   }

   /*
    * Called when the Activity is going into the background. Parts of the UI may
    * be visible, but the Activity is inactive.
    */
   @Override
   public void onPause() {
      // Save any current setting ? Nothing for now.
      super.onPause();
   }

   /*
    * Called when the Activity is restarted, even before it becomes visible.
    */
   @Override
   public void onStart() {
      super.onStart();
   }

   private void setUpMapIfNeeded() {
      // Do a null check to confirm that we have not already instantiated the
      // map.
      if (mMap == null) {
         // Try to obtain the map from the SupportMapFragment.
         mMap = ((SupportMapFragment) getSupportFragmentManager()
               .findFragmentById(R.id.map_trip)).getMap();
         // Check if we were successful in obtaining the map.
         if (mMap != null) {
            setUpMap();
         } else {
            Log.d(TAG, "could not setupMap - mMap is null");
         }
      }
   }

   /**
    * Add all the note locations as markers and draw lines between them.
    */
   private void setUpMap() {

      UiSettings uiSettings = mMap.getUiSettings();
      uiSettings.setZoomControlsEnabled(true);

      // Add lots of markers to the map.
      addMarkersToMap();
   }

   private void addMarkersToMap() {
      /*
      Maybe TODO: run this in a thread, yielding ever so often, otherwise, UI
      thread may be blocked for too long if adding 1000s of markers.
      Tested with 300-900 markers on a 2011 phone, and that seems to be fine.
      For large number of notes, saving the file is probably a bigger issuse than this.
      Use new Thread() and runOnUiThread or use AsyncThread for loading markers, if needed.
    */

      LatLngBounds.Builder bounds = new LatLngBounds.Builder();
      PolylineOptions polylineOptions = new PolylineOptions();
      BitmapDescriptor icon = mIconFirst; 

      for (int i = 0; i < mBlogData.getMaxBlogElements(); ++i) {
         BlogElement blog = mBlogData.getBlogElement(i);
         if ((blog.name == null) || (blog.location == null)
               || (blog.location.length() == 0) || (blog.name.length() == 0)) {
            continue;
         }
         String[] temp;
         LatLng latlng = null;
         try {
            temp = blog.location.split(",");
            if (temp.length < 2) {
               Log.d(TAG, "skipping invalid location string " + blog.location);
               continue;
            }
            float lon = Float.parseFloat(temp[0]);
            float lat = Float.parseFloat(temp[1]);
            latlng = new LatLng(lat, lon);
         } catch (NumberFormatException e) {
            Log.e(TAG, "Program error: location is not numbers "
                  + blog.location);
            continue;
         }

         bounds.include(latlng);
         polylineOptions.add(latlng);

         mMap.addMarker(new MarkerOptions().position(latlng).title(blog.name)
               .snippet(blog.description).icon(icon));
         icon = mIcon; // after the first icon is used, then use normal marker icon
      }

      mMap.addPolyline(polylineOptions.width(4).color(Color.BLUE));

      // fit map to points. Pan to see all markers in view.
      // Cannot zoom to bounds until the map has a size, so have to do this:
      Log.d(TAG, "addMarkers bounds = " + bounds.build());
      final View mapView = getSupportFragmentManager().findFragmentById(
            R.id.map_trip).getView();
      final LatLngBounds boundsB = bounds.build();
      if (mapView.getViewTreeObserver().isAlive()) {
         mapView.getViewTreeObserver().addOnGlobalLayoutListener(
               new OnGlobalLayoutListener() {
                  // @SuppressWarnings("deprecation") // We use the new method
                  // when supported
                  @SuppressLint("NewApi")
                  // We check which build version we are using.
                  @Override
                  public void onGlobalLayout() {
                     /*
                      * Looks like don't have access to Build.VERSION_CODES.JELLY_BEAN
                      * for this project since we are targeting older GINGERBREAD,
                      * so just use the old API.
                      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                      } else {
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                      }
                      */
                     mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                     mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                           boundsB, 50));
                  }
               });
      }
   }

      }
