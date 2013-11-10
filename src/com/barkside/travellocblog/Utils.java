package com.barkside.travellocblog;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Shared utility functions used by multiple activities.
 * 
 * @author avinash
 *
 */
public enum Utils {
   UTILITY_FUNCTIONS_CLASS;

   // For logging and debugging purposes
   private static final String TAG = "Utils";

   // Internal app Uri construction: content://AUTHORITY/MyFirstTrip.kml refers
   // to the blog MyFirstTrip.kml. It is stored in appropriate place as determined
   // by the mBlogData implementation (or any alternate implementation).
   
   public static final String AUTHORITY = "com.barkside.travellocblog.tripfile";
   public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

   // Open default blog, create it if it does not already exist.
   // Display brief message if it cannot be created.
   // Usually used to ensure default blog exists.
   public static String createDefaultTrip(Context context, BlogDataManager blogMgr)
   {
      String defaultTrip = context.getString(R.string.default_trip);

      String blogname = Utils.displayToBlogname(defaultTrip);
      Uri uri = Utils.blognameToUri(blogname);
      boolean opened = true;
      Log.d(TAG, "Exists file ?" + blogMgr.existingBlog(blogname));

      if (blogMgr.existingBlog(blogname)) {
         opened = blogMgr.openBlog(context, uri);
      } else {
         opened = blogMgr.newBlog(blogname);
      }
      return opened ? blogname : null;
   }

   // Shared function to determine blog name from Uri, or Preferences.
   public static String getBlognameFromIntent(Context context, Uri uri)
   {
      String blogname = null;
      
      if (uri != null)
      {
         blogname = Utils.uriToBlogname(uri);
         Log.d(TAG, "Launched with given file " +  blogname);
      }

      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      /* Attempt to open last used blog file. Try new default prefs, and deprecated prefs too */
      if (blogname == null)
      {
         blogname = settings.getString(SettingsActivity.LAST_OPENED_TRIP_KEY, null);
         if (blogname != null && blogname.isEmpty()) blogname = null;
         Log.d(TAG, "Trying last opened file: " +  blogname);
      }
      if (blogname == null)
      {
         // versionCode 5 (version 1.7) or older
         settings = context.getSharedPreferences(TravelLocBlogMain.PREFS_NAME, 0);
         blogname = settings.getString("defaultTrip", null);
         if (blogname != null && blogname.isEmpty()) blogname = null;
      }
      if (blogname == null)
      {
         blogname = context.getString(R.string.default_trip);
         if (blogname != null && blogname.isEmpty()) blogname = null;
      }
      Log.d(TAG, "getBlognameFromIntent returns: " + blogname);
      return blogname;
   }

   // Given an input Uri, determine the blog name to use. If not found, try default file.
   // If neither can be opened, report appropriate message using Toast.
   // Used by activities that can be started from other tasks: Main, as well as Edit Post.
   public static String openBlogFromIntent(Context context, Uri uri, BlogDataManager blogMgr)
   {
      String firstname = Utils.getBlognameFromIntent(context, uri);
      String secondname = null;
      String blogname = firstname;
      Uri blogUri = Utils.blognameToUri(blogname);
      boolean opened = blogMgr.openBlog(context, blogUri);
      
      if (!opened) {
         // If we are trying to open default name and it is not present,
         // we try to create that file.
         secondname = context.getString(R.string.default_trip);
         if (secondname.equals(firstname) == true)
         {
            secondname = Utils.createDefaultTrip(context, blogMgr);
            opened = secondname != null;
            blogname = secondname;
         }
      }
      
      // See if we need to report any file open failure
      String message = null;
      if (!opened) {
         // could not open any file, report which files failed to open
         if (secondname != null) {
            message = String.format(context.getString(R.string.open_failed_both_files),
                  firstname, secondname);
         } else {
            message = String.format(context.getString(R.string.open_failed_one_file),
                  firstname);
         }
      } else {
         // opened one file, but if it is the second one, report that first one failed.
         // only need to do this if secondname is different from firstname.
         if (secondname != null && !secondname.equals(firstname)) {
            message = String.format(context.getString(R.string.open_failed_one_file),
                  firstname);
         }
      }
      if (message != null)
         Toast.makeText(context, message, Toast.LENGTH_LONG).show();
      
      return opened ? blogname : null;
   }

