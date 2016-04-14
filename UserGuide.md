<a href='Hidden comment: 
== Important: some of the features mentioned here are for a future 2.2 release of the application and not present in currently deployed version 2.0. ==

The first two sections Description and Features are the same as seen on the Google Play store page:
https://play.google.com/store/apps/details?id=com.barkside.travellocblog
and can be copied from here.

Links to images in working dir:
https://travel-blog-android.googlecode.com/hg-history/aczoom-01/external/main_phone4in.jpg

'></a>

# Description #

**Version 2.2 (Released December 2013)**

Travel Blog app creates posts that are also tagged using the location and timestamp of each post.

Existing text field as well as the location can be edited at any time. Location edits can be made by moving a marker on a displayed map.

Multiple trips are supported, and trips can be shared by sending KML files through email or uploaded. These can be displayed on a map by applications such as Google Earth.

[Source Code and Docs](https://code.google.com/p/travel-blog-android/)
<br><a href='https://play.google.com/store/apps/details?id=com.barkside.travellocblog'>Google Play Store</a>
<br><a href='https://groups.google.com/forum/#!forum/travel-blog-android'>Mailing List</a>

<h1>Features</h1>

<ul><li>Log your location from GPS, Wifi or cell-id (tells you how accurate the fix is). By default, GPS turns off automatically after 15 seconds.<br>
</li><li>Attach title and description text with current timestamp.<br>
</li><li>Keep track of multiple trips. Manage trips by renaming or deleting them.<br>
</li><li>Edit or delete old posts. Both the text part, as well as location part can be edited.<br>
</li><li>Save as KML file to SD card in the /TravelBlog/ directory.<br>
</li><li>KML file can then be Shared via email, or anything else you have installed such as Dropbox.<br>
</li><li>If KML file is uploaded at a internet-accessible URL, you can view trip on Google Maps or Bing Maps.<br>
</li><li>KML files can be imported to Google Earth and other similar applications.  Time-stamp of each post is saved so Google Earth animations and timelines can be used.<br>
</li><li>[2.2+] <a href='UserGuide#Widgets.md'>Widgets</a> can be installed on the home screen to directly launch into the New Post activity.<br>
</li><li>[2.2+] <a href='UserGuide#Import.md'>Import</a> KML file capability to allow other applications to send or share KML files to this application.</li></ul>

It is not a battery hogger as it doesn't continually log your location, perfect for traveling in remote parts of the world, or long periods away from power. It also doesn't need internet (unless you want to edit location, or view trip in Google maps, or share it via email etc), perfect for travel abroad where data roaming costs a lot.<br>
<br>
<h1>Setup</h1>

For entering new posts, the phone or tablet used must have location services turned on.<br>
For best performance, turn on both <i>GPS Satellites</i> and <i>Wi-Fi and/or Mobile Networks</i> in the Location Services settings on the device.<br>
<br>
<h1>Help</h1>

This section describes use of each of the screens in the app.<br>
<br>
<a href='https://travel-blog-android.googlecode.com/hg-history/default/external/'>Additional screen shots</a>.<br>
<br>
<h2>Main Screen</h2>

<img src='https://travel-blog-android.googlecode.com/hg-history/default/external/main_phone4in.jpg' />

When the app is started, it shows the last trip opened and a list view of all the posts in the trip. On first run, it will create the default trip name <i>MyFirstTrip</i>.<br>
<br>
Clicking on any list entry will take the user to the View and Edit Note screen.<br>
<br>
Long-clicking on any list entry note will show additional commands, and will allow the user to Delete the note.<br>
<br>
Clicking on the New Post icon (shows a location marker with + symbol) will bring up a screen similar to the View and Edit screen.<br>
<br>
<h2>New Post Screen</h2>

To activate this, click on the icon on the top Action Bar in the app. The icon displays a location marker with the + sign.<br>
<br>
This screen displays the title, the description, and the location.<br>
<br>
Type in the title and the description on this screen. The first line of the description may be filled in with the current date and time, depending on the Settings preferences values. This can be edited as needed, by using the normal text editing capabilities.<br>
<br>
The location text area will show the continuously updated location as three values: longitude, latitude, and accuracy. The <a href='http://developer.android.com/reference/android/location/Location.html#getAccuracy%28%29'>accuracy</a> is reported as a ± meters distance.<br>
The updates will be turned off after a fixed time period, to conserve battery. The location fix updates will also be turned off when the user clicks on the location text to edit the location.<br>
<br>
If you have internet access and wish to edit the location, click on the location text to bring up the Edit Location Screen. But it may be better to do any fine editing of the location at some later time at your leisure, especially if there is better Wifi access at a later time.<br>
<br>
When done editing, click on Save to save, or on Cancel to discard the post. Clicking on the Android Back button will also discard the post.<br>
<br>
<h2>View and Edit Post Screen</h2>

This displays the title, the description, and the location.<br>
<br>
The title and the description can be edited directly on this page.<br>
<br>
The location is displayed as a pair of numbers which represent the longitude and the latitude.<br>
If you have internet access and wish to edit the location, click on the location text to bring up the Edit Location Screen.<br>
<br>
When done editing the note, click on Save to save, or on Cancel to discard the edits. Clicking on the Android Back button will also discard the edit session changes.<br>
<br>
<h2>Edit Location Screen</h2>

This shows the title, the location longitude and latitude, and a map with a marker at the location.<br>
Click on the marker to select it and hold and drag the marker on the map to move the location.<br>
<br>
Use pinch to zoom or the zoom buttons to zoom in and out of the map. And use swipe motion to pan the map.<br>
<br>
If the marker location is not visible in the area of the map displayed, click on the location text to re-position the map so that the marker is seen.<br>
<br>
When done editing the location, click on Use Location to return to the View and Edit screen with the updated location.<br>
Or, click Cancel to discard the location edit and keep the location unchanged on the View and Edit screen. Clicking on the Android Back button will also discard the location edit.<br>
<br>
<h2>Menu Commands</h2>

Files are saved and loaded from a fixed directory named /TravelBlog/. A subset of the KML format is used to store the data, so all the files are .kml files.<br>
<br>
<b>Share Trip</b> (standard Android icon)<br>
<blockquote>Shares the trip file (in .kml format) to any application that can accept it. This includes a large number of applications such as Google Mail, Facebook, Google+, Keep, etc.</blockquote>

<b>Open Trip</b>
<blockquote>Lists all the .kml files. Select the trip file name to load.</blockquote>

<b>Map Trip</b>
<blockquote>Display all the posts locations on a map. The first location will be indicated by green colored start marker.<br>
The Map Trip has been tested with over 1000 locations, which displays in under an second on a Samsung Galaxy Nexus. Much larger number of notes, say a thousand or above, could cause the app to slowdown a lot. The recommendation is to keep the number of notes to below 500, or even 200 just to keep it manageable.</blockquote>

<blockquote>[2.2+] The screen title displays the name of the trip and the subtitle displays a trip summary information with number of places in the trip, and the total distance travelled. The distance units can be changed in the Settings menu.</blockquote>

<b>New Trip</b>
<blockquote>To start a new trip. Enter name of the new trip.</blockquote>

<b>Manage Trips ...</b>
<blockquote>This has two commands. <b>Delete Trip</b> lists all the .kml files. Select the trip file name to delete. If the currently open trip file is deleted, the app will delete it and open some other existing trip file. If all trip files are deleted, the default trip name <i>MyFirstTrip</i> will be used. <b>Rename Current Trip</b> changes the name of the currently loaded trip file.</blockquote>

<b>Delete Post</b>
<blockquote>This command is shown on the Edit Post screen. It can be used to delete the post currently being edited.</blockquote>

<b>Help</b>
<blockquote>Displays a help message, which also includes a link back to this page.</blockquote>

<b>Send Feedback</b>
<blockquote>This opens up the Email app to send feedback. This command first displays a privacy notice. On confirmation to proceed, the email message is created. This command will fail unless an email app is setup on the device.</blockquote>

<b>Settings</b>
<blockquote>Displays settings that can be changed from the default.<br>
<ol><li>Location fix duration changes the amount of time to scan for an updated location in the New Post screen. The default is 15 seconds after getting the first update, which will keep scanning until it receives an updated location, and then listen for 15 second more. This can be set to "No auto-off", which will keep looking for location updates until the user finishes editing the note, or until the user clicks on the location text to manually edit it. It can also be set to "15 seconds" exactly which will stop location scanning in exactly 15 seconds. If there is no location fix obtained, the app will reuse the location of the last entry in the trip, if it exists.<br>
</li><li>Auto-fill description can be set to be no auto-fill to leave the description blank, or it can be filled in with the current date and time.<br>
</li><li>Map trip info distance units can be set to Kilometers or Miles.<br>
</li><li><b>About</b> command displays version number and other information about the application.</li></ol></blockquote>

<h2>Widgets</h2>

Android home screen widgets can be installed to directly jump to the New Post editing screen. This allows for quick entry of a new location and note into an existing trip file.<br>
<br>
Widgets are installed in different ways, depending on the Android versions. On newer Android systems (probably 4.1 and later), click on the Application launcher, and on the Widgets tab. The <i>Travel Blog</i> widget will be displayed. Hold and copy it to any of the home screens.<br>
<br>
When the widget is installed, it will start up the Widget Configuration screen. This is where the behavior of the widget click can be configured.<br>
<br>
To always add a New Post to a particular trip file, select the trip file using the Select Trip button. There is also a New Trip button to create a new trip at this time.<br>
<br>
If the widget should always use the last opened file in this application, then select that option from the configuration screen.<br>
The last opened trip file is the last file that was manually opened using the Open Trip menu command in the main Travel Blog activity screen. Trips that are automatically opened by widget clicks, or in other Travel Blog activity screens are not marked as last opened, and only the Open Trip command in the main screen will be used in this widget configuration.<br>
<br>
Alternately, the specific trip file to use can be selected. In this case the two buttons<br>
Select Trip and New Trip become usable. Use Select Trip button to select an existing trip file.<br>
Use New Trip button to create a new trip file and select it.\n<br>
\n<br>
<br>
When done, click on the button with the check-mark. This button also displays the name of the trip file to be used by the widget.<br>
<br>
<h3>Resize Widgets</h3>

Once the widget is installed, it can be moved to any home screen as desired.<br>
<br>
The widget displays a clickable <i>New Post</i> icon, and the name of the file (if a specific trip was selected) under it.<br>
<br>
Long-clicking on the widget allows a resize of the widget. If necessary, reduce or increase the width of the widget as desired.<br>
<br>
<h2>Import</h2>

Other Android applications that can share or send .kml files can now send such files to this application. Whenever Android needs to show a list of applications that can receive a .kml file, this application will now show up as one of the receivers. The name shown will be <b>Travel Blog Import</b>.<br>
<br>
The Import screen shows a map of the points in the imported file. The default name to use for the imported data is the name of the original KML file. This name can be edited as needed, and then  click on Save to save it, and Cancel to cancel the import.<br>
<br>
At this time, the only way to import data is by having some other application send it to this application. In the future, we may add a <i>Import File</i> command to the application itself to browse the filesystem and load an external KML file.<br>
<br>
The data is loaded in Travel Blog format. If the data cannot be loaded, a message is printed to that effect and the import fails. Most KML files may load, because the application only looks at a small subset of the fields.<br>
The following fields from a .KML file are supported: a list of <b>Placemark</b> elements with <b>name</b>, <b>description</b>, and <b>Point</b> values.<br>
Example Placemark data that can be imported:<br>
<pre><code>          &lt;Placemark&gt;<br>
            &lt;name&gt;My First TravelBlog Post&lt;/name&gt;<br>
            &lt;description&gt;01/06/2011 21:16<br>
               This is fun!&lt;/description&gt;<br>
            &lt;Point&gt;<br>
               &lt;coordinates&gt;-1.8266,52.8473,0&lt;/coordinates&gt;<br>
            &lt;/Point&gt;<br>
            &lt;TimeStamp&gt;<br>
              &lt;when&gt;2011-06-01T09:16:50Z&lt;/when&gt;<br>
            &lt;/TimeStamp&gt;<br>
          &lt;/Placemark&gt;<br>
</code></pre>

<h1>Example Uses</h1>

Whenever you want to record your location manually (only record the interesting bits), this is the app for the job! You can then look at your trips directly on your phone, or share them with others.<br>
<br>
If, like me, you like to use all the free technology available to you, here is a more involved use case example. This works with the versions of Dropbox and Google Maps as of 2012, but may not work if they change. Use the description below as a guideline as to what is possible:<br>
<br>
Say you are going on a long holiday abroad. You have your Android phone with you, but know the battery doesn't last long, and you don't have data roaming. You would like to record where you went or keep your family and friends abreast of your travels.<br>
<br>
The setup before you leave on vacation:<br>
<ol><li>Set up your own travel blog on Blogger.<br>
</li><li>Find a website where you can remotely upload a file, to the same URL. for example, you can set up your own <a href='https://www.dropbox.com/'>Dropbox</a> account which is online place to store files.<br>
</li><li>Set your up your blog website with a link to the Google Maps search of you Dropbox Travel Blog file: In Dropbox, copy the link to your KML file by going to share... Now in Google Maps, enter this copied link into the Search Maps box and hit enter. This is your map, and you can now share this map. Goto Link, and either embed the HTML into your Blog website, or just share the link in emails, or on your blog website.<br>
</li><li>If the previous step does not work, try using this map display url: <pre>https://maps.google.com/maps?q=<your kml url></pre></li></ol>

Now you can use the Travel Blog app to update your Google Maps/Dropbox/Blogger pages:<br>
<ol><li>Use the Travel Blog app and record your location (say your hotel, favorite beach, day trip to a city, weekend trip to the mountains etc etc) with any quick notes.<br>
</li><li>No need to be precise. All notes and locations can be edited later if needed.<br>
</li><li>You come across Wifi access in your hotel, cafe or train - now you can send your Travel Blog trip file to Dropbox.<br>
</li><li>If you have no Wifi access, you decide to plug your phone into a PC at the local cyber cafe, and transfer the file manually to the PC, and from there to Dropbox.<br>
</li><li>As long as you also overwrite the same file in Dropbox with your updated Travel Blog trip, your Blog website will always be up-to-date, and won't need changing each time to point to a new file. Genius!</li></ol>

<h1>What's New</h1>

<a href='Hidden comment: 
This section is a copy of the most of the text in assets/release_notes.txt file in the source code.
'></a><br>
<br>
<h2>Version 2.2</h2>
December 2013<br>
<br>
<ul><li>App widgets can be installed on the home screen to jump to the New Post editing screen.<br>
<blockquote>Multiple widgets can be installed, each widget can point to a different trip file.<br>
Each widget invokes a configuration screen on install, which allows user to select<br>
an existing trip file, or create a new trip file, or to use the app default last used<br>
trip file on widget click.</blockquote></li></ul>

<ul><li>Importing capability added. Other Android applications can now send .kml files to this app.<br>
<blockquote>This screen will show the imported KML data on a map, with a Save As file name option.</blockquote></li></ul>

<ul><li>The Edit Post activity now shows a appropriate subtitle - "New Post" or "Edit Post" as appropriate. It also shows the index number of the entry being added or edited. Edit Location screen also shows the same number.</li></ul>

<ul><li>Rename Trip menu command added. This and Delete Trip is now under a Manage Trips submenu.</li></ul>

<ul><li>Map Trip screen now shows the trip info summary in the subtitle. This includes the number of places in the blog, and the total distance of the trip. The old menu command "Trip Info" has been removed.</li></ul>

<ul><li>Alert dialog now displayed if there is an error saving edited blog post or deleting a post.<br>
<blockquote>This used to be just a brief Toast message, but since failing to update trip file is a serious error, display it in a dialog box and do not terminate the edit activity.<br>
User can still use Cancel button, or the device back or parent button or home button to move to a new activity.</blockquote></li></ul>

<ul><li>Send feedback email now contains app version code and number, and subject includes device information.</li></ul>

<h2>Version 2.0.1</h2>
October 2013<br>
<br>
<ul><li>Editing location points now possible using visual maps to update the address.</li></ul>

<ul><li>Single click on blog entry in the main list now shows details of the entry and provides editing capability same as the "Edit Post" context command.</li></ul>

<ul><li>Single click on the location longitude,latitude text will open up the visual location editor.</li></ul>

<ul><li>Now uses Google Play Services API, which takes care of power usage and provides a Fused location provider for updating location info.<br>
<blockquote>The new Location API handles power and accuracy on its own. So we no longer can shut it off by looking at the provider type, instead, we just keep updating the location for a brief amount of time - around 15 seconds or so after the first update is received.<br>
After that, the updater is shut off to conserve battery.</blockquote></li></ul>

<ul><li>There is no Retry Location fix anymore, to avoid complexity. The new Location Api should give a pretty good location fix fast, and user can always edit the location on a map.</li></ul>

<ul><li>Fixed reported <a href='https://code.google.com/p/travel-blog-android/issues/detail?id=#2'>issue#2</a> kml file format now correctly adds the xmlns attribute to kml tag and not the Document tag.</li></ul>

<ul><li>TBD - not final, may support  Android 2.2 (Froyo) API 8 too.<br>
</li></ul><blockquote>Minimum Android version: Android 2.3.3 (Gingerbread and newer).<br>
Required SDK Version is now 10 (was 4).  Google Play Services (to display maps, get location updates using the Fused provider) requires Android 2.2+ (Froyo+).</blockquote>


<h2>Version 1.7</h2>
August 2011<br>
<br>
<ul><li>The first open-sourced version, where adverts are removed.</li></ul>

<h1>Source Code</h1>

This app is open-sourced. The source code for this app is available at <a href='http://code.google.com/p/travel-blog-android/'>http://code.google.com/p/travel-blog-android/</a>.