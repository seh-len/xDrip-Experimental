package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
//import android.os.Handler;
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

    private BgGraphBuilder bgGraphBuilder;
    private BgReading mBgReading;

    private static final int GEAR_ACCESSORY_CHANNEL_ID = 314;
    private static final Class<GearSyncConnection> SASOCKET_CLASS = GearSyncConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private Context mContext;
    private GearSyncConnection mConnectionHandler = null;
    //Handler mHandler = new Handler();

    public GearSync() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        bgGraphBuilder = new BgGraphBuilder(mContext);
        mBgReading = BgReading.last();
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
            findPeers();
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e) == true) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        }
        init();
    }



    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for(SAPeerAgent peerAgent:peerAgents)
                requestServiceConnection(peerAgent);
        } else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED) {
            Log.e(TAG, "FINDPEER_DEVICE_NOT_CONNECTED");
        } else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND) {
            Log.e(TAG, "FINDPEER_SERVICE_NOT_FOUND");
        } else {
            Log.e(TAG, "NO_PEERS_FOUND");
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        if (result == SAAgent.CONNECTION_SUCCESS) {
            Log.i(TAG, "CONNECTION_SUCCESS");
            this.mConnectionHandler = (GearSyncConnection) socket;
        } else if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
            Log.d(TAG, "CONNECTION_ALREADY_EXIST");
        } else if (result == SAAgent.CONNECTION_DUPLICATE_REQUEST) {
            Log.e(TAG, "CONNECTION_DUPLICATE_REQUEST");
        } else {
            Log.e(TAG, "CONNECTION_FAILURE");
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        final SAPeerAgent[] peers = peerAgents;
        final int status = result;

        if (peers != null) {
            if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                Log.d(TAG, "PEER_AGENT_AVAILABLE");
            } else {
                Log.d(TAG, "PEER_AGENT_UNAVAILABLE");
            }
        }
        /*mHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });*/
    }

    public class GearSyncConnection extends SASocket {
        public GearSyncConnection() {
            super(GearSyncConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            final String message = new String(data);
            Log.d(TAG, "Received: " + message);
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            Log.d(TAG, "DISCONNECTED");
            closeConnection();
        }
    }

    public class LocalBinder extends Binder {
        public GearSync getService() {
            return GearSync.this;
        }
    }

    public void findPeers() {
        findPeerAgents();
    }

    public boolean sendDataToGear(final String data) {
        boolean retvalue = false;
        if (mConnectionHandler != null) {
            try {
                mConnectionHandler.send(GEAR_ACCESSORY_CHANNEL_ID, data.getBytes());
                retvalue = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Sent: " + data);
        }
        return retvalue;
    }

    public boolean closeConnection() {
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            return true;
        } else {
            return false;
        }
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            /*
             * Your application can not use Samsung Accessory SDK. You application should work smoothly
             * without using this SDK, or you may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
            return false;
        }
        return true;
    }

    private void init() {
        Log.i(TAG, "Initialising...");
        Log.i(TAG, "configuring GearDataProvider");
        sendData();
    }

    public void sendData(){
        mBgReading = BgReading.last();
        if (mBgReading != null) {
            sendDataToGear(bgReading());
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
}