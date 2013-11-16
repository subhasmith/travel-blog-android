/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

/**
 * http://developer.android.com/training/location/receive-location-updates.html
 * 
 * The code from MainActivity has been wrapped into a abstract LocationUpdatesActivity
 * class which may be used by multiple activities that need to turn on/off location updates
 * and receive the current position. Like the original class this also provides address
 * information.
 * The LocationUtils class fields have been merged into this one since only this class needs it. 
 * 
 * Since we are using this as a superclass activity for the TravelLocationBlog app, there may
 * be other common functionality that is executed here. Other than error dialogs, there may be
 * no UI elements used by this base activity, it merely exists to return information to
 * subclasses which can display it as needed.
 * This class is declared abstract so there is no need to add to the Android Manifest.
 *
 * Subclasses should implement:
 * {@link #getLayoutResourceId} this layout id will be passed to setContentView in onCreate.
 * {@link #getProgressBar} if used, to indicate progress.
 *
 * Subclasses can call:
 * {@link #enableLocationUpdates} to indicate interest in periodic update requests.
 *  Accepts a duration value after which updates are turned off.
 * {@link #disableLocationUpdates} to indicate that periodic update requests are no longer needed
 * {@link #stopLocationUpdates} cancels previous periodic update requests.
 * {@link #getLastLocation} gets the current location using the library API getLastLocation()
 * function.
 * {@link #getAddress} calls geocoding to get a street address for the current location.
 * {@link #startLocationUpdates} sends a request to Location Services to send periodic
 * location updates to the Activity. [Not usually needed, since enableLocationUpdates is more
 * useful.]
 *
 * The update interval is hard-coded to be 5 seconds, and so on - to try to get accurate readings,
 * but not use much battery.
 */
