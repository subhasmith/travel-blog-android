package com.barkside.travellocblog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

/** Implementation of BlogData using the filesystem files and folder.
 * There is usually no need to use this class directly, use BlogDataManager instead.
 * 
 * This class is an implementation of the BlogDataManager, one that uses the filesystem
 * to store each blog as a file. It also provides functions to return directory listings
 * of blog, etc. Anything that is related to how blogs are stored is here - callers don't
 * need to know how and where blogs are stored.
 * 
 * This class is file location agnostic - caller will send it full file paths, and
 * full directory paths. So opening a blog, returning list of all blogs, deleting a blog,
 * all will take pathnames.
 * 
 */

public class BlogData
{
   private List<BlogElement> mBlogPosts = new ArrayList<BlogElement>();
   
   // All trip files will be stored in this directory under this app's storage
   public static final String TRIP_PATH = "/TravelBlog";

   private String mFilename = null;
   private String mTripRoot = null; // root dir of all the blog/trip files
   
   // For logging and debugging purposes
   private static final String TAG = "BlogData";

   
   public BlogData(String tripPath) {
      super();
      mTripRoot = tripPath;
   }

   public BlogData() {
      super();
      mTripRoot = TRIP_PATH;
   }

   /*
    * this function closes anything that is already open, then opens new file,
    * or creates
    */
   public Boolean openBlog(String filename)
   {
      return privOpenFile(filename);
   }

   private Boolean privOpenFile(String filename)
   {
      if (filename != null)
      {
         // Log.d(TAG, "Opening file: "+ filename);
         String path = fullBlogPath(filename);
         File newFile = new File(fullBlogPath(null));
         try
         {
            newFile.mkdirs();
         }
         catch (Exception e)
         {
            Log.d(TAG, "Failed to mkdirs: " + path);
         }
      }

      clearBlog();
      if (filename != null)
      {
         mFilename = filename;
         return loadDataFromFile();
      }
      return false;
   }
   
   /** Open blog from given stream. Mark blog name as unknown.
    * This is a readonly stream, cannot call saveBlogElement but can
    * call saveBlogToFile to save to a new file.
    * 
    */
   public Boolean openBlog(InputStream stream)
   {
      clearBlog();
      return loadDataFromStream(stream);
   }

   /* for a new blog, we simply open it, and save it */
   public Boolean newBlog(String filename)
   {
      privOpenFile(filename);
      return saveBlogToFile(filename);
   }

   /* delete the given blog */
   public Boolean deleteBlog(String filename)
   {
      if (filename == null)
      {
         return false;
      }
      
      String path = fullBlogPath(filename);
      // Following file functions throw no exception as long as filename != null
      File file = new File(path);
      boolean status = file.delete();
      
      if (status)
         {
         Log.d(TAG, "Deleted file " + path);
         // Did we delete the current file belonging to this object?
         if (mFilename.equals(filename))
         {
            clearBlog();
         }
      }
      return status;
   }

   /* rename current blog */
   public Boolean renameBlog(String filename, String newname) {
      File oldfile = blogToFile(filename);
      File newfile = blogToFile(newname);
      boolean renamed = false;
      if (oldfile != null && newfile != null) {
         renamed = oldfile.renameTo(newfile);
      }
      if (renamed)
      {
         mFilename = newname;
      }
      return renamed;
   }

   public void clearBlog() {
      mFilename = "";
      mBlogPosts.clear();
   }

   /* return true if the given blog exists */
   public Boolean existingBlog(String filename)
   {
      return blogToFile(filename).exists();
   }

   public File blogToFile(String filename)
   {
      String path = fullBlogPath(filename);
      // Following file functions throw no exception as long as filename != null
      return new File(path);
   }

   public CharSequence[] getBlogsList()
   {
      CharSequence[] fileList = null;
      try
      {
         fileList = new File(fullBlogPath(null)).list();
      }
      catch (Exception e)
      {
         Log.e(TAG, "error occurred while reading file list");
      }
      return fileList;
   }

   /**
    * Return the full path to the given relative filepath.
    * 
    * @param filepath   null to return root dir, otherwise a relative file path
    * @return full path to file
    */
   private String fullBlogPath(String filepath)
   {
      // TODO: http://developer.android.com/reference/android/os/Environment.html#getExternalStoragePublicDirectory%28java.lang.String%29
      // says to not use this directory. But use Context.getExternalFilesDir(null) instead
      // which is /storage/emulated/0/Android/data/com....travellocblog/files folder.
      String path = Environment.getExternalStorageDirectory() + mTripRoot;
      if (filepath != null && !filepath.equals(""))
      {
         if (filepath.startsWith("/"))
            path += filepath;
         else
            path += ("/" + filepath);         
      }
      return path;
   }

