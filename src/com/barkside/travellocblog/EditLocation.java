package com.barkside.travellocblog;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * EditLocation activity shows a screen that contains a map with a draggable marker
 * that allows the user to pick the exact location to use.
 *
 * Uses the abstract class LocationUpdates in case we need features such as getting
 * current location, geocoding current location. For now, not using those features, though.
 * Since the goal is to reduce battery usage, seems like there is no need to turn on
 * location updates when we already have an approximate location and user can now edit
 * the location by manually positioning the marker as needed in the location editor.
 *
 * Map code taken from
 * android-sdks/extras/google/google_play_services/samples/maps/
 *       src/com/example/mapdemo/MyLocationDemoActivity.java
 *
 */
public class EditLocation extends LocationUpdates implements OnMarkerDragListener
{
   // For logging and debugging purposes
   private static final String TAG = "EditLocation";

   // Initial map zoom level (2.0 to 21.0) for showing the location on the map
   public static final float INITIAL_MAP_ZOOM = 16.0f;
   // Initial map zoom for the case where we did not get a specified starting location
   // Shows a much larger area so user can easily pan the map to required region.
   public static final float INITIAL_MAP_ZOOM_FALLBACK = 6.0f;

   private LatLng mNewLatLng = null;
   private LatLng mOldLatLng = null;
   
   private GoogleMap mMap;
   private Marker mMapMarker;
   
   // Save/restore information keys
   public static final String BLOG_NAME_KEY = "EditLocationBlogName";
   public static final String POST_TITLE_KEY = "EditLocationPostTitle";
   public static final String POST_INDEX_KEY = "EditLocationPostName";
   public static final String INITIAL_LATLNG_KEY = "EditLocationInitialLatlng";
   public static final String CURRENT_LATLNG_KEY = "EditLocationCurrentLatlng";

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      // done by base class: setContentView(R.layout.edit_location);

      Log.d(TAG, "onCreate");