public abstract class LocationUpdates extends ActionBarActivity implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    // Constants
    private static final String TAG = "LocationUpdates";

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /*
     * Constants for location update parameters
     */
    // Milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    // A fast interval ceiling
    public static final int FAST_CEILING_IN_SECONDS = 1;

    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;

    // Create an empty string for initializing strings
    public static final String EMPTY_STRING = new String();

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient = null;

    // Handles to UI widgets - usually created by the derived concrete class
    private ProgressBar mActivityIndicator;

    /*
     * Note if updates have been turned on. Starts out as "false".
     * Subclass have to enable/disable this flag.
     *
     */
    private boolean mUpdatesRequested = false;
    
    public final static int mNoAutoOff = -9999; // should match @location_duration_values array
    private int mUpdatesDurationSecs = mNoAutoOff;
    private long mExpirationTime = 0; // setExpirationTime for LocationRequest
    
    private final Handler timerHandler = new Handler();
    private final Runnable timerEvent = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "timer triggered to stop location updates");
            timerCancelUpdates(); 
        }
    };

    // If no location services are turned on, and we need location updates, we display a warning.
    private boolean mDisplayedServiceOffMessage =false;
        
    /*
     * Returns the R.layout.id for the activity content.
     * To be implemented by the concrete class.
     */
    protected abstract int getLayoutResourceId();
    
    /*
     * Some activities may turn on a indefinite progress bar.
     * If the following function returns a non-null ProgressBar it will be used to
     * indicate progress.
     */
    protected abstract ProgressBar getProgressBar();

    /*
     * Initialize the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        // Get handles to the UI view objects - for now there are none in this abstract class.
        // Parent class may define some common UI controls, and those can be accessed here.
        
        mActivityIndicator = getProgressBar();

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are off until the concrete class turns them on
        mUpdatesRequested = false;

        mExpirationTime = 0;
        if (savedInstanceState != null) {
           mExpirationTime = savedInstanceState.getLong("mExpirationTime");
        }

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

    }
    
    /**
     * The concrete subclass needs to turn on/off the location updates.
     * Call this in an onCreate or onStart to enable the receipt of location updates.
     * By the time an onConnected event is called, this must be set correctly to attach
     * the LocationRequest object to the LocationClient.
     * Note that to actually stop updates, call stopLocationUpdates().
     * 
     * @param durationSecs how long to look for location updates
     */
    protected void enableLocationUpdates(int durationSecs) {
        mUpdatesRequested = true;
        mUpdatesDurationSecs = durationSecs;

        // Compute the expiration time as now + durationSecs.
        // We don't use durationSecs if mExpirationTime is already known - which happens
        // when this activity is recreated on a screen orientation change, etc.
        if (mUpdatesDurationSecs != mNoAutoOff && mExpirationTime == 0) {
           long durationMillis = Math.abs(durationSecs) * MILLISECONDS_PER_SECOND;
           mExpirationTime = SystemClock.uptimeMillis() + durationMillis;
        }
        
        if (durationSecs > 0 && mLocationClient != null) {
           Log.d(TAG, "Starting location timer for seconds: " + durationSecs);
           Log.d(TAG, "Starting location timer to time: " + mExpirationTime);

           // Location Updates are to expire at given time
           // Since this is exact time, this is not affected by screen orientation changes,
           // it will not restart timer. This will be affected if phone goes to sleep, since
           // uptimeMillis does not advance in that case. If mExpirationTime is less than
           // current uptimeMillis, it means no location updates will be received.
           //
           // Since we may be called multiple times in case caller has to turn on/off location
           // updates multiple times, for each enable call we set the time again, and
           // we make sure to remove old callbacks before we add a new one.
           timerHandler.removeCallbacks(timerEvent);
           timerHandler.postAtTime(timerEvent, mExpirationTime);

           // mLocationRequest.setExpirationDuration(durationSecs * MILLISECONDS_PER_SECOND); 
           // Not using setExpirationDuration since we may be called multiple times, and
           // need to keep resetting the duration, which can't be done with setExpirationDuration
           // More importantly, we need to know when the timer has stopped, so we can update
           // the location, or print a message, as is done in the run() method.

           if (mLocationClient.isConnected()) {
              // Most likely we have not received a onConnected event but if we have,
              // we need to start up the updates again, in case they are no longer active.
              // This can be called multiple times - if called again, will just replace
              // the old listener with "this" object as per the API doc.
              mLocationClient.requestLocationUpdates(mLocationRequest, this);
           }
        }
    }
    
    /**
     * Turn off the request for location updates. We don't actually stop the listener,
     * that will happen in normal onStop or other such events.
     * Probably not used? stopPeriodicUpdates is sufficient for all current use cases.
     */
    protected void disableLocationUpdates() {
       mUpdatesRequested = false;
    }
    
    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     * Based on the examples seen, this class uses onStart and onStop to connect and
     * disconnect to the location client. onResume does not do any location client or request
     * updates objects setup.
     */
    @Override
    public void onStop() {

        stopPeriodicUpdates();

        // After disconnect() is called, the client is considered "dead".
        if (mLocationClient != null) {
           mLocationClient.disconnect();           
        }
        
        // In case any callbacks are still around, remove all.
        timerHandler.removeCallbacksAndMessages(null);

        super.onStop();
    }
    /*
     * Called when the Activity is going into the background.
     * Parts of the UI may be visible, but the Activity is inactive.
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        Log.d(TAG, "onStart calling .connect");
        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         * Issue report#8 reported a NullPointerException
         * at com.barkside.travellocblog.LocationUpdates.onStart
         * Cannot reproduce it, web search seems to suggest may happen when Google Play
         * is not installed? In any case, we check for it here.
         */
        if (mLocationClient != null) {
           mLocationClient.connect();           
        }
    }
    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // If user has turned off location services, there will never be location
        // event - neither onConnected nor onLocationChanged. Detect that and report to user.
        if (mUpdatesRequested && !isLocationServiceOn()) 
        {
           if (mDisplayedServiceOffMessage)
           {
              // We display the dialog only once, after that, just a Toast is enough
              Toast.makeText(this, R.string.no_location_services_message, Toast.LENGTH_SHORT).show();
           } else
           {
              // First time in, display a warning dialog about turning on location services.
              mDisplayedServiceOffMessage = true;
              AlertDialog.Builder builder = new AlertDialog.Builder(this);
              builder.setMessage(this.getString(R.string.no_location_services_message));
              builder.setTitle(this.getString(R.string.no_location_services_title));
              builder.setPositiveButton(R.string.OK,
                    new DialogInterface.OnClickListener()
                    {
                       public void onClick(DialogInterface dialog, int id)
                       {
                          dialog.cancel();
                       }
                    });
              builder.setCancelable(true);
              builder.create().show();
           }
        }
    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        Log.d(TAG, getString(R.string.resolved));

                        // Display the result (if needed)
                        // mConnectionState.setText(R.string.connected);
                        // mConnectionStatus.setText(R.string.resolved);
                    break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(TAG, getString(R.string.no_resolution));

                        // Display the result (if needed)
                        // mConnectionState.setText(R.string.disconnected);
                        // mConnectionStatus.setText(R.string.no_resolution);

                    break;
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(TAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }

    /**
     * Calls getLastLocation() to get the current location
     */
    public Location getLastLocation() {

       Location location = null;
        // If Google Play Services is available
        if (Utils.playServicesAvailable(this) && mLocationClient != null) {

            // Get the current location
            location = mLocationClient.getLastLocation();
        }
        return location;
    }

    /**
     * Stop periodic updates as a result of timer firing.
     * This can be also defined in the subclass to display appropriate messages if location
     * has still not been found.
     */
    protected void timerCancelUpdates() {
       stopPeriodicUpdates();
    }

    /**
     * We need to survive a device orientation change. Android will completely destroy
     * and recreate this activity.
     * If we don't remember mExpirationTime for example, we may restart this activity and
     * forget that the timer had been already set previously.
     */
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      Log.d(TAG, "save instance state");
      // Save UI state changes to the savedInstanceState.
      // This bundle will be passed to onCreate if the process is
      // killed and restarted.
      savedInstanceState.putLong("mExpirationTime", mExpirationTime);
    }

    /**
     * Get the address of the current location, using reverse geocoding. This only works if
     * a geocoding service is available.
     * NOT USED YET
     *
     */
    // For Eclipse with ADT, suppress warnings about Geocoder.isPresent()
    @SuppressLint("NewApi")
    public void getAddress() {

        // In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && !Geocoder.isPresent()) {
            // No geocoder is present. Issue an error message
            Toast.makeText(this, getString(R.string.no_geocoder_available), Toast.LENGTH_LONG).show();
            return;
        }

        if (Utils.playServicesAvailable(this) && mLocationClient != null) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Turn the indefinite activity indicator on
            if (mActivityIndicator != null) {
                mActivityIndicator.setVisibility(View.VISIBLE);
            }

            // Start the background task
            (new LocationUpdates.GetAddressTask(this)).execute(currentLocation);
        }
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        if (mUpdatesRequested) {
           startPeriodicUpdates();
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        Log.d(TAG, getString(R.string.disconnected));
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * Handle location updates.
     * This base class does nothing, since we don't need the location, and the
     * concrete derived class is the one that will use the updates.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
       // We received a location fix.
       // If needed: the text of the location is == getLatLng(this, location);

       // If this is the first location fix received, we may need to start timer
       int durationSecs = mUpdatesDurationSecs; 
       if (durationSecs < 0 && durationSecs != mNoAutoOff) {
          timerHandler.removeCallbacks(timerEvent);
          timerHandler.postAtTime(timerEvent, mExpirationTime);
          mUpdatesDurationSecs = 0; // reset it since we only need to set timer once
       }
    }

    /**
     * Sends a request to start location updates.
     *
     * This should only be called after a onConnected event, after the location client has been
     * connected.
     */
    protected void startPeriodicUpdates() {
        if (!mUpdatesRequested) {
            // If updates are not enabled, we return. We have to check for this since we may
            // receive multiple disconnected and connected to Google Play Services events,
            // and on each onConnected, we need to restart the updates when necessary.
            // That will happen if some subclass calls us in a onConnected handler.
            return;
        }

        Log.d(TAG, "starting periodic updates");
        
        // This can be called multiple times - if called again, will just replace
        // the old listener with "this" object.
        if (mLocationClient != null) {
           mLocationClient.requestLocationUpdates(mLocationRequest, this);           
        }
    }

    protected boolean updatesRequested() {
       return mUpdatesRequested;
    }

    /**
     * Sends a request to remove location updates and stop updates.
     * 
     * This may be called multiple times in the same activity, and it is safe to do so.
     */
    protected void stopPeriodicUpdates() {
       // Unlike startPeriodicUpdates, we don't look at existing value of mUpdatesRequested
       // but always force it to be off. Once this function is called, location updates
       // will never be received.
       mUpdatesRequested = false;
       
        Log.d(TAG, "stopping periodic updates");
        // Remove all location updates, ok to call multiple times, just removes
        // whatever listener has been attached by requestLocationUpdates.
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
    }

    /**
     * An AsyncTask that calls getFromLocation() in the background.
     * The class uses the following generic types:
     * Location - A {@link android.location.Location} object containing the current location,
     *            passed as the input parameter to doInBackground()
     * Void     - indicates that progress units are not used by this subclass
     * String   - An address passed to onPostExecute()
     */
    protected class GetAddressTask extends AsyncTask<Location, Void, String> {

        // Store the context passed to the AsyncTask when the system instantiates it.
        Context localContext;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context) {

            // Required by the semantics of AsyncTask
            super();

            // Set a Context for the background task
            localContext = context;
        }

        /**
         * Get a geocoding service instance, pass latitude and longitude to it, format the returned
         * address, and return the address to the UI thread.
         */
        @Override
        protected String doInBackground(Location... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */
            Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

            // Get the current location from the input parameter list
            Location location = params[0];

            // Create a list to contain the result address
            List <Address> addresses = null;

            // Try to get an address for the current location. Catch IO or network problems.
            try {

                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
                addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1
                );

                // Catch network or other I/O problems.
                } catch (IOException exception1) {

                    // Log an error and return an error message
                    Log.e(TAG, getString(R.string.IO_Exception_getFromLocation));

                    // print the stack trace
                    exception1.printStackTrace();

                    // Return an error message
                    return (getString(R.string.IO_Exception_getFromLocation));

                // Catch incorrect latitude or longitude values
                } catch (IllegalArgumentException exception2) {

                    // Construct a message containing the invalid arguments
                    String errorString = getString(
                            R.string.illegal_argument_exception,
                            location.getLatitude(),
                            location.getLongitude()
                    );
                    // Log the error and print the stack trace
                    Log.e(TAG, errorString);
                    exception2.printStackTrace();

                    //
                    return errorString;
                }
                // If the reverse geocode returned an address
                if (addresses != null && addresses.size() > 0) {

                    // Get the first address
                    Address address = addresses.get(0);

                    // Format the first line of address
                    String addressText = getString(R.string.address_output_string,

                            // If there's a street address, add it
                            address.getMaxAddressLineIndex() > 0 ?
                                    address.getAddressLine(0) : "",

                            // Locality is usually a city
                            address.getLocality(),

                            // The country of the address
                            address.getCountryName()
                    );

                    // Return the text
                    return addressText;

                // If there aren't any addresses, post a message
                } else {
                  return getString(R.string.no_address_found);
                }
        }

        /**
         * A method that's called once doInBackground() completes. Set the text of the
         * UI element that displays the address. This method runs on the UI thread.
         */
        @Override
        protected void onPostExecute(String address) {

            // Turn off the progress bar
            mActivityIndicator.setVisibility(View.GONE);
            
            // Set the address in the UI
            // when geoCoding is needed, figure out how to send this data to the subclass.
            // mAddress.setText(address);
        }
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), TAG);
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    // Utility functions, maybe better someplace else?
    /**
     * Extract the latitude and longitude from the Location object returned by
     * Location Services.
     *
     * @param currentLocation A Location object containing the current location
     * @return The string latitude and longitude of the current location, or "" if no
     * location is available.
     */
    public static String getLatLng(Context context, Location currentLocation) {
        // If the location is valid
        if (currentLocation != null) {
            // Return the latitude and longitude as strings
            return context.getString(
                    R.string.latitude_longitude_format,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {
            // Otherwise, return the empty string
            return EMPTY_STRING;
        }
    }

   /**
     * Extract the latitude and longitude from the Location object returned by
     * Location Services.
     * Uses Longitude,Latitude format to match KML file coordinates element.
     * 
     * @param currentLocation
     *           A Location object containing the current location
     * @return The string longitude and latitude of the current location, or "" if no
     *         location is available.
     */
    public static String getLngLat(Context context, Location currentLocation)
    {    
        // If the location is valid
        if (currentLocation != null)
        {
            // Return the longitude and latitude as strings
            return context.getString(R.string.longitude_latitude_format,
                   currentLocation.getLongitude(), currentLocation.getLatitude());
        } else {
            // Otherwise, return the empty string
            return EMPTY_STRING;
        }
    }
    public static String getLngLat(Context context, LatLng latlng)
    {    
        // If the location is valid
        if (latlng != null)
        {
            // Return the longitude and latitude as strings
            return context.getString(R.string.longitude_latitude_format,
                   latlng.longitude, latlng.latitude);
        } else {
            // Otherwise, return the empty string
            return EMPTY_STRING;
        }
    }

    public static String getAccuracy(Context context, Location currentLocation)
    {
	// If the location is valid
	if (currentLocation != null) {
	    // Return the latitude and longitude as strings
	    return context.getString(R.string.accuracy_format, currentLocation.getAccuracy());
	} else {
	    // Otherwise, return the empty string
	    return EMPTY_STRING;
	}
    }

    // Parse the stored blog.location string into Location object
    public static Location stringToLocation(String locstr)
    {
	if (locstr == null) {
	    return null;
	}
	Location locobj = null;
	try {
	    String temp[] = locstr.split(",");
	    if (temp.length >= 2) {
        	float lon = Float.parseFloat(temp[0]);
        	float lat = Float.parseFloat(temp[1]);
        	locobj = new Location("INTERNAL");
        	locobj.setLatitude(lat);
        	locobj.setLongitude(lon);
            }
        } catch (PatternSyntaxException e) {
            Log.e(TAG, "Internal program error incorrect split argument");
        }
        return locobj;
    }
    // Parse the stored blog.location string into LatLng object.
    // null or "" locstr returns null, as does an invalid locstr.
    public static LatLng stringToLatLng(String locstr)
    {
        if (locstr == null) {
            return null;
        }
        LatLng latlng = null;
        try {
            String temp[] = locstr.split(",");
            if (temp.length >= 2) {
                float lon = Float.parseFloat(temp[0]);
                float lat = Float.parseFloat(temp[1]);
                latlng = new LatLng(lat, lon);
            }
        } catch (PatternSyntaxException e) {
            Log.e(TAG, "Internal program error incorrect split argument");
        }
        return latlng;
    }

    /** Determines whether one Location reading is better than the current Location fix
     *
     * From http://developer.android.com/guide/topics/location/strategies.html
     * 
     * http://developer.android.com/guide/topics/location/strategies.html
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
       if (currentBestLocation == null) {
          // A new location is always better than no location
          return true;
       }

       // Check whether the new location fix is newer or older
       long timeDelta = location.getTime() - currentBestLocation.getTime();
       boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
       boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
       boolean isNewer = timeDelta > 0;

       // If it's been more than two minutes since the current location, use the new location
       // because the user has likely moved
       if (isSignificantlyNewer) {
          return true;
          // If the new location is more than two minutes older, it must be worse
       } else if (isSignificantlyOlder) {
          return false;
       }

       // Check whether the new location fix is more or less accurate
       int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
       boolean isLessAccurate = accuracyDelta > 0;
       boolean isMoreAccurate = accuracyDelta < 0;
       boolean isSignificantlyLessAccurate = accuracyDelta > 200;

       // Check if the old and new location are from the same provider
       boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

       // Determine location quality using a combination of timeliness and accuracy
       if (isMoreAccurate) {
          return true;
       } else if (isNewer && !isLessAccurate) {
          return true;
       } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
          return true;
       }
       return false;
    }
    
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    
    /** Checks whether two providers are the same.
     * Not really necessary for the single Fused provider using new Google Maps API v2.
     */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    /**
     * Check if user has turned on location services. If not, then we'll never get
     * onConnected or onLocationChanged events.
     * Google Play services does not catch this condition, so have to manually check it.
    http://stackoverflow.com/questions/16862987/android-check-location-services-enabled-with-play-services-location-api
     */
    public boolean isLocationServiceOn()
    {
       LocationManager locationManager = (LocationManager)
             getSystemService(Context.LOCATION_SERVICE);

       // Was checking for non-empty list locationManager.getProviders(true) but
       // that also contains the PASSIVE provider, which may not return
       // any location in some cases.
       // For best performance, need both GPS and NETWORK, but for this call,
       // will accept either one being on.
       return (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }
}
