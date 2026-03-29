package com.workly.helpprovider.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpprovider.data.repository.ProfileRepository;
import com.workly.helpprovider.data.repository.JobRepository;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.workly.helpprovider.data.remote.ApiResponse;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private final MutableLiveData<Boolean> availabilityUpdated = new MutableLiveData<>();
    private LiveData<java.util.List<com.workly.helpprovider.data.model.Job>> availableJobs;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public HomeViewModel(ProfileRepository profileRepository, JobRepository jobRepository, com.workly.helpprovider.util.AppLogger appLogger) {
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.appLogger = appLogger;
        appLogger.d(TAG, "HomeViewModel(Provider): Initialized");
    }

    public LiveData<Boolean> getAvailabilityUpdated() {
        return availabilityUpdated;
    }

    public LiveData<java.util.List<com.workly.helpprovider.data.model.Job>> getAvailableJobs() {
        if (availableJobs == null) {
            appLogger.d(TAG, "HomeViewModel(Provider): First-time fetch of available jobs");
            availableJobs = jobRepository.getAvailableJobs();
        }
        return availableJobs;
    }

    public void refreshJobs() {
        appLogger.d(TAG, "HomeViewModel(Provider): Force-refreshing available jobs");
        availableJobs = jobRepository.getAvailableJobs();
    }

    public void setAvailability(boolean isAvailable) {
        appLogger.d(TAG, "HomeViewModel(Provider): Setting availability to " + isAvailable);
        profileRepository.updateAvailability(isAvailable, new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    appLogger.d(TAG, "HomeViewModel(Provider): Availability updated successfully");
                    availabilityUpdated.setValue(true);
                } else {
                    appLogger.e(TAG, "HomeViewModel(Provider): Availability update failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                appLogger.e(TAG, "HomeViewModel(Provider): Network error updating availability: " + t.getMessage(), t);
            }
        });
    }
}
