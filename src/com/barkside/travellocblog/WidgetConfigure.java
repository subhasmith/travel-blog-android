package com.barkside.travellocblog;

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

/**
 * When user installs this widget, configure it by tying it to a particular
 * trip file. So that when the widget is clicked, it acts on that trip file.
 * Also allows no trip to be selected - in which case, widget acts like a button
 * to invoke the Edit Post activity using the last opened trip in the app.
 * 
 * @author avinash
 *
 */
public class WidgetConfigure extends ActionBarActivity {

   private static final String TAG = "WidgetConfigure";

   // Get list of files from blog manager when needed
   private BlogDataManager mBlogData = BlogDataManager.getInstance();

   private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

   private String mFilename; // currently selected trip file

   // New Trip and Select Trip buttons may be enabled and disabled at runtime
   private Button mBtnNewTrip;
   private Button mBtnOpenTrip;
   // Display selected trip name here - this is also the done button
   private Button mBtnDone;
   
   // Whether to use app default trip, or to use specified trip file
   private boolean mUseSelected = false;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Set the result to CANCELED.  This will cause the widget host to cancel
      // out of the widget placement if they press the back button.
      setResult(RESULT_CANCELED);

      // Set the view layout resource to use.
      setContentView(R.layout.widget_configure);

      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      if (extras != null) {
         mAppWidgetId = extras.getInt(
               AppWidgetManager.EXTRA_APPWIDGET_ID, 
               AppWidgetManager.INVALID_APPWIDGET_ID);
      }

