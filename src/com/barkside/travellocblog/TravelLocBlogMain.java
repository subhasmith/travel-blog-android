package com.barkside.travellocblog;

import java.io.File;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Main activity. Comes here from the launcher.
 * 
 * Using a FragmentActivity instead of Activity to call getSupportFragmentManager
 * for the Help, About, and What's New message screens.
 *
 * Displays a list of blog entries from the last used trip file.
 */

public class TravelLocBlogMain extends ActionBarActivity
{
   // For logging and debugging purposes
   private static final String TAG = "TravelLocBlogMain";

   private BlogData mBlogData = new BlogData();

   public static final int NEW_BLOG_ENTRY = 100;
   public static final int EDIT_BLOG_ENTRY = 111;
   
   // Custom result returned by EditBlogPost activity
   public static final int RESULT_DELETE_POST = RESULT_FIRST_USER;
   
   Dialog mNewDialog;
   CharSequence[] mFileList;
   public static String TRIP_PATH = "/TravelBlog";
   private static final int CONTEXTMENU_EDITITEM = 0;
   private static final int CONTEXTMENU_DELETEITEM = 1;
   
   // Named preference file use deprecated - see SettingsActivity class comments.
   public static final String PREFS_NAME = "MyPrefsFile"; // deprecated as of versionCode 6
   
   private int mEditItem = 0;
   private int mDeleteItem = -1;
   private static final String KML_SUFFIX = ".kml";
   private String mFileName = null;
   
   private boolean mShowWhatsnew = false;
   
   ListView mBlogList;

   /* Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      this.mBlogList = (ListView) this.findViewById(R.id.blog_list);
      
      // When item is clicked, open up the Edit Post activity.
      // For now, long-click continues to show context menu, will be later
      // removed, to work like other Android 4.0+ apps. TODO: Make this
      // class a ListActivity?
      mBlogList.setOnItemClickListener(new ListView.OnItemClickListener()
      {
         @Override
         public void onItemClick(AdapterView<?> a, View v, int position, long id)
         {
            editBlogEntry(mirrorElement(position));
         }
      });

      /* Attempt to open last used blog file. Try new default prefs, and deprecated prefs too */
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      mFileName = settings.getString(SettingsActivity.LAST_OPENED_TRIP_KEY, null);
      boolean has_file_name = settings.contains(SettingsActivity.LAST_OPENED_TRIP_KEY);
      if (mFileName == null)
      {
         // versionCode 5 (version 1.7) or older
         settings = getSharedPreferences(PREFS_NAME, 0);
         mFileName = settings.getString("defaultTrip", null);
         has_file_name = settings.contains("defaultTrip");
      }
      if (mFileName == null)
      {
         mFileName = getString(R.string.default_trip);
      }

      if ((mBlogData.openBlog(mFileName) == false) && has_file_name)
      {
         Toast.makeText(this, R.string.last_file_open_failed,
               Toast.LENGTH_SHORT).show();
      }
      mNewDialog = new Dialog(this);
      initList();
      
      // All other settings always use standard default settings location
      settings = PreferenceManager.getDefaultSharedPreferences(this);

      // Check if app got updated after the last time it was run
      int lastVersionCode = settings.getInt(SettingsActivity.LAST_VERSION_USED_KEY, -1);
      int versionCode = getVersionCode();
      SharedPreferences.Editor editor = settings.edit();
      // Save new version code to preferences
      if (lastVersionCode != versionCode)
      {
         editor.putInt(SettingsActivity.LAST_VERSION_USED_KEY, versionCode);
      }
      if (lastVersionCode < versionCode)
      {
         mShowWhatsnew = true;
      }

      // Settings menu: These two lines are working around an android bug to set boolean defaults
      // http://code.google.com/p/android/issues/detail?id=6641
      String key = SettingsActivity.DEFAULT_DESC_ON_KEY;
      editor.putBoolean(key, settings.getBoolean(key, true));

