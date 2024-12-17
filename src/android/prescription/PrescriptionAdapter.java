package com.appdinx.cardlink.prescription;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import com.appdinx.cardlink.R;

public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.PrescriptionViewHolder> {

    private final List<Prescription> prescriptions;
    private final OnItemClickListener listener;

    public PrescriptionAdapter(List<Prescription> prescriptions, OnItemClickListener listener) {
        this.prescriptions = prescriptions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription_card, parent, false);
        return new PrescriptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PrescriptionViewHolder holder, int position) {
        Prescription prescription = prescriptions.get(position);
        holder.bind(prescription, listener);
    }

    @Override
    public int getItemCount() {
        return prescriptions.size();
    }

    static class PrescriptionViewHolder extends RecyclerView.ViewHolder {
        TextView prescriptionIdTextView;
        TextView accessCodeTextView;
        ImageView checkmarkImageView;

        PrescriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            prescriptionIdTextView = itemView.findViewById(R.id.prescriptionIdTextView);
            accessCodeTextView = itemView.findViewById(R.id.accessCodeTextView);
            checkmarkImageView = itemView.findViewById(R.id.checkmarkImageView);
            // Bind more views as needed
        }

        void bind(Prescription prescription, OnItemClickListener listener) {
            prescriptionIdTextView.setText(prescription.getPrescriptionId());
            accessCodeTextView.setText(prescription.getAccessCode());

            itemView.setOnClickListener(v -> {
                prescription.setSelected(!prescription.isSelected());
                updateSelectedPrescriptionsBorders(prescription.isSelected());
                updateCheckmark(prescription.isSelected());
                if (listener != null) {
                    listener.onItemClick(prescription);
                }
            });
        }

        void updateSelectedPrescriptionsBorders(boolean isSelected) {
            if (isSelected) {
                ((MaterialCardView)itemView).setStrokeColor(Color.parseColor("#00463D"));
                ((MaterialCardView)itemView).setStrokeWidth(4);
            } else {
                ((MaterialCardView)itemView).setStrokeWidth(0);
            }
        }

        void updateCheckmark(boolean isSelected) {
            if (isSelected) {
                checkmarkImageView.setImageResource(R.drawable.circle_checkmark);
            } else {
                checkmarkImageView.setImageResource(R.drawable.circle_empty);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Prescription prescription);
    }

}
