package com.barkside.travellocblog;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
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

   private BlogDataManager mBlogMgr = BlogDataManager.getInstance();

   private Uri mImportUri;
   private TextView mSaveNameTV;

   /* Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.import_trip);

      Intent intent = getIntent();
      mImportUri = intent.getData();
      Log.d(TAG, " Got Uri " +  mImportUri);

      String action = intent.getAction();
      if (Intent.ACTION_SEND.equals(action))
      {
         Log.d(TAG, " Got ACTION_SEND " +  intent);
         Bundle extras = intent.getExtras();
         String sharedText = extras.getString(Intent.EXTRA_TEXT);
         mImportUri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
         Log.d(TAG, " received text " + sharedText);
         Log.d(TAG, " received Uri " + mImportUri);
      } else {
         // Unrecognized action, nothing to do.
         finish();
         return;
      }

      boolean opened = false;
      opened = mBlogMgr.openBlog(this, mImportUri);

      if (!opened) {
         // Failed to open requested file.
         Toast.makeText(this, R.string.file_import_load_failed,
               Toast.LENGTH_LONG).show();
         finish();
         return;
      }

      // Show the name of file being imported. Use the last segment of pathname.
      String name = mImportUri.getLastPathSegment();

      // update ActionBar title or subtitle
      // to support SDK 11 and older, need to use getSupportActionBar
      ActionBar actionBar = getSupportActionBar();

      // Show the name of file being imported. Use the last segment of pathname.
      actionBar.setSubtitle(name);
      int subtitleId = R.string.import_from_format;
      String subtitle = String.format(getString(subtitleId), name);
      actionBar.setSubtitle(subtitle);
      // Do not want to show parent activity since that would be confusing
      // because the file may not have been imported. Treat this activity as
      // an independent activity.
      actionBar.setDisplayHomeAsUpEnabled(false);

      // User will name the here - use the same name as imported file as initial value
      // Strip off any .kml if present.
      name = Utils.blogToDisplayname(name);
      mSaveNameTV = (TextView) findViewById(R.id.file_name);
      // Move cursor to end of file name, and maintain cursor behavior (blinking, etc)
      mSaveNameTV.setText("");
      mSaveNameTV.append(name);

      // Display the Map as a fragment
      if (savedInstanceState == null) {
         // First-time init; create fragment to embed in activity.
         FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         MapTripFragment newFragment = MapTripFragment.newInstance(mImportUri);
         Log.d(TAG, "Got MapTripFragment " + newFragment);
         ft.add(R.id.map_trip_fragment, newFragment);
         ft.commit();
      }
   }

   public void onCancelClicked(View v) {     
      Log.d(TAG, "Got Cancel click");
      finish();
   }

   public void onSaveClicked(View v) {
      // Process the Save button.
      
      String tripname = mSaveNameTV.getText().toString();
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
      inflater.inflate(R.menu.map_trip, menu);
      
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId())
      {
         case R.id.send_feedback:
            Utils.sendFeedback(this, TAG);
            return true;
       case R.id.help:
            showHelp(getSupportFragmentManager());
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
