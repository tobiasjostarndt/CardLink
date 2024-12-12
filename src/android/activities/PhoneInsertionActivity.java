package com.appdinx.cardlink.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.appdinx.cardlink.R;
import com.appdinx.cardlink.artemis.CardTerminalMessage;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.util.DeviceUtils;

public class PhoneInsertionActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private EditText phone_number;
    private Button continueButton;

    BottomSheetDialog dialogInputSmsCode;

    ClientManager clientManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_phone_insert);

        phone_number = findViewById(R.id.phone_number);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS}, 10);
        } else {
            setPhoneNumber();
        }


        clientManager = ClientManager.getInstance();


        continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(v -> {
            // Get the entered digits
            String phone_number_string = phone_number.getText().toString();
            try {
                CardTerminalMessage smsCodeRequestMessage = new CardTerminalMessage();
                smsCodeRequestMessage.setApdu(false);
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "requestSMSCode");
                JSONObject payload = new JSONObject();
                payload.put("senderId", "cardlink");
                payload.put("textTemplate", "Bitte geben Sie in der CardLink App folgenden Code ein: {0}");
                payload.put("phoneNumber", phone_number_string);
                payload.put("textReassignmentTemplate", "Ihre Gesundheitskarte {0} wurde der Telefonnummer {1} neu zugeordnet. Wenn Sie diese Telefonnummer kennen, ist alles in Ordnung. Wenn Ihre Karte gestohlen wurde, lassen Sie diese bitte von Ihrer Versicherung sperren.");
                jsonObject.put("payload", Base64.getEncoder().encodeToString(payload.toString().getBytes()));
                jsonArray.put(jsonObject);
                jsonArray.put(clientManager.getCardSessionId());
                smsCodeRequestMessage.setMessage(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
                clientManager.publish(smsCodeRequestMessage);

                showDialogInputSmsCode();
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Add any additional logic or functionality here

        // Go back to MainActivity
        Button previousButton = findViewById(R.id.previous_button);
        previousButton.setOnClickListener(v -> {
            finishAndRemoveTask();
        });
    }

    private void showDialogInputSmsCode() {
        dialogInputSmsCode = new BottomSheetDialog(this);
        dialogInputSmsCode.setContentView(R.layout.dialog_input_sms_code);
        dialogInputSmsCode.setCanceledOnTouchOutside(false);
        Button buttonCancel = dialogInputSmsCode.findViewById(R.id.button_confirm);
        assert buttonCancel != null;
        dialogInputSmsCode.show();
        buttonCancel.setOnClickListener(v -> {


            TextView sms_code = dialogInputSmsCode.findViewById(R.id.sms_code);
            if(sms_code.getText() == null || sms_code.getText().toString().equals("") || sms_code.getText().toString().length() != 6) {
                sms_code.setError("Bitte 6 Ziffern eingeben.");
                return;
            }
            dialogInputSmsCode.dismiss();
            String smsCode = sms_code.getText().toString();

            try {
                CardTerminalMessage smsCodeRequestMessage = new CardTerminalMessage();
                smsCodeRequestMessage.setApdu(false);
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "confirmSMSCode");
                jsonObject.put("payload", Base64.getEncoder().encodeToString(("{\"smsCode\":\""+smsCode+"\"}").getBytes()));
                jsonArray.put(jsonObject);
                jsonArray.put(clientManager.getCardSessionId());
                smsCodeRequestMessage.setMessage(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
                clientManager.publish(smsCodeRequestMessage);

                // Start the CanInsertionActivity
                Intent intent = new Intent(PhoneInsertionActivity.this, CanInsertionActivity.class);
                startActivity(intent);
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }

            finishAndRemoveTask();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setPhoneNumber();
    }

    private void setPhoneNumber() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();
        phone_number.setText(mPhoneNumber);
    }

}
