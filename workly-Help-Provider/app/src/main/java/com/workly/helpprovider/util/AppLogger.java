package com.workly.helpprovider.util;

import android.util.Log;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppLogger {
    private final boolean isDebugEnabled;

    @Inject
    public AppLogger(Properties properties) {
        this.isDebugEnabled = Boolean.parseBoolean(properties.getProperty("app.debug_enabled", "false"));
    }

    public void d(String tag, String message) {
        if (isDebugEnabled) {
            Log.d(tag, message);
        }
    }

    public void e(String tag, String message) {
        if (isDebugEnabled) {
            Log.e(tag, message);
        }
    }

    public void w(String tag, String message) {
        if (isDebugEnabled) {
            Log.w(tag, message);
        }
    }

    public void e(String tag, String message, Throwable t) {
        if (isDebugEnabled) {
            Log.e(tag, message, t);
        }
    }
}