      // If they gave us an intent without the widget id, just bail.
      if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
         finish();
      }

      mBtnNewTrip = (Button) findViewById(R.id.new_trip);
      mBtnOpenTrip = (Button) findViewById(R.id.open_trip);
      mBtnDone = (Button) findViewById(R.id.use_trip);

      // Use the layout config to determine mUseSelected
      RadioButton radio_option = (RadioButton) findViewById(R.id.option_selected);
      updateUseSelected(radio_option.isChecked());

      // Finally: attempt to find and display this widget's trip file name.
      updateFilename(getKeyTripFile(this, mAppWidgetId));
   }

   @Override
   protected void onResume() {
      super.onResume();
   }
      
   public void onOpenTripClicked(View v) {
      // Open trip button
      final CharSequence[] fileList = mBlogData.getBlogsList();
      final Context context = WidgetConfigure.this;
      TravelLocBlogMain.selectTripDialog(context, fileList,
            new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int item) {
            dialog.dismiss();
            updateFilename(fileList[item].toString());
            Log.d(TAG, "got open file name " + mFilename);
         }
      });
   }

   public void onNewTripClicked(View v) {
      final Dialog newFileDialog = new Dialog(this);
      TravelLocBlogMain.newTrip(newFileDialog, R.string.menu_new,
            new View.OnClickListener() {
         public void onClick(View v) {
            String str = TravelLocBlogMain.newTripFilename(newFileDialog, mBlogData);
            if (str != null) {
               updateFilename(str);
            }
         }
      });
   }
   
   public void onDoneClicked(View v) {
      final Context context = WidgetConfigure.this;
      String filename = mFilename;
      // When the button is clicked, save the string in our prefs and return that they
      // clicked OK.
      if (mUseSelected) {
         Log.d(TAG, "selected trip " + filename);
         setKeyTripFile(context, mAppWidgetId, filename);         
      } else {
         Log.d(TAG, "use app default trip, none selected ");
         filename = null;
      }

      // Android does not send an update to the widgets, so we have to do it ourselves.
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
      TravelLocWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId, filename);

      // Make sure we pass back the original appWidgetId
      Intent resultValue = new Intent();
      resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(RESULT_OK, resultValue);
      finish();
   }
   
   public void onTripOptionClicked(View v) {
      
      // get selected radio button from radioGroup
      RadioButton radio_option = (RadioButton) v;

      Log.d(TAG, "Got radio option: " + radio_option.getText());
      switch(v.getId()) {
      case R.id.option_last_opened:
         Log.d(TAG, "Got option_last_opened ");
         // Disable the two trip select buttons
         updateUseSelected(false);
         break;
      case R.id.option_selected:
         Log.d(TAG, "Got option_selected ");
         updateUseSelected(true);
         break;
      }
   }
   
   // Update the mUseSelected variable and the UI appropriately
   private void updateUseSelected(boolean newvalue) {      
      mUseSelected = newvalue;
      String doneFormat = getString(R.string.widget_done_format);
      String doneText;

      if (mUseSelected) {
         mBtnNewTrip.setEnabled(true);
         mBtnOpenTrip.setEnabled(true);
         doneText = Utils.blogToDisplayname(mFilename);
      } else {
         // Disable the two trip select buttons
         mBtnNewTrip.setEnabled(false);
         mBtnOpenTrip.setEnabled(false);
         // Use getText since widget_last_opened_trip may contain HTML styling
         doneText = getString(R.string.widget_last_opened_trip);
      }
      mBtnDone.setText(String.format(doneFormat, doneText));    
   }
   
   // Update name of currently selected file, and also show it in action bar
   private void updateFilename(String filename) {
      mFilename = filename;
      // If necessary update ActionBar title here. Not doing that, since it is
      // difficult to notice this in this activity, so created mSelectedTripText
      // instead to display the file name.
      updateUseSelected(true);
   }


   /**
    * User may install multiple widgets on home screen, with each widget tied to
    * a different trip file. We save each widget's properties in preferences, and
    * distinguish each key with widgetId.
    */
   public static String keyForWidgetId(int appWidgetId, String key) {
      String widgetKey = SettingsActivity.LAST_OPENED_TRIP_KEY +
         "_widget_" + String.valueOf(appWidgetId);
      return widgetKey;
   }

   // Write the trip file key to the SharedPreferences object for this widget
   public static void setKeyTripFile(Context context, int appWidgetId, String name) {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      Editor editor = settings.edit();
      String widgetKey = keyForWidgetId(appWidgetId, SettingsActivity.LAST_OPENED_TRIP_KEY);
      Log.d(TAG, "Widget set trip name key: " + widgetKey);
      Log.d(TAG, "Widget set trip name value: " + name);

      editor.putString(widgetKey, name);
      editor.commit();
   }

   // Read the trip file key from the SharedPreferences object for this widget.
   // If there is no preference saved, get the default from a resource
   public static String getKeyTripFile(Context context, int appWidgetId) {

      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      String widgetKey = keyForWidgetId(appWidgetId, SettingsActivity.LAST_OPENED_TRIP_KEY);
      Log.d(TAG, "Widget get trip name key: " + widgetKey);

      String name = settings.getString(widgetKey, null);
      if (name == null) {
         name = context.getString(R.string.default_trip);
      }
      return name;
   }
   
   // Delete all the keys from the SharedPreferences object for this widget.
   public static void deleteKeys(Context context, int appWidgetId) {

      String widgetKey = keyForWidgetId(appWidgetId, SettingsActivity.LAST_OPENED_TRIP_KEY);
      Log.d(TAG, "Widget delete trip name key: " + widgetKey);

      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      Editor editor = settings.edit();
      // Just one key to delete at this time
      editor.remove(widgetKey);
      editor.commit();
   }
   
   // Menu commands
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.widget, menu);

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
      default:
         return super.onOptionsItemSelected(item);
      }
   }
   
   private void showHelp(FragmentManager fm)
   {
      MessagesDialog helpDialog = new MessagesDialog();
      Bundle args = new Bundle();
      args.putInt(MessagesDialog.MESSAGE1_STRING_ID_ARG, R.string.configure_help);
      args.putInt(MessagesDialog.TITLE_STRING_ID_ARG, R.string.configure_help_title);
      helpDialog.setArguments(args);
      helpDialog.show(fm, getString(R.string.help_dialog_title));
   }

}
