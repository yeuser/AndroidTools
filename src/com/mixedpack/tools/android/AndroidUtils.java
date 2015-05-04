package com.mixedpack.tools.android;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.mindpipe.android.logging.log4j.LogConfigurator;

@SuppressWarnings("deprecation")
public class AndroidUtils {
  public static class TelephonyData {
    public String telephonyPhoneNumber;
    public String telephonySimSerialNumber;
    public String telephonySimOperator;
    public String telephonyNetworkCountryIso;
    public String telephonyNetworkOperatorName;
    public String telephonyNetworkOperator;
    public String telephonyDeviceSoftwareVersion;
    public String telephonyDeviceId;
    public String telephonyGroupIdLevel1;
    public String telephonySubscriberId;

    @Override
    public TelephonyData clone() {
      TelephonyData td = new TelephonyData();
      td.telephonyPhoneNumber = telephonyPhoneNumber;
      td.telephonySimSerialNumber = telephonySimSerialNumber;
      td.telephonySimOperator = telephonySimOperator;
      td.telephonyNetworkCountryIso = telephonyNetworkCountryIso;
      td.telephonyNetworkOperatorName = telephonyNetworkOperatorName;
      td.telephonyNetworkOperator = telephonyNetworkOperator;
      td.telephonyDeviceSoftwareVersion = telephonyDeviceSoftwareVersion;
      td.telephonyDeviceId = telephonyDeviceId;
      td.telephonyGroupIdLevel1 = telephonyGroupIdLevel1;
      td.telephonySubscriberId = telephonySubscriberId;
      return td;
    }
  }

  public static class ContactData {
    public String phoneDisplayName;
    public String phoneNumber;
    public String phoneNormalizedNumber;
    public String phoneLabel;
    public String phoneType;

    @Override
    public ContactData clone() {
      ContactData cd = new ContactData();
      cd.phoneDisplayName = phoneDisplayName;
      cd.phoneNumber = phoneNumber;
      cd.phoneNormalizedNumber = phoneNormalizedNumber;
      cd.phoneLabel = phoneLabel;
      cd.phoneType = phoneType;
      return cd;
    }
  }

  private String                  DEFAULT_FONT    = null;
  private static final String     PREF_DEVICE_ID  = "PREF_DEVICE_ID";
  private static final Gson       GSON            = new Gson();
  private Logger                  logger;
  private Context                 context;
  private File                    INT_DATA_DIR;
  private File                    EXT_DATA_DIR;
  private String                  ANDROID_ID      = "";
  private TelephonyData           telephonyData   = new TelephonyData();
  private Collection<ContactData> contactDataList = new ArrayList<ContactData>();

  private AndroidUtils() {
  }

  private static AndroidUtils instance           = null;
  private static Semaphore    instantiation_lock = new Semaphore(1);

