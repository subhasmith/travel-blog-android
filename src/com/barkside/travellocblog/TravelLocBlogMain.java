package com.barkside.travellocblog;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
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
 * Displays a list of blog entries from the last used trip file.
 */

public class TravelLocBlogMain extends Activity
{
   // For logging and debugging purposes
   private static final String TAG = "TravelLocBlogMain";

   private BlogData mBlogData = new BlogData();

   public static final int NEW_BLOG_ENTRY = 100;
   public static final int EDIT_BLOG_ENTRY = 111;
   Dialog mNewDialog;
   CharSequence[] mFileList;
   public static String TRIP_PATH = "/TravelBlog";
   protected static final int CONTEXTMENU_EDITITEM = 0;
   protected static final int CONTEXTMENU_DELETEITEM = 1;
   public static final String PREFS_NAME = "MyPrefsFile";
   private int mEditItem = 0;
   private int mDeleteItem = -1;
   private static final String KML_SUFFIX = ".kml";
   private String mFileName = "MyFirstTrip" + KML_SUFFIX;
   ListView mBlogList;

   /* Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      this.mBlogList = (ListView) this.findViewById(R.id.blog_list);
      Button newPost = (Button) this.findViewById(R.id.new_blog);
      newPost.setOnClickListener(new OnClickListener()
      {
         public void onClick(View v)
         {
            newBlogEntry();
         }
      });
      
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


      /* Attempt to open last used blog file */
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      mFileName = settings.getString("defaultTrip",
            getString(R.string.default_trip));
      if ((mBlogData.openBlog(mFileName) == false)
            && (settings.contains("defaultTrip") == true))
      {
         Toast toast = Toast.makeText(this,
               "Failed to open last used blog file", Toast.LENGTH_LONG);
         toast.show();
      }
      mNewDialog = new Dialog(this);
      initList();
   }

   /* Called to edit a blog post, by passing extras in a bundle */
   private void editBlogEntry(int index)
   {
      // Log.d(TAG, "Edit Log Entry "+index);
      if ((index >= mBlogData.getMaxBlogElements()) || (index < 0))
      {
         return;
      }
      BlogElement blog = mBlogData.getBlogElement(index);
      Intent i = new Intent(this, EditBlogElement.class);
      Bundle b = new Bundle();
      b.putString("BLOG_NAME", blog.name);
      b.putString("BLOG_DESCRIPTION", blog.description);
      b.putString("BLOG_LOCATION", blog.location);
      b.putString("BLOG_TIMESTAMP", blog.timeStamp);
      i.putExtras(b);
      i.setAction("com.barkside.travellocblog.EDIT_BLOG_ENTRY");
      mEditItem = index;
      startActivityForResult(i, EDIT_BLOG_ENTRY);
   }

   /* new blog post has no extras */
   private void newBlogEntry()
   {
      Intent i = new Intent(this, EditBlogElement.class);
      Bundle b = new Bundle();
      b.putString("BLOG_NAME", null);
      b.putString("BLOG_DESCRIPTION", null);
      b.putString("BLOG_LOCATION", null);
      b.putString("BLOG_TIMESTAMP", null);
      i.putExtras(b);
      i.setAction("com.barkside.travellocblog.NEW_BLOG_ENTRY");
      Log.d(TAG, "New Log Entry");
      startActivityForResult(i, NEW_BLOG_ENTRY);
   }

   /*
    * Used to refresh the blog list by running through the mBlogData data
    * structure
    */
   void refreshList()
   {
      BlogListAdapter adapter = new BlogListAdapter(this);
      mBlogList.setAdapter(adapter);
      for (int i = 0; i < mBlogData.getMaxBlogElements(); i++)
      {
         BlogElement blog = mBlogData.getBlogElement(mirrorElement(i));
         adapter.addItem(new BlogListData(blog.name, blog.description));
      }
      mBlogList.setAdapter(adapter);
      TextView tv2 = (TextView) findViewById(R.id.trip_name);
      tv2.setText(fileToTripName(mFileName));
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
               areYouSure();
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
            Toast toast = Toast.makeText(this, "Failed to Delete Blog Post",
                  Toast.LENGTH_SHORT);
            toast.show();
         }
         else
         {
            Toast toast = Toast.makeText(this, "Blog Post Deleted",
                  Toast.LENGTH_SHORT);
            toast.show();
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
      BlogElement blog = new BlogElement();
      if (resultCode == RESULT_OK)
      {
         Bundle extras = data.getExtras();

         blog.name = extras.getString("BLOG_NAME");
         blog.description = extras.getString("BLOG_DESCRIPTION");
         blog.location = extras.getString("BLOG_LOCATION");
         blog.timeStamp = extras.getString("BLOG_TIMESTAMP");
      }

      // Log.d("TRAVEL_DEBUG", "resultCode = " + resultCode + " requestCode = "
      // + requestCode);

      switch (requestCode)
      {
         case NEW_BLOG_ENTRY:
            if (resultCode == RESULT_OK)
            {
               if (mBlogData.saveBlogElement(blog, -1) == false)
               {
                  Toast toast = Toast.makeText(this, "Blog Post Save Failed",
                        Toast.LENGTH_SHORT);
                  toast.show();
               }
               else
               {
                  Toast toast = Toast.makeText(this, "Blog Post Saved",
                        Toast.LENGTH_SHORT);
                  toast.show();
               }
            }
            break;
         case EDIT_BLOG_ENTRY:
            if (resultCode == RESULT_OK)
            {
               if (mBlogData.saveBlogElement(blog, mEditItem) == false)
               {
                  Toast toast = Toast.makeText(this, "Blog Post Save Failed",
                        Toast.LENGTH_SHORT);
                  toast.show();
               }
               else
               {
                  Toast toast = Toast.makeText(this, "Blog Post Saved",
                        Toast.LENGTH_SHORT);
                  toast.show();
               }
            }
            break;
      }
      refreshList();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.trip_menu, menu);
      return true;
   }

   @Override
   public boolean onMenuItemSelected(int featureId, MenuItem item)
   {
      switch (item.getItemId())
      {
         case R.id.open_trip:
            openTrip();
            return true;
         case R.id.send_trip:
            sendTrip();
            return true;
         case R.id.new_trip:
            newTrip();
            return true;
         case R.id.map_trip:
            mapTrip();
            return true;
         case R.id.help:
            showHelpDialog();
            return true;
         case R.id.trip_info:
            showTripInfo();
            return true;
         case R.id.about:
            startActivity(new Intent(this, AboutActivity.class));
            return true;
         default:
            return super.onMenuItemSelected(featureId, item);
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
         sendIntent.setType("*/*");
         sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
         sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         sendIntent.putExtra(Intent.EXTRA_SUBJECT,
               "Your Travel Blog KML file is attached");
         sendIntent.putExtra(Intent.EXTRA_TEXT,
               "Thank you for using Travel Blog.");
         startActivity(Intent.createChooser(sendIntent,
               "Choose how to send your trip KML file:"));
      }
      catch (Exception e)
      {
         Log.e(TAG, "error occurred while sending file");
         return;
      }

   }

   void openTrip()
   {
      try
      {
         mFileList = new File(Environment.getExternalStorageDirectory()
               + TRIP_PATH).list();
      }
      catch (Exception e)
      {
         Log.e(TAG, "error occurred while reading file list");
         return;
      }

      if (mFileList == null)
      {
         Toast toast = Toast.makeText(this, "No files to open",
               Toast.LENGTH_SHORT);
         toast.show();
         return;
      }
      if (mFileList.length == 0)
      {
         Toast toast = Toast.makeText(this, "No files to open",
               Toast.LENGTH_SHORT);
         toast.show();
         return;
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Choose a Trip");
      builder.setSingleChoiceItems(mFileList, -1,
            new DialogInterface.OnClickListener()
            {
               public void onClick(DialogInterface dialog, int item)
               {
                  dialog.dismiss();
                  openTripOnClick(mFileList[item].toString());
               }
            });
      builder.create();
      builder.show();
   }

   public void openTripOnClick(String file)
   {
      // Log.d(TAG, "open file: " + file);
      openTripFile(file);
   }

   void setDefaultTrip(String file)
   {
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString("defaultTrip", file);
      editor.commit();
   }

   void newTrip()
   {
      mNewDialog.setTitle(R.string.menu_new);
      mNewDialog.setContentView(R.layout.new_trip);
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
         Toast toast = Toast.makeText(this, "Invalid filename",
               Toast.LENGTH_SHORT);
         toast.show();
         return;
      }
      if (str.contains(".") == true)
      {
         Toast toast = Toast.makeText(this,
               "Invalid filename (remove the period)", Toast.LENGTH_SHORT);
         toast.show();
         return;
      }
      File newFile = new File(Environment.getExternalStorageDirectory()
            + TRIP_PATH + "/" + str + KML_SUFFIX);
      if (newFile.exists() == true)
      {
         Toast toast = Toast.makeText(this, "File already exists)",
               Toast.LENGTH_SHORT);
         toast.show();
         return;
      }
      mNewDialog.cancel();
      if (mBlogData.newBlog(str + KML_SUFFIX) == false)
      {
         Toast toast = Toast.makeText(this,
               "Failed to create new file (SD card problem?)",
               Toast.LENGTH_SHORT);
         toast.show();
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

   private void showHelpDialog()
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(
            TravelLocBlogMain.this);
      String str = this.getString(R.string.app_name) + " "
            + this.getString(R.string.HelpMsg);
      builder.setMessage(str);
      builder.setTitle(this.getString(R.string.menu_help));
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

   private void areYouSure()
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(
            TravelLocBlogMain.this);
      builder.setMessage(this.getString(R.string.are_you_sure_msg));
      builder.setTitle(this.getString(R.string.are_you_sure));
      builder.setPositiveButton(R.string.Yes,
            new DialogInterface.OnClickListener()
            {
               public void onClick(DialogInterface dialog, int id)
               {
                  dialog.cancel();
                  deletePost();
               }
            });
      builder.setNegativeButton(R.string.cancel,
            new DialogInterface.OnClickListener()
            {
               public void onClick(DialogInterface dialog, int id)
               {
                  mDeleteItem = -1;
                  dialog.cancel();
               }
            });
      builder.setCancelable(true);
      builder.create().show();
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
         Toast toast = Toast
               .makeText(this, "Nothing to map", Toast.LENGTH_LONG);
         toast.show();
      }
   }

   /**
    * Utility function to convert file name to trip name
    */
   public static String fileToTripName(String fileName)
   {
      String name = fileName;
      // Show the name without the suffix, and check that name is at least 1 char
      int lastSuffix = name.lastIndexOf(KML_SUFFIX);
      if (lastSuffix > 1) name = name.substring(0, lastSuffix);
      return name;
   }
   
}
