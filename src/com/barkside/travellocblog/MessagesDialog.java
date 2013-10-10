package com.barkside.travellocblog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
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
   
   // Asset name for each TextView, passed in as arg to this dialog
   // Each name represents in a file in res/assets folder.
   public static final String MESSAGE1_ASSET_ARG = "MESSAGE1_ASSSET";
   public static final String MESSAGE2_ASSET_ARG = "MESSAGE2_ASSSET";
   public static final String MESSAGE_TITLE_ARG = "MESSAGE_TITLE";
   // LATER public static final String DISPLAY_LEGAL_NOTICES = "LEGAL_NOTICES"; // 0/1 value
   
   Context mContext = null;

   public MessagesDialog() {
       // Empty constructor required for DialogFragment
   }
   
   // Caller sends in string resource id for each message box
   String mMessage1Asset = "";
   String mMessage2Asset = "";
   String mMessageTitle = "Dialog";
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       mMessage1Asset = getArguments().getString(MESSAGE1_ASSET_ARG);
       mMessage2Asset = getArguments().getString(MESSAGE2_ASSET_ARG);
       mMessageTitle = getArguments().getString(MESSAGE_TITLE_ARG);
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
      
      getDialog().setTitle(mMessageTitle);

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
      TextView tv = (TextView) view.findViewById(R.id.app_name_version);
      String name = getString(R.string.app_name_version_format, getString(R.string.app_name),
            version);
      tv.setText(name);
      
      AssetManager assetManager = context.getAssets();

      // Display text in Message1 TextView, for example, the Help message
      if (mMessage1Asset != null) {
         tv = (TextView) view.findViewById(R.id.message1);
         tv.setText(readTextFromAsset(mMessage1Asset, assetManager));
         tv.setMovementMethod(LinkMovementMethod.getInstance());         
      }
      
      // Display text in Message2 TextView, for example, What's New
      if (mMessage2Asset != null) {
         tv = (TextView) view.findViewById(R.id.message2);
         tv.setText(readTextFromAsset(mMessage2Asset, assetManager));
         tv.setMovementMethod(LinkMovementMethod.getInstance());         
      }
      
      // Display text in Message3 TextView, this is the Google Play Service legal notice
      // Only necessary for About screen, which is not yet supported in a Dialog,
      // so no need to handle this case now.
      //tv = (TextView) view.findViewById(R.id.message3);
      //tv.setText(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(context));
      
      // User can dismiss the dialog by clicking on button
      Button button = (Button)view.findViewById(R.id.buttonOK);
      button.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
              dismiss();
          }
      });
      
      return view;
   }

   /**
    * This method reads simple text file from res/assets folder.
    * @param assetName
    * @return data from file
    */

   private String readTextFromAsset(String assetName, AssetManager assetManager) {

      InputStream inputStream = null;
      try {
         inputStream = assetManager.open(assetName);
      } catch (IOException e) {
         Log.e(TAG, e.getMessage());
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte buf[] = new byte[1024];
      int len;

      try {
         while ((len = inputStream.read(buf)) != -1) {
            outputStream.write(buf, 0, len);
         }
         outputStream.close();
         inputStream.close();
      } catch (IOException e) {
         Log.e(TAG, e.getMessage());
      }

      return outputStream.toString();
   }

}
