package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.DailyIntentService;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.Services.WifiCollectionService;
import com.eveningoutpost.dexdrip.Services.XDripViewer;

import java.io.IOException;
import java.util.Calendar;

/**
 * Created by stephenblack on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;

    private final static String TAG = CollectionServiceStarter.class.getSimpleName();


    public static boolean isWifiandBTWixel(Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("WifiBlueToothWixel") == 0) {
            return true;
        }
        return false;
    }
    
    private static boolean isWifiandBTWixel(String collection_method, Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        return collection_method.equals("WifiBlueToothWixel"); 
    }

    public static boolean isBTWixel(Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("BluetoothWixel") == 0) {
            return true;
        }
        return false;
    }

    private static boolean isBTWixel(String collection_method, Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        return collection_method.equals("BluetoothWixel"); 
    }

    public static boolean isDexbridgeWixel(Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("DexbridgeWixel") == 0) {
            return true;
        }
        return false;
    }
    
    private static boolean isDexbridgeWixel(String collection_method, Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        return collection_method.equals("DexbridgeWixel"); 
    }

    public static boolean isBTShare(Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("DexcomShare") == 0) {
            return true;
        }
        return false;
    }
    public static boolean isBTShare(String collection_method, Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return false;
        }
        return collection_method.equals("DexcomShare"); 
    }

    public static boolean isBTG5(String collection_method, Context context) {
       if(XDripViewer.isxDripViewerMode(context)) {
        return false;
       }
        return collection_method.equals("DexcomG5");
    }


    public static boolean isBTG5(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("DexcomG5") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isWifiWixel(Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return true;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("WifiWixel") == 0) {
            return true;
        }
        return false;
    }
    public static boolean isWifiWixel(String collection_method, Context context) {
        if(XDripViewer.isxDripViewerMode(context)) {
            return true;
        }
        return collection_method.equals("WifiWixel"); 
    }

    public static void newStart(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.start(context);
    }

    public void start(Context context, String collection_method) {
        mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if(isBTWixel(collection_method, context)||isDexbridgeWixel(collection_method, context)) {
            Log.d("DexDrip", "Starting bt wixel collector");
            stopWifWixelThread();
            stopBtShareService();
            stopG5ShareService();
            startBtWixelService();
        } else if(isWifiWixel(collection_method, context)){
            Log.d("DexDrip", "Starting wifi wixel collector");
            stopBtWixelService();
            stopBtShareService();
            stopG5ShareService();
            startWifWixelThread();
        } else if(isBTShare(collection_method, context)) {
            Log.d("DexDrip", "Starting bt share collector");
            stopBtWixelService();
            stopWifWixelThread();
            stopG5ShareService();
            startBtShareService();
        } else if(isBTG5(collection_method, context)) {
            Log.d("DexDrip", "Starting G5 share collector");
            stopBtWixelService();
            stopWifWixelThread();
            stopBtShareService();
            startBtG5Service();
        } else if (isWifiandBTWixel(collection_method, context)) {
            Log.d("DexDrip", "Starting wifi and bt wixel collector");
            stopBtWixelService();
            stopWifWixelThread();
            stopBtShareService();
            stopG5ShareService();
            // start both
            Log.d("DexDrip", "Starting wifi wixel collector first");
            startWifWixelThread();
            Log.d("DexDrip", "Starting bt wixel collector second");
            startBtWixelService();
            Log.d("DexDrip", "Started wifi and bt wixel collector");
        }
        if(prefs.getBoolean("broadcast_to_pebble", false)){
            startPebbleSyncService();
        }
        if(prefs.getBoolean("gear_sync", false)){
            startGearSyncService();
        }
        startSyncService();
        startDailyIntentService();
        Log.d(TAG, collection_method);

        // Start logging to logcat
        if(prefs.getBoolean("store_logs",false)) {
            String filePath = Environment.getExternalStorageDirectory() + "/xdriplogcat.txt";
            try {
                String[] cmd = {"/system/bin/sh", "-c", "ps | grep logcat  || logcat -f " + filePath +
                        " -v threadtime AlertPlayer:V com.eveningoutpost.dexdrip.Services.WixelReader:V *:E "};
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e2) {
                Log.e(TAG, "running logcat failed, is the device rooted?", e2);
            }
        }

    }

    public void start(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");

        start(context, collection_method);
    }

    public CollectionServiceStarter(Context context) {
        mContext = context;
    }

    public static void restartCollectionService(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopWifWixelThread();
        collectionServiceStarter.stopG5ShareService();
        collectionServiceStarter.start(context);
    }

    public static void restartCollectionService(Context context, String collection_method) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopWifWixelThread();
        collectionServiceStarter.stopG5ShareService();
        collectionServiceStarter.start(context, collection_method);
    }

    private void startBtWixelService() {
        Log.d(TAG, "starting bt wixel service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexCollectionService.class));
    	}
    }
    private void stopBtWixelService() {
        Log.d(TAG, "stopping bt wixel service");
        mContext.stopService(new Intent(mContext, DexCollectionService.class));
    }

    private void startBtShareService() {
        Log.d(TAG, "starting bt share service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexShareCollectionService.class));
        }
    }

    private void startBtG5Service() {
        Log.d(TAG, "starting G5 share service");
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, G5CollectionService.class));
        //}
    }

    private void startPebbleSyncService() {
        Log.d(TAG, "starting PebbleSync service");
        mContext.startService(new Intent(mContext, PebbleSync.class));
    }
    private void startGearSyncService() {
        Log.d(TAG, "starting GearSync service");
        mContext.startService(new Intent(mContext, GearSync.class));
    }
    private void startSyncService() {
        Log.d(TAG, "starting Sync service");
        mContext.startService(new Intent(mContext, SyncService.class));
    }
    private void startDailyIntentService() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        PendingIntent pi = PendingIntent.getService(mContext, 0, new Intent(mContext, DailyIntentService.class),PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }
    private void stopBtShareService() {
        Log.d(TAG, "stopping bt share service");
        mContext.stopService(new Intent(mContext, DexShareCollectionService.class));
    }

    private void startWifWixelThread() {
        Log.d(TAG, "starting wifi wixel service");
        mContext.startService(new Intent(mContext, WifiCollectionService.class));
    }

    private void stopWifWixelThread() {
        Log.d(TAG, "stopping wifi wixel service");
        mContext.stopService(new Intent(mContext, WifiCollectionService.class));
    }

    private void stopG5ShareService() {
        Log.d(TAG, "stopping G5  service");
        mContext.stopService(new Intent(mContext, G5CollectionService.class));
    }

}
