package com.appdinx.cardlink.activities;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.util.logging.Logger;

import com.appdinx.cardlink.R;

public class PrescriptionRetrieveActivity extends AppCompatActivity {

    private static final Logger log = Logger.getLogger(PrescriptionRetrieveActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_prescription_retrieve);
        Button retrieveButton = findViewById(R.id.continue_button);

//        Button outputRuntimeButton = findViewById(R.id.outputRuntime);

//        outputRuntimeButton.setOnClickListener(v -> {
//            Log.d("OutputRuntime", CardCommandHandler.outputTimings());
//        });

        setupPreviousButton();

        retrieveButton.setOnClickListener(v -> {
            Intent intent = new Intent(PrescriptionRetrieveActivity.this, PrescriptionRedeemActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableReaderMode(this);
    }

    private void setupPreviousButton() {
        Button previousButton = findViewById(R.id.previous_button);
        previousButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CanInsertionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

}
