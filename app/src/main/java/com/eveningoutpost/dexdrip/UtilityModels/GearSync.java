package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Models.BgReading;

import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.HashMap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

/**
 * Created by Simon on 06.04.2016.
 */
public class GearSync extends SAAgent {
    public static final String TAG = GearSync.class.getSimpleName();
    public static final int SERVICE_CONNECTION_RESULT_OK = 0;
    public static final int GEARACCESSORY_CHANNEL_ID = 314;

    HashMap<Integer, GearSyncConnection> mConnectionsMap = null;
    private final IBinder mBinder = new LocalBinder();
    private Context mContext;
    private BgGraphBuilder bgGraphBuilder;
    private BgReading mBgReading;

    public GearSync() {
        super(TAG, GearSyncConnection.class);
    }

    public class LocalBinder extends Binder {
        public GearSync getService() {
            return GearSync.this;
        }
    }

    public class GearSyncConnection extends SASocket {
        private int mConnectionId;

        public GearSyncConnection() {
            super(GearSyncConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorString, int error) {
            Log.e(TAG, "Connection is not alive ERROR: " + errorString + "  "
                    + error);
        }

        public void sendMsgToWatch(String msg) {
            Log.d(TAG, "In sendMsgToWatch");
            final String message = msg;
            final GearSyncConnection uHandler = mConnectionsMap.get(Integer.parseInt(String.valueOf(mConnectionId)));
            if (uHandler == null) {
                Log.e(TAG,
                        "Error, can not get GearSyncConnection handler");
                return;
            }
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(GEARACCESSORY_CHANNEL_ID,
                                message.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            Log.d(TAG, "onReceive");
        }

        @Override
        protected void onServiceConnectionLost(int errorCode) {
            Log.e(TAG, "onServiceConectionLost  for peer = " + mConnectionId
                    + "error code =" + errorCode);

            if (mConnectionsMap != null) {
                mConnectionsMap.remove(mConnectionId);
            }
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();

        Log.i(TAG, "onCreate of smart view Provider Service");

        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // Error Handling
        } catch (Exception e1) {
            Log.e(TAG, "Cannot initialize Accessory package.");
            e1.printStackTrace();
            /*
			 * Your application can not use Accessory package of Samsung Mobile
			 * SDK. You application should work smoothly without using this SDK,
			 * or you may want to notify user and close your app gracefully
			 * (release resources, stop Service threads, close UI thread, etc.)
			 */
            stopSelf();
        }

    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        acceptServiceConnectionRequest(peerAgent);
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent arg0, int arg1) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onFindPeerAgentResponse  arg1 =" + arg1);
    }

    @Override
    protected void onServiceConnectionResponse(SASocket thisConnection, int result) {
        Log.d(TAG, "In onServiceConnectionResponse");
        if (result == CONNECTION_SUCCESS) {
            if (thisConnection != null) {
                GearSyncConnection myConnection = (GearSyncConnection) thisConnection;

                if (mConnectionsMap == null) {
                    mConnectionsMap = new HashMap<Integer, GearSyncConnection>();
                }

                myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);

                Log.d(TAG, "onServiceConnection connectionID = "
                        + myConnection.mConnectionId);

                mConnectionsMap.put(myConnection.mConnectionId, myConnection);

                // Toast.makeText(getBaseContext(),
                // R.string.ConnectionEstablishedMsg, Toast.LENGTH_LONG)
                // .show();
            } else {
                Log.e(TAG, "SASocket object is null");
            }
        } else if (result == CONNECTION_ALREADY_EXIST) {
            Log.e(TAG, "onServiceConnectionResponse, CONNECTION_ALREADY_EXIST");
        } else {
            Log.e(TAG, "onServiceConnectionResponse result error =" + result);
        }
    }

    public void sendData(){
        mBgReading = BgReading.last();
        if(mBgReading != null) {
            //sendMsgToWatch("msg");
        }
    }

    public String bgReading() {
        return bgGraphBuilder.unitized_string(mBgReading.calculated_value);
    }

    public String bgDelta() {
        return bgGraphBuilder.unitizedDeltaString(false, false);
    }

    public String phoneBattery() {
        return String.valueOf(getBatteryLevel());
    }

    public String bgUnit() {
        return bgGraphBuilder.unit();
    }

    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) { return 50; }
        return (int)(((float)level / (float)scale) * 100.0f);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }
}
