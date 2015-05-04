package com.mixedpack.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mixedpack.tools.android.AndroidUtils;
import com.mixedpack.tools.android.Compression;

public abstract class RemoteWorker {
  private final String        remoteHostName;
  private String              remoteHostIP;
  private final String        remotePath;
  private String              remoteAddressPrefix;
  private String              httpMediaURL;
  private long                lastIPcheck = 0;
  private boolean             debug       = false;
  private final static Logger logger      = Logger.getLogger(RemoteWorker.class);
  public final static Gson    GSON        = new GsonBuilder().registerTypeAdapter(Integer.class, new TypeAdapter<Integer>() {
                                            @Override
                                            public Integer read(JsonReader reader) throws IOException {
                                              if (reader.peek() == JsonToken.NULL) {
                                                reader.nextNull();
                                                return null;
                                              }
                                              if (reader.peek() == JsonToken.NUMBER) {
                                                int d = reader.nextInt();
                                                return d;
                                              }
                                              String stringValue = reader.nextString();
                                              try {
                                                Integer value = Integer.valueOf(stringValue);
                                                return value;
                                              } catch (NumberFormatException e) {
                                                return null;
                                              }
                                            }

                                            @Override
                                            public void write(JsonWriter writer, Integer value) throws IOException {
                                              if (value == null) {
                                                writer.nullValue();
                                                return;
                                              }
                                              writer.value(value);
                                            }
                                          }).create();
  private static final long   FIVE_MINUTE = 5 * 60 * 1000;

  public RemoteWorker(String remoteHostName, String remoteHostIP, String remotePath) {
    assert remoteHostName != null || remoteHostIP != null;
    this.remoteHostName = remoteHostName;
    this.remoteHostIP = remoteHostIP;
    this.remotePath = remotePath;
    this.remoteAddressPrefix = "http://" + (this.remoteHostIP != null ? this.remoteHostIP : this.remoteHostName) + "/" + this.remotePath + "/";
    this.httpMediaURL = remoteAddressPrefix + "edit/media/";
  }

  private void resolveIP() {
    if (remoteHostName == null)
      return;
    long NOW = System.currentTimeMillis();
    if (remoteHostIP != null && lastIPcheck + FIVE_MINUTE < NOW)
      return;
    lastIPcheck = NOW;
    try {
      remoteHostIP = InetAddress.getByName(remoteHostName).getHostAddress();
      remoteAddressPrefix = "http://" + remoteHostIP + "/" + remotePath + "/";
      httpMediaURL = remoteAddressPrefix + "edit/media/";
    } catch (Throwable t) {
      Logger.getLogger(RemoteWorker.class).trace("Error @ RemoteWorker.resolveIP()", t);
    }
  }

  public String getRemoteAddressPrefix() {
    resolveIP();
    return remoteAddressPrefix;
  }

  public String getRemoteMediaAddress() {
    resolveIP();
    return httpMediaURL;
  }

  protected String callRemoteFunction(String function, Map<String, String> getParameters) throws IOException {
    return callRemoteFunction(function, getParameters, null);
  }