  public static AndroidUtils getInstance() {
    if (instance == null) {
      try {
        instantiation_lock.acquire();
      } catch (InterruptedException e) {
        Log.e(AndroidUtils.class.getName(), "Instantiation Error", e);
      }
      if (instance == null)
        instance = new AndroidUtils();
      instantiation_lock.release();
    }
    return instance;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public void init(String extFolder, Context context, String defaultAssetFontPath) {
    // this.AppName = AppName;
    this.DEFAULT_FONT = defaultAssetFontPath;
    EXT_DATA_DIR = new File(Environment.getExternalStorageDirectory().getPath(), extFolder);
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    INT_DATA_DIR = context.getFilesDir();
    if (!EXT_DATA_DIR.exists()) {
      EXT_DATA_DIR.mkdirs();
    }
    File log_dir = new File(EXT_DATA_DIR, "log");
    if (!log_dir.exists()) {
      log_dir.mkdirs();
    }
    this.context = context;
    LogConfigurator logConfigurator = new LogConfigurator();
    Calendar calendar = Calendar.getInstance();
    logConfigurator.setFileName(log_dir + File.separator + //
        "log4j-" + calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH) + ".log");
    logConfigurator.setRootLevel(Level.ALL);
    logConfigurator.setLevel("com.mixedpack", Level.ALL);
    logConfigurator.setUseFileAppender(true);
    logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
    logConfigurator.setMaxFileSize(1024 * 1024 * 5);
    logConfigurator.setImmediateFlush(false);
    logConfigurator.setUseLogCatAppender(true);
    logConfigurator.configure();
    logger = Logger.getLogger(AndroidUtils.class);
    initPolicy();
    TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    telephonyData.telephonyDeviceId = tMgr.getDeviceId();
    telephonyData.telephonyDeviceSoftwareVersion = tMgr.getDeviceSoftwareVersion();
    telephonyData.telephonyNetworkCountryIso = tMgr.getNetworkCountryIso();
    telephonyData.telephonyNetworkOperator = tMgr.getNetworkOperator();
    telephonyData.telephonyNetworkOperatorName = tMgr.getNetworkOperatorName();
    telephonyData.telephonyPhoneNumber = tMgr.getLine1Number();
    telephonyData.telephonySimSerialNumber = tMgr.getSimSerialNumber();
    telephonyData.telephonySimOperator = tMgr.getSimOperator();
    telephonyData.telephonySubscriberId = tMgr.getSubscriberId();
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
      infoJELLY_BEAN_MR2(tMgr);
    Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
    while (phones.moveToNext()) {
      ContactData contactData = new ContactData();
      contactData.phoneDisplayName = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
      contactData.phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
      contactData.phoneNormalizedNumber = contactData.phoneNumber;
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
        getJBInfo(phones, contactData);
      } else {
        if (contactData.phoneNumber.trim().startsWith("00")) {
          contactData.phoneNormalizedNumber = "+" + contactData.phoneNumber.substring(2);
        } else if (contactData.phoneNumber.trim().startsWith("0")) {
          contactData.phoneNormalizedNumber = "+98" + contactData.phoneNumber.substring(1);
        }
      }
      contactData.phoneLabel = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
      contactData.phoneType = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
      contactDataList.add(contactData);
    }
    phones.close();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void getJBInfo(Cursor phones, ContactData contactData) {
    contactData.phoneNormalizedNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void infoJELLY_BEAN_MR2(TelephonyManager tMgr) {
    telephonyData.telephonyGroupIdLevel1 = tMgr.getGroupIdLevel1();
  }

  public void initID() {
    ANDROID_ID = generateID();
  }

  // generate a unique ID for each device
  // use available schemes if possible / generate a random signature instead
  private String generateID() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String deviceId = sharedPreferences.getString(PREF_DEVICE_ID, null);
    if (deviceId == null) {
      // use the ANDROID_ID constant, generated at the first device boot
      deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
      // in case known problems are occured
      if ("9774d56d682e549c".equals(deviceId) || deviceId == null) {
        // get a unique deviceID like IMEI for GSM or ESN for CDMA phones
        // don't forget:
        // <uses-permission android:name="android.permission.READ_PHONE_STATE" />
        deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        // if nothing else works, generate a random number
        if (deviceId == null) {
          Random tmpRand = new Random();
          deviceId = String.valueOf(tmpRand.nextLong());
        }
      }
      sharedPreferences.edit().putString(PREF_DEVICE_ID, deviceId).commit();
    }
    // any value is hashed to have consistent format
    return getHash(deviceId);
  }

  public String getAndroidID() {
    return ANDROID_ID;
  }

  // generates a SHA-1 hash for any string
  public String getHash(String stringToHash) {
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      if (logger.isEnabledFor(Priority.WARN))
        logger.warn("Error @ getHash(stringToHash=" + stringToHash + ")", e);
    }
    byte[] result = null;
    try {
      result = digest.digest(stringToHash.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      if (logger.isEnabledFor(Priority.WARN))
        logger.warn("Error @ getHash(stringToHash=" + stringToHash + ")", e);
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : result) {
      sb.append(String.format("%02X", b));
    }
    String messageDigest = sb.toString();
    return messageDigest;
  }

