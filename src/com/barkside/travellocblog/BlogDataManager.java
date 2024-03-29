package com.barkside.travellocblog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Provides an interface to the BlogData store which contains a list of posts
 * 
 * Terms: Blogname is name of the blog or trip, example: MyFirstTrip.kml
 * Sometimes we also call this a filename, for historical reasons.
 * Displayname is the name shown in some parts of the UI - the blogname without
 * the .kml suffix.
 * 
 * This class is created based on the Uri of the blog, and then it uses a class
 * variable static field to store the actual loaded blog posts. Today this is
 * a static field that stores file data, in the future it could be a non-static
 * field pointing to a database.
 * 
 * Using a static filed in here instead of in an Application subclass:
 * As per the Android developer docs: There is normally no need to subclass Application.
 * In most situation, static singletons can provide the same functionality in a more modular way.
 * 
 * Important: Note that Android may start Travel Blog activity screens in multiple tasks.
 * The assumption here is that each activity will maintain its own "copy" of the blog
 * data by using its own instance of BlogDataManager.
 * (Under the covers, BlogDataManager may use a single, task-wide, shared static field
 * to store BlogData).
 * The Uri should be saved and re-loaded at appropriate times in the classes that use
 * BlogDataManager.
 * Not thread safe - this is supposed to be used by one activity or fragment at a time. 
 * 
 * @author avinash
 *
 */
public final class BlogDataManager {

   // For logging and debugging purposes
   private static final String TAG = "BlogDataManager";
   
   // The real BlogData functions follow
   
   // The actual implementation is in this BlogDataHolder object, uses default
   // directory to store all the blogs. mBlogData.fullBlogPath() returns full path.
   // Class Variable - single instance in a single task, shared among multiple
   // activities that create a BlogDataManager class. BlogDataHolder is a singleton.
   private final BlogData mBlogData = BlogDataHolder.getInstance();
   
   // The opened file name is stored here as its filename, and full Uri
   private String mBlogname = "";
   private Uri mUri = null;
   
   // Saving key value in a bundle for pause/restore
   private static final String BLOG_URI_KEY = "BlogDataManagerUri";
   
   /*
    * Open the given blog Uri. And load if it is not already loaded.
    * If open fails, prints a Toast message with failmessageId if it is > 0.
    * failMessageId must be a string resource with one %s in it to display blogname.
    * 
    * Main public interface. Use this to open a blog. If blog is already loaded, then
    * this does not any file reading. This function should be called in onResume methods
    * of activities that use BlogDataManager since Android may start multiple activities
    * in a single Travel Blog task, and some of these activities may change open different
    * blogs. So to maintain Android back button semantics correctly, and since loaded data
    * is shared among all activities in a single Travel Blog task, need to call this
    * function in onResume. The most common use is to stick to a single blog for a single
    * launch of Travel Blog and in such cases, the data will already be loaded even when
    * user moves between activities.
    */
   public boolean openBlog(Context context, Uri uri) {
      return openBlog(context, uri, 0);
   }
   
   public boolean openBlog(Context context, Uri uri, int failMessageId) {
      
      if (uri == null || Uri.EMPTY.equals(uri)) {
         clearBlog();
         return false;
      }
      boolean opened = false;
      
      // If same blog name, then just use existing mBlogData
      String blogname = Utils.uriToBlogname(uri);
      String currentName = mBlogData.blogname();
      
      Log.d(TAG, "openBlog name: " + blogname);
      
      if (!currentName.equals("") && currentName.equals(blogname)) {
         Log.d(TAG, "return blog already opened: " + blogname);
         mBlogname = blogname;
         mUri = uri;
         return true;         
      }

      // Have to read blog data from the Uri file or stream
      // Clear all data first, then load the blog.
      clearBlog();
      if (Utils.uriIsInternal(uri)) {
         // It is an internal implicit intent with blogname.
         // This call will check if we have this already opened, nothing to do in that case.
         opened = mBlogData.openBlog(blogname);
      } else {
         // This is an external Uri, probably from an ACTION_SEND
         ContentResolver cr = context.getContentResolver();
         InputStream cis;
         try {
            cis = cr.openInputStream(uri);
            opened = mBlogData.openBlog(cis, blogname);
         } catch (FileNotFoundException e) {
            cis = null;
            Log.d(TAG, "Content resolver failed for " + uri);
         }
      }
      
      if (opened) {
         // Successfully opened blog
         mBlogname = blogname;
         mUri = uri;
         
      } else {
         // Failed to open blog
         if (failMessageId > 0) {
            // Report that we failed to open requested file.
            String message = String.format(context.getString(failMessageId), blogname);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
         }
      }

      return opened;
   }
   
