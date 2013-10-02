package com.barkside.travellocblog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to edit a note (a blog entry) that contains title, description,
 * and longitude/latitude location.
 *
 * Uses the abstract class LocationUpdates for getting the current location.
 *
 */
public class EditBlogElement extends LocationUpdates
{
   // Constants
   private static final String TAG = "EditBlogElement";
   
   // Default amount of time to listen to location updates, in seconds
   // this is now stored in shared preferences for each user.
   private static final int LOC_UPDATE_DURATION = 15;
   
   private Boolean isNewBlog = false;
   
   // We have two sources of current position. One comes from the phone location listener,
   // and it returns a Location object. The second comes from editing a saved note (blog entry)
   // or from the EditLocation activity, both of which return LatLng data.
   private Location mBestLocation = null;
   private LatLng mNewLatLng = null;
   
   private String mNoteName = null;
   
   // Previously saved values, if editing an existing note entry.
   private String mOldLngLat = null;
   private String mOldTime = null;
   
   private final int EDIT_LOCATION_REQUEST = 100;
   
   /**
    * Return the layout to inflate, to the abstract base class.
    *
    * @return a layout Id.
    */
   @Override
   protected int getLayoutResourceId()
   {
      return R.layout.edit_blog;
   }

   /**
    * Return a progress indicator to the abstract base class.
    * Not using any progress indicator.
    *
    * @return the ProgressBar to update, or null if none.
    */
   @Override
   protected ProgressBar getProgressBar()
   {
      return null;
   }

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      // setContentView handled by base class, using getLayoutResourceId()

