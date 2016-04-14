# Target users #

This document is for programmers who want to contribute changes to Travel Blog. If you just want to request a feature or file a bug, but have someone else work on it, please use the "Issues" tab above.

# Android SDK #

Before getting any code, you should install the [Android SDK](http://developer.android.com/sdk/installing.html) and the required [components](http://developer.android.com/sdk/adding-components.html).

For release version 2.0 as of September 2013, you'll need "SDK Platform Android 4.3, API 18" and the support libraries listed below.
For release version 1.7 as of August 2011, you'll need "SDK Platform Android 1.6, API 4".

# Eclipse setup #

Travel Blog is primarily developed using [Eclipse](http://www.eclipse.org). To set it up, first install the Android SDK as described above, then install the Eclipse plugin as described [here](http://developer.android.com/sdk/eclipse-adt.html#installing).

Once you have the code in your local clone (see below for instructions on using version control to get it), use File > Import > General > Existing Projects into Workspace, then select the root directory.

To properly compile and run tests, you'll also need to set an Eclipse variables, which can be set under Eclipse Preferences > Java > Build path > Classpath variables:
  * **ANDROID\_SDK** - make this variable point to the Android SDK's root directory (the one inside which is the platforms directory)

**HACK**: If Eclipse complains about a missing gen directory, do the following steps:
  1. Disable auto-building (uncheck Project > Build Automatically)
  1. Clean the project without building (Project > Clean, uncheck Start a Build Immediately)
  1. Create an empty directory called "gen" in the projects
  1. Build normally

Also, when writing code for Travel Blog, please keep your coding style consistent with the one in the rest of the app.

[Install Google Play Services](http://developer.android.com/google/play-services/setup.html) libraries, and SupportMapFragment. This is sometimes tricky, may need to follow help from other web pages too, here are the essential steps:
  1. Start Android SDK Manager, download "Google Play Services" under Extras. File -> Import -> Android -> Existing Android Code Into Workspace android-sdk-windows\extras\google\google\_play\_services\libproject\google-play-services\_lib. Make sure copy project into workspace is checked on.
  1. In Eclipse, select the project, and then Project -> Properties -> Android and add the Google Play Services lib.

[Install v7 appcompat](http://developer.android.com/tools/support-library/setup.html#download) library, follow the instructions very carefully on that page. Section "Adding libraries with resources" is relevant here. And just as for the previous Google Play Services instructions, selecting "Copy to workspace" may avoid problems later on. One key step is missing the Android docs: [use latest build target](http://stackoverflow.com/questions/18293088/adding-support-libraries-to-android-project) even if building for older API 7. This app has been built with API 18, but since it will use the appcompat v7 library (which also includes the v4 support library), the built app should run on API 7 and above.

If the build still shows some errors about missing libraries, try to clean all the project and rebuild it. If that still fails to resolve all errors, try changing build target to some other version, clean project, build it, change target back to Google APIs 18, clean, build, and this usually fixes Eclipse compile issues.
minSDK is 8, Froyo and newer, to get the Google Play Services location providers.

# Maps API keys #

Maps Android API v2 Key to add to AndroidManifest.xml (this applies to Travel Blog version 2.0 or newer only):

[Get key from API Console](https://code.google.com/apis/console/) for Google Maps Android API v2.
Navigate to API Access -> Create New Android Key and enter `SHA1;package-name` For example:
> `67:78:...:4F;com.barkside.travellocblog`
Find the appropriate SHA1 - for debug, or for release, as per the instructions below.

With the key, fill in this part in the manifest:
```
  <meta-data
      android:name="com.google.android.maps.v2.API_KEY"
      android:value="FILL-IN-KEY" />
```

If testing on a device with a changed API key, make sure to completely uninstall the old app from the device before debugging or running it on the device from Eclipse.

API v1 Key to add to map\_trip.xml (this is only necessary for v1.7 or older versions of Travel Blog):

Please note that using the Maps API to display maps on Android requires a per-developer key. The key that's saved in the repository is the one for packages signed with the travel-blog-android release key - if you'd like maps to show up when running your own copy of Travel Blog, then you'll need to change res/layout/map\_trip.xml to set your own key.

To obtain your own key, first get the fingerprint of your certificate:

```
$ keytool -list -keystore ~/.android/debug.keystore
```

(the password is "android")

Take the displayed fingerprint and [request a maps api key from Google](http://code.google.com/intl/pt-BR/android/maps-api-signup.html). Put the provided api key in map\_trip.xml, and **remember not to commit the key change back to the repository**.

# Version control #

## Overview ##

Travel Blog uses Mercurial, a distributed version control system. What this means is that, even though this page hosts a central repository, there can be many clone repositories with changes of their own, and then some of those can be merged back into the main repository.

The model we've chosen for developing Travel Blog is the following:
  1. Each developer creates a google code hosting clone of the main Travel Blog repository. This clone is hosted on Google servers.
  1. The developer then makes a local clone of his code hosting clone, which is then at his local machine.
  1. The developer writes new code into his local clone and commits it locally
  1. When a change is ready to be integrated back into the main repository, that change is pushed from the developer's local clone to his code hosting clone
  1. He then requests a code review by opening a new issue under "Issues" above, saying which clone has the code to be reviewed, what it's supposed to do, and what are the relevant changesets
  1. The code will be reviewed on the user's clone - if any further changes are suggested, the process repeats from (3)
  1. Once the change is approved, a member of the Travel Blog team will merge it back into the main repository

Even though this may sound complicated, this process makes code reviews easy and allows a lot of people to work on changes in parallel.

Next is an overview of each step, but if you want to really learn mercurial, please look at the references at the bottom of this page.

## Mercurial installation ##

First, make sure you have Mercurial installed by running the command:

```
$ hg version
Mercurial Distributed SCM (version 1.2)

...
```

If you don't want your hostname and username to be made public, you can change how you're identified in commits you make by editing your ~/.hgrc file:

```
[ui]
username = John Doe
```

## Making a clone of the repository ##

We'll need to create two clones of the main Travel Blog repository - one online, and then a local clone of that one.

To create the online clone, click on "Source" above, then on "Create Clone". Give your clone a name, summary and description, then click on "Create repository clone". At that point the online clone is ready.

**IMPORTANT**
If you plan to have your code reviewed, then you also want to go into "Administer", "Source", and check "Allow non-members to review code".

To create the local clone, click on "Source" tab of your clone page, and then use the checkout command provided there:

```
hg clone https://joeblogs-travel-blog-android.googlecode.com/hg/ joeblogs-travel-blog-android
```

Optionally, you can add your username and password to it (so you don't have to type them in every time):

```
hg clone https://joeblogs:mypassword@joeblogs-travel-blog-android.googlecode.com/hg/ joeblogs-travel-blog-android
```

and that's it - you have a local copy of your clone (in this example, in subdirectory "joeblogs-travel-blog-android") which you can then make changes to.

## Bringing in new changes from the master repository ##

The recommended way of bringing changes in from the main repository is the use of "hg fetch":

```
$ hg fetch http://travel-blog-android.googlecode.com/hg/
```

Please note that the fetch command is a Mercurial extension which is equivalent to "hg pull -u" plus "hg merge" plus "hg commit" - for more details please see the references, but this basically means that it will try to merge the incoming changes with your local changes.

If you want to see what will be brought in with the above command before running it, you can use:

```
$ hg incoming
```


## Committing changes locally ##

Commiting changes locally is easy - run "hg status" to see the state of your local clone:

```
$ hg status
?  MyNewFile
M  MyChangedFile
!  MyDeletedFile
```

In the above example, it shows one file that it knows nothing about (MyNewFile), one that it knows about but is missing (MyDeletedFile) and one that has had changes made to it.

To add all the previously unknown files and remove any missing files, use the addremove command:
```
$ hg addremove
Adding MyNewFile
Deleting MyDeletedFile
```

hg status then shows the new status:

```
$ hg status
A  MyNewFile
M  MyChangedFile
D  MyDeletedFile
```

If you wish to see what has changed, you can use the "hg diff" command.
Finally, you can commit the changes with

```
$ hg commit
```

which will open an editor for you to type in a description for these changes. Optionally, you can specify filenames to hg commit in order to commit only part of your current changes.

**IMPORTANT**: When your change is pulled into the main Travel Blog source, the change description that you entered here will show up as changes in the main travel-blog-android source, so please use a meaninful description - "fixing bug", "making changes", etc. are not ok, please instead use something like "fixing GPX import bug caused by null pointer", "adding Russian translation", etc. so that it makes sense in the context of travel-blog-android as a whole, not just your clone.

## Pushing changes to your online clone ##

Pushing changes to your online clone is incredibly simple:

```
$ hg push
pushing to https://joeblogs:***@joeblogs-travel-blog-android.googlecode.com/hg/
searching for changes
adding changesets
adding manifests
adding file changes
added 1 changesets with 1 changes to 1 files
```

and you're done.

If you want to see what changes you're going to push before you do it, you can also use the following command:

```
$ hg outgoing
comparing with https://joeblogs:***@joeblogs-travel-blog-android.googlecode.com/hg/
searching for changes
changeset:   5:b6fed4f21233
tag:         tip
user:        Joe Blogs
date:        Tue May 05 06:55:53 2009 +0000
summary:     Added an extra line of output
```

## Requesting a code review ##

To request a code review, go into the "Issues" tab of the Travel Blog project, click new Issue, select template "Review request", fill out the fields from the template. Someone will then review the code changes and integrate them when ready. Please notice that if the code change is for an existing, open issue, there's no need to file a separate request for the code review - simply post the link to the relevant changes on that issue (and if you have permission to, change its state to "UnderReview").

# Maintainer instructions #

These are instructions for the Travel Blog commiters who will be necessarily doing code reviews and integrating changes into the main repository.

## Merging changes into the main repository ##

Main repository maintainers should usually have a local clone of the main repository:

```
hg clone https://user%40google.com:password@travel-blog-android.googlecode.com/hg/ travel-blog-android
```

To integrate changes from a clone http://X.googlecode.com/hg/, first check what will be pulled, then pull them to your local clone:

```
$ hg incoming http://X.googlecode.com/hg/
<list of diffs>
$ hg pull -u http://X.googlecode.com/hg/
pulling from http://X.googlecode.com/hg/
searching for changes
adding changesets
adding manifests
adding file changes
added 1 changesets with 1 changes to 1 files
```

If you need to pull just specific revisions, you can do that with the -r flag:

```
$ hg pull -r7e95bb -u http://X.googlecode.com/hg/
```

If there are any conflicts, you'll need to fix them by using "hg merge" then "hg commit" (see the reference at the bottom of this page for details) - you can also use "hg fetch" which is equivalent to "hg pull -u" plus "hg merge" plus "hg commit" (when necessary).

It is important to understand that this pulls revision "7e95bb" and ALL the other remote revisions that happened before that one - in other words, it "merges" up to that revision. If you need to pull a single revision from the middle of the repository, you want to use the "transplant" extension:

```
$ hg transplant -s http://X.googlecode.com/hg/ 7e95bb
searching for changes
applying 7e95bb
```

At this point, **test** the change - make sure it works for you as well as it did for the original author. Some things just can't be caught in code reviews.

Once you know the change is ok, push it up to the main repository:
```
$ hg push
```

## Making releases ##

Please see [ReleaseProcess](ReleaseProcess.md).

# References #

[Mercurial: The definitive guide](http://hgbook.red-bean.com/)