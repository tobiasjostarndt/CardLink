package com.appdinx.cardlink.service;
import android.content.Intent;

import com.appdinx.cardlink.artemis.ClientManager;

public class ConnectionService extends AbstractService {
    private ClientManager clientManager;

    public ConnectionService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        clientManager = ClientManager.getInstance();
        clientManager.setConnectionListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        clientManager.connect(this);

        // Service will not be recreated if it gets terminated
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        clientManager.disconnect();
    }

}
