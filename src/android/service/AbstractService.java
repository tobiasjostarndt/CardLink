package com.appdinx.cardlink.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.appdinx.cardlink.artemis.ConnectionListener;

public class AbstractService extends Service implements ConnectionListener {
    public static final String CONNECTION_STATUS_ACTION = "com.appdinx.cardlink.CONNECTION_STATUS";
    public static final String CONNECTION_STATUS_EXTRA = "connection_status";


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionSuccess() {
        // Connection success, send a broadcast to notify the MainActivity
        Intent intent = new Intent(CONNECTION_STATUS_ACTION);
        intent.putExtra(CONNECTION_STATUS_EXTRA, true);
        sendBroadcast(intent);
    }

    @Override
    public void onConnectionFailure() {
        // Connection failure, send a broadcast to notify the MainActivity
        Intent intent = new Intent(CONNECTION_STATUS_ACTION);
        intent.putExtra(CONNECTION_STATUS_EXTRA, false);
        sendBroadcast(intent);
    }
}
