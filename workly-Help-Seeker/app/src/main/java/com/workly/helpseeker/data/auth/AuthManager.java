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

    @Inject
    public AuthManager(@ApplicationContext Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveMobileNumber(String mobileNumber) {
        sharedPreferences.edit().putString(KEY_MOBILE_NUMBER, mobileNumber).apply();
    }

    public String getMobileNumber() {
        return sharedPreferences.getString(KEY_MOBILE_NUMBER, null);
    }
}