      // Commit the edits!
      editor.commit();
   }

   /* Called to edit a blog post, by passing extras in a bundle */
   private void editBlogEntry(int index)
   {
      if ((index >= mBlogData.getMaxBlogElements()) || (index < 0))
      {
         Log.d(TAG, "Edit Log Entry illegal: " + index);
         return;
      }
      BlogElement blog = mBlogData.getBlogElement(index);
      Intent i = new Intent(this, EditBlogElement.class);
      Bundle b = new Bundle();
      b.putString("BLOG_NAME", blog.title);
      b.putString("BLOG_DESCRIPTION", blog.description);
      b.putString("BLOG_LOCATION", blog.location);
      b.putString("BLOG_TIMESTAMP", blog.timeStamp);
      i.putExtras(b);
      i.setAction("com.barkside.travellocblog.EDIT_BLOG_ENTRY");
      mEditItem = index;
      startActivityForResult(i, EDIT_BLOG_ENTRY);
   }

   /* new blog post passes in a fallback location in the  extras,
    * to be used if no location found
    */
   private void newBlogEntry()
   {
      int index = mBlogData.getMaxBlogElements() - 1;
      String fallbackLngLat = "";
      if (index >= 0)
      {
         BlogElement blog = mBlogData.getBlogElement(index);
         fallbackLngLat = blog.location;
      }
      else
      {
         // No fallback here - send in "". The EditLocation call will then
         // display appropriate message and zoom levels, and use final_fallback_lnglat
         // fallbackLngLat = getString(R.string.final_fallback_lnglat);
      }

      Intent i = new Intent(this, EditBlogElement.class);
      Bundle b = new Bundle();
      b.putString("BLOG_NAME", null);
      b.putString("BLOG_DESCRIPTION", null);
      b.putString("BLOG_LOCATION", fallbackLngLat);
      b.putString("BLOG_TIMESTAMP", null);
      i.putExtras(b);
      i.setAction("com.barkside.travellocblog.NEW_BLOG_ENTRY");
      Log.d(TAG, "New Log Entry");
      startActivityForResult(i, NEW_BLOG_ENTRY);
   }

   
   @Override
   protected void onResume() {
      super.onResume();
      
      // If we are starting an updated version of the app, show the what's new blurb
      if (mShowWhatsnew)
      {
         FragmentManager fm = getSupportFragmentManager();
         MessagesDialog whatsnewDialog = new MessagesDialog();
         Bundle args = new Bundle();
         args.putInt(MessagesDialog.MESSAGE1_STRING_ID_ARG, R.string.whatsnew_message);
         args.putInt(MessagesDialog.TITLE_STRING_ID_ARG, R.string.whatsnew_title);
         whatsnewDialog.setArguments(args);
         whatsnewDialog.show(fm, "What's New Dialog");
         mShowWhatsnew = false; //don't show this dialog on any subsequent onResume
      }
      
   }

   public int getVersionCode()
   {
      PackageManager packageManager = getPackageManager();
      String packageName = getPackageName();

      int versionCode = 0;

      try {
         versionCode = packageManager.getPackageInfo(packageName, 0).versionCode;
      } catch (PackageManager.NameNotFoundException e) {
          Log.w(TAG, "Could not lookup application version number code");
      }
      return versionCode;
   }
   /*
    * Used to refresh the blog list by running through the mBlogData data
    * structure
    */
   void refreshList()
   {
      Log.d(TAG, "refreshList " + mFileName);
      BlogListAdapter adapter = new BlogListAdapter(this);
      mBlogList.setAdapter(adapter);
      for (int i = 0; i < mBlogData.getMaxBlogElements(); i++)
      {
         BlogElement blog = mBlogData.getBlogElement(mirrorElement(i));
         adapter.addItem(new BlogListData(blog.title, blog.description));
      }
      mBlogList.setAdapter(adapter);
      
      // update ActionBar title with blog name
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();
      actionBar.setTitle(fileToTripName(mFileName));
   }

   /* Because we have the most recent post at the top (standard blog view) */
   int mirrorElement(int in)
   {
      int out;
      out = mBlogData.getMaxBlogElements() - in;
      out -= 1;
      return out;
   }

   void initList()
   {
      /*
      // DEBUG: dump it all out
      for(int i = 0; i < mBlogData.getMaxBlogElements(); ++i) {
         BlogElement blog = mBlogData.getBlogElement(i);
         Log.d(TAG, "Name: "+ blog.name + " Descr: "+ blog.description + " Loc: "+ blog.location);
      }
       */
      
      refreshList();

      mBlogList.setDividerHeight(3);

      /* Add Context-Menu listener to the ListView. */
      mBlogList
            .setOnCreateContextMenuListener(new OnCreateContextMenuListener()
            {
               @Override
               public void onCreateContextMenu(ContextMenu conMenu, View v,
                     ContextMenuInfo conMenuInfo)
               {
                  conMenu.setHeaderTitle("Post Menu");
                  conMenu.add(ContextMenu.NONE, CONTEXTMENU_EDITITEM,
                        ContextMenu.NONE, "Edit Post");
                  conMenu.add(ContextMenu.NONE, CONTEXTMENU_DELETEITEM,
                        ContextMenu.NONE, "Delete Post");
               }

            });
   }

   @Override
   public boolean onContextItemSelected(MenuItem aItem)
   {
      AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
            .getMenuInfo();

      /* Switch on the ID of the item, to get what the user selected. */
      switch (aItem.getItemId())
      {
         case CONTEXTMENU_EDITITEM:
            /* Get the selected item out of the Adapter by its position. */
            editBlogEntry(mirrorElement(menuInfo.position));
            return true; /* true means: "we handled the event". */

         case CONTEXTMENU_DELETEITEM:
            if (menuInfo.position < mBlogData.getMaxBlogElements())
            {
               mDeleteItem = mirrorElement(menuInfo.position);
               TravelLocBlogMain.areYouSure(this, this.getString(R.string.are_you_sure_post),
                     new DialogInterface.OnClickListener()
               {
                  public void onClick(DialogInterface dialog, int id)
                  {
                     dialog.dismiss();
                     switch (id)
                     {
                     case DialogInterface.BUTTON_POSITIVE:
                        deletePost();
                        break;
                     case DialogInterface.BUTTON_NEGATIVE:
                     default:
                        mDeleteItem = -1;
                        break;
                     }
                  }});
            }
            refreshList();
            return true; /* true means: "we handled the event". */
      }
      return false;
   }

   /* To delete a post, then notify the user, then refresh the listview */
   void deletePost()
   {
      if ((mDeleteItem != -1) && (mDeleteItem < mBlogData.getMaxBlogElements()))
      {
         if (mBlogData.deleteBlogElement(mDeleteItem) == false)
         {
            Toast.makeText(this, R.string.failed_post_delete,
                  Toast.LENGTH_SHORT).show();
         }
         else
         {
            Toast.makeText(this, R.string.post_deleted,
                  Toast.LENGTH_SHORT).show();
         }
         mDeleteItem = -1;
         refreshList();
      }
   }
   /**
    * This method is called when the user clicks a note in the displayed list.
    *
    * This method handles incoming actions of either PICK (get data from the provider) or
    * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
    * new Intent to start NoteEditor.
    * @param l The ListView that contains the clicked item
    * @param v The View of the individual item
    * @param position The position of v in the displayed list
    * @param id The row ID of the clicked item
    */
   // @Override LATER TODO make this a ListActivity
   protected void onListItemClick(ListView l, View v, int position, long id)
   {

      // Constructs a new URI from the incoming URI and the row ID
      // Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

      // Gets the action from the incoming Intent
      String action = getIntent().getAction();

      // Handles requests for note data
      if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action))
      {

         // Sets the result to return to the component that called this Activity. The
         // result contains the new URI
         //setResult(RESULT_OK, new Intent().setData(uri));
      }
      else
      {

         // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
         // Intent's data is the note ID URI. The effect is to call NoteEdit.
         //startActivity(new Intent(Intent.ACTION_EDIT, uri));
      }
   }

   /*
    * The edit blog post activity comes back to here, so we save what we need to
    * from the extras bundle
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data)
   {
      super.onActivityResult(requestCode, resultCode, data);
      if (!(requestCode == NEW_BLOG_ENTRY || requestCode == EDIT_BLOG_ENTRY))
      {
         Log.e(TAG, "Unexpected activity code " + requestCode);
         return;
      }
            
      int editItem = (requestCode == EDIT_BLOG_ENTRY) ? mEditItem : -1;

      switch (resultCode)
      {
      case RESULT_DELETE_POST:
         Log.d(TAG, "Got result delete post " + editItem);
         mDeleteItem = editItem;
         deletePost();
         break;

      case RESULT_CANCELED:
         break;
         
      case RESULT_OK:
         BlogElement blog = new BlogElement();
         Bundle extras = data.getExtras();

         blog.title = extras.getString("BLOG_NAME");
         blog.description = extras.getString("BLOG_DESCRIPTION");
         blog.location = extras.getString("BLOG_LOCATION");
         blog.timeStamp = extras.getString("BLOG_TIMESTAMP");

         // Log.d(TAG, "resultCode = " + resultCode + " requestCode = " + requestCode);
         Log.d(TAG, "Edit complete, saving location " + blog.location);

         if (mBlogData.saveBlogElement(blog, editItem) == false)
         {
            Toast.makeText(this, R.string.failed_post_save,
                  Toast.LENGTH_SHORT).show();
         }
         else
         {
            Toast.makeText(this, R.string.post_saved,
                  Toast.LENGTH_SHORT).show();
         }
         refreshList();
         break;
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.trip_menu, menu);
      
      // Do not use ShareActionProvider
      // Turns out there is no way (that works) to avoid Android inserting an extra icon
      // in the action bar on a Share click, taking up valuable action bar space.
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId())
      {
         case R.id.new_note:
            newBlogEntry();
            return true;
         case R.id.open_trip:
            openTrip();
            return true;
         case R.id.send_trip:
            sendTrip();
            return true;
         case R.id.new_trip:
            newTrip();
            return true;
         case R.id.delete_trip:
            deleteTrip();
            return true;
         case R.id.map_trip:
            mapTrip();
            return true;
         case R.id.help:
            TravelLocBlogMain.showHelp(getSupportFragmentManager());
            return true;
         case R.id.trip_info:
            showTripInfo();
            return true;
         case R.id.settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
         case R.id.send_feedback:
            TravelLocBlogMain.sendFeedback(this);
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   void showTripInfo()
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(
            TravelLocBlogMain.this);
      String str = "Trip: " + TRIP_PATH + "/" + mFileName;
      str = str + "\nPosts: " + mBlogData.getMaxBlogElements();
      String dist = "";
      float fdistkm = mBlogData.getTotalDistance();
      fdistkm /= 1000.0F;
      dist = String.format(Locale.US, "%.2f km", fdistkm);
      str = str + "\nTotal Distance: " + dist;
      fdistkm *= 0.6213711;
      dist = String.format(Locale.US, "%.2f miles", fdistkm);
      str = str + " or " + dist;
      builder.setMessage(str);
      builder.setTitle(this.getString(R.string.menu_info));
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

   void sendTrip()
   {
      try
      {
         File file = new File(Environment.getExternalStorageDirectory()
               + TRIP_PATH + "/" + mFileName);
         Intent sendIntent = new Intent(Intent.ACTION_SEND);
         
         // text/plain would be best, but seems like some mail apps (HTC
         // phones?) need text/html, etc, so just use text/* as the setType
         sendIntent.setType("text/*");
         
         sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
         sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         sendIntent.putExtra(Intent.EXTRA_SUBJECT,
               getString(R.string.share_subject_line));
         sendIntent.putExtra(Intent.EXTRA_TEXT,
               String.format(getString(R.string.share_body_text), mFileName));
         startActivity(Intent.createChooser(sendIntent,
               getString(R.string.share_prompt)));
      }
      catch (Exception e)
      {
         Log.e(TAG, "error occurred while sending file");
         return;
      }
   }

   CharSequence[] getFileList()
   {
      CharSequence[] fileList = null;
      try
      {
         fileList = new File(Environment.getExternalStorageDirectory()
               + TRIP_PATH).list();
      }
      catch (Exception e)
      {
         Log.e(TAG, "error occurred while reading file list");
      }
      return fileList;
   }

   /**
    * Display the list of trips (.kml files) and executes listener code
    * on a click.
    * @param listener code to execute on a file select and click.
    */
   void tripsDialog(DialogInterface.OnClickListener listener)
   {
      mFileList = getFileList();
      if (mFileList == null)
      {
         Toast.makeText(this, R.string.no_files_found,
               Toast.LENGTH_SHORT).show();
         return;
      }
      if (mFileList.length == 0)
      {
         Toast.makeText(this, R.string.no_files_found,
               Toast.LENGTH_SHORT).show();
         return;
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Choose a Trip");
      builder.setSingleChoiceItems(mFileList, -1, listener);
      builder.create();
      builder.show();
   }

   void openTrip()
   {
      tripsDialog(new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item)
         {
            dialog.dismiss();
            openTripFile(mFileList[item].toString());
         }
      });
   }

   void deleteTrip()
   {
      final Context context = this;
      // Display the list of files, and when item is selected for delete,
      // display the areYouSure dialog.
      // Essentially:   tripsDialog( listListener, areYouSure(msg, yesNoListener) )

      DialogInterface.OnClickListener listListener
      = new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, final int item)
         {
            dialog.dismiss();
            final String fileName = mFileList[item].toString();
            String msg = String.format(getString(R.string.are_you_sure_file), fileName);

            // areYouSure dialog listener
            DialogInterface.OnClickListener yesNoListener
            = new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id)
               {
                  dialog.dismiss();
                  boolean deleted = false;
                  switch (id)
                  {
                     case DialogInterface.BUTTON_POSITIVE:
                        deleted = mBlogData.deleteBlog(fileName);
                        break;
                     case DialogInterface.BUTTON_NEGATIVE:
                     default:
                        break;
                  }
                  if (deleted)
                  {
                     mFileList = getFileList();
                     if (mFileName.equals(fileName))
                     {
                        // Deleting currently open file so open some other file
                        // Pick the 0th file to display (any will do),
                        // otherwise use default
                        if (mFileList != null && mFileList.length > 0)
                           mFileName = mFileList[0].toString();
                        else
                           mFileName = getString(R.string.default_trip);
                        
                        setDefaultTrip(mFileName);
                        mBlogData.openBlog(mFileName);
                        refreshList();            
                     }
                     Toast.makeText(context, R.string.deleted_file,
                           Toast.LENGTH_LONG).show();
                  }
                  else
                  {
                     Toast.makeText(context, R.string.delete_file_failed,
                           Toast.LENGTH_LONG).show();
                  }
               }
            };
            // Display the areYouSure dialog, and on Yes, delete the file
            TravelLocBlogMain.areYouSure(context, msg, yesNoListener);
         }
      };
      
      // Display the list of files, and when item is selected for delete,
      // display the areYouSure dialog.
      tripsDialog(listListener);
   }

   void setDefaultTrip(String file)
   {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString(SettingsActivity.LAST_OPENED_TRIP_KEY, file);
      editor.commit();
   }

   void newTrip()
   {
      mNewDialog.setTitle(R.string.menu_new);
      mNewDialog.setContentView(R.layout.new_trip);
      
      // Show keyboard since we need to type in a file name
      mNewDialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
      
      Button save = (Button) mNewDialog.findViewById(R.id.new_button);
      save.setOnClickListener(new OnClickListener()
      {
         public void onClick(View view)
         {
            newTripOnClick();
         }
      });
      Button cancel = (Button) mNewDialog.findViewById(R.id.cancel_button);
      cancel.setOnClickListener(new OnClickListener()
      {
         public void onClick(View view)
         {
            mNewDialog.cancel();
         }
      });
      mNewDialog.show();
   }

   public void newTripOnClick()
   {
      EditText text = (EditText) mNewDialog.findViewById(R.id.file_name);
      String str = text.getText().toString().trim();
      // Log.d(TAG, "new file: " + str);
      if (str.length() == 0)
      {
         Toast.makeText(this, R.string.invalid_filename,
               Toast.LENGTH_SHORT).show();
         return;
      }
      if (str.contains(".") == true)
      {
         Toast.makeText(this, R.string.invalid_filename_period,
               Toast.LENGTH_SHORT).show();
         return;
      }
      File newFile = new File(Environment.getExternalStorageDirectory()
            + TRIP_PATH + "/" + str + KML_SUFFIX);
      if (newFile.exists() == true)
      {
         Toast.makeText(this, R.string.invalid_filename_exists,
               Toast.LENGTH_SHORT).show();
         return;
      }
      mNewDialog.cancel();
      if (mBlogData.newBlog(str + KML_SUFFIX) == false)
      {
         Toast.makeText(this, R.string.failed_file_create,
               Toast.LENGTH_SHORT).show();
         return;
      }
      mFileName = str + KML_SUFFIX;

      setDefaultTrip(mFileName);

      refreshList();
   }

   void openTripFile(String file)
   {
      mFileName = file;
      setDefaultTrip(mFileName);
      mBlogData.openBlog(mFileName);
      refreshList();
   }

   boolean deleteTripFile(String file)
   {
      boolean deleted = mBlogData.deleteBlog(mFileName);
      if (deleted == true)
      {
      }
      return deleted;
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
      
      // After dialog is shown, we can get the textview and make it HTML capable
      // TODO: not working, does not display HTML - similar code works in
      // AboutActivity though. Leaving it in for now, may look into this later.
      TextView tv = (TextView)dialog.findViewById(android.R.id.message);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      tv.setClickable(true);
   }

   /* Open the map view activity to display the trip */
   void mapTrip()
   {
      if (mBlogData.getMaxBlogElements() > 0)
      {
         // Log.d(TAG, "Map Trip");
         Intent i = new Intent(this, TripMapView.class);
         Bundle b = new Bundle();
         b.putString("TRIP", mFileName);
         i.putExtras(b);
         startActivity(i);
      }
      else
      {
         Toast.makeText(this, R.string.map_trip_empty,
               Toast.LENGTH_SHORT).show();
      }
   }
   
   /**
    * Send Feedback opens up an Email client addressed to Travel Blog mailing list.
    * Called from multiple activities so uses activity Context as an arg.
    */
   private static void sendFeedbackEmail(Context context)
   {
      
      Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
            Uri.fromParts("mailto", context.getString(R.string.feedback_email), null));
      emailIntent.putExtra(Intent.EXTRA_SUBJECT,
            context.getString(R.string.feedback_subject));
      
      // Initialize body text with a few system details
      String text = String.format("%s. Android %s (SDK %d)\n",
            Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
      
      emailIntent.putExtra(Intent.EXTRA_TEXT, text);

      context.startActivity(Intent.createChooser(emailIntent,
            context.getString(R.string.feedback_title)));
   }
   
   /**
    * Top level feedback command.
    * First, display Private Notice and then on confirmation, call sendFeedBackEmail.
    * Called from multiple activities so uses activity Context as an arg.
    */

   public static void sendFeedback(final Context context)
   {
      TravelLocBlogMain.areYouSure(context,
            context.getString(R.string.feedback_privacy_notice),
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id)
         {
            dialog.dismiss();
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
               TravelLocBlogMain.sendFeedbackEmail(context);
               break;
            case DialogInterface.BUTTON_NEGATIVE:
            default:
               Toast.makeText(context, R.string.feedback_cancel,
                     Toast.LENGTH_LONG).show();
               break;
            }
         }});
   }

   public static void showHelp(FragmentManager fm)
   {
      MessagesDialog helpDialog = new MessagesDialog();
      Bundle args = new Bundle();
      args.putInt(MessagesDialog.MESSAGE1_STRING_ID_ARG, R.string.help_message);
      args.putInt(MessagesDialog.MESSAGE2_STRING_ID_ARG, R.string.whatsnew_message);
      args.putInt(MessagesDialog.TITLE_STRING_ID_ARG, R.string.help_title);
      helpDialog.setArguments(args);
      helpDialog.show(fm, "Help Dialog");
   }
   
   /**
    * Utility function to convert file name to trip name
    */
   public static String fileToTripName(String filename)
   {
      String name = filename == null ? "" : filename;
      // Show the name without the suffix, and check that name is at least 1 char
      int lastSuffix = name.lastIndexOf(KML_SUFFIX);
      if (lastSuffix > 1) name = name.substring(0, lastSuffix);
      return name;
   }
   
}
