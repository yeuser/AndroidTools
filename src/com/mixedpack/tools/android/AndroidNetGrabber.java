package com.mixedpack.tools.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.widget.ImageView;

@SuppressWarnings("deprecation")
public class AndroidNetGrabber {
  private static AndroidNetGrabber instance                  = null;
  private final static Semaphore   instantiation_lock        = new Semaphore(1);
  private final static Logger      logger                    = Logger.getLogger(AndroidNetGrabber.class);
  @SuppressWarnings("unused")
  private long                     cachedBitmapsMemory       = 0;
  private Map<String, Bitmap>      cachedBitmaps             = new HashMap<String, Bitmap>();
  private Map<Bitmap, String>      cachedBitmapsKeys         = new HashMap<Bitmap, String>();
  private Map<Bitmap, int[]>       cachedBitmapsCount        = new HashMap<Bitmap, int[]>();
  private static String            REMOTE_MEDIA_PATH;
  private static String            REMOTE_MEDIA_HOST;
  private static File              DATA_DIR;
  private static boolean           REMOVE_FROM_CACHE_ENABLED = true;

  public static void enableRemoveFromCache() {
    REMOVE_FROM_CACHE_ENABLED = true;
  }

  public static void disableRemoveFromCache() {
    REMOVE_FROM_CACHE_ENABLED = false;
  }

  public static void initialize(String remoteMediaPath, String remoteMediaHost, File localDataDir) {
    AndroidNetGrabber.REMOTE_MEDIA_PATH = remoteMediaPath;
    AndroidNetGrabber.REMOTE_MEDIA_HOST = remoteMediaHost;
    AndroidNetGrabber.DATA_DIR = localDataDir;
  }

  private int imageLoadFactor = 0;

  private AndroidNetGrabber() {
    InternetCacher.getInstance();
  }

