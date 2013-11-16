package com.barkside.travellocblog;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ImportTrip extends ActionBarActivity {

   private static final String TAG = "ImportTrip";

   private BlogDataManager mBlogMgr = new BlogDataManager();

   private TextView mSaveNameTv;

   /* Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.import_trip);

      Intent intent = getIntent();
      Uri uri = null;

      String action = intent.getAction();
      if (Intent.ACTION_SEND.equals(action))
      {
         Log.d(TAG, " Got ACTION_SEND " +  intent);
         Bundle extras = intent.getExtras();
         String sharedText = extras.getString(Intent.EXTRA_TEXT);
         uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
         Log.d(TAG, " received text " + sharedText);
         Log.d(TAG, " received Uri " + uri);
      } else {
         // Unrecognized action, nothing to do.
         finish();
         return;
      }

      boolean opened = false;
      opened = mBlogMgr.openBlog(this, uri, R.string.file_import_load_failed);

      if (!opened) {
         finish();
         return;
      }

      ActionBar actionBar = getSupportActionBar();
      // Do not want to show parent activity since that would be confusing
      // because the file may not have been imported. Treat this activity as
      // an independent activity.
      actionBar.setDisplayHomeAsUpEnabled(false);
      initializeTitles();
      
      // Display the Map as a fragment
      MapTripFragment mapFrag;
      if (savedInstanceState == null) {
         // First-time init; create fragment to embed in activity.
         FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         mapFrag = MapTripFragment.newInstance();
         Log.d(TAG, "Got new " + mapFrag);
         ft.add(R.id.map_trip_fragment, mapFrag);
         ft.commit();
      } else {
         mapFrag = (MapTripFragment)
               getSupportFragmentManager().findFragmentById(R.id.map_trip_fragment);
      }
      
      mapFrag.useBlogMgr(mBlogMgr);
      // Trip has already been opened, so no need to call mapFrag.openTrip

   }

   // Note that fragment onResume is called after the parent activity onResume
   // Do not depend on order of onResume calls, use onResumeFragments instead.
   @Override
   protected void onResumeFragments() {
      super.onResumeFragments();
   }

   // Update ActionBar title and subtitle.
   private void initializeTitles() {
      Uri uri = mBlogMgr.uri();
      
      // update ActionBar title or subtitle
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();

      // Show the name of file being imported. Use the last segment of pathname.
      String title = "";
      if (uri != null)
         title = uri.getLastPathSegment();
      
      // Strip off any .kml suffix if present.
      title = Utils.blogToDisplayname(title);

      actionBar.setSubtitle(title);
      
      int subtitleId = R.string.import_from_format;
      String subtitle = "";
      if (uri != null)
         subtitle = String.format(getString(subtitleId), title);
      else
         subtitle = getString(R.string.open_failed_title);

      actionBar.setSubtitle(subtitle);
      
      // User will name the file in mSaveNameTv. For its initial value, use the same name
      // as imported file. Use the last segment of pathname, strip off any .kml if present.
      mSaveNameTv = (TextView) findViewById(R.id.file_name);
      // Move cursor to end of file name, and maintain cursor behavior (blinking, etc)
      mSaveNameTv.setText("");
      mSaveNameTv.append(title);
   }

   public void onCancelClicked(View v) {     
      Log.d(TAG, "Got Cancel click");
      finish();
   }

   public void onSaveClicked(View v) {
      // Process the Save button.
      
      String tripname = mSaveNameTv.getText().toString();
      String blogname = Utils.displayToBlogname(tripname);
      
      boolean exists = mBlogMgr.existingBlog(blogname);
      Log.d(TAG, "Got save clicked " + blogname + " Exists? " + exists);
      if (exists) {
         confirmSaveTripUI(blogname);
      } else {
         saveTrip(blogname);
      }
   }
   
   // Save imported trip into selected name. On success, finish() activity.
   // Does not check for existing file, will overwrite.
   private boolean saveTrip(String blogname) {
      boolean saved = mBlogMgr.saveBlogToFile(blogname);
      if (saved) {
         Toast.makeText(this, R.string.file_imported,
               Toast.LENGTH_SHORT).show();
         
         // Start the TracelLocBlogMain activity screen to load imported file.
         Intent i = new Intent(this, TravelLocBlogMain.class);
         Uri uri = Utils.blognameToUri(blogname);
         i.setData(uri);
         
         // We could start a Travel Blog activity in this non-Travel Blog task.
         // But seems simpler to start it in an existing Travel Blog task, if it
         // exists, or create a new task.
         // TODO: confirm the best thing to do here. For now, make Travel Blog
         // start a new task. This avoids seeing Travel Blog screens shown under
         // names of other tasks. Don't use FLAG_ACTIVITY_CLEAR_TOP since that may
         // obliterate any existing, in-progress previous Travel Blog activity task.
         // i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         i.setAction(Intent.ACTION_MAIN);
         startActivity(i);

         // Finish up this activity.
         finish();
         
      } else {
         Toast.makeText(this, R.string.file_import_save_failed,
               Toast.LENGTH_LONG).show();
         // Failed, so stay on same activity.
      }

      return saved;
   }
   
   // Save and overwrite existing file. Asks for confirmation, then saves the file.
   private void confirmSaveTripUI(final String blogname)
   {
      final Context context = this;

      String msg = String.format(getString(R.string.are_you_sure_overwrite), blogname);

      // areYouSure dialog listener
      DialogInterface.OnClickListener yesNoListener
      = new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id)
         {
            dialog.dismiss();
            switch (id)
            {
               case DialogInterface.BUTTON_POSITIVE:
                  saveTrip(blogname);
                  break;
               case DialogInterface.BUTTON_NEGATIVE:
               default:
                  break;
            }
         }
      };
      // Display the areYouSure dialog, and on Yes, import the file
      Utils.areYouSure(context, msg, yesNoListener);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.import_trip, menu);
      
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId()) {
      case R.id.send_feedback:
         Utils.sendFeedback(this, TAG);
         return true;
      case R.id.help:
         showHelp(getSupportFragmentManager());
         return true;
      case android.R.id.home:
         // Respond to the action bar's Up/Home button
         Utils.handleUpNavigation(this, mBlogMgr.uri());
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }

   private void showHelp(FragmentManager fm)
   {
      MessagesDialog helpDialog = new MessagesDialog();
      Bundle args = new Bundle();
      args.putInt(MessagesDialog.MESSAGE1_STRING_ID_ARG, R.string.import_help);
      args.putInt(MessagesDialog.TITLE_STRING_ID_ARG, R.string.import_help_title);
      helpDialog.setArguments(args);
      helpDialog.show(fm, getString(R.string.help_dialog_title));
   }


}