   /* updates element if index valid, otherwise adds new element */
   public Boolean saveBlogElement(BlogElement blog, int index)
   {
      if (!blog.valid())
      {
         return false;
      }
      
      // keep user-entered strings unchanged - so no trimming on save of the
      // text fields such as blog.description, etc

      if (index >= 0 && index < mBlogPosts.size())
      {
         mBlogPosts.set(index, blog);
      }
      else
      {
         mBlogPosts.add(blog);
      }
      
      if (saveBlogToFile(mFilename) == false)
      {
         refreshData();
         return false;
      }
      return true;
   }

   /* deletes blog element */
   public Boolean deleteBlogElement(int index)
   {
      if (index < mBlogPosts.size())
      {
         mBlogPosts.remove(index);
         if (saveBlogToFile(mFilename) == false)
         {
            refreshData();
            return false;
         }
         return true;
      }
      return false;
   }

   private void refreshData()
   {
      mBlogPosts.clear();
      loadDataFromFile();
   }

   public int getMaxBlogElements()
   {
      return mBlogPosts.size();
   }

   public BlogElement getBlogElement(int index)
   {
      if (index < mBlogPosts.size())
         return mBlogPosts.get(index);
      return null;
   }

   /* This parses the kml file, adding all the data to our class data structure mBlogs.
    * Note only files created by this app can be parsed (v limited).
    * We also ignore the lines at the end of the file - these will be recreated when
    * we save anyway.  Example KML:
      <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
      <kml xmlns="http://www.opengis.net/kml/2.2">
        <Document>
          <Placemark>
            <name>My First TravelBlog Post</name>
            <description>01/06/2011 21:16
               This is fun!</description>
            <Point>
              <coordinates>-1.8266775000000002,52.8473925,0</coordinates>
            </Point>
            <TimeStamp>
              <when>2011-06-01T09:16:50Z</when>
            </TimeStamp>
          </Placemark>
        </Document>
      </kml>    
    */
   private Boolean loadDataFromStream(InputStream is)
   {
      // Dom it up
      // Create instance of DocumentBuilderFactory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

      InputStream bis = null;

      try
      {
         bis = new BufferedInputStream(is);

         // Get the DocumentBuilder
         DocumentBuilder docBuilder = factory.newDocumentBuilder();
         Document dom = docBuilder.parse(bis);
         
         if (dom == null)
            return false;
         
         Element root = dom.getDocumentElement();
         if (root == null)
            return false;
         
         NodeList placemarks = root.getElementsByTagName("Placemark");
         if (placemarks == null)
            return false;
         
         for (int i = 0; i < placemarks.getLength(); i++)
         {
            BlogElement blog = new BlogElement();
            Node placemark = placemarks.item(i);
            NodeList properties = placemark.getChildNodes();
            Boolean foundPoint = false;
            int j;
            for (j = 0; j < properties.getLength(); j++)
            {
               Node property = properties.item(j);
               String name = property.getNodeName();
               if (name.equalsIgnoreCase("Point"))
               {
                  foundPoint = true;
                  break;
               }
            }
            if (foundPoint == false)
            {
               continue;
            }

            for (j = 0; j < properties.getLength(); j++)
            {
               Node property = properties.item(j);
               String name = property.getNodeName();
               if (name.equalsIgnoreCase("name"))
               {
                  Node child = property.getFirstChild();
                  if (child != null)
                     blog.title = property.getFirstChild().getNodeValue();
               }
               else if (name.equalsIgnoreCase("description"))
               {
                  StringBuilder text = new StringBuilder();
                  NodeList chars = property.getChildNodes();
                  for (int k = 0; chars != null && k < chars.getLength(); k++)
                  {
                     text.append(chars.item(k).getNodeValue());
                  }
                  blog.description = text.toString();
               }
               else if (name.equalsIgnoreCase("Point"))
               {
                  NodeList pointProperties = property.getChildNodes();
                  for (int k = 0; k < pointProperties.getLength(); k++)
                  {
                     Node pointProperty = pointProperties.item(k);
                     if (pointProperty.getNodeName().equalsIgnoreCase(
                           "coordinates"))
                     {
                        blog.location = (pointProperty.getFirstChild()
                              .getNodeValue());
                     }
                  }
               }
               else if (name.equalsIgnoreCase("TimeStamp"))
               {
                  NodeList pointProperties = property.getChildNodes();
                  for (int k = 0; k < pointProperties.getLength(); k++)
                  {
                     Node pointProperty = pointProperties.item(k);
                     if (pointProperty.getNodeName().equalsIgnoreCase("when"))
                     {
                        Node data = pointProperty.getFirstChild();
                        if (data == null) {
                           blog.timeStamp = "";
                        }
                        else 
                        {
                           blog.timeStamp = data.getNodeValue();
                        }
                     }
                  }
               }
            }
            mBlogPosts.add(blog);
         }

      }
      catch (Exception e)
      {
         Log.e(TAG, "DOM Parse error loading file " + e);
         /**
          * TODO: added this return false in version 2.0.0 (Sep 2013).
          * There was no return false on exception, so sometimes, even when loaded file had
          * a problem, the rest of the app would continue to execute, and sometimes it worked
          * fine, sometimes it failed. This should only happen on non-TravelBlog generated
          * KML files, but leaving in TODO to have it work - handle all possible data files,
          * and fill in blog data structure as best as it is possible. Basically, need to check
          * for null pointers in all of the navigation above, as in the blog.timeStamp above.
          */
         return false;
      }
      finally {
         if (bis != null) {
            try {
               bis.close();
            } catch (IOException e) {
               Log.e(TAG, e.getMessage());
            }
         }
      }

      Log.d(TAG, "Loaded KML from stream ");
      return true;
   }

