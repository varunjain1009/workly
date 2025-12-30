package com.workly.helpseeker.ui.jobs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class JobViewModel extends ViewModel {

    private final MutableLiveData<List<Job>> jobs = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ApiService apiService;

    @Inject
    public JobViewModel(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<List<Job>> getJobs() {
        return jobs;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadJobs(boolean forceRefresh) {
        if (!forceRefresh && jobs.getValue() != null) {
            return;
        }

        isLoading.setValue(true);
        apiService.getJobs().enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    jobs.setValue(response.body().getData());
                } else {
                    error.setValue("Failed to load jobs: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void addJobLocal(Job job) {
        List<Job> currentJobs = jobs.getValue();
        if (currentJobs == null) {
            currentJobs = new ArrayList<>();
        } else {
            currentJobs = new ArrayList<>(currentJobs);
        }
        currentJobs.add(0, job); // Add to top
        jobs.setValue(currentJobs);
    }
}
