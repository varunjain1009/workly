package com.workly.helpprovider.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.AuthResponse;
import com.workly.helpprovider.data.repository.AuthRepository;
import com.workly.helpprovider.data.repository.ProfileRepository;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository;
    private final AuthManager authManager;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";

    private final MutableLiveData<Boolean> isSessionValid = new MutableLiveData<>();
    private final MutableLiveData<Boolean> availabilityUpdated = new MutableLiveData<>();

    @Inject
    public MainViewModel(AuthRepository authRepository, ProfileRepository profileRepository, AuthManager authManager, com.workly.helpprovider.util.AppLogger appLogger) {
        this.authRepository = authRepository;
        this.profileRepository = profileRepository;
        this.authManager = authManager;
        this.appLogger = appLogger;
        appLogger.d(TAG, "MainViewModel(Provider): Initialized");
    }

    public LiveData<Boolean> getIsSessionValid() {
        return isSessionValid;
    }

    public LiveData<Boolean> getAvailabilityUpdated() {
        return availabilityUpdated;
    }

    public void checkSession() {
        appLogger.d(TAG, "MainViewModel(Provider): Checking session validity");
        if (!authManager.isLoggedIn()) {
            appLogger.d(TAG, "MainViewModel(Provider): No active session found");
            isSessionValid.setValue(false);
            return;
        }
        refreshSession();
    }

    private void refreshSession() {
        appLogger.d(TAG, "MainViewModel(Provider): Refreshing session state");
        isSessionValid.setValue(authManager.isLoggedIn());
    }

    public void setAvailability(boolean isAvailable) {
        appLogger.d(TAG, "MainViewModel(Provider): Setting availability to " + isAvailable);
        profileRepository.updateAvailability(isAvailable, new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    appLogger.d(TAG, "MainViewModel(Provider): Availability updated successfully");
                    availabilityUpdated.setValue(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                appLogger.e(TAG, "MainViewModel(Provider): Availability update failed: " + t.getMessage(), t);
            }
        });
    }

    public void logout() {
        appLogger.d(TAG, "MainViewModel(Provider): Logging out - clearing session");
        authManager.clearSession();
        isSessionValid.setValue(false);
    }
}
