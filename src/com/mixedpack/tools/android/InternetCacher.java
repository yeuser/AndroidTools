package com.mixedpack.tools.android;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import android.os.Build;

import com.google.gson.Gson;
import com.mixedpack.tools.ActivityBlocker;
import com.mixedpack.tools.android.AndroidUtils;

@SuppressWarnings("deprecation")
public class InternetCacher {
  public static final String    DATA_FILE_NAME     = "modifs.json";
  private static final boolean  SERVER_FETCH_DEBUG = false;
  private static InternetCacher instance           = null;
  private static Semaphore      instantiation_lock = new Semaphore(1);
  private static Semaphore      data_file_lock     = new Semaphore(1);
  private LocalData             localData;
  private ActivityBlocker       downloadBlocker    = new ActivityBlocker();
  private ExecutorService       threadPool         = Executors.newFixedThreadPool(2);
  private final static Logger   logger             = Logger.getLogger(InternetCacher.class);

  public class LocalData {
    private HashMap<String, String> server_file_modifications = new HashMap<String, String>();

    void setServerFileModifications(String key, String value) {
      if (logger.isTraceEnabled())
        logger.trace("setServerFileModifications(key=" + key + ",value=" + value + ")");
      if (server_file_modifications == null)
        server_file_modifications = new HashMap<String, String>();
      server_file_modifications.put(key, value);
    }

    String getServerFileModifications(String key) {
      if (server_file_modifications == null)
        server_file_modifications = new HashMap<String, String>();
      if (logger.isTraceEnabled())
        logger.trace(server_file_modifications.get(key) + "<-getServerFileModifications(key=" + key + ")");
      return server_file_modifications.get(key);
    }

    public void mergeBy(LocalData ld) {
      if (ld == null)
        return;
      if (server_file_modifications == null) {
        server_file_modifications = ld.server_file_modifications;
        if (server_file_modifications == null) {
          server_file_modifications = new HashMap<String, String>();
        }
      } else {
        server_file_modifications.putAll(ld.server_file_modifications);
      }
    }
  }

  private InternetCacher() {
    try {
      /**
       * Sometimes we get: "Fatal Exception: java.lang.NoClassDefFoundError android.os.AsyncTask"
       * 
       * yet another Google Play Services bug...
       * 
       * => https://groups.google.com/forum/#!topic/google-admob-ads-sdk/_x12qmjWI7M
       * 
       * Confirmed by Google staff:
       * 
       * => https://groups.google.com/d/msg/google-admob-ads-sdk/_x12qmjWI7M/9ZQs-v0ZZTMJ
       * 
       */
      Class.forName("android.os.AsyncTask");
    } catch (Throwable ignore) {
      if (logger.isEnabledFor(Priority.FATAL))
        logger.fatal("Couldn't load android.os.AsyncTask", ignore);
    }
    loadData();
    saveData();
  }

