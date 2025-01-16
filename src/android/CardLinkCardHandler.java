package com.appdinx.cardlink;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.appdinx.cardlink.artemis.Broker;
import com.appdinx.cardlink.artemis.WebSocketClient;
import com.appdinx.cardlink.service.AbstractService;
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
import com.appdinx.cardlink.artemis.CardTerminalMessage;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.artemis.CallbackHandler;
import com.appdinx.cardlink.egk.CardAbsentEventSubscriber;
import com.appdinx.cardlink.egk.CardCommandHandler;
import com.appdinx.cardlink.util.DeviceUtils;

public class CardLinkCardHandler extends Service implements CallbackHandler {

    private static final Logger log = Logger.getLogger(CardLinkCardHandler.class.getName());

    private NfcCardReader nfcCardReader;
    private NfcAdapter nfcAdapter;
    private NfcCardChecker nfcCardChecker;
    private CardCommandHandler cardCommandHandler;
    private CardAbsentEventSubscriber cardAbsentEventSubscriber;
    private Handler handler;
    private ClientManager clientManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String canNumber = intent.getStringExtra("canNumber");

        clientManager = ClientManager.getInstance();
        clientManager.subscribe(this);
        handler = new Handler(Looper.getMainLooper());

        initializeNfcAdapter();
        initializeCardReader();

        cardCommandHandler = new CardCommandHandler(canNumber, this);
        cardAbsentEventSubscriber = new CardAbsentEventSubscriber(this);

        initializeEventBus();

        subscribeToDeviceEvents();
        publishDeviceFUMessage();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterEventBus();

        if(nfcAdapter != null){
            nfcAdapter.disableReaderMode(CardLink.getCordovaActivity());
        }
    }

    @Override
    public void onConnectionLost() {
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
        byte[] payload = message.getPayload();

        if (isLastMessage(payload)) {
            Log.w("Payload", payload.toString());
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

    private void initializeNfcAdapter() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void initializeCardReader() {
        nfcCardReader = new NfcCardReader(nfcAdapter, this);
        //nfcCardChecker = new NfcCardChecker(nfcCardReader);

        final Bundle options = new Bundle();

        nfcAdapter.enableReaderMode(CardLink.getCordovaActivity(), nfcCardReader, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, options);
        nfcCardReader.setOnline(true);
    }

    private void initializeEventBus() {
        try{
            EventBus eventBus = EventBus.getDefault();
            eventBus.register(cardCommandHandler);
            eventBus.register(cardAbsentEventSubscriber);
        }catch(Exception x){
        }
    }

    private void unregisterEventBus() {
        try{
            EventBus eventBus = EventBus.getDefault();
            eventBus.unregister(cardCommandHandler);
            eventBus.unregister(cardAbsentEventSubscriber);
        }catch(Exception x){
        }
    }

    private void subscribeToDeviceEvents() {
        clientManager.subscribe(this);
    }

    private void publishDeviceFUMessage() {
        try {
            clientManager.sendFUMessage(DeviceUtils.getDeviceId(this));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error communicating with the server: ", e);
        }
    }

}
