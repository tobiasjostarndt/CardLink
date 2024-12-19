package com.appdinx.cardlink.artemis;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface Callback {
    public void connectionLost(Throwable cause);
    public void messageArrived(String topic, MqttMessage message);
    public void deliveryComplete(IMqttDeliveryToken token);
}
