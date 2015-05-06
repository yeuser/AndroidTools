# Android Tools
Basic Android Tools, e.g. Asyncrounous Image Loading, Internet File Download, customizable Server Service Worker and more.
Many outstanding "Image Loading" libraries for android could be found over the net, but these libraries could not satisfy our concept of unique and simple usage.
Also, local file cache, blind network cache, client-server insteractions, device specific ID and background multi-threading over android

This android library answer these questions:
* [How to <b>download</b> a file from the Net?](#how1)
* [How can I get a BitmapImage from an <b>internet source</b>? Is it cached locally?](#how2)
* [How can I have a <b>local gallery</b> as a cached version of an <b>internet source</b>?](#how3)

### Notice:
In this android library some codes from [7-zip](http://www.7-zip.org/sdk.html) are used, and libraries of  [gson](http://code.google.com/p/google-gson/), [log4j](http://logging.apache.org/log4j/) and [android-logging-log4j](http://code.google.com/p/android-logging-log4j) are referenced.

## Features
There are some tools and functions which are basis of any android application. Here, some of these functions, in their most general usage is created.

* <b>Internet File Cache ([InternetCacher](src/com/mixedpack/tools/android/InternetCacher.java)):</b>
A singletone file cache to store and get files from local storage or to fetch remote files and save them automatically in local storage with given name. This class supports file modification sensing through a local json file.
* <b>Blind Internet Image Cache ([AndroidNetGrabber](src/com/mixedpack/tools/android/AndroidNetGrabber.java)):</b>
You can assign duty of loading remote image files to this tool. It asynchronously fetchs remote images, chaches them in your local storage then load and resize them to your preferences. And, it's done with just one line of code!
* <b>Client-Server insteractions ([RemoteWorker](src/com/mixedpack/tools/RemoteWorker.java)):</b>
A solution to remote data communication for non de-facto services. With implementing one method of this abstract class, you will be able to work with the server.
```java
  protected abstract String getRemoteCallAddressPostfix(String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException;

// A Simple implementation (REST with parameters in queryString)
  @Override
  protected String getRemoteCallAddressPostfix(String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException {
    return function + "/";
  }
  
// Another implementation (REST with parameters in url)
  @Override
  protected String getRemoteCallAddressPostfix(String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException {
    String retValue = function + "/";
    for (Entry<String, String> entry : getParameters.entrySet()) {
      retValue += entry.getKey() + "/" + entry.getValue() + "/";
    }
    // Empty getParameters to avoid appearance of parameters in queryString
    getParameters.clear();
    return retValue;
  }
```

* <b>Device Specific ID:</b>
An ID for device, to count your users, track their behaviours and improve your app.
```java
AndroidUtils.getInstance().getAndroidID()
```

* <b>Background multi-threading:</b>
Background task in Android need to request lower priority and run a thread. To reduce line of code and Thread Creations this code was added to lib.
```java 
AndroidUtils.doInBackground(new Runnable() {
  @Override
  public void run() {
    // Background Task!
  }
});
```

## Installation

Copy this project in your workspace, and add it as an android library to your android project.
You can use below command to grab this project:
```console
$ git clone https://github.com/yeuser/AndroidTools.git
```
use code below to initialize the Util:
```java
// extFolder: the folder where you want to store your application's files. (To hide your folder from user, start extFolder with a dot.)
// context: a Context instance to initialize SystemPrivateFiles/Assets/ThreadPolicy/SystemPreferences/ContactData
// defaultAssetFontPath: path to custom font placed in asset folder. (It could be null, but if #changeFont() is used an Exception arises)
AndroidUtils.getInstance().init(/*String*/ extFolder, /*Context*/ context, /*String*/ defaultAssetFontPath);
// Create a folder named cache to place cached files. (This is a sample, you can change it however you like.)
File appCacheDir = new File(AndroidUtils.getInstance().getExternalDataDir(), "cache");
// URL_PREFIX_TO_REMOTE_IMAGE_FOLDER: Prefix of URL of remote storage of images. (You can place IP of server here, and set HOST_NAME, to avoid unnecessary DNS resolution.)
// HOST_NAME: hostname/domain of server, can be null. (Set this only if you used IP of host in URL_PREFIX_TO_REMOTE_IMAGE_FOLDER)
AndroidNetGrabber.initialize(/*String*/ URL_PREFIX_TO_REMOTE_IMAGE_FOLDER, /*String*/ HOST_NAME, appCacheDir);
// Disable automatic removal of images from memory. (Use only if the images are small)
AndroidNetGrabber.disableRemoveFromCache();
// Generate an ID based on the Device inforamtion
AndroidUtils.getInstance().initID();
```

## Sample Usage

Here are some sample code to use each part of library:

### How to <b>download</b> a file from the Net? ([InternetCacher](src/com/mixedpack/tools/android/InternetCacher.java))
To get a file from this [video sample source](http://techslides.com/sample-webm-ogg-and-mp4-video-files-for-html5) you just need to write one line of code:
```java
File sampleVideo = InternetCacher.getInstance().fetchFile("http://techslides.com/demos/sample-videos/small.mp4", null, "techslides-small-sample.mp4", DATA_DIR);
```
where `DATA_DIR` is the directory file is stored into! (DATA_DIR can be private or public, as long as your app is permitted to write there.)
Also, it is possible to force downloading the file from the link, using `force` param:
```java
File sampleVideo = InternetCacher.getInstance().fetchFile("http://techslides.com/demos/sample-videos/small.mp4", null, "techslides-small-sample.mp4", DATA_DIR, true);
```

You can always hit local storage without getting files from internet with:
```java
File sampleVideo = InternetCacher.getInstance().hitCache("techslides-small-sample.mp4", DATA_DIR);
```

### How can I get a BitmapImage from an <b>internet source</b>? Is it cached locally? ([AndroidNetGrabber](src/com/mixedpack/tools/android/AndroidNetGrabber.java))
There is a good wrapper for downloading images from internet directories and resizing them, and it is placed in [AndroidNetGrabber](src/com/mixedpack/tools/android/AndroidNetGrabber.java).
This class is designed to justify need for browsing some remote image folder or gallery, so first it's required to initialize variables for remote folder, host & local cache dir.
Let's say we want to browse some images from [Pexels](http://www.pexels.com/):
+ http://static.pexels.com/wp-content/uploads/2014/06/pasted-street-art-wall-art-843.jpg
+ http://static.pexels.com/wp-content/uploads/2014/06/background-blur-blurred-1350.jpg
+ http://static.pexels.com/wp-content/uploads/2015/02/clouds-dawn-dust-4530.jpg
+ http://static.pexels.com/wp-content/uploads/2015/04/industry-rails-train-5348.jpg

Clearly, remote path is `/wp-content/uploads/` on domain `static.pexels.com` with IP of `108.162.204.188`.
```java
// CACHE_DIR: a File object representing Root folder of image cache
AndroidNetGrabber.initialize("http://108.162.204.188/wp-content/uploads/", "static.pexels.com", CACHE_DIR);
```
Also, direct form of initialization is possible:
```java
AndroidNetGrabber.initialize("http://static.pexels.com/wp-content/uploads/", null, CACHE_DIR);
```
Next, get bitmap object for each image: (If it is still not cached it, first download and cache them.)
```java
Bitmap bitmap1 = AndroidNetGrabber.getInstance().getImageBitmap("2014/06/pasted-street-art-wall-art-843.jpg", -1, -1);
Bitmap bitmap2 = AndroidNetGrabber.getInstance().getImageBitmap("2014/06/background-blur-blurred-1350.jpg", -1, -1);
Bitmap bitmap3 = AndroidNetGrabber.getInstance().getImageBitmap("2015/02/clouds-dawn-dust-4530.jpg", -1, -1);
Bitmap bitmap4 = AndroidNetGrabber.getInstance().getImageBitmap("2015/04/industry-rails-train-5348.jpg", -1, -1);
```
with this; the images are stored locally in below files respectively:
- [CACHE_DIR]2014/06/pasted-street-art-wall-art-843.jpg
- [CACHE_DIR]2014/06/background-blur-blurred-1350.jpg
- [CACHE_DIR]2015/02/clouds-dawn-dust-4530.jpg
- [CACHE_DIR]2015/04/industry-rails-train-5348.jpg


<!--
### How can I have a <b>local gallery</b> as a cached version of an <b>internet source</b>? ([AndroidNetGrabber](src/com/mixedpack/tools/android/AndroidNetGrabber.java))
-->

<!--
### <b>Client-Server insteractions ([RemoteWorker](src/com/mixedpack/tools/RemoteWorker.java))</b>
-->

<!-- ## [Changelog](CHANGELOG.md) -->

## [License](LICENSE)
The MIT License (MIT)

Copyright (c) 2015 Yaser Eftekhari
