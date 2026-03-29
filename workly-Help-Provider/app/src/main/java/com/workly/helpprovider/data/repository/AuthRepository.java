package com.workly.helpprovider.data.repository;

import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.ApiService;
import com.workly.helpprovider.data.remote.AuthResponse;
import com.workly.helpprovider.data.remote.LoginRequest;
import com.workly.helpprovider.data.remote.OtpRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class AuthRepository {

    private final ApiService apiService;
    private final AuthManager authManager;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public AuthRepository(ApiService apiService, AuthManager authManager, com.workly.helpprovider.util.AppLogger appLogger) {
        this.apiService = apiService;
        this.authManager = authManager;
        this.appLogger = appLogger;
    }

    public void requestOtp(String mobileNumber, Callback<ApiResponse<Void>> callback) {
        appLogger.d(TAG, "AuthRepository: Initiating requestOtp API call for " + mobileNumber);
        apiService.requestOtp(new OtpRequest(mobileNumber)).enqueue(callback);
    }

    public void login(String mobileNumber, String otp, Callback<ApiResponse<AuthResponse>> callback) {
        appLogger.d(TAG, "AuthRepository: Intercepting AuthRepository login for processing OTP validation.");
        apiService.login(new LoginRequest(mobileNumber, otp)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    appLogger.d(TAG, "AuthRepository: Received valid successful Session Token from upstream API payload.");
                    authManager.saveToken(response.body().getData().getToken());
                } else {
                    appLogger.e(TAG, "AuthRepository: Upstream responded negatively to session auth payload creation. HTTP Code " + response.code());
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                appLogger.e(TAG, "AuthRepository: Raw Network or DNS timeout error on login sequence call: " + t.getMessage(), t);
                callback.onFailure(call, t);
            }
        });
    }

    public boolean isLoggedIn() {
        return authManager.isLoggedIn();
    }
}
