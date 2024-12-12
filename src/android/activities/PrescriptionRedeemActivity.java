package com.appdinx.cardlink.activities;

import android.app.AlertDialog;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

import com.appdinx.cardlink.prescription.Prescription;
import com.appdinx.cardlink.prescription.PrescriptionAdapter;
import com.appdinx.cardlink.R;

public class PrescriptionRedeemActivity extends AppCompatActivity implements PrescriptionAdapter.OnItemClickListener {

    private static final Logger log = Logger.getLogger(PrescriptionRedeemActivity.class.getName());
    public static List<Prescription> prescriptions = new ArrayList<>();
    private List<Prescription> selectedPrescriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_prescription_redeem);
        selectedPrescriptions = new ArrayList<>();

        setupPreviousButton();
        setupAddToBasketButton();

        showPrescriptions();

    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onItemClick(Prescription prescription) {
        // Toggle the selection state of the clicked prescription
        if (selectedPrescriptions.contains(prescription)) {
            selectedPrescriptions.remove(prescription);
        } else {
            selectedPrescriptions.add(prescription);
        }
    }

    private void showPrescriptions() {
        // Set up RecyclerView and adapter
        RecyclerView recyclerView = findViewById(R.id.prescriptionRecyclerView);
        PrescriptionAdapter prescriptionAdapter = new PrescriptionAdapter(prescriptions, PrescriptionRedeemActivity.this);
        recyclerView.setAdapter(prescriptionAdapter);

        // Set the layout manager for RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(PrescriptionRedeemActivity.this));
    }


    private void setupPreviousButton() {
        Button previousButton = findViewById(R.id.previous_button);
        previousButton.setOnClickListener(v -> {
            finishAndRemoveTask();
        });
    }

    private void setupAddToBasketButton() {
        Button addToBasketButton = findViewById(R.id.continue_button);
        addToBasketButton.setOnClickListener(v -> {
            int numPrescriptions = selectedPrescriptions.size();
            String message = "Im Warenkorb sind " + numPrescriptions + " Rezepte vorhanden";

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Perform any additional actions upon dialog button click if needed
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        });
    }
}
