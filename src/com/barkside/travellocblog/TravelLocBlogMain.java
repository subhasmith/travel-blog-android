package com.barkside.travellocblog;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Main activity. Comes here from the launcher.
 * 
 * Using a FragmentActivity instead of Activity to call getSupportFragmentManager
 * for the Help, About, and What's New message screens.
 *
 * Displays a list of blog entries from the last used trip file.
 * Terms: Blogname is name of the blog or trip, example: MyFirstTrip.kml
 * Sometimes we also call this a filename, for historical reasons.
 * Displayname is the name shown in some parts of the UI - the blogname without
 * the .kml suffix.
 * 
 */

public class TravelLocBlogMain extends ActionBarActivity
{
   // For logging and debugging purposes
   private static final String TAG = "TravelLocBlogMain";

   private BlogDataManager mBlogData = BlogDataManager.getInstance();

   public static final int NEW_BLOG_ENTRY = 100;
   public static final int EDIT_BLOG_ENTRY = 111;
   
   // Custom result returned by EditBlogPost activity
   public static final int RESULT_DELETE_POST = RESULT_FIRST_USER + 0;
   public static final int RESULT_SAVE_FAILED = RESULT_FIRST_USER + 1;

   private static final int CONTEXTMENU_EDITITEM = 0;
   private static final int CONTEXTMENU_DELETEITEM = 1;
   
   // Named preference file use deprecated - see SettingsActivity class comments.
   public static final String PREFS_NAME = "MyPrefsFile"; // deprecated as of versionCode 6
   
   private CharSequence[] mFileList;
   private String mBlogname = null;
   
   private boolean mShowWhatsnew = false;
   
   ListView mBlogList;

   // Shared Utility Variables
   // Certain characters that may cause problems downloading or emailing are disallowed
   // in file names
   // Note that there are many other characters such as + etc that are sometimes
   // prohibited, but they all work fine through the Android API so allowing them.
   static final String INVALID_CHARS = "[/\\\\*?<>:]";
   static final Pattern INVALID_CHARS_PATTERN = Pattern.compile(INVALID_CHARS);

   /* Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      initListUI(); // sets up mBlogList
      
      // We may be called with no data, in which case we open the last opened file,
      // or we may be passed an Uri to open.
      Intent intent = getIntent();
      Uri uri = intent.getData();
      Log.d(TAG, " Got Uri " +  uri);

      mBlogname = Utils.openBlogFromIntent(this, uri, mBlogData);
      if (mBlogname == null)
      {
         // Could not open either named file, or default file. App is unusable.
         Log.w(TAG, "Failed to open any file including default - app is unusable.");
         mBlogname = "";
      }
      setOpenedFile(mBlogname);         

      Log.d(TAG, "onCreate file opened " + mBlogname);
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      
      // All new (Oct 2013+) settings always use standard default settings location
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
      Intent i = new Intent(this, EditBlogElement.class);
      Uri uri = Utils.blognameToUri(mBlogname);
      uri = ContentUris.withAppendedId(uri, index);
      i.setData(uri);

      i.setAction(Intent.ACTION_INSERT_OR_EDIT);
      startActivityForResult(i, EDIT_BLOG_ENTRY);
   }

   /* new blog post passes in a fallback location in the  extras,
    * to be used if no location found
    */
   private void newBlogEntry()
   {
      Intent i = new Intent(this, EditBlogElement.class);
      Uri uri =  Utils.blognameToUri(mBlogname);
      i.setData(uri);
      i.setAction(Intent.ACTION_INSERT_OR_EDIT);
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
      Log.d(TAG, "refreshList " + mBlogname);
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
      actionBar.setTitle(Utils.blogToDisplayname(mBlogname));
   }

   /* Because we have the most recent post at the top (standard blog view) */
   int mirrorElement(int in)
   {
      int out;
      out = mBlogData.getMaxBlogElements() - in;
      out -= 1;
      return out;
   }

