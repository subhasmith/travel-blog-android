package com.barkside.travellocblog;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * TravelLocWidget sets up the widgets. Widgets are used as buttons only,
 * tied to a particular trip file, to involve New Post command.
 */

public class TravelLocWidget extends AppWidgetProvider {
   private static final String TAG = "TravelLocWidget";

   /**
    * onUpdate is not necessary. We never push any data from app to widget, widgets
    * are used as buttons only, tied to a particular trip file, to involve New Post command.
    * So while we have implemented the function below, the xml file sets
    * android:updatePeriodMillis="0" to never call this function. Internally, we use
    * the static updateAppWidget function to setup the widget when it is installed.

    */
   public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
      final int N = appWidgetIds.length;

      // Perform this loop procedure for each App Widget that belongs to this
      // provider
      for (int i = 0; i < N; i++) {
         int appWidgetId = appWidgetIds[i];

         String filename = WidgetConfigure.getKeyTripFile(context, appWidgetId);
         Log.i(TAG,  "onUpdate " + appWidgetId + " filename: " + filename);

         TravelLocWidget.updateAppWidget(context, appWidgetManager,
               appWidgetId, filename);
      }
   }

   @Override
   public void onDeleted(Context context, int[] appWidgetIds) {
      // When the user deletes the widget, delete the preference associated with it.
      final int N = appWidgetIds.length;
      for (int i=0; i<N; i++) {
          WidgetConfigure.deleteKeys(context, appWidgetIds[i]);
      }
      super.onDeleted(context, appWidgetIds);
   }

   /**
    * Update the text fields of a widget. Use by both this class, and the
    * WidgetConfigure class.
    * 
    * @param context
    * @param appWidgetManager
    * @param appWidgetId
    * @param titlePrefix
    */
   static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
         int appWidgetId, String filename) {

      // Construct the RemoteViews object. It takes the package name (in our case, it's our
      // package, but it needs this because on the other side it's the widget host inflating
      // the layout from our package).
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_new_post);
      boolean useSelectedTrip = true;
      if (filename == null || "".equals(filename)) {
         // No trip file assigned, this widget acts like a button and uses
         // the default/last opened trip file of the app. Remove the textview entirely.
         views.setViewVisibility(R.id.widget_tripname, View.GONE);
         useSelectedTrip = false;
      } else {
         // Show the trip name since this widget is assigned a specific trip file
         String tripname = Utils.blogToDisplayname(filename);
         views.setTextViewText(R.id.widget_tripname, tripname);        
      }

      // Create the intents to launch the new post activity.
      // If needed use TaskStackBuilder to construct a back stack that includes
      // TraveLocBlogMain and then EditBlogElement.
      
      Uri uri =  null;
      if (useSelectedTrip) uri = Utils.blognameToUri(filename);

      Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " Uri=" + uri);
      // Create a stack of activities. As per new Android guidelines, try to create
      // the stack such that back button goes to the normally seen previous activity.
      
      // First: the main screen. Pass it the file URI so it uses this instead of
      // the last-opened-file in the app.
      Intent launchIntent = new Intent(context, TravelLocBlogMain.class);
      launchIntent.setData(uri);
      launchIntent.setAction(Intent.ACTION_MAIN);
      
      // Second: the actual New Post activity
      Intent intent = new Intent(context, EditBlogElement.class);
      intent.setData(uri);
      intent.setAction(Intent.ACTION_INSERT_OR_EDIT);

      // As per Android guidelines, stack up the activities so back button
      // works like it does in a normal invocation of the New Post screen.
      TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
      stackBuilder.addNextIntent(launchIntent);
      stackBuilder.addNextIntent(intent);

      // As per Android docs, need to make sure the system does not reuse an old
      // PendingIntent. One way to do this is to make sure we use a unique requestCode,
      // which we don't need here at all, so it can be anything: using appWidgetId for that.
      PendingIntent pendingIntent = stackBuilder.getPendingIntent(appWidgetId,
            PendingIntent.FLAG_UPDATE_CURRENT);

      // Get the layout for the App Widget and attach an on-click listener
      // to the button
      views.setOnClickPendingIntent(R.id.widget_new_post, pendingIntent);

      // Tell the widget manager
      appWidgetManager.updateAppWidget(appWidgetId, views);
   }
}
