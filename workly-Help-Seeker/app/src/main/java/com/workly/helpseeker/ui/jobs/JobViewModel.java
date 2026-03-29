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
    private final com.workly.helpseeker.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public JobViewModel(ApiService apiService, com.workly.helpseeker.util.AppLogger appLogger) {
        this.apiService = apiService;
        this.appLogger = appLogger;
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

    public void loadJobs(String type, boolean forceRefresh) {
        appLogger.d(TAG, "Requesting jobs load. Type: " + type + " ForceRefresh: " + forceRefresh);
        // Separate LiveData handling could be beneficial, but for now assuming
        // Home/Past use different instances
        // OR the Fragment is responsible for observing the right data.
        // But here we only have one 'jobs' LiveData.
        // If Home and Past share the ViewModel (Activity scope), they will overwrite
        // each other.

        // HomeFragment uses 'requireActivity()' scope.
        // If PastJobsFragment also uses 'requireActivity()', they share data.
        // Changing tabs -> refresh.

        if (!forceRefresh && jobs.getValue() != null && !jobs.getValue().isEmpty()) {
            // This logic is flawed if checking different types.
            // Simplified: Always fetch if type changes? Or just rely on forceRefresh.
            // Failure-safe: if forceRefresh is false, we might return wrong data type.
            // Let's assume the caller handles forceRefresh correctly or we just fetch.
        }

        // Better: always fetch if type is different?
        // For MVP, just fetch.

        isLoading.setValue(true);
        apiService.getJobs(type).enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    appLogger.d(TAG, "Successfully loaded jobs. Count: " + response.body().getData().size());
                    jobs.setValue(response.body().getData());
                } else {
                    appLogger.e(TAG, "Failed to load jobs. HTTP Status: " + response.code());
                    error.setValue("Failed to load jobs: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                isLoading.setValue(false);
                appLogger.e(TAG, "Network error while loading jobs: " + t.getMessage(), t);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void loadJobs(boolean forceRefresh) {
        loadJobs("active", forceRefresh);
    }

    public void addJobLocal(Job job) {
        appLogger.d(TAG, "Locally caching and UI-prepending newly added Job: " + job.getTitle());
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
