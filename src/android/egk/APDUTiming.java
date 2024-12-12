package com.appdinx.cardlink.egk;

import androidx.annotation.NonNull;

public class APDUTiming {
    private final String apduPrefix;
    private final long start;
    private long end;

    public APDUTiming(String apduPrefix) {
        if (apduPrefix == null) {
            throw new IllegalArgumentException("apduPrefix cannot be null");
        }
        this.apduPrefix = apduPrefix;
        this.start = System.currentTimeMillis();
    }

    public void stop() {
        end = System.currentTimeMillis();
    }

    public long getDifference() {
        return end - start;
    }

    public String getApduPrefix() {
        return apduPrefix;
    }

    @NonNull
    @Override
    public String toString() {
        return apduPrefix + "," + start + "," + end;
    }
}