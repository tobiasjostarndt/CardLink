package com.appdinx.cardlink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.slf4j.impl.HandroidLoggerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.appdinx.cardlink.activities.CanInsertionActivity;
import com.appdinx.cardlink.activities.PhoneInsertionActivity;
import com.appdinx.cardlink.artemis.Broker;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.artemis.MqttClient;
import com.appdinx.cardlink.artemis.WebSocketClient;
import com.appdinx.cardlink.exception.GlobalExceptionHandler;
import com.appdinx.cardlink.service.AbstractService;
import com.appdinx.cardlink.service.ConnectionService;

public class CardLinkStart extends AppCompatActivity {

    private static final Logger log = Logger.getLogger(CardLinkStart.class.getName());

    public boolean isConnectedWSS = false;

    static {
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG;
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT;
        HandroidLoggerAdapter.APP_NAME = "APPDINX";
    }

    private final BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AbstractService.CONNECTION_STATUS_ACTION.equals(intent.getAction())) {
                this.isConnectedWSS  = intent.getBooleanExtra(AbstractService.CONNECTION_STATUS_EXTRA, false);
            }
        }
    };

    public void initializeAndStartService(String brokerUrl) {
        Broker.setBrokerUrl(brokerUrl);

        ClientManager.client = new WebSocketClient();

        Intent intent = new Intent(this, ConnectionService.class );
        this.startService(intent);
    }

    private void showNfcNotEnabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("NFC ist auf diesem Gerät nicht aktiviert. Möchten Sie es aktivieren, um die App verwenden zu können?")
                .setPositiveButton("Ja", (dialog, which) -> startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS)))
                .setNegativeButton("Nein", (dialog, which) -> {
                    finish(); // Close the application
                })
                .setCancelable(false)
                .show();
    }

    public interface DialogCallback {
        void onConfirm(Map<String,String> userInput);
    }
}