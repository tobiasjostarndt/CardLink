package com.appdinx.cardlink.artemis;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.DatatypeConverter;

import com.appdinx.cardlink.R;
import com.appdinx.cardlink.util.DeviceUtils;

public class MqttClient implements Client {
    private static final Logger log = Logger.getLogger(MqttClient.class.getName());

    static ObjectMapper objectMapper = new ObjectMapper();

    private org.eclipse.paho.client.mqttv3.MqttClient client;

    @Override
    public void connect(Context context, ConnectionListener connectionListener) {
// Early return if client is already connected
        if (client != null && client.isConnected()) {
            return;
        }
        String deviceId = DeviceUtils.getDeviceId(context);

        try {
            client = new org.eclipse.paho.client.mqttv3.MqttClient(Broker.getBrokerUrl(), deviceId, new MemoryPersistence());

            // Create MQTT connection options
            MqttConnectOptions options = new MqttConnectOptions();

            // Configure SSL/TLS options
            options.setSocketFactory(createSSLSocketFactory(context));

            CardTerminalMessage lwtMessage = new CardTerminalMessage();
            lwtMessage.setDeviceId(deviceId);
            lwtMessage.setMessage(constructFURemovedMessage());
            byte[] lwtPayload = serializeDeviceMessage(lwtMessage);

            if (lwtPayload == null) {
                log.log(Level.SEVERE, "Error serializing LWT message");
                return;  // If we can't serialize the message, there's no point in going further
            }

            options.setWill(Topics.CARD_TERMINAL, lwtPayload, 2, false);

            client.connect(options);
            // Connection success
            if (connectionListener != null) {
                connectionListener.onConnectionSuccess();
            }

            try {
                client.subscribe(Topics.DEVICE + deviceId);
            } catch (MqttException e) {
                log.log(Level.SEVERE, "Error subscribing to the topics: ", e);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error communicating with the server: ", e);
            // Connection failure
            if (connectionListener != null) {
                connectionListener.onConnectionFailure();
            }
        }
    }

    @Override
    public void publish(CardTerminalMessage cardTerminalMessage) {
        byte[] payload = serializeDeviceMessage(cardTerminalMessage);
        MqttMessage message = new MqttMessage(payload);
        try {
            publish(Topics.CARD_TERMINAL, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            Log.e("ACTIVEMQ", "Error during MQTT disconnection: " + e.getMessage());
        }
    }

    @Override
    public void setCallback(CallbackHandler callbackHandler) {
        MqttCallback mqttCallback = this.createCallback(callbackHandler);
        client.setCallback(mqttCallback);
    }

    private SSLSocketFactory createSSLSocketFactory(Context context) {
        try {

            // Load the keystore
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream ksFile = context.getResources().openRawResource(R.raw.client);
            ks.load(ksFile, "123456".toCharArray());

            // Create KeyManagerFactory and initialize it with the keystore
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "123456".toCharArray());

            // Create SSL context and configure it with the truststore
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Return the SSLSocketFactory from the SSL context
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error creating SSL Socket Factory: ", e);
        }
        return null;
    }


    private byte[] serializeDeviceMessage(CardTerminalMessage deviceMessage) {
        try {
            return objectMapper.writeValueAsBytes(deviceMessage);
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "Error serializing LWT message: ", e);
            return null;
        }
    }


    private void publish(String topic, MqttMessage message) throws MqttException {
        publish(topic, message, true);
    }

    private void publish(String topic, MqttMessage message, boolean retry) throws MqttException {
        try {
            client.publish(topic, message);
        } catch(MqttException e) {
            if(e.getReasonCode() == 32104 && retry) {
                client.connect();
                publish(topic, message, false);
            }
            throw e;
        }
    }

    public void sendFUMessage(String deviceId)  {
        CardTerminalMessage deviceMessage = new CardTerminalMessage();
        deviceMessage.setDeviceId(deviceId);
        deviceMessage.setMessage(constructFUMessage());
        byte[] payload = serializeDeviceMessage(deviceMessage);
        MqttMessage message = new MqttMessage(payload);
        try {
            publish(Topics.CARD_TERMINAL, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCardSessionId() {
        return null;
    }

    @Override
    public void sendCardInsertedMessage(String deviceId, String json) {
        sendCardInsertedMessage(deviceId, null);
    }

    public void sendCardInsertedMessage(String deviceId) {
        CardTerminalMessage deviceMessage = new CardTerminalMessage();
        deviceMessage.setDeviceId(deviceId);
        deviceMessage.setMessage(constructCardInsertedMessage());
        byte[] payload = serializeDeviceMessage(deviceMessage);
        MqttMessage message = new MqttMessage(payload);
        try {
            publish(Topics.CARD_TERMINAL, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendCardRemovedEvent(String deviceId) {
        CardTerminalMessage deviceMessage = new CardTerminalMessage();
        deviceMessage.setDeviceId(deviceId);
        deviceMessage.setMessage(constructCardRemovedMessage());
        byte[] payload = serializeDeviceMessage(deviceMessage);
        MqttMessage message = new MqttMessage(payload);
        try {
            publish(Topics.CARD_TERMINAL, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] constructFUMessage() {
        // 50 indicates an event in SICCT
        // 82 at the 11th position indicates FU hinzugekommen (inserted)
        return DatatypeConverter.parseHexBinary("500000000E000000000482020000");
    }

    private byte[] constructFURemovedMessage() {
        // 50 indicates an event in SICCT
        // 83 at the 11th position indicates FU entfernt (removed)
        return DatatypeConverter.parseHexBinary("500000000E000000000483020000");
    }

    private byte[] constructCardInsertedMessage() {
        // 50 indicates an event in SICCT
        // 84 at the 11th position indicates Karte eingesteckt
        return DatatypeConverter.parseHexBinary("500000000E000000000484020000");
    }

    private byte[] constructCardRemovedMessage() {
        // 50 indicates an event in SICCT
        // 85 at the 11th position indicates Karte entfernt
        return DatatypeConverter.parseHexBinary("500000000E000000000485020000");
    }


    public MqttCallback createCallback(CallbackHandler callbackHandler) {
        return new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("ACTIVEMQ", "Connection with the broker lost. It will try to reconnect...");
                try {
                    client.reconnect();
                } catch (MqttException e) {
                    Log.e("ACTIVEMQ", "Error while reconnecting " + e.getMessage());
                }
                if (callbackHandler != null) {
                    callbackHandler.onConnectionLost();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (callbackHandler != null) {
                    callbackHandler.onMessageArrived(topic, message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                if (callbackHandler != null) {
                    callbackHandler.onDeliveryComplete(token);
                }
            }
        };
    }
}
