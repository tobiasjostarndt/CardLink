package com.appdinx.cardlink.artemis;

import android.content.Context;

import java.util.logging.Logger;

public class ClientManager {
    private static final Logger log = Logger.getLogger(ClientManager.class.getName());

    public static String protocol;
    private static ClientManager instance;
    public static Client client;
    private CallbackHandler callbackHandler;

    private ConnectionListener connectionListener;

    Context context;

    public ClientManager() {

    }

    public static ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }


    public void connect(Context context) {
        this.context = context;
        connect();
    }
    public void connect() {
        client.connect(context, connectionListener);
    }

    public void publish(CardTerminalMessage cardTerminalMessage) {
        client.publish(cardTerminalMessage);
    }


    //TODO: check if this is needed and where
    public void disconnect() {
        client.disconnect();
    }

    public void subscribe(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
        client.setCallback(this.callbackHandler);
    }

    public void sendCardRemovedEvent(String deviceId) {
        client.sendCardRemovedEvent(deviceId);
    }

    public void sendFUMessage(String deviceId) {
        client.sendFUMessage(deviceId);
    }

    public void sendCardInsertedMessage(String deviceId, String json) {
        client.sendCardInsertedMessage(deviceId, json);
    }

    public String getCardSessionId() {
        return client.getCardSessionId();
    }
}
