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
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Toast;

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
   private Uri                 mTripUri;

   private BitmapDescriptor    mIcon     = null;
   private BitmapDescriptor    mIconFirst= null;
   
   /**
    * Create a new instance of MyFragment that will be initialized
    * with the given arguments.
    */
   static MapTripFragment newInstance(Uri uri) {
      MapTripFragment f = new MapTripFragment();
      Bundle b = new Bundle();
      b.putParcelable("BLOG_URI", uri);
      f.setArguments(b);
      Log.d(TAG, "newInstance " + f);
      return f;
   }


   @Override
   public void onCreate(Bundle savedInstanceState) {
      Log.d(TAG, "onCreate ");
      super.onCreate(savedInstanceState);
      
      mTripUri = null;
      final Bundle args = getArguments();
      if (args != null) {
          mTripUri = args.getParcelable("BLOG_URI");
      }

      Log.d(TAG, "got uri " + mTripUri);
      /* use one pointer for the first location, and another for the rest */
      mIconFirst = BitmapDescriptorFactory.fromResource(R.drawable.marker_green_go);
      mIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_blue_circle);
   }

   @Override
   public void onResume() {
      super.onResume();
      
      // Display all the markers on the map. The GoogleMap object remembers all the
      // markers on a pause and resume, so we only need to this once.
      if (mMap == null) {
         openTrip(mTripUri);         
      }
      // We may come here from another Travel Blog activity screen through the
      // Android back stack, so the current trip pointed to mBlogMgr may have
      // changed. But we don't need care, since we have already displayed the
      // map markers, and GoogleMap takes care of onResume redrawing.
   }
   
   // Open given blog, and display it on the map.
   // User may select specific trip files from the menu, we will open them and display
   // the locations on the map. This is a view-only, secondary activity operation therefore
   // we don't save any newly opened trip file to the Preferences. This means that the
   // back button will behave sensibly - when user returns to any previous Travel Blog
   // activity screen, that screen will continue to show the same trip file it was displaying
   // and not be affected by the trip files opened in this Map Trip activity.
   public boolean openTrip(Uri uri) {
      
      if (uri == null) return false;
      
      BlogDataManager blogMgr = BlogDataManager.getInstance();

      Context context = getActivity();
      boolean opened = false;
      opened = blogMgr.openBlog(context, uri);

      if (opened) {
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
         Log.d(TAG, "mMap is " + mMap);
         if (mMap != null) {
            Log.d(TAG, "Map trip " + uri);
            addMarkersToMap(blogMgr);
         }
      } else {
         // Failed to open requested file.
         Toast.makeText(context, R.string.file_open_failed,
               Toast.LENGTH_SHORT).show();
      }
      return opened;
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
   private void addMarkersToMap(BlogDataManager blogMgr) {
      /*
      Maybe TODO: run this in a thread, yielding ever so often, otherwise, UI
      thread may be blocked for too long if adding 1000+ markers.
      Tested with 300-1000 markers on a 2011 phone, and that seems to be fine.
      For large number of notes, saving the file is probably a bigger issuse than this.
      Use new Thread() and runOnUiThread or use AsyncThread for loading markers, if needed.
    */

      int count = blogMgr.getMaxBlogElements();
      Log.d(TAG, "Add markers to map, count: " + count);
      if (count <= 0) {
         return;
      }
      
      LatLngBounds.Builder bounds = new LatLngBounds.Builder();
      PolylineOptions polylineOptions = new PolylineOptions();
      BitmapDescriptor icon = mIconFirst; 

      for (int i = 0; i < blogMgr.getMaxBlogElements(); ++i) {
         BlogElement blog = blogMgr.getBlogElement(i);
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
      // Cannot zoom to bounds until the map has a size, so have to do this:
      Log.d(TAG, "addMarkers bounds = " + bounds.build());
      
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

}
