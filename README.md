# Android Tools
Basic Android Tools, e.g. Asyncrounous Image Loading, Internet File Download, customizable Server Service Worker and more.
Many outstanding "Image Loading" libraries for android could be found over the net, but these libraries could not satisfy our concept of unique and simple usage.
Also, local file cache, blind network cache, client-server insteractions, device specific ID and background multi-threading over android

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
```java AndroidUtils.getInstance().getAndroidID()```

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

<!-- ## [Changelog](CHANGELOG.md) -->

## [License](LICENSE)
The MIT License (MIT)

Copyright (c) 2015 Yaser Eftekhari

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
