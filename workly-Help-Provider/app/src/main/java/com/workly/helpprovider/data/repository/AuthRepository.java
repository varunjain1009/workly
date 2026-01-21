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

    @Inject
    public AuthRepository(ApiService apiService, AuthManager authManager) {
        this.apiService = apiService;
        this.authManager = authManager;
    }

    public void requestOtp(String mobileNumber, Callback<ApiResponse<Void>> callback) {
        apiService.requestOtp(new OtpRequest(mobileNumber)).enqueue(callback);
    }

    public void login(String mobileNumber, String otp, Callback<ApiResponse<AuthResponse>> callback) {
        apiService.login(new LoginRequest(mobileNumber, otp)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    authManager.saveToken(response.body().getData().getToken());
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public boolean isLoggedIn() {
        return authManager.isLoggedIn();
    }
}
