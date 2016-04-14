# Release Process #

This process is driven by barkside-apps.

May need to use [ProGuard](http://developer.android.com/tools/help/proguard.html). Since the Android v7 and v4 support libraries have a lot code, and not all of it will be used in this app, it may be worth using ProGuard to remove unused classes. The [newer ProGuard instructions](http://tools.android.com/recent/proguardimprovements) say to uncomment this line in project.properties, assuming use of ADT 17 or later:
```
proguard.config=${sdk.dir}/tools/proguard/proguard-android.txt:proguard-project.txt

proguard-project.txt can be edited to contain any config changes necessary for this app.
```