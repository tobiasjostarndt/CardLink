package com.appdinx.cardlink.artemis;

import android.content.Context;

interface Client {

    public void connect(Context context, ConnectionListener connectionListener);

    public void publish(CardTerminalMessage message);

    public void disconnect();

    public void setCallback(CallbackHandler callback);

    public void sendCardRemovedEvent(String deviceId);

    public void sendFUMessage(String deviceId);

    public String getCardSessionId();

    public void sendCardInsertedMessage(String deviceId, String json);
}
