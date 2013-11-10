package com.barkside.travellocblog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Provides an interface to the BlogData store which contains a list of posts
 * 
 * Terms: Blogname is name of the blog or trip, example: MyFirstTrip.kml
 * Sometimes we also call this a filename, for historical reasons.
 * Displayname is the name shown in some parts of the UI - the blogname without
 * the .kml suffix.
 * 
 * Follow the Single Instance Java pattern. For now we just use the BlogData
 * class but in the future could use a sqlite database, etc, and use
 * dependency injection to have caller determine whether to use BlogData or
 * some other storage mechanism.
 * And as per the Android developer docs: There is normally no need to subclass Application.
 * In most situation, static singletons can provide the same functionality in a more modular way.
 * 
 * Important: Since there is a single instance, if the app ever ends up having two
 * activity screens that show different blog files, then each activity may change
 * the shared data.
 * The assumption here is that all activities from launch onwards will use same blog
 * opened in an onCreate call. But this is easily fixed - each activity that needs
 * to ensure a specific blog is opened should just open the blog in an onResume.
 * 
 * Note that we don't really care if Android starts up multiple TravelLocBlogMain apps,
 * and uses multiple of this class. But the most common use is probably a single
 * invocation of the app, and all the activities for that instance will share
 * a single BlogDataManager.
 * Keeping a single open blog for the app activities is also a good thing - so that when
 * users navigate from one screen to another, they see the same trip file, the one that was
 * last opened.
 * 
 * @author avinash
 *
 */
public final class BlogDataManager {
   private static final BlogDataManager instance = new BlogDataManager();
   
   public static BlogDataManager getInstance() { return instance; }

   private BlogDataManager() {}

   // For logging and debugging purposes
   private static final String TAG = "BlogDataManager";
   
   // The real BlogData functions follow
   
   // The actual implementation is in this BlogData object, uses default
   // directory to store all the blogs. mBlogData.fullBlogPath() returns full path.
   private BlogData mBlogData = new BlogData();
   
   // The opened file name is stored here as its filename, and full Uri
   private String mOpenedBlogname = "";
   private Uri mOpenedUri = Uri.EMPTY;
   
   /*
    * Open the given blog Uri. Returns the same object if same file name/Uri.
    * External interface. Use this to open a blog.
    */
   public boolean openBlog(Context context, Uri uri) {
      if (uri == null || Uri.EMPTY.equals(uri)) {
         clearBlog();
         return false;
      }
      String filename = Utils.uriToBlogname(uri);
      Log.d(TAG, "uri to blogname " + filename);
      boolean opened = false;

      if (filename == null || filename.isEmpty()) {
         // This is an external Uri, probably from an ACTION_SEND
         ContentResolver cr = context.getContentResolver();
         InputStream cis;
         try {
            cis = cr.openInputStream(uri);
            opened = this.openBlog(cis, uri);
            Log.d(TAG, "Opened blog from uri input stream " + cis);
         } catch (FileNotFoundException e) {
            cis = null;
            Log.d(TAG, "Content resolver failed for " + uri);
         }

      } else {
         // Otherwise, we assume it is an internal implicit intent with blogname.
         opened = this.openBlog(filename);
      }
      
      if (opened) {
         mOpenedBlogname = filename;
         mOpenedUri = uri;
      } else {
         clearBlog();         
      }

      return opened;
   }
   
  private Boolean openBlog(String blogname) {
      
      // If same blog name, then just use existing mBlogData
      if (mOpenedBlogname.isEmpty() && mOpenedBlogname.equals(blogname)) {
         Log.d(TAG, "return previously opened filename blog: " + mOpenedBlogname);
         return true;         
      }
      clearBlog();
      if (mBlogData.openBlog(blogname)) {
         mOpenedBlogname = blogname;
         return true;
      }
      return false; // failed open
   }

   // Internal function to open a blog.
   private Boolean openBlog(InputStream stream, Uri uri) {
      
      // If same blog name, then just use existing mBlogData
      if (mOpenedUri.equals(uri)) {
         Log.d(TAG, "return previously opened Uri blog: " + uri);
         return true;         
      }
      clearBlog();
      if (mBlogData.openBlog(stream)) {
         mOpenedUri = uri;
         return true;
      }
      return false; // failed open
   }

   public Boolean newBlog(String blogname) {
      clearBlog();
      if (mBlogData.newBlog(blogname)) {
         mOpenedBlogname = blogname;
         mOpenedUri = Utils.blognameToUri(blogname);
         return true;
      }
      return false; // failed
   }

   /* delete the given blog */
   public Boolean deleteBlog(String blogname) {
      if (mOpenedBlogname.equals(blogname))
         clearBlog(); // reset opened file

      return mBlogData.deleteBlog(blogname);
   }

   public void clearBlog() {
      mBlogData.clearBlog();
      mOpenedBlogname = ""; // reset opened file
      mOpenedUri = Uri.EMPTY;
   }

   public Boolean saveBlogElement(BlogElement element, int index) {
      return mBlogData.saveBlogElement(element, index);
   }

   public Boolean saveBlogToFile(String blogname) {
      return mBlogData.saveBlogToFile(blogname);
   }

   public Boolean deleteBlogElement(int index) {
      return mBlogData.deleteBlogElement(index);
   }

   /* return true if the given blog exists */
   public Boolean existingBlog(String blogname) {
      return mBlogData.existingBlog(blogname);
   }
   
   /* rename current blog */
   public Boolean renameBlog(String oldname, String newname) {
      if (mBlogData.renameBlog(oldname, newname)) {
         if (mOpenedBlogname.equals(oldname)) {
            mOpenedBlogname = newname;
            mOpenedUri = Utils.blognameToUri(newname);
         }
         return true;
      }
      return false;
   }

   /* return File object, necessary to implement Share ACTION_SEND, file rename, etc */
   public File blogToFile(String blogname) {
      return mBlogData.blogToFile(blogname);
   }

   // Accessors for currently opened blog name and Uri
   public String openedName() {
      return mOpenedBlogname;
   }

   public Uri openedUri() {
      return mOpenedUri;
   }

   public CharSequence[] getBlogsList() {
      return mBlogData.getBlogsList();      
   }

   public int getMaxBlogElements() {
      return mBlogData.getMaxBlogElements();  
   }
 
   public BlogElement getBlogElement(int index) {
      return mBlogData.getBlogElement(index);  
   }

   public float getTotalDistance() {
      return mBlogData.getTotalDistance();  
   }
}