  public static InternetCacher getInstance() {
    if (instance == null) {
      try {
        instantiation_lock.acquire();
      } catch (InterruptedException t) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("instantiation_lock Error", t);
      }
      if (instance == null)
        instance = new InternetCacher();
      instantiation_lock.release();
    }
    return instance;
  }

  public File hitCache(String name, File dir) throws InterruptedException, ExecutionException {
    final File file = new File(dir, name);
    if (file.exists()) {
      if (logger.isTraceEnabled())
        logger.trace("[file.exists()]:true<-hitCache(name=" + name + ",dir=" + dir + ")");
      return file;
    }
    if (logger.isTraceEnabled())
      logger.trace("[file.exists()]:false<-hitCache(name=" + name + ",dir=" + dir + ")");
    return null;
  }

  public File fetchFile(String url, String host, final String name, File dir, boolean force) throws InterruptedException, ExecutionException {
    final File file = new File(dir, name);
    file.getParentFile().mkdirs();
    downloadBlocker.acquire(name);
    if (!force && file.exists()) {
      if (logger.isTraceEnabled())
        logger.trace("[!force && file.exists()]:true<-fetchFile(url=" + url + ",name=" + name + ",dir=" + dir + ",force=" + force + ")");
      downloadBlocker.release(name);
      return file;
    }
    if (logger.isTraceEnabled())
      logger.trace("[!force && file.exists()]:false<-fetchFile(url=" + url + ",name=" + name + ",dir=" + dir + ",force=" + force + ")");
    File f = new File(dir, name);
    try {
      f = fetch_remote(url, host, f);
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("doInBackground Error", t);
    }
    downloadBlocker.release(name);
    return f;
  }

  public File fetchFile(String url, String host, String name, File dir) throws InterruptedException, ExecutionException {
    return fetchFile(url, host, name, dir, false);
  }

  public void shutdownAllNow() {
    threadPool.shutdownNow();
  }

  private File fetch_remote(final String url, final String host, final File file) throws InterruptedException, ExecutionException {
    file.getParentFile().mkdirs();
    File f = threadPool.submit(new Callable<File>() {
      @Override
      public File call() throws Exception {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        RandomAccessFile out = new RandomAccessFile(file, "rws");
        String lastModified = localData.getServerFileModifications(url);
        for (int i = 0; i < 5; i++) {
          long downloaded = out.length();
          try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            if (host != null)
              con.setRequestProperty("Host", host);
            con.setConnectTimeout(30000);
            if (Build.VERSION.SDK_INT > 13) {
              con.setRequestProperty("Connection", "close");
            }
            if (lastModified != null) {
              if (i == 0)
                con.setIfModifiedSince(Date.parse(lastModified));
              con.setRequestProperty("Range", "bytes=" + downloaded + "-");
              con.setRequestProperty("If-Range", lastModified);
            } else {
              downloaded = 0l;
            }
            con.setInstanceFollowRedirects(true);
            con.connect();
            int fileLength = con.getContentLength();
            int responseCode = con.getResponseCode();
            switch (responseCode) {
            case 200:
            case 206:
              break;
            case 304:
              if (fileLength == file.length()) {
                try {
                  out.close();
                } catch (Throwable t) {
                  if (logger.isEnabledFor(Priority.WARN))
                    logger.warn("fetch_remote IO close Error", t);
                }
                return file;
              }
              con.disconnect();
              con = (HttpURLConnection) new URL(url).openConnection();
              if (host != null)
                con.setRequestProperty("Host", host);
              con.setConnectTimeout(30000);
              if (Build.VERSION.SDK_INT > 13) {
                con.setRequestProperty("Connection", "close");
              }
              con.setRequestProperty("Range", "bytes=" + downloaded + "-");
              con.setInstanceFollowRedirects(true);
              con.connect();
              fileLength = con.getContentLength();
              responseCode = con.getResponseCode();
              Map<String, List<String>> _headerFields = con.getHeaderFields();
              if (logger.isEnabledFor(Priority.FATAL))
                logger.fatal("Trying after 304, Server returned a ResponseCode:" + responseCode + " with ResponseMessage:\"" + con.getResponseMessage() + "\" & ResponseHeaders:"
                    + new Gson().toJson(_headerFields));
              break;
            default:
              Map<String, List<String>> headerFields = con.getHeaderFields();
              if (logger.isEnabledFor(Priority.FATAL))
                logger.fatal("Server returned a ResponseCode:" + responseCode + " with ResponseMessage:\"" + con.getResponseMessage() + "\" & ResponseHeaders:"
                    + new Gson().toJson(headerFields));
              return null;
            }
            if (lastModified == null || !lastModified.equalsIgnoreCase(con.getHeaderField("Last-Modified"))) {
              lastModified = con.getHeaderField("Last-Modified");
              localData.setServerFileModifications(url, lastModified);
              saveData();
            }
            InputStream in = con.getInputStream();
            out.seek(downloaded);
            byte[] b = new byte[63 * 1024];
            int avail = in.available();
            avail = Math.max(100, Math.min(b.length, avail));
            int l = in.read(b, 0, avail);
            while (l >= 0) {
              out.write(b, 0, l);
              avail = in.available();
              avail = Math.max(100, Math.min(b.length, avail));
              l = in.read(b, 0, avail);
            }
            try {
              out.close();
            } catch (Throwable t) {
              if (logger.isEnabledFor(Priority.WARN))
                logger.warn("fetch_remote IO close Error", t);
            }
            try {
              in.close();
            } catch (Throwable t) {
              if (logger.isEnabledFor(Priority.WARN))
                logger.warn("fetch_remote IO close Error", t);
            }
            return file;
          } catch (Throwable t) {
            if (logger.isEnabledFor(Priority.WARN))
              logger.warn("fetch_remote Error", t);
          }
        }
        return null;
      }
    }).get();
    if (logger.isTraceEnabled())
      logger.trace(f + "<-fetch_remote(url=" + url + ",file=" + file.getPath() + ")");
    return f;
  }

  private void loadData() {
    if (!SERVER_FETCH_DEBUG) {
      try {
        data_file_lock.acquire();
        try {
          localData = AndroidUtils.getInstance().getObjectFromFile(DATA_FILE_NAME, LocalData.class);
        } catch (Throwable t) {
          if (logger.isEnabledFor(Priority.ERROR))
            logger.error("loadData Error", t);
        }
        data_file_lock.release();
      } catch (InterruptedException e1) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("data_file_lock Error", e1);
      }
    }
    if (localData == null) {
      localData = new LocalData();
    }
  }

  private void saveData() {
    try {
      data_file_lock.acquire();
      try {
        AndroidUtils.getInstance().saveObjectToFile(DATA_FILE_NAME, localData);
      } catch (Throwable t) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("saveData Error", t);
      }
      data_file_lock.release();
    } catch (InterruptedException e1) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("data_file_lock Error", e1);
    }
  }
}
