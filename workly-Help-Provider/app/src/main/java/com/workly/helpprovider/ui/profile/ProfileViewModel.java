package com.workly.helpprovider.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.workly.helpprovider.data.model.Profile;
import com.workly.helpprovider.data.repository.ProfileRepository;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, com.workly.helpprovider.util.AppLogger appLogger) {
        this.profileRepository = profileRepository;
        this.appLogger = appLogger;
        appLogger.d(TAG, "ProfileViewModel(Provider): Initialized");
    }

    public LiveData<Profile> getProfile() {
        return profileRepository.getProfile();
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void clearStatusMessage() {
        statusMessage.setValue(null);
    }

    public void updateProfile(Profile profile) {
        appLogger.d(TAG, "ProfileViewModel(Provider): [ENTER] updateProfile - name: " + profile.getName());
        isLoading.setValue(true);
        if (profile.getExpertise() != null && !profile.getExpertise().trim().isEmpty()) {
            java.util.List<String> skillsList = java.util.Arrays.asList(profile.getExpertise().split(","));
            java.util.List<String> trimmedSkills = new java.util.ArrayList<>();
            for (String s : skillsList) {
                trimmedSkills.add(s.trim());
            }
            profile.setSkills(trimmedSkills);
            appLogger.d(TAG, "ProfileViewModel(Provider): Parsed " + trimmedSkills.size() + " skills from expertise string");
        }
        profileRepository.updateProfile(profile);
        isLoading.setValue(false);
        statusMessage.setValue("Profile updating...");
        appLogger.d(TAG, "ProfileViewModel(Provider): [EXIT] updateProfile dispatched");
    }

    private final MutableLiveData<Double> averageRating = new MutableLiveData<>();
    private boolean isRatingFetched = false;

    public LiveData<Double> getAverageRating() {
        return averageRating;
    }

    public void fetchAverageRating(String mobileNumber) {
        if (mobileNumber == null || isRatingFetched) {
            return;
        }

        appLogger.d(TAG, "ProfileViewModel(Provider): Fetching average rating for " + mobileNumber);
        profileRepository.getWorkerAverageRating(mobileNumber,
                new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Double>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Double>> call,
                            retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Double>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            appLogger.d(TAG, "ProfileViewModel(Provider): Average rating received: " + response.body().getData());
                            averageRating.setValue(response.body().getData());
                            isRatingFetched = true;
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Double>> call,
                            Throwable t) {
                        appLogger.e(TAG, "ProfileViewModel(Provider): Failed to fetch rating: " + t.getMessage(), t);
                    }
                });
    }

    public void updateAvailability(boolean isAvailable) {
        appLogger.d(TAG, "ProfileViewModel(Provider): Updating availability to " + isAvailable);
        isLoading.setValue(true);
        profileRepository.updateAvailability(isAvailable,
                new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful()) {
                            appLogger.d(TAG, "ProfileViewModel(Provider): Availability updated OK");
                            statusMessage.setValue("Availability updated");
                        } else {
                            appLogger.e(TAG, "ProfileViewModel(Provider): Availability update failed. Code: " + response.code());
                            statusMessage.setValue("Failed to update availability");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            Throwable t) {
                        isLoading.setValue(false);
                        appLogger.e(TAG, "ProfileViewModel(Provider): Availability network error: " + t.getMessage(), t);
                        statusMessage.setValue("Error updating availability");
                    }
                });
    }
}
