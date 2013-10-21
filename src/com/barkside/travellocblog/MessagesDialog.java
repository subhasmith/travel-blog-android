package com.barkside.travellocblog;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * General dialog, used to display multiple screens: Help, What's New etc.
 * Based on the arguments passed in the bundle, sets the TextView objects appropriately.
 */

public class MessagesDialog extends DialogFragment {

   // For logging and debugging purposes
   private static final String TAG = "MessagesDialog";
   
   // Bundle keys used to pass data to this dialog
   // Each key represents a string id number to display in a TextView.
   public static final String MESSAGE1_STRING_ID_ARG = "MESSAGE1_STRING_ID";
   public static final String MESSAGE2_STRING_ID_ARG = "MESSAGE2_STRING_ID";
   public static final String TITLE_STRING_ID_ARG = "TITLE_STRING_ID";
   
   Context mContext = null;

   public MessagesDialog() {
       // Empty constructor required for DialogFragment
   }
   
   // Caller sends in string resource id for each message box
   int mMessage1Id = 0;
   int mMessage2Id = 0;
   int mMessageTitleId = 0;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       mMessage1Id = getArguments().getInt(MESSAGE1_STRING_ID_ARG);
       mMessage2Id = getArguments().getInt(MESSAGE2_STRING_ID_ARG);
       mMessageTitleId = getArguments().getInt(TITLE_STRING_ID_ARG);
  }
   
   @Override
   public void onAttach(Activity activity) {
       super.onAttach(activity);
       mContext = activity;
       Log.d(TAG, "got activity context " + mContext);
   }
   
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {

      View view = inflater.inflate(R.layout.messages_dialog, container);

      Context context = mContext;
      if (container != null)
      {
         context = container.getContext();
      }

      if (context == null)
      {
         // Internal program error?
         Log.e(TAG, "Could not display message, context is null");
         return null;
      }
      
      getDialog().setTitle(mMessageTitleId);

      String version = "0.0.0"; // to denote unknown version
      try
      {
         PackageInfo pInfo = context.getPackageManager()
               .getPackageInfo(context.getPackageName(), 0);
         version = pInfo.versionName;
      } catch (NameNotFoundException e)
      {
         Log.d(TAG, "Failed to get version string " + e);
      }

      // App name and version number
      TextView tv = (TextView) view.findViewById(R.id.app_name);
      tv.setText(R.string.app_name);
      tv = (TextView) view.findViewById(R.id.app_version);
      tv.setText(version);
      
      // Display text in Message1 TextView, for example, the Help message
      if (mMessage1Id != 0) {
         tv = (TextView) view.findViewById(R.id.message1);
         tv.setText(mMessage1Id);
         tv.setMovementMethod(LinkMovementMethod.getInstance());         
      }
      
      // Display text in Message2 TextView, for example, What's New
      if (mMessage2Id != 0) {
         tv = (TextView) view.findViewById(R.id.message2);
         tv.setText(mMessage2Id);
         tv.setMovementMethod(LinkMovementMethod.getInstance());         
      }
      
      // User can dismiss the dialog by clicking on button
      Button button = (Button)view.findViewById(R.id.buttonOK);
      button.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
              dismiss();
          }
      });
      
      return view;
   }
}
