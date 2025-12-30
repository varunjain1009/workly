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

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> otpSent = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
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
        isLoading.setValue(true);
        authRepository.requestOtp(mobileNumber, new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    otpSent.setValue(true);
                } else {
                    error.setValue("Failed to send OTP: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void verifyOtp(String mobileNumber, String otp) {
        isLoading.setValue(true);
        authRepository.login(mobileNumber, otp, new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    loginSuccess.setValue(true);
                } else {
                    error.setValue("Login failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