   // Load data from mFileName into mBlogPosts etc
   private Boolean loadDataFromFile()
   {
      String path = fullBlogPath(mFilename);
      File file = new File(path);
      try {
         return loadDataFromStream(new FileInputStream(file));
      }
      catch (FileNotFoundException e) {
         return false;
      }
   }
   
   /* Only used for the overall trip stats - rudimentary stuff */
   public float getTotalDistance()
   {
      float total = 0.0F;
      Double previousLon = 0.0;
      Double previousLat = 0.0;
      Boolean previousLocValid = false;
      for (int i = 0; i < mBlogPosts.size(); i++)
      {
         BlogElement blog = (BlogElement) mBlogPosts.get(i);
         String[] temp;
         Double lat, lon;
         try
         {
            temp = blog.location.split(",");
            if (temp.length < 2)
            {
               continue;
            }
            lon = Double.parseDouble(temp[0]);
            lat = Double.parseDouble(temp[1]);
         }
         catch (Exception e)
         {
            continue;
         }

         if (previousLocValid == true)
         {
            float results[] = { 0, 0, 0, 0, 0 };
            Location.distanceBetween(previousLat, previousLon, lat, lon, results);
            total += results[0];
            // Log.d(TAG, "Distance: "+ Math.round(results[0]) +
            // "m");
         }

         previousLocValid = true;
         previousLon = lon;
         previousLat = lat;
      }

      return total;
   }

