package com.appdinx.cardlink.prescription;

public class Prescription {
    private final String prescriptionId;
    private final String accessCode;
    private boolean selected;

    public Prescription(String prescriptionId, String accessCode) {
        this.prescriptionId = prescriptionId;
        this.accessCode = accessCode;
        this.selected = false; // Initialize as unselected
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}







