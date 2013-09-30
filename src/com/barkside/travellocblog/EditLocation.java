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
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

   private LatLng mNewLatLng = null;
   private LatLng mOldLatLng = null;
   
   private String mOldTime = null;
   private String mNoteName = null; // Blog Element is a Note, and the Name of that field.

   private GoogleMap mMap;
   private Marker mMapMarker;

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
         mNoteName = extras.getString("BLOG_NAME");
         mOldTime = extras.getString("BLOG_TIMESTAMP");
         mOldLatLng = extras.getParcelable("BLOG_LATLNG");
         mNewLatLng = mOldLatLng;

         TextView tv = (TextView) findViewById(R.id.show_name);
         tv.setText(mNoteName);
         
         setUpMapIfNeeded();
      }
   }

   @Override
   public void onResume()
   {
      super.onResume();
      Log.d(TAG, "onResume");
 
      if (super.servicesConnected()) {
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

         if (mNewLatLng == null)
         {
            setResult(RESULT_CANCELED);
            // Internal program error?
            Toast toast = Toast.makeText(this, "Failed: got no location", Toast.LENGTH_SHORT);
            toast.show();
            break;
         }

         useNewPosition(mMapMarker);

         Intent intent = new Intent();
         Bundle extras = new Bundle();
         extras.putString("BLOG_NAME", mNoteName);
         extras.putParcelable("BLOG_LATLNG", mNewLatLng);
         Log.d(TAG, "done with location edit " + mNewLatLng);
         extras.putString("BLOG_TIMESTAMP", mOldTime);
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
             * Move the marker to the current mBestLocation. Creates marker if no marker
             * added yet.
             * NOTE: should only be called to initialize the map, after that,
             * user moves the marker manually as desired. Otherwise, if map is moved too often,
             * looks startling in the user interface.
             */
            if (mMapMarker == null)
            {
               mMapMarker = mMap.addMarker(new MarkerOptions().position(mOldLatLng).draggable(true));
            }

            Log.d(TAG, "setup map: setting marker " + mOldLatLng);

            mMapMarker.setPosition(mOldLatLng);
            // Move/animate the camera to that position.
            // animateCamera takes up time, so just use moveCamera which seems better here.
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOldLatLng, INITIAL_MAP_ZOOM));
            
            useNewPosition(mMapMarker);
         }
      }
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