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
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
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

    public void updateProfile(Profile profile) {
        isLoading.setValue(true);
        if (profile.getExpertise() != null && !profile.getExpertise().trim().isEmpty()) {
            java.util.List<String> skillsList = java.util.Arrays.asList(profile.getExpertise().split(","));
            // Trim each skill
            java.util.List<String> trimmedSkills = new java.util.ArrayList<>();
            for (String s : skillsList) {
                trimmedSkills.add(s.trim());
            }
            profile.setSkills(trimmedSkills);
        }
        profileRepository.updateProfile(profile);
        // Repository update is async but doesn't have a callback in current interface
        // for success/fail
        // assuming optimistic success for now or we could refactor repo to return
        // LiveData state
        isLoading.setValue(false);
        statusMessage.setValue("Profile updating...");
    }

    private final MutableLiveData<Double> averageRating = new MutableLiveData<>();

    public LiveData<Double> getAverageRating() {
        return averageRating;
    }

    public void fetchAverageRating(String mobileNumber) {
        if (mobileNumber == null)
            return;
        profileRepository.getWorkerAverageRating(mobileNumber,
                new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Double>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Double>> call,
                            retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Double>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            averageRating.setValue(response.body().getData());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Double>> call,
                            Throwable t) {
                        // Ignore failure for rating
                    }
                });
    }

    public void updateAvailability(boolean isAvailable) {
        isLoading.setValue(true);
        profileRepository.updateAvailability(isAvailable,
                new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful()) {
                            statusMessage.setValue("Availability updated");
                        } else {
                            statusMessage.setValue("Failed to update availability");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            Throwable t) {
                        isLoading.setValue(false);
                        statusMessage.setValue("Error updating availability");
                    }
                });
    }
}
