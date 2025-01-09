package com.appdinx.cardlink;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import org.slf4j.impl.HandroidLoggerAdapter;

import java.util.logging.Logger;

import com.appdinx.cardlink.artemis.Broker;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.artemis.WebSocketClient;
import com.appdinx.cardlink.service.AbstractService;
import com.appdinx.cardlink.service.ConnectionService;

public class CardLinkService extends Service {

    private static final Logger log = Logger.getLogger(CardLinkService.class.getName());

    static {
        HandroidLoggerAdapter.DEBUG = false;
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT;
        HandroidLoggerAdapter.APP_NAME = "APPDINX";
    }

    private final BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AbstractService.CONNECTION_STATUS_ACTION.equals(intent.getAction())) {
                setConnectedWSS(intent.getBooleanExtra(AbstractService.CONNECTION_STATUS_EXTRA, false));
            }
        }
    };

    private void setConnectedWSS(boolean isConnected) {
        CardLink.isConnectedWSS = isConnected;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String brokerUrl = intent.getStringExtra("brokerUrl");

        Broker.setBrokerUrl(brokerUrl);
        ClientManager.client = new WebSocketClient();

        setConnectedWSS(false);

        registerReceiver(connectionStatusReceiver, new IntentFilter(AbstractService.CONNECTION_STATUS_ACTION));

        setupConnection();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectionStatusReceiver);
    }

    private void setupConnection() {
        Intent intent = new Intent(this, ConnectionService.class );
        this.startService(intent);
    }
}
