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

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

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

/**
 * Fragment to display all the blog entry locations on a map.
 * 
 * Map code taken from
 * android-sdks/extras/google/google_play_services/samples/maps/
 * src/com/example/mapdemo/MarkerDemo java file.
 * 
 * Note: use getChildFragmentManager() to add SupportMapFragment not through xml inflate.
 * http://developer.android.com/about/versions/android-4.2.html#NestedFragments
 * 
 */
public class MapTripFragment extends SupportMapFragment {
   // For logging and debugging purposes
   private static final String TAG       = "MapTripFragment";

   private GoogleMap           mMap;

   private BitmapDescriptor    mIcon     = null;
   private BitmapDescriptor    mIconFirst= null;
   
   // mBlogMgr is set by container activity in its onCreate
   private BlogDataManager     mBlogMgr  = null;

   /**
    * Create a new instance of MyFragment that will be initialized
    * with the given arguments.
    */
   public static MapTripFragment newInstance() {
      MapTripFragment f = new MapTripFragment();
      /*
       * Not passing args. Uri may change in the middle of the activity if
       * user opens a new trip, so just use the mBlogMgr to always get
       * correct blog name.
      Bundle b = new Bundle();
      b.putParcelable("BLOG_URI", uri);
      f.setArguments(b);
       */
      return f;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      // Note: this onCreate may be called before the parent activities
      // onCreate after a device orientation change. So the Blog uri may be incorrect.
      // Container activity should make sure to call useBlogMgr to set it correctly.
      
      /* use one pointer for the first location, and another for the rest */
      mIconFirst = BitmapDescriptorFactory.fromResource(R.drawable.marker_green_go);
      mIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_blue_circle);
      
      mMap = null;
   }

   public void useBlogMgr(BlogDataManager blogMgr) {
      mBlogMgr = blogMgr;
   }
   
   // Note that fragment onResume is called after the parent activity onResume
   // Do not depend on order of onResume calls.
   @Override
   public void onResume() {
      super.onResume();
      
      Uri uri = mBlogMgr == null ? null : mBlogMgr.uri();
      // Display all the markers on the map. The GoogleMap object remembers all the
      // markers on a pause and resume, so we only need to this once.
      if (mMap == null && uri != null) {
         // Always open the file:
         // We may come here from another Travel Blog activity screen through the
         // Android back stack, so the current trip data pointed to mBlogMgr may have
         // changed.
         mBlogMgr.openBlog(getActivity(), uri, R.string.open_failed_one_file);
         displayTrip();         
      }
   }
   
   public void displayTrip() {
      
      String blogname = mBlogMgr.blogname();
      if (blogname.equals("")) return;

      if (mMap == null) {
         // Try to obtain the map from the SupportMapFragment.
         mMap = super.getMap();
         // Check if we were successful in obtaining the map.
         if (mMap != null) {
            UiSettings uiSettings = mMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(true);
         }
      } else {
         // We may have displayed a trip previously, and now need to display
         // another trip. Place the markers from the new trip on the map. 
         // Since the trip may be empty, reset the map to start with.
         LatLng latlng = LocationUpdates.stringToLatLng(
               getString(R.string.final_fallback_lnglat));
         mMap.clear();
         mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 6.0f));
      }
      addMarkersToMap();
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
   
   /**
    * Add all the note locations as markers and draw lines between them.
    */
   private void addMarkersToMap() {
      /*
      Maybe TODO: run this in a thread, yielding ever so often, otherwise, UI
      thread may be blocked for too long if adding 1000+ markers.
      Tested with 300-1000 markers on a 2011 phone, and that seems to be fine.
      For large number of notes, saving the file is probably a bigger issue than this.
      Use new Thread() and runOnUiThread or use AsyncThread for loading markers, if needed.
    */

      int count = mBlogMgr.getMaxBlogElements();
      Log.d(TAG, "Add markers to map, count: " + count);
      if (count <= 0 || mMap == null) {
         return;
      }
      
      LatLngBounds.Builder bounds = new LatLngBounds.Builder();
      PolylineOptions polylineOptions = new PolylineOptions();
      BitmapDescriptor icon = mIconFirst; 

      for (int i = 0; i < mBlogMgr.getMaxBlogElements(); ++i) {
         BlogElement blog = mBlogMgr.getBlogElement(i);
         if ((blog.title == null) || (blog.location == null)
               || (blog.location.length() == 0) || (blog.title.length() == 0)) {
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

         mMap.addMarker(new MarkerOptions().position(latlng).title(blog.title)
               .snippet(blog.description).icon(icon));
         icon = mIcon; // after the first icon is used, then use normal marker icon
      }

      mMap.addPolyline(polylineOptions.width(4).color(Color.BLUE));

      // fit map to points. Pan to see all markers in view.
      // Log.d(TAG, "addMarkers bounds = " + bounds.build());
      // Cannot zoom to bounds until the map has a size, so have to do this:
      
      final View mapView = getView();
      final LatLngBounds boundsB = bounds.build();
      if (mapView.getViewTreeObserver().isAlive()) {
         mapView.getViewTreeObserver().addOnGlobalLayoutListener(
               new OnGlobalLayoutListener() {
                  @SuppressWarnings("deprecation") // We use the new method when supported
                  @SuppressLint("NewApi")
                  // We check which build version we are using.
                  @Override
                  public void onGlobalLayout() {
                     /*
                      * Looks like don't have access to Build.VERSION_CODES.JELLY_BEAN
                      * for this project since we are targeting older GINGERBREAD,
                      * so just use the old API.
                      */
                     if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                      } else {
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                      }
                     mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                           boundsB, 50));
                  }
               });
      }
   }

   // Parent activity may need to know name of the uri being displayed and display
   // other such info about the displayed trip. Pass the BlogDataManager for it to
   // query it for needed information.
   public BlogDataManager blogMgr() { return mBlogMgr; }
   
}
