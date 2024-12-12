package com.appdinx.cardlink.exception;

import androidx.annotation.NonNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @Override
    public void uncaughtException(@NonNull Thread thread, Throwable throwable) {
        log.log(Level.WARNING, "Uncaught exception: ", throwable.getMessage());
    }
}

