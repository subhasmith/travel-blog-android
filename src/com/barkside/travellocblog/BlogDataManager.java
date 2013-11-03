package com.barkside.travellocblog;

import java.io.File;

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
   
   private String mOpenedBlogname = "";

   /*
    * Open the given file. Returns the same object if same file name.
    */
   public Boolean openBlog(String blogname) {
      
      // If same blog name, then just use existing mBlogData
      if (mOpenedBlogname.equals(blogname)) {
         Log.d(TAG, "return previously opened blog");
         return true;         
      }
      mOpenedBlogname = ""; // reset opened file
      if (mBlogData.openBlog(blogname)) {
         mOpenedBlogname = blogname;
         return true;
      }
      return false; // failed open
   }

   public Boolean newBlog(String blogname) {
      mOpenedBlogname = ""; // reset opened file
      if (mBlogData.newBlog(blogname)) {
         mOpenedBlogname = blogname;
         return true;
      }
      return false; // failed
   }

   /* delete the given blog */
   public Boolean deleteBlog(String blogname) {
      if (mOpenedBlogname.equals(blogname))
         mOpenedBlogname = ""; // reset opened file

      return mBlogData.deleteBlog(blogname);
   }

   public Boolean saveBlogElement(BlogElement element, int index) {
      return mBlogData.saveBlogElement(element, index);
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
      return mBlogData.renameBlog(oldname, newname);
   }

   /* return File object, necessary to implement Share ACTION_SEND, file rename, etc */
   public File blogToFile(String blogname) {
      return mBlogData.blogToFile(blogname);
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