   public Boolean newBlog(String blogname) {
      clearBlog();
      if (mBlogData.newBlog(blogname)) {
         mBlogname = blogname;
         mUri = Utils.blognameToUri(blogname);
         return true;
      }
      return false; // failed
   }

   /* delete the given blog */
   public Boolean deleteBlog(String blogname) {
      if (mBlogname.equals(blogname))
         clearBlog(); // reset opened file

      return mBlogData.deleteBlog(blogname);
   }

   private void clearBlog() {
      mBlogData.clearBlog();
      mBlogname = ""; // reset opened file
      mUri = null;
   }

   public Boolean saveBlogElement(BlogElement element, int index) {
      this.validateBlogName();
      return mBlogData.saveBlogElement(element, index);
   }

   public Boolean saveBlogToFile(String blogname) {
      this.validateBlogName();
      return mBlogData.saveBlogToFile(blogname);
   }

   public Boolean deleteBlogElement(int index) {
      this.validateBlogName();
      return mBlogData.deleteBlogElement(index);
   }

   /* return true if the given blog exists */
   public Boolean existingBlog(String blogname) {
      return mBlogData.existingBlog(blogname);
   }
   
   /* rename given blog */
   public Boolean renameBlog(String oldname, String newname) {
      this.validateBlogName();
      if (mBlogData.renameBlog(oldname, newname)) {
         if (mBlogname.equals(oldname)) {
            mBlogname = newname;
            mUri = Utils.blognameToUri(newname);
         }
         return true;
      }
      return false;
   }

   /* return File object, necessary to implement Share ACTION_SEND, file rename, etc */
   public File blogToFile(String blogname) {
      this.validateBlogName();
      return mBlogData.blogToFile(blogname);
   }

   // Access methods for currently opened blog name (with suffix) and Uri
   // Note that actual data loaded may be of a different file. See this.onResume().
   public String blogname() {  return mBlogname; }
   public Uri uri() { return mUri; }

   public CharSequence[] getBlogsList() {
      return mBlogData.getBlogsList();      
   }

   public int getMaxBlogElements() {
      this.validateBlogName();
      return mBlogData.getMaxBlogElements();  
   }
 
   public BlogElement getBlogElement(int index) {
      return mBlogData.getBlogElement(index);  
   }

   public float getTotalDistance() {
      return mBlogData.getTotalDistance();  
   }
   
   // Check if the current data pointed to by the manager is same as the name it expects.
   // This can be used to detect incorrect code in cases like this:
   // Activity A Android back stack. It starts Activity B with the same blog name. B then
   // opens another blog. User hits back button and goes back to A. A must reopen the blog
   // in its onResume, otherwise it will get wrong data.
   public boolean validateBlogName() {
      String dataname = mBlogData.blogname();
      String expectedname = this.blogname();
      
      if (!expectedname.equals(dataname)) {
         String message = String.format(Locale.US, "Expected blog (%s), got (%s)",
               expectedname, dataname);
         Log.e(TAG, "Error: " + message);
         // This is a program error, client at that point can't really do anything
         // therefore we throw an unchecked exception or error and terminate the program.
         throw new AssertionError(message);
         // return false;
      }
      return true;
   }
   
   // Support for Activity actions to save and restore blog name, and to setup data
   // data correctly on an Activity.onResume.
   
   // Save instance state by recording the name of the blog being used.
   // This will allow us to restore it when needed, and reload it in an onCreate.
   public void onSaveInstanceState(Bundle savedInstanceState) {
      Uri uri = this.uri();
      Log.d(TAG, "save instance state uri: " + uri);
      savedInstanceState.putParcelable(BLOG_URI_KEY, uri);
   }
   
   // Reload the blog name from saved state. Call this in an onCreate or
   // Activity.onRestoreInstanceState().
   public void onRestoreInstanceState(Bundle savedInstanceState) {
      mUri = savedInstanceState.getParcelable(BLOG_URI_KEY);
      mBlogname = Utils.uriToBlogname(mUri);
   }
   
   // Reload the correct blog to resume activity on the blog.
   // Blog is actually reloaded from disk only if it is not already loaded.
   public boolean onResumeSetup(Context context, int failMessageId) {
      return this.openBlog(context, mUri, failMessageId);
   }
   public boolean onResumeSetup(Context context) {
      return this.openBlog(context, mUri);
   }
   
   // Single instance of the BlogData object.
   // Nothing much here, but in case need to add other functions, made this
   // into a separate class instead of just using BlogData object in BlogDataManager.
   private static class BlogDataHolder extends BlogData {
      private static BlogDataHolder instance = new BlogDataHolder();
      public static BlogDataHolder getInstance() { return instance; }
      private BlogDataHolder() {}
   }
}
