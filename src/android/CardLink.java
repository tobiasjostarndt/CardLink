package com.appdinx.cardlink;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CardLink extends CordovaPlugin {
    public static String correlationId = "";
    public static String cardSessionID = "";
    public static String canNumber = "";
    public static String eRezeptTokensFromAVS = "";
    public static String eRezeptBundlesFromAVS = "";
    public static boolean isCodeCorrect = false;
    public static boolean cardScanned = false;
    public static boolean isConnectedWSS = false;

    // Logger tag
    private static final String TAG = "CardLink";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
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
            default:
                return false;
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

                callbackContext.success("true");
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

                callbackContext.success("true");
            } else {
                callbackContext.error("Verification code is empty");
            }
        } catch (JSONException e) {
            callbackContext.error("Error verifying code");
        }
    }

    private void isSMSCodeCorrect(CallbackContext callbackContext) {
        if (this.isCodeCorrect) {
            callbackContext.success("true");
        } else {
            callbackContext.success("false");
        }
    }

    private void setCanNumber(JSONArray args, CallbackContext callbackContext) {
        try {
            String canNumberInput = args.getString(0);
            if (canNumberInput != null && !canNumberInput.isEmpty()) {
                this.canNumber = canNumberInput;
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
            this.cardScanned = true;
            callbackContext.success("true");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start reading card", e);
            callbackContext.error("Error reading card");
        }
    }

    private void isCardScanned(CallbackContext callbackContext) {
        if (this.cardScanned) {
            callbackContext.success("true");
        } else {
            callbackContext.success("false");
        }
    }

    private void getERezeptTokensFromAVS(CallbackContext callbackContext) {
        if (this.cardScanned && !this.eRezeptTokensFromAVS.isEmpty()) {
            callbackContext.success(this.eRezeptTokensFromAVS);
        } else {
            callbackContext.error("No tokens available");
        }
    }

    private void getERezeptBundlesFromAVS(CallbackContext callbackContext) {
        if (this.cardScanned && !this.eRezeptBundlesFromAVS.isEmpty()) {
            callbackContext.success(this.eRezeptBundlesFromAVS);
        } else {
            callbackContext.error("No bundles available");
        }
    }
}