   /* here we run through the mBlogs data structure and save to XML */
   public Boolean saveBlogToFile(String blogname)
   {
      String path = fullBlogPath(blogname);
      File newxmlfile = new File(path);
      try
      {
         newxmlfile.createNewFile();
      }
      catch (IOException e)
      {
         Log.e(TAG, "IOException exception in createNewFile() method");
         return false;
      }
      // we have to bind the new file with a FileOutputStream
      FileOutputStream fileos = null;
      try
      {
         fileos = new FileOutputStream(newxmlfile);
      }
      catch (FileNotFoundException e)
      {
         Log.e(TAG, "FileNotFoundException can't create FileOutputStream");
         return false;
      }
      // we create a XmlSerializer in order to write xml data
      XmlSerializer serializer = Xml.newSerializer();
      try
      {

         // we set the FileOutputStream as output for the serializer, using
         // UTF-8 encoding
         serializer.setOutput(fileos, "UTF-8");
         // Write <?xml declaration with encoding (if encoding not null) and
         // standalone flag (if standalone not null)
         serializer.startDocument(null, Boolean.valueOf(true));
         // set indentation option
         serializer.setFeature(
               "http://xmlpull.org/v1/doc/features.html#indent-output", true);
         // start a tag called "root"
         serializer.startTag(null, "kml");
         // set an attribute called "xmlns" with a "http:..." for <kml>
         serializer.attribute(null, "xmlns", "http://www.opengis.net/kml/2.2");
         serializer.startTag(null, "Document");
         /* Perhaps use for icon in future?:
          * <Style id="desired_id"> <IconStyle> <Icon>
          * <href>http://www.yourwebsite.com/your_preferred_icon.png</href>
          * <scale>1.0</scale> </Icon> </IconStyle> </Style> then in placemark:
          * <styleUrl>#desired_id</styleUrl>
          */
         for (int j = 0; j < 2; j++)
         {
            String previousLocation = null;
            for (int i = 0; i < mBlogPosts.size(); i++)
            {
               BlogElement blog = (BlogElement) mBlogPosts.get(i);

               if ((j == 1) && (i == 0))
               {
                  /* if we are running through this blog loop for the second time
                   * we are adding the kml stuff to draw a line from one point to 
                   * the next.  This will be in a folder that isn't open (no-one wants to see 
                   * it on the left-hand pane in Google Maps anyway):
                   * 
                   <Folder>
                     <name>Lines</name>
                     <open>0</open>
                     <Placemark>
                       <LineString>
                         <coordinates>-1.8266775000000002,52.8473925,0 -1.8266775000000002,53.8473925,0</coordinates>
                       </LineString>
                     </Placemark>
                     <Placemark>
                       <LineString>
                         <coordinates>-1.8266775000000002,53.8473925,0 -1.8266919000000001,54.847400300000004,0</coordinates>
                       </LineString>
                     </Placemark>
                   </Folder>
                   */
                  serializer.startTag(null, "Folder");
                  serializer.startTag(null, "name");
                  serializer.text("Lines");
                  serializer.endTag(null, "name");
                  serializer.startTag(null, "open");
                  serializer.text("0");
                  serializer.endTag(null, "open");
               }
               else
               {
                  serializer.startTag(null, "Placemark");
               }
               if ((blog.title != null) && (j == 0))
               {
                  serializer.startTag(null, "name");
                  // write some text inside <name>
                  serializer.text(blog.title);
                  serializer.endTag(null, "name");
               }
               if ((blog.description != null) && (j == 0))
               {
                  serializer.startTag(null, "description");

                  BufferedReader reader = new BufferedReader(new StringReader(
                        blog.description));
                  String line;
                  try
                  {
                     while ((line = reader.readLine()) != null)
                     {
                        if (line.length() > 0)
                        {
                           serializer.text(line);
                           serializer.text("\r");
                        }
                     }
                  }
                  catch (Exception e)
                  {
                     Log.e(TAG, "Exception error occurred while reading blog name");
                  }
                  serializer.endTag(null, "description");
               }
               if (blog.location != null)
               {
                  if (j == 0)
                  {
                     serializer.startTag(null, "Point");
                     serializer.startTag(null, "coordinates");
                     serializer.text(blog.location);
                     serializer.endTag(null, "coordinates");
                     serializer.endTag(null, "Point");
                  }
                  if ((previousLocation != null) && (j == 1))
                  {
                     serializer.startTag(null, "LineString");
                     serializer.startTag(null, "coordinates");
                     String coods = previousLocation + " " + blog.location;
                     serializer.text(coods);
                     serializer.endTag(null, "coordinates");
                     serializer.endTag(null, "LineString");
                  }
               }
               if (blog.timeStamp != null)
               {
                  if (j == 0)
                  {
                     /* timestamp can be used in Google Earth */
                     serializer.startTag(null, "TimeStamp");
                     serializer.startTag(null, "when");
                     serializer.text(blog.timeStamp);
                     serializer.endTag(null, "when");
                     serializer.endTag(null, "TimeStamp");
                  }

               }
               previousLocation = blog.location;
               if ((j == 0) || (i > 0))
               {
                  serializer.endTag(null, "Placemark");
               }
               if ((j == 1) && (i == (mBlogPosts.size() - 1)))
               {
                  serializer.endTag(null, "Folder");
               }
            }
         }
         serializer.endTag(null, "Document");
         serializer.endTag(null, "kml");
         serializer.endDocument();
         // write xml data into the FileOutputStream
         serializer.flush();
         // finally we close the file stream
         fileos.close();
      }
      catch (Exception e)
      {
         Log.e(TAG, "Exception error occurred while creating xml file");
         return false;
      }
      Log.d(TAG, "Saved file " + path);
      return true;
   }

}

/* The Blog Element used in the array */
class BlogElement
{
   public String description = "";
   public String title = "";
   public String location = "";
   public String timeStamp = "";
   /**
    * Many parts of code expect string for these fields. And in error cases such as
    * when an foreign .kml file is loaded, having null here can cause the app to never
    * be able to start, crashes on trying to load the bad file.
    * So, make these variables non-null so a string call like trim() or split() won't
    * terminate the app.
    */

   /**
    * Given a BlogElement, check if it has valid data, can be saved, etc.
    * @param blog the BlogElement object to check
    * @return true if object is ok and can be saved, etc
    */
   public Boolean valid()
   {
      if ((this.location == null) || (this.location.length() == 0)) {
         // Must have a location
         return false;
      }
      if ((this.title == null) || (this.description == null)) {
         return false;
      }
      if ((this.title.length() == 0) && (this.description.length() == 0)) {
         // Both strings can't be empty
         return false;
      }
      return true;
   }

}
