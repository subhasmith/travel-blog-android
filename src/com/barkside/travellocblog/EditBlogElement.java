package com.barkside.travellocblog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
 * This can be a standalone activity - it creates and saves entries and handles
 * all tasks completely, there is no need for calling activity to save blogs, for example.
 * This activity is started from two places:
 * 1: The main activity
 * 2: Any user installed widgets with the New Post functionality. 
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
   private int mUpdatesDuration = 0; // 0 means no auto-off
   
   private BlogDataManager mBlogData = BlogDataManager.getInstance();

   // Internal request code for sub-activity
   private final int EDIT_LOCATION_REQUEST = 100;

   // Opened blog full file name
   private String mBlogname;

   // Index of blog element/post being edited/created
   private int mEditItem;

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
      
      // We only expect an ACTION_INSERT_OR_EDIT here
      setResult(RESULT_CANCELED);
      Intent intent = getIntent();
      String action = intent.getAction();
      if (!Intent.ACTION_INSERT_OR_EDIT.equals(action))
      {
         Log.e(TAG, "bad action, finishing " + action);
         finish();
      }

      mBestLocation = null;
      mEditedLatLng = null;
      // Restore UI state from the savedInstanceState.
      // This bundle is also been passed to onRestoreInstanceState, called after onCreate.
      if (savedInstanceState != null)
      {
         Log.d(TAG, "restore instance state");
         mEditedLatLng = savedInstanceState.getParcelable("mEditedLatLng");
         mBestLocation = savedInstanceState.getParcelable("mBestLocation");
         // No need to restore (or save) the title and description TextView fields,
         // since the system does that automatically.
      }
      
      Uri uri = intent.getData();
      mBlogname = Utils.openBlogFromIntent(this, uri, mBlogData);
      if (mBlogname == null)
      {
         // If we could not open requested file or the default file, we have
         // nothing to do, so have to error out.
         // Toast is shown by the openBlogFromIntent function.
         Log.e(TAG, "bad blogname, finishing " + uri);
         finish();
      }

      // update ActionBar title with blog name
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();
      actionBar.setTitle(Utils.blogToDisplayname(mBlogname));
      int subtitleId = 0; // screen subtitle R.string resource id

      // Entry to edit, or -1 or no id to create new post
      mEditItem = uri != null ? Utils.uriToEntryIndex(uri) : -1;
      
      if (mEditItem < 0)
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
         
         // If we don't get a starting location, use a fallback location
         // based on last created entry.
         String fallbackLngLat = "";
         int elementCount = mBlogData.getMaxBlogElements();
         if (elementCount > 0)
         {
            BlogElement blog = mBlogData.getBlogElement(elementCount - 1);
            fallbackLngLat = blog.location;
         }

         mEditItem = elementCount; // we display this in screen subtitle
         mOldLngLat = fallbackLngLat;
         subtitleId = R.string.new_post_name_format;
      }
      else
      {
         // Requested to edit: set that state, and the data being edited.
         mIsNewBlog = false;
         BlogElement blog = mBlogData.getBlogElement(mEditItem);

         mTitle = blog.title;
         String descr = blog.description;
         mOldTime = blog.timeStamp;
         mOldLngLat = blog.location;

         EditText tv = (EditText) findViewById(R.id.edit_title);
         tv.setText(mTitle);
         tv.setSelection(tv.getText().length());
         tv = (EditText) findViewById(R.id.edit_description);
         tv.setText(descr);
         tv.setSelection(tv.getText().length());
         
         subtitleId = R.string.edit_post_name_format;
      }

      // Display a subtitle with the 1-based index of the element
      String subtitle = String.format(getString(subtitleId), mEditItem + 1);
      actionBar.setSubtitle(subtitle);

      // Note: enable up navigation? - that will cancel edit. Is it confusing?
      // For now, enabling it. That follows Android guidelines, and while it is
      // not necessary for Android 4.0+ which supports the Task Stack Builder to allow
      // back button to go back to TravelLocBlogMain, that is not supported in older
      // Android releases. Since we support older Android release as of now (Nov 2013),
      // enable the parent activity so that user can go back to main screen if needed.
      actionBar.setDisplayHomeAsUpEnabled(true);

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
      b.putString("BLOG_NAME", mBlogname);
      b.putString("POST_NAME", mTitle);
      b.putInt("POST_INDEX", mEditItem);
      LatLng latlng = getCurrentLatLng();
      Log.d(TAG, "calling editLocation with " + latlng);
      if (latlng != null)
      {
         b.putParcelable("POST_LATLNG", latlng);         
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
         setResult(TravelLocBlogMain.RESULT_SAVE_FAILED); // default

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

         BlogElement blog = new BlogElement();
         blog.title = title;
         blog.description = desc;
         blog.location = locString;
         blog.timeStamp = mOldTime;
         Log.d(TAG, "Edit done, title: (" + title + ") location: " + locString);
         Log.d(TAG, "Edit done, location: " + blog.location);

         if (mBlogData.saveBlogElement(blog, mEditItem) == true)
         {
            setResult(RESULT_OK);
            Toast.makeText(this, R.string.post_saved,
                  Toast.LENGTH_SHORT).show();
         }
         else
         {
            // Toast.makeText(this, R.string.failed_post_save, Toast.LENGTH_LONG).show();
            // There have been at least 2 reports of blog corruption. We don't have
            // a reproducible case, so have no fix for it.
            // Not being able to save a post may mean file is corrupted,
            // so instead of using a Toast, use an AlertDialog and don't finish
            // this activity (this also avoids leaked window).
            Utils.okDialog(this, getString(R.string.failed_post_save));
            return; // stay in this activity, don't sendResult here
         }

         break;
         
      case R.id.cancel_button:
         setResult(RESULT_CANCELED);
         break;
         
      default:
         // Unknown button clicked? We finish the activity anyway.
         Log.e(TAG, "Unknown button click received, performing cancel.");
         setResult(RESULT_CANCELED);
         break;
      }
      
      sendResult();
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
            LatLng latlng = extras.getParcelable("POST_LATLNG");
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
     savedInstanceState.putParcelable("mBestLocation", mBestLocation);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.edit_post, menu);
      
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId())
      {
         case R.id.delete_post:
            deletePostUI();
            return true;
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

   private void sendResult()
   {
      // Since we going to exit, turn off the location updates.
      stopPeriodicUpdates();
      
      finish();
   }
   
   /**
    * Menu command to delete this post. Just like the contextual menu delete post,
    * we ask for confirmation before acting on it.
    */
   private void deletePostUI()
   {
      // For both New Post, or editing an existing Post,
      // send delete_post result, so caller can handle it correctly.
      final Context context = this;
      Utils.areYouSure(this, this.getString(R.string.are_you_sure_post),
            new DialogInterface.OnClickListener()
      {
         public void onClick(DialogInterface dialog, int id)
         {
            dialog.dismiss();
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
               setResult(TravelLocBlogMain.RESULT_DELETE_POST);
               Log.d(TAG, "Got delete post " + mEditItem);
               if (EditBlogElement.deletePost(context, mEditItem, mBlogData))
               {
                  sendResult(); // finishes activity                  
               }
               // If we could not delete the post, stay on this activity
               break;
            case DialogInterface.BUTTON_NEGATIVE:
            default:
               Toast.makeText(context, R.string.cancel_post_delete,
                     Toast.LENGTH_LONG).show();
               break;
            }
         }});
   }
   
   // Deletes the given blog item from the blog
   public static boolean deletePost(final Context context, int editItem,
         BlogDataManager blogData)
   {
      boolean deleted = false;
      if ((editItem >= 0) && (editItem < blogData.getMaxBlogElements()))
      {
         if (blogData.deleteBlogElement(editItem) == false)
         {
            // Toast.makeText(context, R.string.failed_post_delete, Toast.LENGTH_LONG).show();
            // Not being able to delete a post may mean file is corrupted,
            // so instead of using a Toast, use an AlertDialog. Caller should make sure
            // not to close this activity on this error, since that will result in a leaked
            // window. User can use back button or parent button to exit the activity.
            Utils.okDialog(context, context.getString(R.string.failed_post_delete));
         }
         else
         {
            deleted = true;
            Toast.makeText(context, R.string.post_deleted,
                  Toast.LENGTH_SHORT).show();
         }
      }
      return deleted;
   }

}
