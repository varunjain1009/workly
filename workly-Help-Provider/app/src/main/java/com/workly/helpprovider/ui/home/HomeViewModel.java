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
    private final MutableLiveData<Boolean> availabilityUpdated = new MutableLiveData<>();
    private LiveData<java.util.List<com.workly.helpprovider.data.model.Job>> availableJobs;

    @Inject
    public HomeViewModel(ProfileRepository profileRepository, JobRepository jobRepository) {
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
    }

    public LiveData<Boolean> getAvailabilityUpdated() {
        return availabilityUpdated;
    }

    public LiveData<java.util.List<com.workly.helpprovider.data.model.Job>> getAvailableJobs() {
        if (availableJobs == null) {
            availableJobs = jobRepository.getAvailableJobs();
        }
        return availableJobs;
    }

    public void refreshJobs() {
        availableJobs = jobRepository.getAvailableJobs();
    }

    public void setAvailability(boolean isAvailable) {
        profileRepository.updateAvailability(isAvailable, new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    availabilityUpdated.setValue(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // handle error
            }
        });
    }
}