  private void initPolicy() {
    try {
      Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, Thread.currentThread().getContextClassLoader());
      Class<?> threadPolicyClass = Class.forName("android.os.StrictMode$ThreadPolicy", true, Thread.currentThread().getContextClassLoader());
      Class<?> threadPolicyBuilderClass = Class.forName("android.os.StrictMode$ThreadPolicy$Builder", true, Thread.currentThread().getContextClassLoader());
      Method setThreadPolicyMethod = strictModeClass.getMethod("setThreadPolicy", threadPolicyClass);
      Method detectAllMethod = threadPolicyBuilderClass.getMethod("detectAll");
      Method penaltyMethod = threadPolicyBuilderClass.getMethod("penaltyLog");
      Method buildMethod = threadPolicyBuilderClass.getMethod("build");
      Constructor<?> threadPolicyBuilderConstructor = threadPolicyBuilderClass.getConstructor();
      Object threadPolicyBuilderObject = threadPolicyBuilderConstructor.newInstance();
      Object obj = detectAllMethod.invoke(threadPolicyBuilderObject);
      obj = penaltyMethod.invoke(obj);
      Object threadPolicyObject = buildMethod.invoke(obj);
      setThreadPolicyMethod.invoke(strictModeClass, threadPolicyObject);
    } catch (Exception e) {
      if (logger.isEnabledFor(Priority.WARN))
        logger.warn("Error @ initPolicy()", e);
    }
    if (Build.VERSION.SDK_INT > 9) {
      makeThreadPolicy();
    }
  }

  @TargetApi(10)
  private void makeThreadPolicy() {
    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);
  }

  public void changeFont(View root) {
    try {
      changeFont(root, DEFAULT_FONT);
    } catch (Throwable t) {
      logger.error("changeFont", t);
    }
  }

  public void changeFont(View root, String assetFontPath) {
    try {
      Typeface tf = Typeface.createFromAsset(context.getAssets(), assetFontPath);
      changeFont(root, tf);
    } catch (Throwable t) {
      logger.error("changeFont", t);
    }
  }

  private void changeFont(View root, Typeface tf) {
    if (root instanceof TextView) {
      TextView view = (TextView) root;
      Typeface typeface = view.getTypeface();
      if (typeface != null)
        view.setTypeface(tf, typeface.getStyle());
      else
        view.setTypeface(tf);
    } else if (root instanceof Button) {
      Button view = (Button) root;
      Typeface typeface = view.getTypeface();
      if (typeface != null)
        view.setTypeface(tf, typeface.getStyle());
      else
        view.setTypeface(tf);
    } else if (root instanceof EditText) {
      EditText view = (EditText) root;
      Typeface typeface = view.getTypeface();
      if (typeface != null)
        view.setTypeface(tf, typeface.getStyle());
      else
        view.setTypeface(tf);
    } else if (root instanceof ViewGroup) {
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        changeFont(((ViewGroup) root).getChildAt(i), tf);
      }
    }
  }

  public boolean isNetworkAvailable() {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.isConnected()) {
      return true;
    }
    return false;
  }

  public <T> T getObjectFromFile(String fileName, TypeToken<T> typeToken) throws Throwable {
    if (logger.isTraceEnabled())
      logger.trace("getObjectFromFile(fileName=" + fileName + ", typeToken=" + typeToken + ")");
    T ret = null;
    BufferedReader br = null;
    FileInputStream openFileInput = context.openFileInput(fileName);
    br = new BufferedReader(new InputStreamReader(openFileInput, Charset.forName("utf8")));
    String data = br.readLine();
    ret = GSON.fromJson(data, typeToken.getType());
    try {
      br.close();
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.WARN))
        logger.warn("getObjectFromFile IO close Error", t);
    }
    return ret;
  }

  public <T> T getObjectFromFile(String fileName, Class<T> ct) throws Throwable {
    if (logger.isTraceEnabled())
      logger.trace("getObjectFromFile(fileName=" + fileName + ", ct=" + ct + ")");
    T ret = null;
    BufferedReader br = null;
    FileInputStream openFileInput = context.openFileInput(fileName);
    br = new BufferedReader(new InputStreamReader(openFileInput, Charset.forName("utf8")));
    String data = br.readLine();
    ret = (T) GSON.fromJson(data, ct);
    try {
      br.close();
    } catch (Throwable t) {
      if (logger.isEnabledFor(Priority.WARN))
        logger.warn("getObjectFromFile IO close Error", t);
    }
    return ret;
  }

  public void saveObjectToFile(String fileName, Object obj) throws Throwable {
    if (logger.isTraceEnabled())
      logger.trace("saveObjectToFile(fileName=" + fileName + ", obj=" + obj + ")");
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE), Charset.forName("utf8")));
    bw.write(GSON.toJson(obj));
    bw.newLine();
    bw.flush();
    bw.close();
  }

  public void saveObjectToExtFile(String fileName, Object obj) throws Throwable {
    if (logger.isTraceEnabled())
      logger.trace("saveObjectToFile(fileName=" + fileName + ", obj=" + obj + ")");
    File outputFile = new File(EXT_DATA_DIR, fileName);
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("utf8")));
    bw.write(GSON.toJson(obj));
    bw.newLine();
    bw.flush();
    bw.close();
  }

  public void appendObjectToExtFile(String fileName, Object obj, boolean afterNewLine, boolean beforeNewLine) throws Throwable {
    if (logger.isTraceEnabled())
      logger.trace("appendObjectToFile(fileName=" + fileName + ", obj=" + obj + ")");
    File outputFile = new File(EXT_DATA_DIR, fileName);
    RandomAccessFile raf = new RandomAccessFile(outputFile, "rws");
    raf.seek(raf.length());
    if (beforeNewLine)
      raf.write("\n\r".getBytes());
    raf.write(GSON.toJson(obj).getBytes());
    if (afterNewLine)
      raf.write("\n\r".getBytes());
    raf.close();
  }

  public void deleteFile(String fileName) {
    if (logger.isTraceEnabled())
      logger.trace("deleteFile(fileName=" + fileName + ")");
    File file = new File(INT_DATA_DIR, fileName);
    file.delete();
  }

  public File getExternalDataDir() {
    return EXT_DATA_DIR;
  }

  public File getInternalDataDir() {
    return INT_DATA_DIR;
  }

  public static void doInBackground(final Runnable task) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        task.run();
      }
    }).start();
  }

  public ArrayList<ContactData> getContactDataList() {
    ArrayList<ContactData> list = new ArrayList<ContactData>();
    for (ContactData contactData : contactDataList) {
      list.add(contactData.clone());
    }
    return list;
  }

  public TelephonyData getTelephonyData() {
    return telephonyData.clone();
  }
}