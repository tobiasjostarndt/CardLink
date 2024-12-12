package com.appdinx.cardlink.activities;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import de.gematik.ti.cardreader.provider.nfc.control.NfcCardChecker;
import de.gematik.ti.cardreader.provider.nfc.entities.NfcCardReader;
import com.appdinx.cardlink.activities.task.FetchPrescriptions;
import com.appdinx.cardlink.artemis.CardTerminalMessage;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.artemis.CallbackHandler;
import com.appdinx.cardlink.egk.CardAbsentEventSubscriber;
import com.appdinx.cardlink.egk.CardCommandHandler;
import com.appdinx.cardlink.R;
import com.appdinx.cardlink.util.DeviceUtils;

public class CardInsertionActivity extends AppCompatActivity implements CallbackHandler {

    private static final Logger log = Logger.getLogger(CardInsertionActivity.class.getName());

    private NfcCardReader nfcCardReader;
    private NfcAdapter nfcAdapter;
    private NfcCardChecker nfcCardChecker;
    private CardCommandHandler cardCommandHandler;
    private CardAbsentEventSubscriber cardAbsentEventSubscriber;
    private BottomSheetDialog bottomSheetDialog;
    private Handler handler;
    private ClientManager clientManager;
    static ObjectMapper objectMapper = new ObjectMapper();
    static int messageCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_card_insert);
        clientManager = ClientManager.getInstance();
        clientManager.subscribe(this);
        handler = new Handler(Looper.getMainLooper());
        initializeViews();
        initializeNfcAdapter();
        initializeCardReader();
        cardCommandHandler = new CardCommandHandler(getCombinedDigits(), this);
        cardAbsentEventSubscriber = new CardAbsentEventSubscriber(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeEventBus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterEventBus();
    }

    @Override
    public void onConnectionLost() {
        setStatusText("Fehler mit Serververbindung");
        try {
            sendCardRemovedEvent(DeviceUtils.getDeviceId(this));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) {
        handleCardResponseMessage(DeviceUtils.getDeviceId(this), message);
    }

    @Override
    public void onDeliveryComplete(IMqttDeliveryToken token) {
        // Handle delivery complete
    }

    private void handleCardResponseMessage(String deviceId, MqttMessage message) {
        messageCount++;
        setMessageCount(messageCount);

        byte[] payload = message.getPayload();

        if (isLastMessage(payload)) {
                runOnUiThread(() -> new FetchPrescriptions(this, payload).execute());
                messageCount = 0;
        } else {
            IResponseApdu iResponseApdu = CardCommandHandler.sendCommand(payload);
            if (iResponseApdu != null) {
                sendCardResponseEvent(deviceId, payload, iResponseApdu);
            } else {
                Log.w("APDU", "No response given");
            }
        }

    }

    public void sendCardRemovedEvent(String deviceId) throws MqttException {
        CardTerminalMessage deviceMessage = new CardTerminalMessage();
        deviceMessage.setDeviceId(deviceId);
        deviceMessage.setMessage(constructCardRemovedMessage());
        clientManager.publish(deviceMessage);
    }

    private byte[] constructCardRemovedMessage() {
        // 50 indicates an event in SICCT
        // 85 at the 11th position indicates Karte entfernt
        return DatatypeConverter.parseHexBinary("500000000E000000000485020000");
    }

    private boolean isLastMessage(byte[] payload) {
        return payload[0] == '{';
    }

    private void sendCardResponseEvent(String deviceId, byte[] commandBytes, IResponseApdu response) {
        CardTerminalMessage cardTerminalMessage = new CardTerminalMessage();
        cardTerminalMessage.setDeviceId(deviceId);
        cardTerminalMessage.setMessage(response.getBytes());
        clientManager.publish(cardTerminalMessage);
    }

    private void initializeViews() {
        setupPreviousButton();
        setupConnectCardButton();
    }

    private void initializeNfcAdapter() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void initializeCardReader() {
        nfcCardReader = new NfcCardReader(nfcAdapter, this);
        nfcCardChecker = new NfcCardChecker(nfcCardReader);
    }

    private void initializeEventBus() {
        EventBus eventBus = EventBus.getDefault();
        eventBus.register(cardCommandHandler);
        eventBus.register(cardAbsentEventSubscriber);
    }

    private void unregisterEventBus() {
        EventBus eventBus = EventBus.getDefault();
        eventBus.unregister(cardCommandHandler);
        eventBus.unregister(cardAbsentEventSubscriber);
    }

    private String getCombinedDigits() {
        Bundle enteredDigits = getIntent().getExtras();
        if (enteredDigits == null) {
            return null;
        }
        String digit1 = enteredDigits.getString("digit1");
        String digit2 = enteredDigits.getString("digit2");
        String digit3 = enteredDigits.getString("digit3");
        String digit4 = enteredDigits.getString("digit4");
        String digit5 = enteredDigits.getString("digit5");
        String digit6 = enteredDigits.getString("digit6");
        return digit1 + digit2 + digit3 + digit4 + digit5 + digit6;
    }

    private void subscribeToDeviceEvents() {
        clientManager.subscribe(this);
        String text = "Für Nachrichten angemeldet";
        setStatusText(text);
    }

    private void publishDeviceFUMessage() {
        try {
            clientManager.sendFUMessage(DeviceUtils.getDeviceId(this));
            String text = "Neues Gerät angemeldet";
            setStatusText(text);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error communicating with the server: ", e);
            String text = "Fehler bei Gerät anmelden";
            setStatusText(text);
        }
    }

    public void setStatusText(String text) {
        if (bottomSheetDialog != null) {
            TextView textView = bottomSheetDialog.findViewById(R.id.statusText);
            if (textView != null) {
                textView.setText(text);
            } else {
                Log.w("VIEW", "TextView not available in bottomSheetDialog");
            }
        } else {
            Log.w("VIEW", "bottomSheetDialog not available");
        }
    }

    private void setupPreviousButton() {
        Button previousButton = findViewById(R.id.previous_button);
        previousButton.setOnClickListener(v -> {
            finishAndRemoveTask();
        });
    }

    private void setupConnectCardButton() {
        Button connectCardButton = findViewById(R.id.continue_button);
        connectCardButton.setOnClickListener(v -> {
            subscribeToDeviceEvents();
            publishDeviceFUMessage();
            showScanningDialog();
        });
    }

    private void showScanningDialog() {
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.dialog_scanning_card);
        bottomSheetDialog.setCanceledOnTouchOutside(false);
        Button buttonCancel = bottomSheetDialog.findViewById(R.id.button_cancel);
        assert buttonCancel != null;
        bottomSheetDialog.show();
        buttonCancel.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            finishAndRemoveTask();
            ClientManager.getInstance().sendCardRemovedEvent(DeviceUtils.getDeviceId(this));
        });
    }

    public void setMessageCount(int progress) {
        ProgressBar progressBar = bottomSheetDialog.findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setProgress(progress, true);
        } else {
            Log.w("VIEW", "ProgressBar not available in bottomSheetDialog");
        }
    }

    public void showScanningCompletedDialog() {
        bottomSheetDialog.dismiss();
        bottomSheetDialog.setContentView(R.layout.dialog_card_reading_completed);
        bottomSheetDialog.show();
        handler.postDelayed(this::navigateToNextActivity, 1000);
    }

    private void navigateToNextActivity() {
        bottomSheetDialog.dismiss();
        Intent intent = new Intent(CardInsertionActivity.this, PrescriptionRetrieveActivity.class);
        startActivity(intent);
    }

}
