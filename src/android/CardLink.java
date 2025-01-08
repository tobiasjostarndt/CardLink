package com.appdinx.cardlink;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.appdinx.cardlink.artemis.CardTerminalMessage;
import com.appdinx.cardlink.artemis.ClientManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CardLink extends CordovaPlugin {
    public static String correlationId = "";
    public static String cardSessionID = "";
    public static String canNumber = "";
    public static String eRezeptTokensFromAVS = "";
    public static String eRezeptBundlesFromAVS = "";
    public static boolean isCodeCorrect = false;
    public static boolean cardScanned = false;
    public static boolean isConnectedWSS = false;
    public static String error = "";
    public static String smsText = "Bitte geben Sie in der CardLink App folgenden Code ein: {0}";

    public static Activity cordovaActivity;

    // Logger tag
    private static final String TAG = "CardLink";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        cordovaActivity = cordova.getActivity();

        switch (action) {
            case "establishWSS":
                establishWSS(args, callbackContext);
                return true;
            case "isConnectedWSS":
                isConnectedWSS(callbackContext);
                return true;
            case "sendRequestSMSCodeMessage":
                sendRequestSMSCodeMessage(args, callbackContext);
                return true;
            case "verifyCode":
                verifyCode(args, callbackContext);
                return true;
            case "isSMSCodeCorrect":
                isSMSCodeCorrect(callbackContext);
                return true;
            case "setCanNumber":
                setCanNumber(args, callbackContext);
                return true;
            case "startReadCard":
                startReadCard(callbackContext);
                return true;
            case "isCardScanned":
                isCardScanned(callbackContext);
                return true;
            case "getERezeptTokensFromAVS":
                getERezeptTokensFromAVS(callbackContext);
                return true;
            case "getERezeptBundlesFromAVS":
                getERezeptBundlesFromAVS(callbackContext);
                return true;
            case "isError":
                isError(callbackContext);
                return true;
            default:
                return false;
        }
    }

    private void setSMSText(JSONArray args, CallbackContext callbackContext) {
        try {
            String sT = args.getString(0);

            if (sT != null && !sT.isEmpty()) {
                smsText = sT;
                callbackContext.success("true");
            }
        } catch (JSONException e) {
        }
    }
    
    private void establishWSS(JSONArray args, CallbackContext callbackContext) {
        try {
            String wssURL = args.getString(0);

            if (wssURL != null && !wssURL.isEmpty()) {
                Intent intent = new Intent(cordova.getActivity(), CardLinkService.class);
                intent.putExtra("brokerUrl", wssURL); // Beispiel-URL
                cordova.getActivity().startService(intent);

                callbackContext.success("true");
            } else {
                callbackContext.error("Invalid URL");
            }
        } catch (JSONException e) {
            callbackContext.error("Error establishing WSS");
        }
    }
    
    private void isConnectedWSS(CallbackContext callbackContext) {
        if (isConnectedWSS) {
            callbackContext.success("true");
        } else {
            callbackContext.success("false");
        }
    }

    private void sendRequestSMSCodeMessage(JSONArray args, CallbackContext callbackContext) {
        try {
            String phoneNumber = args.getString(0);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                try {
                    ClientManager clientManager = ClientManager.getInstance();

                    CardTerminalMessage smsCodeRequestMessage = new CardTerminalMessage();
                    smsCodeRequestMessage.setApdu(false);
                    JSONArray jsonArray = new JSONArray();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "requestSMSCode");
                    JSONObject payload = new JSONObject();
                    payload.put("senderId", "cardlink");
                    payload.put("textTemplate", smsText);
                    payload.put("phoneNumber", phoneNumber);
                    payload.put("textReassignmentTemplate", "Ihre Gesundheitskarte {0} wurde der Telefonnummer {1} neu zugeordnet. Wenn Sie diese Telefonnummer kennen, ist alles in Ordnung. Wenn Ihre Karte gestohlen wurde, lassen Sie diese bitte von Ihrer Versicherung sperren.");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        jsonObject.put("payload", Base64.getEncoder().encodeToString(payload.toString().getBytes()));
                    }
                    jsonArray.put(jsonObject);
                    jsonArray.put(clientManager.getCardSessionId());
                    smsCodeRequestMessage.setMessage(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
                    clientManager.publish(smsCodeRequestMessage);

                    callbackContext.success("true");
                } catch (JSONException ex) {
                    callbackContext.success("false");
                    throw new RuntimeException(ex);
                }
            } else {
                callbackContext.error("Phone number is empty");
            }
        } catch (JSONException e) {
            callbackContext.error("Error sending SMS Code request");
        }
    }

    private void verifyCode(JSONArray args, CallbackContext callbackContext) {
        try {
            String code = args.getString(0);
            if (code != null && !code.isEmpty()) {
                try {
                    ClientManager clientManager = ClientManager.getInstance();

                    CardTerminalMessage smsCodeRequestMessage = new CardTerminalMessage();
                    smsCodeRequestMessage.setApdu(false);
                    JSONArray jsonArray = new JSONArray();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "confirmSMSCode");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        jsonObject.put("payload", Base64.getEncoder().encodeToString(("{\"smsCode\":\""+code+"\"}").getBytes()));
                    }
                    jsonArray.put(jsonObject);
                    jsonArray.put(clientManager.getCardSessionId());
                    smsCodeRequestMessage.setMessage(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
                    clientManager.publish(smsCodeRequestMessage);

                    callbackContext.success("true");
                } catch (JSONException ex) {
                    callbackContext.success("false");
                    throw new RuntimeException(ex);
                }
            } else {
                callbackContext.error("Verification code is empty");
            }
        } catch (JSONException e) {
            callbackContext.error("Error verifying code");
        }
    }

    private void isSMSCodeCorrect(CallbackContext callbackContext) {
        if (isCodeCorrect) {
            callbackContext.success("true");
        } else {
            callbackContext.success("false");
        }
    }

    private void setCanNumber(JSONArray args, CallbackContext callbackContext) {
        try {
            String canNumberInput = args.getString(0);
            if (canNumberInput != null && !canNumberInput.isEmpty()) {
                canNumber = canNumberInput;
                callbackContext.success("true");
            } else {
                callbackContext.error("CAN number is empty");
            }
        } catch (JSONException e) {
            callbackContext.error("Error setting CAN number");
        }
    }

    private void startReadCard(CallbackContext callbackContext) {
        try {
            Intent intent = new Intent(cordova.getActivity(), CardLinkCardHandler.class);

            intent.putExtra("canNumber", canNumber);

            cordova.getActivity().startService(intent);

            callbackContext.success("true");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start reading card", e);
            callbackContext.error("Error reading card");
        }
    }

    private void isCardScanned(CallbackContext callbackContext) {
        if (cardScanned) {
            callbackContext.success("true");
        } else {
            callbackContext.success("false");
        }
    }

    private void getERezeptTokensFromAVS(CallbackContext callbackContext) {
        if (cardScanned && !eRezeptTokensFromAVS.isEmpty()) {
            callbackContext.success(eRezeptTokensFromAVS);
        } else {
            callbackContext.error("No tokens available");
        }
    }

    private void getERezeptBundlesFromAVS(CallbackContext callbackContext) {
        if (cardScanned && !eRezeptBundlesFromAVS.isEmpty()) {
            callbackContext.success(eRezeptBundlesFromAVS);
        } else {
            callbackContext.error("No bundles available");
        }
    }

    private void isError(CallbackContext callbackContext){
        if(!error.isEmpty()){
            callbackContext.success(error);
        }else{
            callbackContext.success("false");
        }
    }

    public static Activity getCordovaActivity(){
        return cordovaActivity;
    }
}