   /**
    * Send Feedback opens up an Email client addressed to Travel Blog mailing list.
    * Called from multiple activities so uses activity Context as an arg.
    */
   private static void sendFeedbackEmail(Context context, String tag)
   {
      Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
            Uri.fromParts("mailto", context.getString(R.string.feedback_email), null));
      
      String subject = String.format("%s: %s Android %s",
            context.getString(R.string.feedback_subject), Build.MODEL, Build.VERSION.RELEASE);

      emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
      
      // Initialize body text with a few system details
      PackageInfo pInfo = Utils.getAppPackageInfo(context);
      String text = String.format("Tag: %s. SDK#%d. %s Version: %s #%d.\n\n",
            tag, Build.VERSION.SDK_INT, context.getString(R.string.app_name),
            pInfo.versionName, pInfo.versionCode);
      
      emailIntent.putExtra(Intent.EXTRA_TEXT, text);

      context.startActivity(Intent.createChooser(emailIntent,
            context.getString(R.string.feedback_title)));
   }
   
   /**
    * Top level feedback command.
    * First, display Private Notice and then on confirmation, call sendFeedBackEmail.
    * Called from multiple activities so uses activity Context as an arg.
    */

   public static void sendFeedback(final Context context, final String tag)
   {
      Utils.areYouSure(context,
            context.getString(R.string.feedback_privacy_notice),
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id)
         {
            dialog.dismiss();
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
               Utils.sendFeedbackEmail(context, tag);
               break;
            case DialogInterface.BUTTON_NEGATIVE:
            default:
               Toast.makeText(context, R.string.feedback_cancel,
                     Toast.LENGTH_LONG).show();
               break;
            }
         }});
   }

   /**
    * Display a Yes No / Confirmation dialog.
    * @param listener called for both BUTTON_POSITIVE and BUTTON_NEGATIVE
    */
   public static void areYouSure(Context context, String msg,
         DialogInterface.OnClickListener listener)
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      
      builder.setMessage(msg);

      builder.setTitle(R.string.are_you_sure);
      builder.setPositiveButton(R.string.Yes, listener);
      builder.setNegativeButton(R.string.cancel, listener);
      builder.setCancelable(true);
      AlertDialog dialog = builder.create();
      dialog.show();
      
      // After dialog is shown, we can get the TextView and make it HTML capable
      // TODO: not working, does not display HTML - similar code works in
      // AboutActivity though. Leaving it in for now, may look into this later.
      TextView tv = (TextView)dialog.findViewById(android.R.id.message);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      tv.setClickable(true);
   }
   
   // Show a single button with OK and a message.
   public static void okDialog(Context context, String msg)
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      
      builder.setMessage(msg);

      builder.setTitle(R.string.ok_dialog_title);
      builder.setCancelable(true);
      builder.setNeutralButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
         }
      });

      AlertDialog dialog = builder.create();
      dialog.show();
   }
   
   // Display the help text in a dialog box
   public static void showHelp(FragmentManager fm)
   {
      MessagesDialog helpDialog = new MessagesDialog();
      Bundle args = new Bundle();
      args.putInt(MessagesDialog.MESSAGE1_STRING_ID_ARG, R.string.help_message);
      args.putInt(MessagesDialog.MESSAGE2_STRING_ID_ARG, R.string.whatsnew_message);
      args.putInt(MessagesDialog.TITLE_STRING_ID_ARG, R.string.help_title);
      helpDialog.setArguments(args);
      helpDialog.show(fm, "Help Dialog"); // getString(R.string.help_dialog_title));
   }

   // Get the app package info with version name, version code, etc.
   public static PackageInfo getAppPackageInfo(Context context)
   {
      PackageInfo pInfo = null; // to denote unknown version
      try
      {
         pInfo = context.getPackageManager()
               .getPackageInfo(context.getPackageName(), 0);
      } catch (NameNotFoundException e)
      {
         Log.d(TAG, "Failed to get PackageInfo " + e);
      }
      return pInfo;
   }
   
   // Get app version string
   public static String getAppVersion(Context context)
   {
      PackageInfo pInfo = Utils.getAppPackageInfo(context);
      return pInfo != null ? pInfo.versionName : "0.0.0";
   }

   // Blogname, Uri, paths, suffixes, and such related functions.
   
   // Convert blog file name to a content Uri.
   // Only used internally, for passing from one activity to another.
   // So content:// distinguishes internal Uris from external file:// Uris.
   // returns: content://authority/filename
   public static Uri blognameToUri(String blogname) {
      String name = displayToBlogname(blogname);
      name = Uri.encode(name); // This may encode / directory separator?
      Uri uri = Uri.withAppendedPath(CONTENT_URI, name);
      return uri;
   }

   // Uri can be: content://authority/path or content://authority/path/id
   // path cannot be a number - it will be taken as an id.
   // Not an issue for this app, since all paths end in .kml suffix.
   public static String uriToBlogname(Uri uri) {
      String scheme = uri.getScheme();
      String name = uri.getPath(); // this includes the id
      boolean hasId = false;
      try {
         ContentUris.parseId(uri);
         hasId = true;
      } catch (NumberFormatException n) {
         hasId = false;
      }
      if (hasId) {
         int idIndex = name.lastIndexOf('/');
         if (idIndex >= 0)
            name = name.substring(0, idIndex);
      }

      // Check if Uri has been generated internally.
      String authority = uri.getAuthority();
      boolean isInternal = ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)
            && AUTHORITY.equalsIgnoreCase(authority);
      
      if (isInternal) {
         // remove leading / if any
         if (name.startsWith("/")) name = name.substring(1);
         if (name.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Uri a path? Not a filename: " + name);
         }
         return name;
      }
      // Not an internal Uri, caller should use ContentResolver to convert
      // to InputFileStream etc.
      return "";
   }

   // Uri can be: content://authority/path or content://authority/path/id
   public static int uriToEntryIndex(Uri uri) {
      int entryIndex = -1;
      try {
         entryIndex = (int) ContentUris.parseId(uri);
      } catch (NumberFormatException n) {
         entryIndex = -1;
      }
      return entryIndex;
   }

   // Utility function to convert file name to trip display name
   private static final String KML_SUFFIX = ".kml";

   public static String blogToDisplayname(String filename) {
      return stripSuffix(filename, KML_SUFFIX);
   }
   
   /**
    * Convert trip name, with out without suffix, to file name which always
    * has a suffix.
    */
   public static String displayToBlogname(String tripname) {
      // If it already has a suffix, strip it off (so we don't double-up suffixes)
      String name = stripSuffix(tripname, KML_SUFFIX);
      // Create file name with the suffix, and check that name is at least 1 char
      if (name.length() > 0) name += KML_SUFFIX;
      return name;
   }

   // Ignore case and return substring without suffix if name ends in given suffix
   private static String stripSuffix(String filename, String suffix) {
      String name = filename == null ? "" : filename;
      int suffixLen = suffix.length();
      int startSuffix = name.length() - suffixLen;
      // return the name without the suffix, and check that name is at least 1 char
      if (startSuffix > 0 &&
            name.regionMatches(true, startSuffix, KML_SUFFIX, 0, suffixLen)) {
         name = name.substring(0, startSuffix);
      }
      return name;
   }
   
   /**
    * Storage directory management. Android docs state that before any I/O, we must
    * check the state of the storage. SD card may be removed, for example, or folders
    * unmounted.
    */
   boolean mExternalStorageAvailable = false;
   boolean mExternalStorageWriteable = false;
   public void updateStorageState() {
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state)) {
         // We can read and write the media
         mExternalStorageAvailable = mExternalStorageWriteable = true;
      } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
         // We can only read the media
         mExternalStorageAvailable = true;
         mExternalStorageWriteable = false;
      } else {
         // Something else is wrong. It may be one of many other states, but all we need
         //  to know is we can neither read nor write
         mExternalStorageAvailable = mExternalStorageWriteable = false;
      }
   }

}
