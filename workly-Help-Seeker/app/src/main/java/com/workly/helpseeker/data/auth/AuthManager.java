package com.workly.helpseeker.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class AuthManager {
    private static final String PREF_NAME = "workly_auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_MOBILE_NUMBER = "mobile_number";
    private final SharedPreferences sharedPreferences;
    private final com.workly.helpseeker.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public AuthManager(@ApplicationContext Context context, com.workly.helpseeker.util.AppLogger appLogger) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.appLogger = appLogger;
    }

    public void saveToken(String token) {
        appLogger.d(TAG, "Saving new authentication token.");
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        appLogger.d(TAG, "Clearing authentication token (Logging out).");
        sharedPreferences.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveMobileNumber(String mobileNumber) {
        appLogger.d(TAG, "Saving mobile number: " + mobileNumber);
        sharedPreferences.edit().putString(KEY_MOBILE_NUMBER, mobileNumber).apply();
    }

    public String getMobileNumber() {
        return sharedPreferences.getString(KEY_MOBILE_NUMBER, null);
    }
}
