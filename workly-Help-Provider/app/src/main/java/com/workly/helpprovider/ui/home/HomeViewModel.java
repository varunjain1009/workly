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
        profileRepository.updateAvailability(isAvailable, new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    availabilityUpdated.setValue(true);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // handle error
            }
        });
    }
}