      Intent intent = getIntent();
      String action = intent.getAction();
      if (action.equals("com.barkside.travellocblog.NEW_BLOG_ENTRY"))
      {
         isNewBlog = true;

         // Create mOldTime note timestamp to store in KML file
         Date dateNow = new Date();
         // KML timestamp, needs to be in UTC
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", Locale.US);
         sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
         CharSequence charSeq = sdf.format(dateNow);

         mOldTime = charSeq.toString(); // will be written to KML file as timestamp
         
         // Check if we should display current date as first line in description
         SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
         String key = SettingsActivity.DEFAULT_DESC_ON_KEY;
         boolean default_desc_on = sharedPref.getBoolean(key, true);
         if (default_desc_on) {
            // User-friendly date string, in local timezone
            java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(
                  java.text.DateFormat.LONG, // dateStyle
                  java.text.DateFormat.SHORT, // timeStyle
                  Locale.US);
            String dateNowString = df.format(dateNow);
            EditText tv = (EditText) findViewById(R.id.edit_description);
            tv.setText(dateNowString + "\n");
            tv.setSelection(tv.getText().length());
         }
         
         TextView tvl = (TextView) findViewById(R.id.location);
         tvl.setText("Finding Location...");
      }
      else if (action.equals("com.barkside.travellocblog.EDIT_BLOG_ENTRY"))
      {
         // Requested to edit: set that state, and the data being edited.
         isNewBlog = false;
         Bundle extras = intent.getExtras();
         mNoteName = extras.getString("BLOG_NAME");
         String descr = extras.getString("BLOG_DESCRIPTION");
         mOldTime = extras.getString("BLOG_TIMESTAMP");
         mOldLngLat = extras.getString("BLOG_LOCATION");
         mNewLatLng = stringToLatLng(mOldLngLat);

         EditText tv = (EditText) findViewById(R.id.edit_name);
         tv.setText(mNoteName);
         tv.setSelection(tv.getText().length());
         tv = (EditText) findViewById(R.id.edit_description);
         tv.setText(descr);
         tv.setSelection(tv.getText().length());

         TextView tvl = (TextView) findViewById(R.id.location);
         String lnglat = getLngLat(this, mNewLatLng);
         tvl.setText(lnglat);
      }
      
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      
      // Turn on a timer event to fire after a while, to turn off any updates. Can't do this
      // in a onStart or onResume because those get called any time this activity is shown such
      // as when we come back to this activity after completion of EditLocation activity.
      // Since we need to start this timer only after an onConnected event, will need to create
      // a mNeedLocationUpdates boolean in here and set it off when updates are to be turned off.
      // All that seems unnecessary, so just a simple call below works fine.
      if (isNewBlog) {
         SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
         // http://code.google.com/p/android/issues/detail?id=2096
         // Expand ListPreference to support alternate array types
         // so can't use int for duration, have to convert from string!
         String selected = sharedPref.getString(SettingsActivity.LOCATION_DURATION_KEY,
               Integer.toString(LOC_UPDATE_DURATION));
         int duration = Integer.parseInt(selected);
         super.enableLocationUpdates(duration);
      }
   }
   
   /**
    * Update the note entry to use the given location.
    * It is assumed that the given loc is to be considered the best location
    * from this time on.
    *
    * @param loc A Location object containing the current location.
    *        null is allowed - it means we don't have a Location object.
    * @param latlng A LatLng object containing the current position.
    *        Only used if loc is null.
    * @return void
    */
   private void updateBestLocation(Location loc, LatLng latlng)
   {
      if(loc == null && latlng == null) {
         Log.w(TAG, "updateBestLocation got both null args");
         return;
      }
      
      mBestLocation = loc; // may be null
      if (mBestLocation != null) {
         Log.d(TAG, "updateBestLocation got new mBestLocation");
         mNewLatLng = new LatLng(mBestLocation.getLatitude(), mBestLocation.getLongitude());
      } else {
         mNewLatLng = latlng;
      }
      
      TextView tv = (TextView) findViewById(R.id.location);
      Context context = getApplicationContext();
      String lnglat = getLngLat(context, mNewLatLng);
      
      if (mBestLocation != null) {
         // We have a Location object and can display Accuracy value
         String accuracy = getAccuracy(context, loc);
         lnglat += " Accuracy: " + accuracy;
      }
      
      tv.setText(lnglat);
   }

   /*
    * User has clicked on the location, so we start a new activity to edit it on a map. 
    */
   public void editLocation(View view)
   {
      if (mNewLatLng == null)
      {
         Log.d(TAG, "start editLocation activity: mNewLatLng is null! ");
         return;
      }
      Log.d(TAG, "start editLocation activity");

      // Since we going to manually edit the location, turn off the location updates.
      stopPeriodicUpdates();
      
      Intent i = new Intent(this, EditLocation.class);
      Bundle b = new Bundle();
      b.putString("BLOG_NAME", mNoteName);
      b.putString("BLOG_TIMESTAMP", mOldTime);
      b.putParcelable("BLOG_LATLNG", mNewLatLng);
      i.putExtras(b);
      i.setAction(Intent.ACTION_EDIT);
      startActivityForResult(i, EDIT_LOCATION_REQUEST);
   }
   
   /**
    * All done, and save the note, or cancel the edit.
    *
    * @param view The button object that was clicked
    */
   public void onSaveOrCancel(View view)
   {
      switch (view.getId()) {
      case R.id.save_button:
         String location = "";
         // Always save using the NewLatLng, should always be available
         if (mNewLatLng != null)
         {
            location = mNewLatLng.longitude + "," + mNewLatLng.latitude + ",0";
         }
         else
         {
            Log.w(TAG, "mNewLatLng is null when saving blog!");
         }

         Intent intent = new Intent();
         Bundle extras = new Bundle();
         EditText et = (EditText) findViewById(R.id.show_name);
         String str = et.getText().toString().trim();
         if(str.length() == 0)
         {
            Toast toast = Toast.makeText(this, "Failed: enter a valid name", Toast.LENGTH_SHORT);
            toast.show();
            return;
         }
         Log.d(TAG, "Edit done (save) for " + str + " location " + location);
         extras.putString("BLOG_NAME", str);
         et = (EditText) findViewById(R.id.edit_description);
         str = et.getText().toString();      
         extras.putString("BLOG_DESCRIPTION", str);
         extras.putString("BLOG_LOCATION", location);
         extras.putString("BLOG_TIMESTAMP", mOldTime);
         intent.putExtras(extras);
         setResult(RESULT_OK, intent);
         break;
         
      case R.id.cancel_button:
         setResult(RESULT_CANCELED);
         break;
      }
      
      // Since we going to exit, turn off the location updates.
      stopPeriodicUpdates();
      
      finish();
   }
   
   @Override
   public void onConnected(Bundle bundle)
   {
      super.onConnected(bundle);
      Log.d(TAG, "onConnected");
   }
   
   @Override
   public void onStart()
   {
      Log.d(TAG, "onStart");
      super.onStart();
   }
   
   @Override
   public void onStop()
   {
      Log.d(TAG, "onStop");
      super.onStop();
   }
   
   /**
    * Base class is setup to call this function when location has changed.
    * 
    * @param location The updated location.
    */
   @Override
   public void onLocationChanged(Location location) {
      super.onLocationChanged(location);
      
      if(LocationUpdates.isBetterLocation(location, mBestLocation))
      {
         updateBestLocation(location, null);
      }
   }
   
   /*
    * Handle results returned to this Activity by other Activities started with
    * startActivityForResult(). In particular, the method onConnectionFailed()
    * in LocationUpdateRemover and LocationUpdateRequester may call
    * startResolutionForResult() to start an Activity that handles Google Play
    * services problems. The result of this call returns here, to
    * onActivityResult.
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {

      // Choose what to do based on the request code
      switch (requestCode)
      {

      case EDIT_LOCATION_REQUEST:
         if (resultCode == RESULT_OK)
         {
            Bundle extras = intent.getExtras();
            LatLng latlng = extras.getParcelable("BLOG_LATLNG");
            updateBestLocation(null, latlng);
            // TODO: timestamp not currently updated anywhere
            // mOldTime = extras.getString("BLOG_TIMESTAMP");
         }
         break;

      // If any other request code was received
      default:
         // Report that this Activity received an unknown requestCode
         Log.d(TAG, getString(R.string.unknown_activity_request_code, requestCode));

         break;
      }
   }

}
