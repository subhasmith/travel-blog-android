package com.barkside.travellocblog;


import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditBlogElement extends Activity
{
   private Boolean isNewBlog = false;
   private LocationListener mLocationListener = null;
   private LocationManager mLocationManager = null;
   private Location mBestLocation = null;
   private Boolean mCancelTimer = false;
   private Boolean mIsLocationSaved = false;
   private String mOldLocation = null;
   private String mOldTime = null;
   final Handler viewUpdateHandler = new Handler();
   Thread mWaitAndCheckGPS = null;
   // Create runnable for posting
   final Runnable mUpdateLocation = new Runnable() {
     public void run() {
        if(mCancelTimer == false)
        {
           Log.d("TRAVEL_DEBUG", "Update location view"); 
           if(mLocationManager != null)
              mLocationManager.removeUpdates(mLocationListener);
           Button but = (Button) findViewById(R.id.loc_again_button);
           but.setVisibility(View.VISIBLE);      
           mCancelTimer = true;
        }
     }
   };      
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.edit_blog);

      Intent intent = getIntent();
      String action = intent.getAction();
      if (action.equals("com.barkside.travellocblog.NEW_BLOG_ENTRY"))
      {
         // Requested to edit: set that state, and the data being edited.
         isNewBlog = true;
         EditText tv = (EditText) findViewById(R.id.edit_name);
         tv = (EditText) findViewById(R.id.edit_description);

         Date dateNow = new Date(); 
         java.text.DateFormat df = DateFormat.getDateFormat(this);
         String myString = df.format(dateNow);
         df = DateFormat.getTimeFormat(this);
         myString += " "+df.format(dateNow);
         CharSequence charSeq;
         DateFormat df2 = new DateFormat();
         charSeq = df2.format("yyyy-MM-ddThh:mm:ssZ",dateNow.getTime());
                 
         mOldTime = charSeq.toString();
         tv.setText(myString + "\n");           
         tv.setSelection(tv.getText().length());
      }
      else if (action.equals("com.barkside.travellocblog.EDIT_BLOG_ENTRY"))
      {
         isNewBlog = false;
         Bundle extras = intent.getExtras();
         String name = extras.getString("BLOG_NAME");
         String descr = extras.getString("BLOG_DESCRIPTION");
         mOldTime = extras.getString("BLOG_TIMESTAMP");
         mOldLocation = extras.getString("BLOG_LOCATION");
         EditText tv = (EditText) findViewById(R.id.edit_name);
         tv.setText(name);
         tv.setSelection(tv.getText().length());
         tv = (EditText) findViewById(R.id.edit_description);
         tv.setText(descr);
         tv.setSelection(tv.getText().length());

         Button but2 = (Button) findViewById(R.id.loc_button);
         but2.setVisibility(View.VISIBLE);          
      }
      TextView tv = (TextView) findViewById(R.id.location);
      tv.setText("Finding Location...");
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
 
      
      startLocationInit();
   }
   
   private void updateLocation(Location loc)
   {
      if(loc == null)
         return;
      mBestLocation = loc;
      TextView tv = (TextView) findViewById(R.id.location);
      String[] temp;
      String lat = String.format("%.6f", loc.getLatitude());
      tv.setText("Latitude: " +lat);
      if((isNewBlog== false) && (mIsLocationSaved == false))
      {
         temp = mOldLocation.split(",");
         if (temp.length == 3)
         {
            int len = temp[1].length();
            if(len > 8) len = 8;
            tv.append(" (Previous: "+temp[1].substring(0,len)+")");
         }
      }
         
      String lon = String.format("%.6f", loc.getLongitude());

      tv.append("\nLongitude: " +lon);
      if((isNewBlog== false) && (mIsLocationSaved == false))
      {
         temp = mOldLocation.split(",");
         if (temp.length == 3)
         {         
            int len = temp[0].length();
            if(len > 8) len = 8;
            tv.append(" (Previous: "+temp[0].substring(0,len)+")");
         }
      }      
      tv.append("\nAccuracy: " +loc.getAccuracy()+"m\n");
      tv.append("Provider: " +loc.getProvider().toUpperCase()+"\n");

      java.text.DateFormat df = DateFormat.getDateFormat(this);
      String myString = df.format(loc.getTime());
      df = DateFormat.getTimeFormat(this);
      myString += " "+df.format(loc.getTime());
      tv.append("Location time: " +myString + "\n");
      if(isNewBlog == true)
      {
         tv.append("Saved status: Auto\n");
      }
      else if(mIsLocationSaved == false)
      {
         tv.append("Saved status: Previous\n");
      }  
      else
      {
         tv.append("Apply status: Applied\n");
      }  
         
      //GpsStatus gps = mLocationManager.getGpsStatus(null);
      //gps.
   }
   /* to save blog */
   public void saveBlogEdit(View view) {
      String location;
      if((isNewBlog == true)||(mIsLocationSaved == true))
      {
         if(mBestLocation == null)
         {
            Toast toast = Toast.makeText(this, "Failed: no location", Toast.LENGTH_SHORT);
            toast.show();
            return;
         }
         location = mBestLocation.getLongitude() + "," + 
            mBestLocation.getLatitude() + ",0";         
      }
      else
      {
         if(mOldLocation == null)
         {
            Toast toast = Toast.makeText(this, "Failed: no location", Toast.LENGTH_SHORT);
            toast.show();
            return;
         }         
         location = mOldLocation;
      }
         
      Intent intent = new Intent();
      Bundle extras = new Bundle();
      EditText et = (EditText) findViewById(R.id.edit_name);
      String str = et.getText().toString().trim();
      if(str.length() == 0)
      {
         Toast toast = Toast.makeText(this, "Failed: enter a valid name", Toast.LENGTH_SHORT);
         toast.show();
         return;
      }
      Log.d("TRAVEL_DEBUG", "Edit done (save) for " + str);
      extras.putString("BLOG_NAME", str);
      et = (EditText) findViewById(R.id.edit_description);
      str = et.getText().toString();      
      extras.putString("BLOG_DESCRIPTION", str);
      extras.putString("BLOG_LOCATION", location);
      extras.putString("BLOG_TIMESTAMP", mOldTime);
      intent.putExtras(extras);
      cancelLocationUpdates();
      setResult(RESULT_OK, intent);
      finish();
   }
   public void cancelBlogEdit(View view) {
      cancelLocationUpdates();
      setResult(RESULT_CANCELED);
      finish();
   }
   
   public void applyLocation(View view) {
      mIsLocationSaved = true;
      Button but = (Button) findViewById(R.id.loc_button);
      but.setVisibility(View.GONE);        
      updateLocation(mBestLocation);
      Log.d("TRAVEL_DEBUG", "applyLocation");
   }
   
   public void applyDate(View view) {
      Log.d("TRAVEL_DEBUG", "applyDate");
   }   
   
   public void restartLocation(View view) {
      Button but = (Button) findViewById(R.id.loc_again_button);
      but.setVisibility(View.GONE); 
      startLocationUpdates();
      Log.d("TRAVEL_DEBUG", "restartLocation");
   }
   public void cancelLocationUpdates()
   {
      // Remove the listener you previously added
      if(mLocationManager != null)
         mLocationManager.removeUpdates(mLocationListener);
      Log.d("TRAVEL_DEBUG", "cancelLocationUpdates");
      mCancelTimer = true;
   }
   
   @Override
   protected void onStop()
   {
      super.onStop();
      cancelLocationUpdates();
      Button but = (Button) findViewById(R.id.loc_again_button);
      //Log.d("TRAVEL_DEBUG", "on Stop");
      but.setVisibility(View.VISIBLE);     
  
   }

   protected void startLocationInit()
   {
     // Fire off a thread to do some work that we shouldn't do directly in the UI thread
     // Acquire a reference to the system Location Manager

      mLocationManager = (LocationManager) this
            .getSystemService(Context.LOCATION_SERVICE);
      
      Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      updateLocation(lastKnownLocation);
      try
      {
         //if(mIsTimerCancelled == false)
         //{
         //   mTimer.schedule(disableGpsLock, 15000L);
         //}
         // Define a listener that responds to location updates
         mLocationListener = new LocationListener()
         {
            public void onLocationChanged(Location location)
            {
               // Called when a new location is found by the network location
               // provider.
               //Log.d("TRAVEL_DEBUG", "location "+ location.getLatitude()+" "+ location.getLongitude()
               //      +" ac "+location.getAccuracy()+" pro "+location.getProvider());
               if(isBetterLocation(location) == true)
               {
                  updateLocation(location);
               }
               if(location.getProvider().equals(LocationManager.GPS_PROVIDER) == true)
               {
                  Log.d("TRAVEL_DEBUG", "disable GPS");
                  cancelLocationUpdates();
               }               
            }
   
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
               //Log.d("TRAVEL_DEBUG", "onStatusChanged");
            }
   
            public void onProviderEnabled(String provider)
            {
               //Log.d("TRAVEL_DEBUG", "onProviderDisabled");
            }
   
            public void onProviderDisabled(String provider)
            {
               //Log.d("TRAVEL_DEBUG", "onProviderDisabled");
            }
         };// Register the listener with the Location Manager to receive location
           // updates
         startLocationUpdates();
      }
      catch (Exception e)
      {
         Log.e("TRAVEL_DEBUG", "Find location error", e);
      }
      
      Log.d("TRAVEL_DEBUG", "Update location thread");
      
      mCancelTimer = false;
      if(mWaitAndCheckGPS != null)
         return;
      mWaitAndCheckGPS = new Thread()
      {
        public void run()
        {
           //Log.d(MoodData.MOOD_TAG, "disableGpsLock start thread"); 
           try{Thread.sleep(15000L,0);}catch (Exception e) {}
           //Log.d(MoodData.MOOD_TAG, "disableGpsLock complete thread"); 
           viewUpdateHandler.post(mUpdateLocation);
        }
     };
     mWaitAndCheckGPS.start();
   }
   
   private void startLocationUpdates()
   {
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
      mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
   }
   
     
   private static final int TWO_MINUTES = 1000 * 60 * 2;

   /**
    * Determines whether one Location reading is better than the current
    * Location fix
    * 
    * @param location
    *           The new Location that you want to evaluate
    * @param currentBestLocation
    *           The current Location fix, to which you want to compare the new
    *           one
    */
   protected boolean isBetterLocation(Location newLocation)
   {
      if(newLocation == null)
      {
         return false;
      }
      if (mBestLocation == null)
      {
         // A new location is always better than no location
         return true;
      }

      // Check whether the new location fix is newer or older
      long timeDelta = newLocation.getTime() - mBestLocation.getTime();
      boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
      boolean isNewer = timeDelta > 0;
      // If it's been more than two minutes since the current location, use the
      // new location
      // because the user has likely moved
      if (isSignificantlyOlder)
      {
         return false;
      }

      // Check whether the new location fix is more or less accurate
      int accuracyDelta = (int) (newLocation.getAccuracy() - mBestLocation
            .getAccuracy());
      boolean isLessAccurate = accuracyDelta > 0;
      boolean isMoreAccurate = accuracyDelta < 0;

      // Determine location quality using a combination of timeliness and
      // accuracy
      if (isMoreAccurate)
      {
         return true;
      }
      else if (isNewer && !isLessAccurate)
      {
         return true;
      }

      return false;
   }

}
