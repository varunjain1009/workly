package com.workly.helpprovider.ui.jobs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.ApiService;
import com.workly.helpprovider.util.AppLogger;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class MyJobsViewModel extends ViewModel {

    private static final String TAG = "WORKLY_DEBUG";

    private final ApiService apiService;
    private final AppLogger appLogger;

    private final MutableLiveData<List<Job>> myJobs = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Inject
    public MyJobsViewModel(ApiService apiService, AppLogger appLogger) {
        this.apiService = apiService;
        this.appLogger = appLogger;
        appLogger.d(TAG, "MyJobsViewModel(Provider): Initialized — loading accepted jobs");
        loadMyJobs();
    }

    public LiveData<List<Job>> getMyJobs() {
        return myJobs;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    /** Fetch worker's jobs from the backend (newest first — server handles ordering). */
    public void loadMyJobs() {
        appLogger.d(TAG, "MyJobsViewModel(Provider): Fetching worker jobs from network");
        isLoading.setValue(true);
        apiService.getWorkerJobs().enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call,
                    Response<ApiResponse<List<Job>>> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> jobs = response.body().getData();
                    appLogger.d(TAG, "MyJobsViewModel(Provider): Loaded " + jobs.size() + " worker jobs");
                    myJobs.postValue(jobs);
                } else {
                    appLogger.e(TAG, "MyJobsViewModel(Provider): Failed to load jobs. Code: " + response.code());
                    error.postValue("Failed to load jobs: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                isLoading.postValue(false);
                appLogger.e(TAG, "MyJobsViewModel(Provider): Network error: " + t.getMessage(), t);
                error.postValue("Network error: " + t.getMessage());
            }
        });
    }
}