  public static AndroidNetGrabber getInstance() {
    if (instance == null) {
      try {
        instantiation_lock.acquire();
      } catch (InterruptedException e) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("instantiation_lock Error", e);
      }
      if (instance == null)
        instance = new AndroidNetGrabber();
      instantiation_lock.release();
    }
    return instance;
  }

  // @SuppressWarnings("unused")
  // private File getPrivateCachedFile(String url, String name) throws InterruptedException, ExecutionException {
  // return InternetCacher.getInstance().getStoredFile(url, name, GeneralParams.INT_DATA_DIR);
  // }
  private File fetchFile(String name) throws InterruptedException, ExecutionException {
    return getSDCachedFile(REMOTE_MEDIA_PATH + name, REMOTE_MEDIA_HOST, name);
  }

  private File fetchFile(String name, boolean force) throws InterruptedException, ExecutionException {
    return getSDCachedFile(REMOTE_MEDIA_PATH + name, REMOTE_MEDIA_HOST, name, force);
  }

  private File getSDCachedFile(String url, String host, String name, boolean force) throws InterruptedException, ExecutionException {
    return InternetCacher.getInstance().fetchFile(url, host, name, DATA_DIR, force);
  }

  private File getSDCachedFile(String url, String host, String name) throws InterruptedException, ExecutionException {
    return InternetCacher.getInstance().getStoredFile(url, host, name, DATA_DIR);
  }

  private File hitSDCache(String name) throws InterruptedException, ExecutionException {
    return InternetCacher.getInstance().hitCache(name, DATA_DIR);
  }

  private synchronized Bitmap checkCache(String name) {
    Bitmap bitmap = cachedBitmaps.get(name);
    if (bitmap == null) {
      return null;
    }
    if (bitmap.isRecycled()) {
      cachedBitmaps.remove(name);
      cachedBitmapsCount.remove(bitmap);
      return null;
    }
    cachedBitmapsCount.get(bitmap)[0]++;
    return bitmap;
  }

  private synchronized void putInCache(String name, Bitmap bitmap) {
    if (name == null || bitmap == null)
      return;
    cachedBitmaps.put(name, bitmap);
    cachedBitmapsKeys.put(bitmap, name);
    cachedBitmapsCount.put(bitmap, new int[] {
      1
    });
    cachedBitmapsMemory += bitmap.getRowBytes();
  }

  private synchronized void deleteFromCache(String name, Bitmap bitmap) {
    if (bitmap == null)
      return;
    cachedBitmapsCount.get(bitmap)[0]--;
    if (cachedBitmapsCount.get(bitmap)[0] == 0 && REMOVE_FROM_CACHE_ENABLED) {
      cachedBitmaps.remove(name);
      cachedBitmapsKeys.remove(bitmap);
      cachedBitmapsCount.remove(bitmap);
      bitmap.recycle();
    }
  }

  public void recycleBitmap(String name) {
    if (name == null) {
      return;
    }
    Bitmap bitmap = cachedBitmaps.get(name);
    deleteFromCache(name, bitmap);
  }

  public void recycleBitmap(Bitmap bitmap) {
    if (bitmap == null) {
      return;
    }
    String name = cachedBitmapsKeys.get(bitmap);
    if (name == null) {
      bitmap.recycle();
      return;
    }
    deleteFromCache(name, bitmap);
  }

  // private Bitmap getACutOfBitmap(Bitmap bitmap, Rect rect) {
  // if (bitmap == null) {
  // return null;
  // }
  // String name = cachedBitmapsKeys.get(bitmap);
  // if (name == null) {
  // if (logger.isTraceEnabled())
  // logger.trace("null<-getACutOfBitmap(bitmap=" + bitmap + "[" + bitmap.getWidth() + "," + bitmap.getHeight() + "],name=" + name + ",rect=[" + rect.left + "," + rect.top
  // + "," + rect.right + "," + rect.bottom + "])");
  // return null;
  // }
  // try {
  // File img = hitSDCache(name);
  // if (img == null) {
  // if (logger.isTraceEnabled())
  // logger.trace("null<-getACutOfBitmap(bitmap=" + bitmap + "[" + bitmap.getWidth() + "," + bitmap.getHeight() + "],name=" + name + ",rect=[" + rect.left + "," + rect.top
  // + "," + rect.right + "," + rect.bottom + "])");
  // return null;
  // }
  // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
  // Bitmap ret = decodeBitmapRegion(new FileInputStream(img), rect);
  // if (logger.isTraceEnabled())
  // logger.trace(ret + "<-getACutOfBitmap(bitmap=" + bitmap + "[" + bitmap.getWidth() + "," + bitmap.getHeight() + "],name=" + name + ",rect=[" + rect.left + "," + rect.top
  // + "," + rect.right + "," + rect.bottom + "])");
  // return ret;
  // }
  // } catch (OutOfMemoryError outOfMemoryError) {
  // throw outOfMemoryError;
  // } catch (Throwable t) {
  // if (logger.isEnabledFor(Priority.ERROR))
  // logger.error("_hitSDCacheBitmap Error", t);
  // }
  // if (logger.isTraceEnabled())
  // logger.trace("null<-getACutOfBitmap(bitmap=" + bitmap + "[" + bitmap.getWidth() + "," + bitmap.getHeight() + "],name=" + name + ",rect=[" + rect.left + "," + rect.top + ","
  // + rect.right + "," + rect.bottom + "])");
  // return null;
  // }
  @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
  private Bitmap decodeBitmapRegion(InputStream is, Rect rect) throws IOException {
    return BitmapRegionDecoder.newInstance(is, false).decodeRegion(rect, null);
  }

  private void reduceMemoryConsumption() {
    imageLoadFactor++;
  }

  private void boostMemoryConsumption() {
    imageLoadFactor--;
    if (imageLoadFactor < 0)
      imageLoadFactor = 0;
  }

  private int getImageLoadFactor() {
    return (int) Math.ceil(Math.pow(1.73, imageLoadFactor));
  }

  public Bitmap hitSDCacheBitmap(String name, int maxWidth, int maxHeight) {
    Bitmap bitmap = checkCache(name);
    if (bitmap != null) {
      return bitmap;
    }
    Point bitmapSize = fetchImageBitmapSize(name);
    if (bitmapSize != null) {
      if (maxWidth <= 0) {
        maxWidth = bitmapSize.x;
      }
      if (maxHeight <= 0) {
        maxHeight = bitmapSize.y;
      }
    }
    boostMemoryConsumption();
    for (int i = 0; i < 10; i++) {
      try {
        bitmap = _hitSDCacheBitmap(name, maxWidth / getImageLoadFactor(), maxHeight / getImageLoadFactor());
        putInCache(name, bitmap);
        return bitmap;
      } catch (OutOfMemoryError outOfMemoryError) {
        if (logger.isTraceEnabled())
          logger.trace("OutOfMemoryError while creating BitmapImage:" + name + "[" + maxWidth + "," + maxHeight + "]");
        reduceMemoryConsumption();
      }
    }
    return null;
  }

  private Bitmap _hitSDCacheBitmap(String name, final int maxWidth, final int maxHeight) {
    try {
      File img = hitSDCache(name);
      if (img == null) {
        if (logger.isTraceEnabled())
          logger.trace("null<-hitSDCacheBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
        return null;
      }
      Point bitmapSize;
      if (maxWidth > 0 && maxHeight > 0 && (bitmapSize = fetchImageBitmapSize(name)) != null) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = (int) (0.1 + Math.pow(2.0, Math.ceil(Math.log(Math.max(1, Math.max(bitmapSize.x / maxWidth, bitmapSize.y / maxHeight))))));
        opts.inPreferredConfig = Config.ARGB_8888;
        Bitmap ret = BitmapFactory.decodeFile(img.getPath(), opts);
        if (logger.isTraceEnabled())
          logger.trace(ret + "<-hitSDCacheBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
        return ret;
      }
      Bitmap ret = BitmapFactory.decodeFile(img.getPath());
      if (logger.isTraceEnabled())
        logger.trace(ret + "<-hitSDCacheBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
      return ret;
    } catch (OutOfMemoryError outOfMemoryError) {
      throw outOfMemoryError;
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("_hitSDCacheBitmap Error", t);
    }
    if (logger.isTraceEnabled())
      logger.trace("null<-hitSDCacheBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
    return null;
  }

  private Point fetchImageBitmapSize(String name) {
    try {
      File img = fetchFile(name);
      if (img == null) {
        if (logger.isTraceEnabled())
          logger.trace("null<-getImageBitmapSize(name=" + name + ")");
        return null;
      }
      return getImageBitmapSize(img.getPath());
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("getImageBitmapSize Error", t);
    }
    if (logger.isTraceEnabled())
      logger.trace("null<-getImageBitmapSize(name=" + name + ")");
    return null;
  }

  private Point getImageBitmapSize(String path) {
    try {
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(path, opts);
      Point ret = new Point(opts.outWidth, opts.outHeight);
      if (logger.isTraceEnabled())
        logger.trace(ret + "<-getImageBitmapSize(path=" + path + ")");
      return ret;
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("getImageBitmapSize Error", t);
    }
    if (logger.isTraceEnabled())
      logger.trace("null<-getImageBitmapSize(path=" + path + ")");
    return null;
  }

  public Bitmap getImageBitmap(String name, int maxWidth, int maxHeight) {
    Bitmap bitmap = checkCache(name);
    if (bitmap != null) {
      return bitmap;
    }
    Point bitmapSize = fetchImageBitmapSize(name);
    if (bitmapSize != null) {
      if (maxWidth <= 0) {
        maxWidth = bitmapSize.x;
      }
      if (maxHeight <= 0) {
        maxHeight = bitmapSize.y;
      }
    }
    boostMemoryConsumption();
    for (int i = 0; i < 10; i++) {
      try {
        bitmap = _getImageBitmap(name, maxWidth / getImageLoadFactor(), maxHeight / getImageLoadFactor());
        putInCache(name, bitmap);
        return bitmap;
      } catch (OutOfMemoryError outOfMemoryError) {
        if (logger.isInfoEnabled())
          logger.info("OutOfMemoryError while creating BitmapImage:" + name + "[" + maxWidth + "," + maxHeight + "]");
        reduceMemoryConsumption();
      }
    }
    return null;
  }

  private Bitmap _getImageBitmap(String name, int maxWidth, int maxHeight) {
    Point bitmapSize;
    if (maxWidth > 0 && maxHeight > 0 && (bitmapSize = fetchImageBitmapSize(name)) != null) {
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inSampleSize = (int) (0.1 + Math.pow(2.0, Math.ceil(Math.log(Math.max(1, Math.max(bitmapSize.x / maxWidth, bitmapSize.y / maxHeight))))));
      try {
        File img = fetchFile(name);
        Bitmap bmp;
        if (img == null || (bmp = BitmapFactory.decodeFile(img.getPath(), opts)) == null) {
          img = fetchFile(name, true);
          if (img == null) {
            if (logger.isTraceEnabled())
              logger.trace("null<-getImageBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
            return null;
          }
          opts.inPreferredConfig = Config.ARGB_8888;
          bmp = BitmapFactory.decodeFile(img.getPath(), opts);
        }
        if (logger.isTraceEnabled())
          logger.trace(bmp + "[" + bmp.getWidth() + "," + bmp.getHeight() + "]" + "<-getImageBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
        return bmp;
      } catch (OutOfMemoryError outOfMemoryError) {
        throw outOfMemoryError;
      } catch (Throwable t) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("_getImageBitmap Error", t);
      }
    } else {
      try {
        File img = fetchFile(name);
        Bitmap bmp;
        if (img == null || (bmp = BitmapFactory.decodeFile(img.getPath())) == null) {
          img = fetchFile(name, true);
          if (img == null) {
            if (logger.isTraceEnabled())
              logger.trace("null<-getImageBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
            return null;
          }
          bmp = BitmapFactory.decodeFile(img.getPath());
        }
        if (logger.isTraceEnabled())
          logger.trace(bmp + "<-getImageBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
        return bmp;
      } catch (OutOfMemoryError outOfMemoryError) {
        throw outOfMemoryError;
      } catch (Throwable t) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("_getImageBitmap Error", t);
      }
    }
    if (logger.isTraceEnabled())
      logger.trace("null<-getImageBitmap(name=" + name + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
    return null;
  }

  public Bitmap hitPathBitmap(String path, final int maxWidth, final int maxHeight) {
    Bitmap bitmap = checkCache(path);
    if (bitmap != null) {
      return bitmap;
    }
    for (int i = 0; i < 10; i++) {
      try {
        bitmap = _hitPathBitmap(path, maxWidth / getImageLoadFactor(), maxHeight / getImageLoadFactor());
        putInCache(path, bitmap);
        return bitmap;
      } catch (OutOfMemoryError outOfMemoryError) {
        if (logger.isInfoEnabled())
          logger.info("OutOfMemoryError while creating BitmapImage:" + path + "[" + maxWidth + "," + maxHeight + "]");
        reduceMemoryConsumption();
      }
    }
    return null;
  }

  private Bitmap _hitPathBitmap(String path, int maxWidth, int maxHeight) {
    try {
      Point bitmapSize;
      if (maxWidth > 0 && maxHeight > 0 && (bitmapSize = getImageBitmapSize(path)) != null) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = (int) (0.1 + Math.pow(2.0, Math.ceil(Math.log(Math.max(1, Math.max(bitmapSize.x / maxWidth, bitmapSize.y / maxHeight))))));
        Bitmap ret = BitmapFactory.decodeFile(path, opts);
        if (logger.isTraceEnabled())
          logger.trace(ret + "<-hitSDCacheBitmap(path=" + path + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
        return ret;
      }
      Bitmap ret = BitmapFactory.decodeFile(path);
      if (logger.isTraceEnabled())
        logger.trace(ret + "<-hitSDCacheBitmap(path=" + path + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
      return ret;
    } catch (OutOfMemoryError outOfMemoryError) {
      throw outOfMemoryError;
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("_hitSDCacheBitmap Error", t);
    }
    if (logger.isTraceEnabled())
      logger.trace("null<-hitSDCacheBitmap(path=" + path + ",maxWidth=" + maxWidth + ",maxHeight=" + maxHeight + ")");
    return null;
  }

  public void asyncImageViewLoadBitmap(final Handler handler, final ImageView imgView, final String remoteImageName, final int width, final int height) {
    imgView.setContentDescription(remoteImageName);
    AndroidUtils.doInBackground(new Runnable() {
      @Override
      public void run() {
        try {
          Bitmap bitmap = hitSDCacheBitmap(remoteImageName, width, height);
          if (bitmap == null) {
            Thread.yield();
            bitmap = getImageBitmap(remoteImageName, width, height);
          }
          if (bitmap != null) {
            final Bitmap image_bitmap = bitmap;
            Thread.yield();
            handler.post(new Runnable() {
              @Override
              public void run() {
                if (imgView.getContentDescription().equals(remoteImageName))
                  imgView.setImageBitmap(image_bitmap);
              }
            });
          }
        } catch (Throwable t) {
          if (logger.isEnabledFor(Priority.ERROR))
            logger.error("asyncImageViewLoadBitmap()" + imgView.getResources().getResourceEntryName(imgView.getId()) + ":"
                + imgView.getResources().getResourceName(imgView.getId()) + " " + remoteImageName, t);
        }
      }
    });
  }
}