  @SuppressWarnings("deprecation")
  protected String callRemoteFunction(String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException {
    String url_address = "";
    String postParamStr = "";
    String response_content = "";
    try {
      if (!AndroidUtils.getInstance().isNetworkAvailable())
        return null;
      if (remoteAddressPrefix == null)
        return null;
      if (getParameters == null)
        getParameters = new HashMap<String, String>();
      if (postParameters == null)
        postParameters = new HashMap<String, String>();
      url_address = remoteAddressPrefix + getRemoteCallAddressPostfix(function, getParameters, postParameters);
      if (url_address.indexOf('?') == url_address.length() - 1) {
        url_address = url_address.substring(0, url_address.length() - 1);
      }
      boolean first = url_address.indexOf('?') < 0;
      for (Entry<String, String> input : getParameters.entrySet()) {
        if (input.getValue() != null)
          url_address += (first ? "?" : "&") + URLEncoder.encode(input.getKey(), "utf-8") + "="
              + (input.getValue() == null ? input.getValue() : URLEncoder.encode(input.getValue(), "UTF-8"));
        else
          url_address += (first ? "?" : "&") + URLEncoder.encode(input.getKey(), "utf-8") + "=null";
        first = false;
      }
      if (logger.isTraceEnabled())
        logger.trace(url_address);
      URL url = new URL(url_address);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      if (remoteHostName != null)
        con.setRequestProperty("Host", remoteHostName);
      con.setConnectTimeout(30000);
      // con.connect();
      first = true;
      if (!postParameters.isEmpty()) {
        for (Entry<String, String> parameter : postParameters.entrySet()) {
          if (parameter.getValue() != null)
            postParamStr += (first ? "" : "&") + URLEncoder.encode(parameter.getKey(), "utf-8") + "="
                + (parameter.getValue() == null ? parameter.getValue() : URLEncoder.encode(parameter.getValue(), "UTF-8"));
          else
            postParamStr += (first ? "" : "&") + URLEncoder.encode(parameter.getKey(), "utf-8") + "=null";
          first = false;
        }
      }
      if (postParamStr.length() > 0) {
        byte[] postData = postParamStr.getBytes("UTF-8");
        int postDataLength = postData.length;
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("charset", "utf-8");
        con.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        con.setUseCaches(false);
        con.getOutputStream().write(postData);
      } else {
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "text/plain");
        con.setRequestProperty("charset", "utf-8");
      }
      con.connect();
      InputStream in = con.getInputStream();
      InputStreamReader reader = null;
      try {
        reader = new InputStreamReader(in, Charset.forName("UTF8"));
        StringBuilder content = new StringBuilder();
        char[] cbuf = new char[8 * 1024];
        int l = reader.read(cbuf);
        while (l >= 0) {
          content.append(cbuf, 0, l);
          l = reader.read(cbuf);
        }
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            if (logger.isTraceEnabled())
              logger.trace("callRemoteFunction IO close Error", e);
          }
        }
        con.disconnect();
        response_content = content.toString().trim();
        if (logger.isTraceEnabled())
          logger.trace("callRemoteFunction URL_ADDRESS=[" + url_address + "] POST_PARAMS=[" + postParamStr + "] HTTP_RESPONSE=[" + response_content + "]");
        writeDebuggingData(getParameters, postParameters, url_address, postParamStr, response_content);
        return response_content;
      } catch (IOException e) {
        if (logger.isTraceEnabled())
          logger.trace("callRemoteFunction IO close Error", e);
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e1) {
            if (logger.isTraceEnabled())
              logger.trace("callRemoteFunction IO close Error", e1);
          }
        }
        con.disconnect();
        writeDebuggingData(getParameters, postParameters, url_address, postParamStr, response_content);
        return null;
      }
    } catch (IOException e) {
      if (logger.isEnabledFor(Priority.ERROR))
        logger.error("", e);
      writeDebuggingData(getParameters, postParameters, url_address, postParamStr, response_content);
      throw e;
    }
  }

  @SuppressWarnings("deprecation")
  private void writeDebuggingData(Map<String, String> getParameters, Map<String, String> postParameters, String url_address, String postParamStr, String response_content) {
    if (isDebug()) {
      try {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("URL_ADDRESS", url_address);
        map.put("GET_PARAMETERS", getParameters);
        map.put("POST_PARAMETERS", postParameters);
        map.put("POST_PARAMS", postParamStr);
        map.put("HTTP_RESPONSE", response_content);
        AndroidUtils.getInstance().saveObjectToExtFile(".debug.info", map);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(AndroidUtils.getInstance().getExternalDataDir(), ".debug.info")), Charset.defaultCharset());
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[512];
        int l = reader.read(buffer);
        while (l >= 0) {
          sb.append(buffer, 0, l);
          l = reader.read(buffer);
        }
        reader.close();
        FileOutputStream outputStream;
        // byte[] compressZIP = Compression.compressZIP(sb.toString(), ".debug.info");
        // outputStream = new FileOutputStream(new File(AndroidUtils.getInstance().getExternalDataDir(), ".debug.info.zip"));
        // outputStream.write(compressZIP);
        // outputStream.flush();
        // outputStream.close();
        byte[] compressGZIP = Compression.compressGZIP(sb.toString());
        outputStream = new FileOutputStream(new File(AndroidUtils.getInstance().getExternalDataDir(), ".debug.info.gz"));
        outputStream.write(compressGZIP);
        outputStream.flush();
        outputStream.close();
        // byte[] compressLZMA = Compression.compressLZMA(sb.toString());
        // outputStream = new FileOutputStream(new File(AndroidUtils.getInstance().getExternalDataDir(), ".debug.info.7z"));
        // outputStream.write(compressLZMA);
        // outputStream.flush();
        // outputStream.close();
        new File(AndroidUtils.getInstance().getExternalDataDir(), ".debug.info").delete();
      } catch (Throwable t) {
        if (logger.isEnabledFor(Priority.ERROR))
          logger.error("", t);
      }
    }
  }

  protected abstract String getRemoteCallAddressPostfix(String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException;

  protected <T> T callRemoteFunction(Class<T> tclass, String function, Map<String, String> getParameters) throws IOException {
    String remoteResponse = callRemoteFunction(function, getParameters);
    if (remoteResponse == null)
      return null;
    return GSON.fromJson(remoteResponse, tclass);
  }

  protected <T> T callRemoteFunction(TypeToken<T> ttypetoken, String function, Map<String, String> getParameters) throws IOException {
    String remoteResponse = callRemoteFunction(function, getParameters);
    if (remoteResponse == null)
      return null;
    return GSON.fromJson(remoteResponse, ttypetoken.getType());
  }

  protected <T> T callRemoteFunction(Class<T> tclass, String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException {
    String remoteResponse = callRemoteFunction(function, getParameters, postParameters);
    if (remoteResponse == null)
      return null;
    return GSON.fromJson(remoteResponse, tclass);
  }

  protected <T> T callRemoteFunction(TypeToken<T> ttypetoken, String function, Map<String, String> getParameters, Map<String, String> postParameters) throws IOException {
    String remoteResponse = callRemoteFunction(function, getParameters, postParameters);
    if (remoteResponse == null)
      return null;
    return GSON.fromJson(remoteResponse, ttypetoken.getType());
  }

  public <T> T[] getRemoteArray(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass) throws Exception {
    String response = callRemoteFunction(remoteFunction, attrs);
    if (logger.isInfoEnabled())
      logger.info("response: " + response);
    return GSON.fromJson(response, returnClass);
  }

  public <T> T[] getRemoteArray(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, int from) throws Exception {
    if (attrs == null) {
      attrs = new HashMap<String, String>();
    }
    attrs.put("from", String.valueOf(from));
    String response = callRemoteFunction(remoteFunction, attrs);
    if (logger.isInfoEnabled())
      logger.info("response: " + response);
    return GSON.fromJson(response, returnClass);
  }

  public <T> T[] getRemoteArray(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, int from, int len) throws Exception {
    if (attrs == null) {
      attrs = new HashMap<String, String>();
    }
    attrs.put("from", String.valueOf(from));
    attrs.put("len", String.valueOf(len));
    String response = callRemoteFunction(remoteFunction, attrs);
    if (logger.isInfoEnabled())
      logger.info("response: " + response);
    return GSON.fromJson(response, returnClass);
  }

  public <T> T[] getRemoteArrayInRange(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, int from, int to) throws Exception {
    if (from < 0)
      from = 0;
    if (to <= 0 || from > to)
      return null;
    if (attrs == null) {
      attrs = new HashMap<String, String>();
    }
    attrs.put("from", String.valueOf(from));
    attrs.put("to", String.valueOf(to));
    String response = callRemoteFunction(remoteFunction, attrs);
    if (logger.isInfoEnabled())
      logger.info("response: " + response);
    return GSON.fromJson(response, returnClass);
  }

  public <T> T[] syncAscendingList(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, T[] list, int list_start, int list_end) throws Exception {
    return syncAscendingList(remoteFunction, attrs, returnClass, list, list_start, list_end, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  @SuppressWarnings("unchecked")
  public <T> T[] syncAscendingList(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, T[] list, int list_start, int list_end, int minValue, int maxValue)
      throws Exception {
    if (list == null || list.length == 0 || list_start > list_end) {
      return getRemoteArrayInRange(remoteFunction, attrs, returnClass, minValue, maxValue);
    }
    T[] remoteArray_pre = getRemoteArrayInRange(remoteFunction, attrs, returnClass, minValue, list_start - 1);
    T[] remoteArray_post = getRemoteArrayInRange(remoteFunction, attrs, returnClass, list_end + 1, maxValue);
    int l = list == null ? 0 : list.length;
    if (remoteArray_pre != null)
      l += remoteArray_pre.length;
    if (remoteArray_post != null)
      l += remoteArray_post.length;
    T[] returnArray = (T[]) Array.newInstance(remoteArray_post.getClass(), l);
    int off = 0;
    if (remoteArray_pre != null) {
      System.arraycopy(remoteArray_pre, 0, returnArray, off, remoteArray_pre.length);
      off += remoteArray_pre.length;
    }
    if (list != null) {
      System.arraycopy(list, 0, returnArray, off, list.length);
      off += list.length;
    }
    if (remoteArray_post != null) {
      System.arraycopy(remoteArray_post, 0, returnArray, off, remoteArray_post.length);
    }
    return returnArray;
  }

  public <T> T[] syncDescendingList(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, T[] list, int list_start, int list_end) throws Exception {
    return syncDescendingList(remoteFunction, attrs, returnClass, list, list_start, list_end, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  @SuppressWarnings("unchecked")
  public <T> T[] syncDescendingList(String remoteFunction, Map<String, String> attrs, Class<T[]> returnClass, T[] list, int list_start, int list_end, int minValue, int maxValue)
      throws Exception {
    if (list == null || list.length == 0 || list_start > list_end) {
      return getRemoteArrayInRange(remoteFunction, attrs, returnClass, minValue, maxValue);
    }
    T[] remoteArray_pre = getRemoteArrayInRange(remoteFunction, attrs, returnClass, minValue, list_start - 1);
    T[] remoteArray_post = getRemoteArrayInRange(remoteFunction, attrs, returnClass, list_end + 1, maxValue);
    if ((remoteArray_pre == null || remoteArray_pre.length == 0) && (remoteArray_post == null || remoteArray_post.length == 0)) {
      return list;
    }
    int l = list == null ? 0 : list.length;
    Class<T> _T_Class = null;
    if (list != null) {
      if (list.length > 0)
        _T_Class = (Class<T>) list[0].getClass();
    }
    if (remoteArray_pre != null) {
      l += remoteArray_pre.length;
      if (remoteArray_pre.length > 0)
        _T_Class = (Class<T>) remoteArray_pre[0].getClass();
    }
    if (remoteArray_post != null) {
      l += remoteArray_post.length;
      if (remoteArray_post.length > 0)
        _T_Class = (Class<T>) remoteArray_post[0].getClass();
    }
    T[] returnArray = (T[]) Array.newInstance(_T_Class, l);
    int off = 0;
    if (remoteArray_post != null) {
      System.arraycopy(remoteArray_post, 0, returnArray, off, remoteArray_post.length);
      off += remoteArray_post.length;
    }
    if (list != null) {
      System.arraycopy(list, 0, returnArray, off, list.length);
      off += list.length;
    }
    if (remoteArray_pre != null) {
      System.arraycopy(remoteArray_pre, 0, returnArray, off, remoteArray_pre.length);
    }
    return returnArray;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }
}