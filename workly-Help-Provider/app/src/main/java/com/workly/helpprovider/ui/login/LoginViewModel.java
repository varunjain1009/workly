package com.workly.helpprovider.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.AuthResponse;
import com.workly.helpprovider.data.repository.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> otpSent = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    @Inject
    public LoginViewModel(AuthRepository authRepository, com.workly.helpprovider.util.AppLogger appLogger) {
        this.authRepository = authRepository;
        this.appLogger = appLogger;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getOtpSent() {
        return otpSent;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public void requestOtp(String mobileNumber) {
        appLogger.d(TAG, "LoginViewModel: Requesting OTP for " + mobileNumber);
        isLoading.setValue(true);
        authRepository.requestOtp(mobileNumber, new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    appLogger.d(TAG, "OTP successfully requested via Auth Repository.");
                    otpSent.setValue(true);
                } else {
                    appLogger.e(TAG, "Failed to send OTP via API: " + response.message());
                    error.setValue("Failed to send OTP: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                isLoading.setValue(false);
                appLogger.e(TAG, "Network error during OTP request: " + t.getMessage(), t);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void verifyOtp(String mobileNumber, String otp) {
        appLogger.d(TAG, "LoginViewModel: Verifying OTP for " + mobileNumber);
        isLoading.setValue(true);
        authRepository.login(mobileNumber, otp, new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    appLogger.d(TAG, "OTP successfully verified, login successful via Auth Repository.");
                    loginSuccess.setValue(true);
                } else {
                    appLogger.e(TAG, "Failed login verification via API: " + response.message());
                    error.setValue("Login failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                isLoading.setValue(false);
                appLogger.e(TAG, "Network error during Login verification: " + t.getMessage(), t);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