      Intent intent = getIntent();
      String action = intent.getAction();
      if (Intent.ACTION_EDIT.equals(action))
      {
         Bundle extras = intent.getExtras();
         String filename = extras.getString(BLOG_NAME_KEY);
         String postTitle = extras.getString(POST_TITLE_KEY);
         int blogIndex = extras.getInt(POST_INDEX_KEY);
         mOldLatLng = extras.getParcelable(INITIAL_LATLNG_KEY); // may be null
         mNewLatLng = mOldLatLng;
         
         // Restore UI state from the savedInstanceState.
         // This bundle is also been passed to onRestoreInstanceState, called after onCreate.
         if (savedInstanceState != null)
         {
            Log.d(TAG, "restore instance state");
            mNewLatLng = savedInstanceState.getParcelable(CURRENT_LATLNG_KEY);
         }
         
         if (mNewLatLng == null)
         {
            // This should rarely happen, caller always send in a starting location
            // But if it does, we have to display the map and the marker anyway
            mNewLatLng = stringToLatLng(getString(R.string.final_fallback_lnglat));
         }

         TextView tv = (TextView) findViewById(R.id.show_title);
         tv.setText(postTitle);
         
         // update ActionBar title with blog name
         // to support SDK 11 and older, need to use getSupportActionBar
         ActionBar actionBar = getSupportActionBar();
         actionBar.setTitle(Utils.blogToDisplayname(filename));

         int subtitleId = R.string.edit_location_name_format;

         // Display a subtitle with the 1-based index of the element
         // Warning: TODO:
         // This works fine if user follows normal path of activity screens,
         // but if user hits the home button and stacks up multiple travel blog activities
         // and edits the same trip in multiple activities, deletes locations, etc,
         // then this index may be wrong.
         String subtitle = String.format(getString(subtitleId), blogIndex + 1);
         actionBar.setSubtitle(subtitle);

         // Finally, show map
         setUpMapIfNeeded();
      }
   }

   @Override
   public void onResume()
   {
      super.onResume();
      Log.d(TAG, "onResume");
 
      if (Utils.playServicesAvailable(this)) {
    	 setUpMapIfNeeded();
      }
   }

   /*
    * Called when the Activity is going into the background. Parts of the UI
    * may be visible, but the Activity is inactive.
    */
   @Override
   public void onPause()
   {
      // Save any current setting ? Nothing for now.
      super.onPause();
   }

   /*
    * Called when the Activity is restarted, even before it becomes visible.
    */
   @Override
   public void onStart()
   {
      super.onStart();
   }

   /**
    * Update our state to use the position of the given marker.
    *
    * @param marker A Marker object containing the current location
    */
   private void useNewPosition(Marker marker)
   {
      if (marker == null)
         return;

      mNewLatLng = marker.getPosition();

      Context context = getApplicationContext();
      String lnglat = getLngLat(context, mNewLatLng);

      TextView tv = (TextView) findViewById(R.id.location);
      tv.setText(lnglat);
   }

   /**
    * All done, and return the new position as the result, or return cancel to keep old location.
    *
    * @param view The button object that was clicked
    */
   public void onSaveOrCancel(View view)
   {
      switch (view.getId()) {
      case R.id.save_button:

         useNewPosition(mMapMarker);

         Intent intent = new Intent();
         Bundle extras = new Bundle();
         
         extras.putParcelable(CURRENT_LATLNG_KEY, mNewLatLng);
         Log.d(TAG, "done with location edit " + mNewLatLng);
         intent.putExtras(extras);
         setResult(RESULT_OK, intent);
         break;
         
      case R.id.cancel_button:
         /*
          * Return a cancel status so caller does not use the edited location.
          */
         setResult(RESULT_CANCELED);
         break;
      }
      finish();
   }

   /**
    * When user clicks on the Location TextView, move the marker so that is it visible
    * on the map shown on the screen
    *
    * @param view
    */
   public void showLocation(View view)
   {
      LatLng latlng = mMapMarker.getPosition();
      // Move/animate the camera to that position.
      mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
   }

   private void setUpMapIfNeeded()
   {
      // Do a null check to confirm that we have not already instantiated the
      // map.
      if (mMap == null)
      {
         // Try to obtain the map from the SupportMapFragment.
         mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(
                  R.id.map_location)).getMap();
         // Check if we were successful in obtaining the map.
         if (mMap != null)
         {

            // Note: not using map's my location layer for many reasons.
            // Mainly: no need to show it since we have the map for editing and we
            // want to reduce battery use.
            // In any case, how should the UI for the map's my location button should work?
            // We don't want user to mistakenly hit that button when editing since
            // the app may be used to edit a location different from the original location.
            // By enabling it below, it will show a small blue marker at current location, but
            // mMapMarker is not affected. To move mMapMarker to current location requires
            // implementing LocationListener here.
            // One option would be to show 2 markers - one draggable, and second one fixed at
            // original location. And have 2 buttons to move the draggable to current location
            // or to original location.
            //
            // mMap.setMyLocationEnabled(true); // do not enable (at least until designed fully)

            // Set listeners for marker events. See the bottom of this class for their behavior.
            mMap.setOnMarkerDragListener(this);

            UiSettings uis = mMap.getUiSettings();
            uis.setZoomControlsEnabled(true);

            /**
             * Move the marker to the current location. Creates marker if no marker
             * added yet.
             * NOTE: should only be called to initialize the map, after that,
             * user moves the marker manually as desired. Otherwise, if map is moved too often,
             * looks startling in the user interface.
             */
            if (mMapMarker == null)
            {
               mMapMarker = mMap.addMarker(new MarkerOptions().position(mNewLatLng).draggable(true));
            }

            Log.d(TAG, "setup map: setting marker " + mNewLatLng);

            mMapMarker.setPosition(mNewLatLng);
            // Move/animate the camera to that position.
            // animateCamera takes up time, so just use moveCamera which seems better here.
            float zoom = (mOldLatLng == null) ? INITIAL_MAP_ZOOM_FALLBACK : INITIAL_MAP_ZOOM;
            Log.d(TAG, "Using zoom " + zoom);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mNewLatLng, zoom));
            
            useNewPosition(mMapMarker);
         }
      }
   }

   /**
    * We need to survive a device orientation change. Android will completely destroy
    * and recreate this activity.
    * If we don't remember mNewLatLng for example, we may restart this activity and
    * forget that the user had already moved it to a new location and mOldLatLng is not
    * the right initial value.
    */
   
   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
     super.onSaveInstanceState(savedInstanceState);
     Log.d(TAG, "save instance state");
     // Save UI state changes to the savedInstanceState.
     // This bundle will be passed to onCreate if the process is
     // killed and restarted.
     savedInstanceState.putParcelable(CURRENT_LATLNG_KEY, mNewLatLng);
   }


   @Override
   public void onMarkerDrag(Marker marker)
   {
      useNewPosition(marker);
   }

   @Override
   public void onMarkerDragEnd(Marker marker)
   {
      // Nothing to do

   }

   @Override
   public void onMarkerDragStart(Marker marker)
   {
      // Nothing to do

   }

   /**
    * Return the layout to inflate, to the abstract base class.
    *
    * @return a layout Id.
    */
   @Override
   protected int getLayoutResourceId()
   {
      return R.layout.edit_location;
   }

   /**
    * Return a progress indicator to the abstract base class.
    * For now, not using any progress indicator.
    *
    * @return the ProgressBar to update, or null if none.
    */
   @Override
   protected ProgressBar getProgressBar()
   {
      return null;
   }
}