   // Setup the ListView and allocate mBlogList variable.
   void initListUI()
   {
      /*
      // DEBUG: dump it all out
      for(int i = 0; i < mBlogData.getMaxBlogElements(); ++i) {
         BlogElement blog = mBlogData.getBlogElement(i);
         Log.d(TAG, "Name: "+ blog.name + " Descr: "+ blog.description + " Loc: "+ blog.location);
      }
       */
      mBlogList = (ListView) this.findViewById(R.id.blog_list);
      
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
               final int deleteItem = mirrorElement(menuInfo.position);
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
                        boolean deleted = EditBlogElement.deletePost(context,
                              deleteItem, mBlogData);
                        // Error messages if any are printed by EditBlogElement.deletePost
                        if (deleted)
                        {
                           refreshList();         
                        }
                        break;
                     case DialogInterface.BUTTON_NEGATIVE:
                     default:
                        break;
                     }
                  }});
            }
            refreshList();
            return true; /* true means: "we handled the event". */
      }
      return false;
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
            
      switch (resultCode)
      {
      case RESULT_CANCELED:
         break;
         
      case RESULT_OK:
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
            openTripUI();
            return true;
         case R.id.send_trip:
            sendTrip();
            return true;
         case R.id.new_trip:
            newTripUI();
            return true;
         case R.id.delete_trip:
            deleteTripUI();
            setOpenedFile(mBlogname);// TODO TESTING DELETE ME
            return true;
         case R.id.rename_trip:
            renameTripUI();
            return true;
         case R.id.map_trip:
            mapTrip();
            return true;
         case R.id.help:
            Utils.showHelp(getSupportFragmentManager());
            return true;
         case R.id.trip_info:
            showTripInfo();
            return true;
         case R.id.settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
         case R.id.send_feedback:
            Utils.sendFeedback(this, TAG);
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   void showTripInfo()
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(
            TravelLocBlogMain.this);
      String str = "Trip: " + mBlogname;
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
         File file = mBlogData.blogToFile(mBlogname);
         Intent sendIntent = new Intent(Intent.ACTION_SEND);
         
         // text/plain would be best, but seems like some mail apps (HTC
         // phones?) need text/html, etc, so just use text/* as the setType
         sendIntent.setType("text/*");
         
         sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
         sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         sendIntent.putExtra(Intent.EXTRA_SUBJECT,
               getString(R.string.share_subject_line));
         sendIntent.putExtra(Intent.EXTRA_TEXT,
               String.format(getString(R.string.share_body_text), mBlogname));
         startActivity(Intent.createChooser(sendIntent,
               getString(R.string.share_prompt)));
      }
      catch (Exception e)
      {
         Log.e(TAG, "error occurred while sending file");
         return;
      }
   }

   /**
    * Display the list of trips (.kml files) and executes listener code
    * on a click.
    * Function shared with multiple classes, also used by WidgetConfigure.
    * @param listener code to execute on a file select and click.
    */
   public static CharSequence[] selectTripDialog(final Context context,
         final CharSequence[] fileList, DialogInterface.OnClickListener listener)
   {
      if (fileList == null)
      {
         Toast.makeText(context, R.string.no_files_found,
               Toast.LENGTH_SHORT).show();
         return null;
      }
      if (fileList.length == 0)
      {
         Toast.makeText(context, R.string.no_files_found,
               Toast.LENGTH_SHORT).show();
         return null;
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle("Choose a Trip");
      builder.setSingleChoiceItems(fileList, -1, listener);
      builder.create();
      builder.show();
      
      return fileList;
   }

   void openTripUI()
   {
      mFileList = mBlogData.getBlogsList();
      TravelLocBlogMain.selectTripDialog(this, mFileList,
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item)
         {
            dialog.dismiss();
            openTrip(mFileList[item].toString());
         }
      });
   }

   void deleteTripUI()
   {
      final Context context = this;
      mFileList = mBlogData.getBlogsList();
      // Display the list of files, and when item is selected for delete,
      // display the areYouSure dialog.
      // Essentially: selectTripDialog( listListener, areYouSure(msg, yesNoListener) )

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
                     // After file deleted, refresh list and check if current file
                     // was removed.
                     mFileList = mBlogData.getBlogsList();
                     Toast.makeText(context, R.string.deleted_file,
                           Toast.LENGTH_LONG).show();
                     if (mBlogname.equals(fileName))
                     {
                        // Deleting currently open file so open some other file
                        // Pick the 0th file to display (any will do),
                        // otherwise use default
                        if (mFileList != null && mFileList.length > 0) {
                           mBlogname = mFileList[0].toString();
                        } else {
                           mBlogname = Utils.createDefaultTrip(context, mBlogData);
                        }
                        if (mBlogname != null) {
                           openTrip(mBlogname);
                           // Refresh screen and save blog name to preferences
                           setOpenedFile(mBlogname);                           
                        }
                     }
                  }
                  else
                  {
                     Toast.makeText(context, R.string.delete_file_failed,
                           Toast.LENGTH_LONG).show();
                  }
               }
            };
            // Display the areYouSure dialog, and on Yes, delete the file
            Utils.areYouSure(context, msg, yesNoListener);
         }
      };
      
      // Display the list of files, and when item is selected for delete,
      // display the areYouSure dialog.
      TravelLocBlogMain.selectTripDialog(this, mFileList, listListener);
   }

   // We have opened this file, so write it to preferences and display its contents
   private void setOpenedFile(String name)
   {
      if (name == null) return;
      mBlogname = name;
      setPreferencesLastOpenedTrip(mBlogname);
      refreshList();
   }

   /**
    * Save given filename as last_opened_trip in the preferences,
    * if a different name was stored.
    */
   void setPreferencesLastOpenedTrip(String filename)
   {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      String oldname = settings.getString(SettingsActivity.LAST_OPENED_TRIP_KEY, null);
      if (oldname == null || !oldname.equals(filename))
      {
         SharedPreferences.Editor editor = settings.edit();
         editor.putString(SettingsActivity.LAST_OPENED_TRIP_KEY, filename);
         Log.d(TAG, "Prefs: Saving last opened trip " + filename);
         editor.commit();         
      }
   }

   // Display new trip UI
   void newTripUI()
   {
      final Dialog newFileDialog = new Dialog(this);
      TravelLocBlogMain.newTrip(newFileDialog, R.string.menu_new,
            new View.OnClickListener() {
         public void onClick(View v) {
            String str = TravelLocBlogMain.newTripFilename(newFileDialog, mBlogData);
            if (str != null)
            {
               setOpenedFile(str);
            }
         }
      });
   }

   // Show a New Trip dialog, and create new trip on success.
   // Used by both TravelLogBlogMain and WidgetConfigure activities.
   // Used for both New File and Rename file dialogs, only different is
   // titleId value of R.string.menu_new or R.string.menu_rename
   public static void newTrip(final Dialog newFileDialog, int titleId,
         View.OnClickListener onSaveListener)
   {
      newFileDialog.setTitle(titleId);
      newFileDialog.setContentView(R.layout.new_trip);
      
      // Show keyboard since we need to type in a file name
      newFileDialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
      
      Button save = (Button) newFileDialog.findViewById(R.id.new_button);
      save.setOnClickListener(onSaveListener);

      Button cancel = (Button) newFileDialog.findViewById(R.id.cancel_button);
      cancel.setOnClickListener(new OnClickListener()
      {
         public void onClick(View view)
         {
            newFileDialog.cancel();
         }
      });
      newFileDialog.show();
   }

   public static String newTripFilename(Dialog newFileDialog, BlogDataManager blogData)
   {
      // Certain characters are disallowed in filenames.
      
      EditText text = (EditText) newFileDialog.findViewById(R.id.file_name);
      String str = text.getText().toString().trim();
      Context context = newFileDialog.getContext();
      newFileDialog.dismiss();

      // Log.d(TAG, "new file: " + str);
      if (str.length() == 0)
      {
         Toast.makeText(context, R.string.invalid_filename,
               Toast.LENGTH_SHORT).show();
         return null;
      }
      // Check for invalid characters
      Log.d(TAG, "regex for invalid chars: " + INVALID_CHARS);
      Matcher m = INVALID_CHARS_PATTERN.matcher(str);
      if (m.find())
      {
         String message = String.format(context.getString(R.string.invalid_filename_message),
               INVALID_CHARS);
         Toast.makeText(context, message, Toast.LENGTH_LONG).show();
         return null;
      }

      str = Utils.displayToBlogname(str);
      if (blogData.existingBlog(str))
      {
         Toast.makeText(context, R.string.invalid_filename_exists,
               Toast.LENGTH_SHORT).show();
         return null;
      }
      
      if (blogData.newBlog(str) == false)
      {
         Toast.makeText(context, R.string.failed_file_create,
               Toast.LENGTH_SHORT).show();
         return null;
      }
      return str;
   }

   void renameTripUI()
   {
      final Dialog newFileDialog = new Dialog(this);
      final Context context = newFileDialog.getContext();

      TravelLocBlogMain.newTrip(newFileDialog, R.string.menu_rename_trip,
            new View.OnClickListener() {
         public void onClick(View v) {
            String str = TravelLocBlogMain.newTripFilename(newFileDialog, mBlogData);
            if (str != null)
            {
               boolean renamed = mBlogData.renameBlog(mBlogname, str);
               if (renamed) {
                  setOpenedFile(str);
               } else {
                  Toast.makeText(context, R.string.failed_file_rename,
                        Toast.LENGTH_LONG).show();
               }
            }
         }
      });
   }

   // Open trip and return true if successful. Also saves file name to preferences.
   boolean openTrip(String blogname)
   {
      boolean opened = mBlogData.openBlog(blogname);
      if (opened) {
         setOpenedFile(blogname);
      } else {
         String message = String.format(getString(R.string.open_failed_one_file),
               blogname);
         Toast.makeText(this, message, Toast.LENGTH_LONG).show();
      }
      return opened;
   }

   /* Open the map view activity to display the trip */
   void mapTrip()
   {
      if (mBlogData.getMaxBlogElements() > 0)
      {
         // Log.d(TAG, "Map Trip");
         Intent i = new Intent(this, TripMapView.class);
         Uri uri = Utils.blognameToUri(mBlogname);
         i.setData(uri);
         startActivity(i);
      }
      else
      {
         Toast.makeText(this, R.string.map_trip_empty,
               Toast.LENGTH_SHORT).show();
      }
   }
   
}
