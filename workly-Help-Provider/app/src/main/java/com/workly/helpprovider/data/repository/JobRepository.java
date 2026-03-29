package com.workly.helpprovider.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.ApiService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class JobRepository {

    private final ApiService apiService;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public JobRepository(ApiService apiService, com.workly.helpprovider.util.AppLogger appLogger) {
        this.apiService = apiService;
        this.appLogger = appLogger;
    }

    public LiveData<List<Job>> getAvailableJobs() {
        appLogger.d(TAG, "JobRepository: Fetching available jobs from network.");
        MutableLiveData<List<Job>> jobsData = new MutableLiveData<>();
        apiService.getAvailableJobs().enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    appLogger.d(TAG, "JobRepository: Successfully retrieved available jobs list.");
                    jobsData.setValue(response.body().getData());
                } else {
                    appLogger.e(TAG, "JobRepository: Failed to retrieve jobs. Status code: " + response.code());
                    jobsData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                appLogger.e(TAG, "JobRepository: Network error fetching available jobs: " + t.getMessage(), t);
                jobsData.setValue(null);
            }
        });
        return jobsData;
    }

    public void acceptJob(String jobId, Callback<ApiResponse<Void>> callback) {
        appLogger.d(TAG, "JobRepository: Accepting job assignment for ID: " + jobId);
        apiService.acceptJob(jobId).enqueue(callback);
    }

    public void completeJob(String jobId, String otp, Callback<ApiResponse<Void>> callback) {
        appLogger.d(TAG, "JobRepository: Completing job " + jobId + " with OTP input tracking payload.");
        java.util.Map<String, String> body = java.util.Collections.singletonMap("otp", otp);
        apiService.completeJob(jobId, body).enqueue(callback);
    }
}
