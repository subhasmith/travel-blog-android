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
   
   private Boolean mIsNewBlog = false;
   
   // We have multiple sources of current location data.
   // The priority is: mEditedLatLng then mBestLocation then mOldLatLng.
   
   // This one comes from LocationListener - can be from a getLastLocation call, or from
   // onLocationChanged event.
   private Location mBestLocation = null;
   
   // This current location comes from the EditLocation activity, if the user clicks
   // on location and manually edits it on the map.
   private LatLng mEditedLatLng = null;

   // This one comes from the calling activity - the old blog location when editing it,
   // or a default location to use (if needed) when creating a new blog.
   // When creating a New Post, if user has turned off GPS or location services, we need
   // to guess at some location to use when the user clicks to edit location.
   private LatLng mOldLatLng = null;
   
   private String mTitle = null;
   
   // Calling activity passes this data as starting values.
   private String mOldLngLat = null;
   private String mOldTime = null;
   
   // Settings value
   private int mUpdatesDuration = 0; // 0 mean no auto-off
   
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

      mBestLocation = null;
      mEditedLatLng = null;
      mOldLatLng = null;
      
      // Restore UI state from the savedInstanceState.
      // This bundle is also been passed to onRestoreInstanceState, called after onCreate.
      if (savedInstanceState != null)
      {
         Log.d(TAG, "restore instance state");
         mEditedLatLng = savedInstanceState.getParcelable("mEditedLatLng");
         // No need to restore (or save) the title and description TextView fields,
         // since the system does that automatically.
      }
      
      Intent intent = getIntent();
      String action = intent.getAction();
      Bundle extras = intent.getExtras();
      if (action.equals("com.barkside.travellocblog.NEW_BLOG_ENTRY"))
      {
         mIsNewBlog = true;

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
         
         mOldLngLat = extras.getString("BLOG_LOCATION");
      }
      else if (action.equals("com.barkside.travellocblog.EDIT_BLOG_ENTRY"))
      {
         // Requested to edit: set that state, and the data being edited.
         mIsNewBlog = false;
         mTitle = extras.getString("BLOG_NAME");
         String descr = extras.getString("BLOG_DESCRIPTION");
         mOldTime = extras.getString("BLOG_TIMESTAMP");
         mOldLngLat = extras.getString("BLOG_LOCATION");

         EditText tv = (EditText) findViewById(R.id.edit_title);
         tv.setText(mTitle);
         tv.setSelection(tv.getText().length());
         tv = (EditText) findViewById(R.id.edit_description);
         tv.setText(descr);
         tv.setSelection(tv.getText().length());
      }

      mOldLatLng = stringToLatLng(mOldLngLat);
      Log.d(TAG, "Starting, created mOldLatLng " + mOldLatLng);
      
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      
      // Read in settings for location updates duration
      SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
      // http://code.google.com/p/android/issues/detail?id=2096
      // Expand ListPreference to support alternate array types
      // so can't use int for duration, have to convert from string!
      String selected = sharedPref.getString(SettingsActivity.LOCATION_DURATION_KEY,
            Integer.toString(LOC_UPDATE_DURATION));
      mUpdatesDuration = Integer.parseInt(selected);

      // Enable location updates if we have not manually edited the location
      this.checkEnableLocationUpdates();
      
      // After all data is setup, including starting location updates, display the location
      displayLocation();
   }
   
   /**
    * Return a LatLng object that represents the current location.
    * This function looks at all the places we have stashed location, and returns the
    * correct LatLng based on the priority order of all the location data objects.
    * In rare cases (such as when entering first new entry in a new trip and there is
    * no location fix yet) this can return null.
    */
   private LatLng getCurrentLatLng()
   {
      LatLng latlng = null;

      // follow priority order of location to use
      if (mEditedLatLng != null)
      {
         latlng = mEditedLatLng;
      }
      else if (mBestLocation != null)
      {
         latlng = new LatLng(mBestLocation.getLatitude(), mBestLocation.getLongitude());
      }
      else
      {
         latlng = mOldLatLng; // in very rare cases, this may be null
      }
      return latlng;
   }
   /**
    * Update the textview showing the location with current location data.
    */
   private void displayLocation()
      {
      TextView tv = (TextView) findViewById(R.id.location);
      
      // Check if new blog and displaying first message before any LocationUpdates location
      // data received.
      // Display a "starting location fix" message. Most likely  the user will never
      // see this because as soon as we get a onConnected message, we will display last
      // known location. The onConnected message comes in pretty fast when there was
      // a previous location on the phone. If user turns off GPS, this is not true -
      // it may never get a onConnected event and this message will stay until timer expires.
      if (mIsNewBlog && mBestLocation == null && updatesRequested())
      {
         tv.setText(R.string.finding_location); 
         return;
      }
      
      Context context = getApplicationContext();
      LatLng latlng = getCurrentLatLng();
      String displayLoc = latlng != null ? getLngLat(context, latlng)
            : getString(R.string.found_no_location);
      
      // Explanation text to display after the location, or accuracy display
      // Display accuracy, if we have it and no manual edits have occurred.
      String suffix = "";
      if (mIsNewBlog && mEditedLatLng == null)
      {
         if (mBestLocation == null)
         {
            if (mOldLatLng != null)
               suffix = "\n[previous post]"; 
         }
         else
         {
            String accuracy = getAccuracy(context, mBestLocation);
            suffix = "\n" + accuracy; 
         }
      }
      
      tv.setText(displayLoc + suffix);
   }

   /*
    * User has clicked on the location, so we start a new activity to edit it on a map. 
    */
   public void editLocation(View view)
   {
      Log.d(TAG, "start editLocation activity");

      // Since we going to manually edit the location, turn off the location updates.
      stopPeriodicUpdates();
      
      // If applicable, display 'No location' warning
      warnNoLocationToast();
      
      // Start new edit location activity
      Intent i = new Intent(this, EditLocation.class);
      Bundle b = new Bundle();
      b.putString("BLOG_NAME", mTitle);
      LatLng latlng = getCurrentLatLng();
      Log.d(TAG, "calling editLocation with " + latlng);
      if (latlng != null)
      {
         b.putParcelable("BLOG_LATLNG", latlng);         
      }
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
         String locString = "";
         // Always save using the current LatLng, should be available nearly always
         LatLng latlng = getCurrentLatLng();
         if(latlng == null)
         {
            Toast.makeText(this, R.string.location_missing,Toast.LENGTH_LONG).show();
            // Don't finish this task, stay on this same screen
            return;
         }
         
         // If applicable, display 'No location, using default' warning
         warnNoLocationToast();
         
         locString = latlng.longitude + "," + latlng.latitude + ",0";

         Log.d(TAG, "Saving blog location " + locString);
         // Either title or description must be provided, if not, stay on this activity screen
         EditText et = (EditText) findViewById(R.id.edit_title);
         String title = et.getText().toString();
         
         et = (EditText) findViewById(R.id.edit_description);
         String desc = et.getText().toString();      

         if(title.trim().length() == 0 && desc.trim().length() == 0)
         {
            Toast.makeText(this, R.string.title_desc_missing,Toast.LENGTH_LONG).show();
            // Don't finish this task, stay on this same screen
            return;
         }

         Intent intent = new Intent();
         Bundle extras = new Bundle();
         
         Log.d(TAG, "Edit done saving title (" + title + ") location: " + locString);
         extras.putString("BLOG_NAME", title);
         extras.putString("BLOG_DESCRIPTION", desc);
         extras.putString("BLOG_LOCATION", locString);
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
      
      // As soon as we connect to Google Play Services, we can get the best known
      // last location, which is good enough to start with. We don't want to wait
      // for an onLocationChanged event, which might take a long time to fire if
      // user has only turned on GPS and not WiFi/Cell location services.
      // As long as user has turned on some Location Services on their phone, this
      // callback comes in pretty fast. Though if the user has turned off Network location
      // services, this can be null since using only GPS can take a long time for a fix.
      // We only need to get location if we are editing a new blog.
      if (mIsNewBlog)
      {
         Location loc = getLastLocation();
         if (loc != null)
         {
            mBestLocation = loc;
            displayLocation();
            Log.d(TAG, "onConnected got loc");
         }
         else
         {
            // According to the doc, this is rare, should always get a non-null getLastLocation
            // That is actually only true if this or some other app got a location fix recently...
            Log.w(TAG, "onConnected got null loc");
         }
      }
   }
     
   /**
    * Super class is setup to call this function when location has changed.
    * 
    * @param location The updated location.
    */
   @Override
   public void onLocationChanged(Location location) {
      super.onLocationChanged(location);
      Log.d(TAG, "onLocationChanged");
      if(LocationUpdates.isBetterLocation(location, mBestLocation))
      {
         mBestLocation = location;
         displayLocation();
      }
   }
   
   /**
    * If we have not received a location update and we needed one at this time,
    * warn the user using a Toast message.
    */
   private void warnNoLocationToast()
   {
      if (mIsNewBlog && mBestLocation == null && mEditedLatLng == null)
      {
         if (mOldLatLng == null)
            Toast.makeText(this, R.string.no_location_fix_no_default,
                  Toast.LENGTH_LONG).show();
         else
            Toast.makeText(this, R.string.no_location_fix_use_previous,
                  Toast.LENGTH_LONG).show();
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
            mEditedLatLng = latlng;
            Log.d(TAG, "Got location from EditLocation exit: " + latlng);
         }
         else if (resultCode == RESULT_CANCELED)
         {
            // If we were listening for location updates and don't have any
            // previous edited value, start listening again. Note that this will restart
            // the timer, but that is fine - if the user interrupted a timer, went into
            // EditLocation, hit cancel, then we do need to listen for updates again.
            this.checkEnableLocationUpdates();
         }
         displayLocation();
         break;

      // If any other request code was received
      default:
         // Report that this Activity received an unknown requestCode
         Log.d(TAG, getString(R.string.unknown_activity_request_code, requestCode));

         break;
      }
   }
   
   /**
    * Internal function to start the location updates.
    * Updates are started for the duration requested in the settings.
    */
   private void checkEnableLocationUpdates()
   {
      if (mEditedLatLng == null && mIsNewBlog)
      {
         super.enableLocationUpdates(mUpdatesDuration);
      }
   }
   
   /**
    * Stop periodic updates as a result of timer firing.
    * This can be also defined in the subclass to display appropriate messages if location
    * has still not been found.
    */
   @Override
   protected void timerCancelUpdates() {
      super.timerCancelUpdates();
      
      // Since we are no longer listening to updates, set the location to the
      // current location fix. This is necessary to avoid restarting location
      // updates on a screen orientation change which calls onCreate/onStart again.
      if (mBestLocation != null)
      {
         mEditedLatLng = new LatLng(mBestLocation.getLatitude(), mBestLocation.getLongitude());
      }
      
      // Update location string
      displayLocation();
      
      // If applicable, display 'No location, using default' warning
      warnNoLocationToast();
   }
   
   /**
    * We need to survive a device orientation change. Android will completely destroy
    * and recreate this activity.
    * If we don't remember mEditedLatLng for example, we may restart the location
    * updates and lose the fact that user had actually entered a location.
    * EditText values are saved by the system, so just need to save mEditedLatLng and such.
    */
   
   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
     super.onSaveInstanceState(savedInstanceState);
     Log.d(TAG, "save instance state");
     // Save UI state changes to the savedInstanceState.
     // This bundle will be passed to onCreate if the process is
     // killed and restarted.
     savedInstanceState.putParcelable("mEditedLatLng", mEditedLatLng);
   }

}
