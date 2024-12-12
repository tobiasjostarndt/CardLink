package com.appdinx.cardlink.artemis;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface CallbackHandler {
    void onConnectionLost();

    void onMessageArrived(String topic, MqttMessage message);

    void onDeliveryComplete(IMqttDeliveryToken token);
}
