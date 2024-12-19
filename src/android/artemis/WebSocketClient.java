package com.appdinx.cardlink.artemis;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import com.appdinx.cardlink.CardLink;
import com.appdinx.cardlink.egk.CardCommandHandler;

public class WebSocketClient implements Client {

    dev.gustavoavila.websocketclient.WebSocketClient webSocketClient;
    private CallbackHandler callback;

    private String cardSessionId = UUID.randomUUID().toString();

    private String correlationId;

    @Override
    public void connect(Context context, ConnectionListener connectionListener) {
        URI uri;

        try {
            uri = new URI(Broker.getBrokerUrl());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
    // 192.168.181.154
        webSocketClient = new dev.gustavoavila.websocketclient.WebSocketClient(uri) {
            @Override
            public void onOpen() {
                if(connectionListener != null) {
                    connectionListener.onConnectionSuccess();
                }
            }

            @Override
            public void onTextReceived(String message) {
                Log.d(WebSocketClient.class.getName(), "onTextReceived "+message);

                // [
                //  {
                //    "type": "sendAPDU",
                //    "payload": "eyJjYXJkU2Vzc2lvbklkIjoiMzUwOTU4NGEtMDRlNy00NzU2LWJhYjAtMGRhNGE4NGQwYzEyIiwiYXBkdSI6IkFJZ0FBQmlBSjJpQkdabVpsb05wRDdiRDI3RWgweTFrbVV0RDN0a0EifQ=="
                //  },
                //  "38a5fb1b-f8e4-4132-8ec6-00fcd07bc5cc"
                //]
                // payload base64 decoded:
                // {
                //  "cardSessionId": "3509584a-04e7-4756-bab0-0da4a84d0c12",
                //  "apdu": "AIgAABiAJ2iBGZmZloNpD7bD27Eh0y1kmUtD3tkA"
                //}
                try {
                    JSONObject firstMessage;
                    if(message.substring(0, 1).equals("{")) {
                        firstMessage = new JSONObject(message);
                    } else {
                        JSONArray jObject = new JSONArray(message);
                        firstMessage = jObject.getJSONObject(0);
                        if(jObject.length() == 3) {
                            correlationId = jObject.getString(2);
                        }
                    }
                    String type = firstMessage.getString("type");

                    if(type == null || (!type.equals("sendAPDU") && !type.equals("eRezeptTokensFromAVS"))) {
                        if(type != null && type.equals("confirmSMSCodeResponse")){
                            String payload = firstMessage.getString("payload");
                            String codeJSON = new String(DatatypeConverter.parseBase64Binary(payload));

                            if(codeJSON.contains("SUCCESS")){
                                CardLink.isCodeCorrect = true;
                            }else{
                                CardLink.isCodeCorrect = false;
                            }
                        }

                        return;
                    }
                    byte[] messagePayload;

                    String payload = firstMessage.getString("payload");
                    String apduJson = new String(DatatypeConverter.parseBase64Binary(payload));
                    JSONObject apduJsonObject = new JSONObject(apduJson);
                    if(type.equals("eRezeptTokensFromAVS")) {
                        messagePayload = apduJsonObject.toString().getBytes();
                    } else {
                        String apdu = apduJsonObject.getString("apdu");
                        messagePayload = DatatypeConverter.parseBase64Binary(apdu);
                    }

                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(messagePayload);
                    callback.onMessageArrived("Websocket", mqttMessage);
                } catch (JSONException e) {
                    Log.e(WebSocketClient.class.getName(), "onTextReceived could not process message", e);
                }
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                Log.d(WebSocketClient.class.getName(),"onBinaryReceived");
            }

            @Override
            public void onPingReceived(byte[] data) {
                Log.d(WebSocketClient.class.getName(),"onPingReceived");
            }

            @Override
            public void onPongReceived(byte[] data) {
                Log.d(WebSocketClient.class.getName(),"onPongReceived");
            }

            @Override
            public void onException(Exception e) {
                Log.e("Websocket", "Websocket Exception", e);
                if(connectionListener != null) {
                    connectionListener.onConnectionFailure();
                }
            }

            @Override
            public void onCloseReceived(int reason, String description) {

            }
        };

        webSocketClient.setSSLSocketFactory(getNaiveSSLSocketFactory());
        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        // webSocketClient.addHeader("Origin", "http://developer.example.com");
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    public static SSLSocketFactory getNaiveSSLSocketFactory() {
        try {
            // Erstelle einen TrustManager, der alle Zertifikate akzeptiert
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Installiere den all-trusting TrustManager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            // Erstelle eine SSLSocketFactory, die alle Zertifikate akzeptiert
            return sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void publish(CardTerminalMessage message) {
        if(message.isApdu()) {
            // [
            //  {
            //    "type": "sendAPDUResponse",
            //    "payload": "eyJjYXJkU2Vzc2lvbklkIjoiMzUwOTU4NGEtMDRlNy00NzU2LWJhYjAtMGRhNGE4NGQwYzEyIiwicmVzcG9uc2UiOiJrYk5BSzR4djRueXZ3RU80ZndnbVptbkZud0dTYjIrV0RmdEpxMlN1czBsd3g1clFVUTl0cTBRMXFLd2x5anhwYUFMMENTSjFnV1JWUWtmVnpvWXh4WkFBIn0="
            //  },
            //  "38a5fb1b-f8e4-4132-8ec6-00fcd07bc5cc"
            //]
            // base 64 decoded payload:
            // {
            //  "cardSessionId": "3509584a-04e7-4756-bab0-0da4a84d0c12",
            //  "response": "kbNAK4xv4nyvwEO4fwgmZmnFnwGSb2+WDftJq2Sus0lwx5rQUQ9tq0Q1qKwlyjxpaAL0CSJ1gWRVQkfVzoYxxZAA"
            //}
            String response = DatatypeConverter.printBase64Binary(message.getMessage());
            String websocketMessage = "[{\"type\": \"sendAPDUResponse\", \"payload\": \"" + DatatypeConverter.printBase64Binary(("{\"response\": \"" + response + "\"}").getBytes()) + "\"}, \""+cardSessionId+"\", \""+correlationId+"\"]";
            Log.d(WebSocketClient.class.getName(), websocketMessage);
            webSocketClient.send(websocketMessage);
        } else {
            String websocketMessage = new String(message.getMessage());
            Log.d(WebSocketClient.class.getName(), websocketMessage);
            webSocketClient.send(websocketMessage);
        }
    }

    @Override
    public void disconnect() {
        webSocketClient.close(0, 0, null);
    }

    @Override
    public void setCallback(CallbackHandler callback) {
        this.callback = callback;
    }

    @Override
    public void sendCardRemovedEvent(String deviceId) {

    }

    @Override
    public void sendFUMessage(String deviceId) {

    }

    @Override
    public String getCardSessionId() {
        return cardSessionId;
    }

    @Override
    public void sendCardInsertedMessage(String deviceId, String json) {
        String registerEgk = "[{\n" +
                "      \"type\": \"registerEGK\",\n" +
                "      \"payload\": \"" + CardCommandHandler.printBase64Binary(json.getBytes()) + "\"\n" +
                "    }, \""+cardSessionId+"\", \""+UUID.randomUUID().toString()+"\"]";
        Log.d(WebSocketClient.class.getName(), registerEgk);
        webSocketClient.send(registerEgk);
    }
}
