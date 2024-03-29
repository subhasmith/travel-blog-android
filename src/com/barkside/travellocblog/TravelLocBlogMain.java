package com.barkside.travellocblog;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
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

   // Blog Manager stores name and uri of current blog, as well
   // as a static shared data pointer to actual file data.
   private BlogDataManager mBlogMgr = new BlogDataManager();

   public static final int NEW_BLOG_ENTRY = 100;
   public static final int EDIT_BLOG_ENTRY = 111;
   
   // Custom result returned by EditBlogPost activity
   public static final int RESULT_DELETE_POST = RESULT_FIRST_USER + 0;
   public static final int RESULT_SAVE_FAILED = RESULT_FIRST_USER + 1;

   private static final int CONTEXTMENU_EDITITEM = 0;
   private static final int CONTEXTMENU_DELETEITEM = 1;
   
   // Named preference file use deprecated - see SettingsActivity class comments.
   public static final String PREFS_NAME = "MyPrefsFile"; // deprecated as of versionCode 6
   
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
      
      if (savedInstanceState != null)
      {
         // Device reoriented, need to get name of correct Uri to use
         mBlogMgr.onRestoreInstanceState(savedInstanceState);
         // No need to reopen blog here, onResume will handle it.
      }
      else
      {
         Uri uri;
         String blogname = "";
         // No saved instance, use data from intent
         // We may be called with no data, in which case we open the last opened file,
         // or we may be passed an Uri to open.
         Intent intent = getIntent();
         uri = intent.getData();
         Log.d(TAG, " Got Uri from intent: " +  uri);
         blogname = Utils.openBlogFromIntent(this, uri, mBlogMgr);
         if (blogname == null)
         {
            // Could not open either named file, or default file.
            // Don't exit activity - let user select new file from the menu.
            Log.w(TAG, "Failed to open any file including default.");
         }
      }
      
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      
      // All new (Oct 2013+) settings always use standard default settings location
      settings = PreferenceManager.getDefaultSharedPreferences(this);

      // Check if app got updated after the last time it was run
      int lastVersionCode = settings.getInt(SettingsActivity.LAST_VERSION_USED_KEY, -1);
      PackageInfo pInfo = Utils.getAppPackageInfo(this);
      int versionCode = pInfo.versionCode;
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
      if ((index >= mBlogMgr.getMaxBlogElements()) || (index < 0))
      {
         Log.d(TAG, "Edit Log Entry illegal: " + index);
         return;
      }
      Uri uri = mBlogMgr.uri();
      if (uri != null)
      {
         Intent i = new Intent(this, EditBlogElement.class);
         uri = ContentUris.withAppendedId(uri, index);
         i.setData(uri);

         i.setAction(Intent.ACTION_INSERT_OR_EDIT);
         startActivityForResult(i, EDIT_BLOG_ENTRY);         
      }
   }

   /* new blog post passes in a fallback location in the  extras,
    * to be used if no location found
    */
   private void newBlogEntry()
   {
      Uri uri = mBlogMgr.uri();
      if (uri != null)
      {
         Intent i = new Intent(this, EditBlogElement.class);
         i.setData(uri);
         i.setAction(Intent.ACTION_INSERT_OR_EDIT);
         Log.d(TAG, "New Log Entry");
         startActivityForResult(i, NEW_BLOG_ENTRY);         
      }
   }

   @Override
   protected void onResume() {
      super.onResume();
      
      // We may come here from another Travel Blog activity screen through the
      // Android back stack, so the current trip pointed to mBlogMgr may have
      // changed. Therefore, we should load the blog again. If current file
      // has not changed, openTrip blogMgr will just use current data and won't
      // actually load the file from disk.
      
      boolean opened = mBlogMgr.onResumeSetup(this, R.string.open_failed_one_file);
      // Even if failed to open, refresh list of items (to empty if needed).
      refreshList();

      if (!opened)
      {
         // Do not finish this main activity. Allow user to select new file
         // from the menu.
         ActionBar actionBar = getSupportActionBar();
         actionBar.setTitle(R.string.open_failed_title);
      }
      
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

   /**
    * We need to survive a device orientation change. Android will completely destroy
    * and recreate this activity.
    * If we don't remember mBlogUri for example, and user had used Open Trip
    * menu command, the onCreate activity may use the original intent uri, which
    * is not correct.
    */
   
   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
     super.onSaveInstanceState(savedInstanceState);
     Log.d(TAG, "save instance state");
     // Save UI state changes to the savedInstanceState.
     // This bundle will be passed to onCreate if the process is
     // killed and restarted.
     mBlogMgr.onSaveInstanceState(savedInstanceState);
   }

   /*
    * Used to refresh the blog list by running through the mBlogData data
    * structure
    */
   void refreshList()
   {
      String blogname = mBlogMgr.blogname();
      Log.d(TAG, "refreshList " + blogname);
      BlogListAdapter adapter = new BlogListAdapter(this);
      for (int i = 0; i < mBlogMgr.getMaxBlogElements(); i++)
      {
         BlogElement blog = mBlogMgr.getBlogElement(mirrorElement(i));
         adapter.addItem(new BlogListData(blog.title, blog.description));
      }
      mBlogList.setAdapter(adapter);
      
      // update ActionBar title with blog name
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();
      if (blogname.equals("")) {
         actionBar.setTitle(R.string.open_failed_title);         
      } else {
         actionBar.setTitle(Utils.blogToDisplayname(blogname));         
      }
   }

   /* Because we have the most recent post at the top (standard blog view) */
   int mirrorElement(int in)
   {
      int out;
      out = mBlogMgr.getMaxBlogElements() - in;
      out -= 1;
      return out;
   }

   // Setup the ListView and allocate mBlogList variable.
   // Does not use mBlogMgr, and only needs to be called once, usually in onCreate.
   void initListUI()
   {
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
            if (menuInfo.position < mBlogMgr.getMaxBlogElements())
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
                              deleteItem, mBlogMgr);
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

   void sendTrip()
   {
      try
      {
         String blogname = mBlogMgr.blogname();
         File file = mBlogMgr.blogToFile(blogname);
         Intent sendIntent = new Intent(Intent.ACTION_SEND);
         
         // text/plain would be best, but seems like some mail apps (HTC
         // phones?) need text/html, etc, so just use text/* as the setType
         sendIntent.setType("text/*");
         
         sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
         sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         sendIntent.putExtra(Intent.EXTRA_SUBJECT,
               getString(R.string.share_subject_line));
         sendIntent.putExtra(Intent.EXTRA_TEXT,
               String.format(getString(R.string.share_body_text), blogname));
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
      final CharSequence[] fileList = mBlogMgr.getBlogsList();

      TravelLocBlogMain.selectTripDialog(this, fileList,
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item)
         {
            dialog.dismiss();
            String newname = fileList[item].toString();
            openTrip(newname);
            // Refresh screen and save blog name to preferences
            setOpenedFile();
         }
      });
   }

   void deleteTripUI()
   {
      final Context context = this;
      final CharSequence[] fileList = mBlogMgr.getBlogsList();
      final String currentName = mBlogMgr.blogname();

      // Display the list of files, and when item is selected for delete,
      // display the areYouSure dialog.
      // Essentially: selectTripDialog( listListener, areYouSure(msg, yesNoListener) )

      DialogInterface.OnClickListener listListener
      = new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, final int item)
         {
            dialog.dismiss();
            final String fileName = fileList[item].toString();
            String msg = String.format(getString(R.string.are_you_sure_delete), fileName);

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
                        deleted = mBlogMgr.deleteBlog(fileName);
                        break;
                     case DialogInterface.BUTTON_NEGATIVE:
                     default:
                        break;
                  }
                  if (deleted)
                  {
                     // After file deleted, refresh list and check if current file
                     // was removed.
                     CharSequence[] fileList = mBlogMgr.getBlogsList();
                     String message = String.format(context.getString(R.string.deleted_one_file),
                           fileName);

                     Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                     
                     if (currentName.equals(fileName))
                     {
                        // Deleting currently open file so open some other file
                        // Pick the 0th file to display (any will do),
                        // otherwise use default
                        String newname = null;
                        if (fileList != null && fileList.length > 0) {
                           newname = fileList[0].toString();
                        } else {
                           newname = Utils.createDefaultTrip(context, mBlogMgr);
                        }
                        if (newname != null) {
                           openTrip(newname);
                           // Refresh screen and save blog name to preferences
                           setOpenedFile();                           
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
      TravelLocBlogMain.selectTripDialog(this, fileList, listListener);
   }

   // We have opened this file, so write it to preferences and display its contents
   private void setOpenedFile()
   {
      String name = mBlogMgr.blogname();
      if (!name.equals(""))
      {
         Utils.setPreferencesLastOpenedTrip(this, name);         
      }
      refreshList();
   }

   // Display new trip UI
   void newTripUI()
   {
      final Dialog newFileDialog = new Dialog(this);
      TravelLocBlogMain.newTrip(newFileDialog, R.string.menu_new,
            new View.OnClickListener() {
         public void onClick(View v) {
            TravelLocBlogMain.newTripFilename(newFileDialog, mBlogMgr);
            setOpenedFile();
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
            String str = TravelLocBlogMain.newTripFilename(newFileDialog, mBlogMgr);
            if (str != null)
            {
               String currentName = mBlogMgr.blogname();
               boolean renamed = mBlogMgr.renameBlog(currentName, str);
               if (renamed) {
                  setOpenedFile();
               } else {
                  Toast.makeText(context, R.string.failed_file_rename,
                        Toast.LENGTH_LONG).show();
               }
            }
         }
      });
   }

   // Open trip and return true if successful. Does not save file name to preferences.
   boolean openTrip(String blogname)
   {
      Uri uri = Utils.blognameToUri(blogname);
      boolean opened = mBlogMgr.openBlog(this, uri, R.string.open_failed_one_file);
      return opened;
   }

   /* Open the map view activity to display the trip */
   void mapTrip()
   {
      Uri uri = mBlogMgr.uri();
      if (uri != null && mBlogMgr.getMaxBlogElements() > 0)
      {
         // Log.d(TAG, "Map Trip");
         Intent i = new Intent(this, MapTrip.class);
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
