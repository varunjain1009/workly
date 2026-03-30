package com.workly.helpprovider.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.ApiService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class JobRepository {

    private static final String TAG = "WORKLY_DEBUG";
    private static final long STALENESS_THRESHOLD_MS = 60_000; // 60 seconds

    private final ApiService apiService;
    private final com.workly.helpprovider.util.AppLogger appLogger;

    // Persistent LiveData — survives across fragment recreations
    private final MutableLiveData<List<Job>> availableJobsData = new MutableLiveData<>();
    private long lastFetchTime = 0;

    @Inject
    public JobRepository(ApiService apiService, com.workly.helpprovider.util.AppLogger appLogger) {
        this.apiService = apiService;
        this.appLogger = appLogger;
    }

    /**
     * Returns persistent LiveData for available jobs. Use refreshAvailableJobs() to trigger updates.
     */
    public LiveData<List<Job>> getAvailableJobs() {
        return availableJobsData;
    }

    /**
     * Refreshes available jobs from network, respecting staleness threshold.
     * @param force If true, ignores staleness and always fetches.
     */
    public void refreshAvailableJobs(boolean force) {
        if (!force && isCacheFresh()) {
            long ageSeconds = (System.currentTimeMillis() - lastFetchTime) / 1000;
            appLogger.d(TAG, "JobRepository: Skipping fetch — cache is fresh (age: " + ageSeconds + "s)");
            return;
        }

        appLogger.d(TAG, "JobRepository: Fetching available jobs from network."
                + (force ? " (forced)" : " (stale/missing)"));
        apiService.getAvailableJobs().enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> jobs = response.body().getData();
                    appLogger.d(TAG, "JobRepository: Successfully retrieved " + jobs.size() + " available jobs.");
                    availableJobsData.setValue(jobs);
                    lastFetchTime = System.currentTimeMillis();
                } else {
                    appLogger.e(TAG, "JobRepository: Failed to retrieve jobs. Status code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                appLogger.e(TAG, "JobRepository: Network error fetching available jobs: " + t.getMessage(), t);
            }
        });
    }

    /**
     * Removes a job from the local cached list (e.g. after accepting it).
     * Avoids a full network re-fetch.
     */
    public void removeJobLocal(String jobId) {
        List<Job> current = availableJobsData.getValue();
        if (current == null) return;

        List<Job> updated = new ArrayList<>();
        for (Job job : current) {
            if (!jobId.equals(job.getId())) {
                updated.add(job);
            }
        }
        appLogger.d(TAG, "JobRepository: Removed job " + jobId + " from local cache. Remaining: " + updated.size());
        availableJobsData.setValue(updated);
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

    public void cancelJob(String jobId, Callback<ApiResponse<Void>> callback) {
        appLogger.d(TAG, "JobRepository: Cancelling job: " + jobId);
        apiService.updateJobStatus(jobId, "CANCELLED").enqueue(callback);
    }

    private boolean isCacheFresh() {
        if (lastFetchTime == 0) return false;
        if (availableJobsData.getValue() == null) return false;
        return (System.currentTimeMillis() - lastFetchTime) < STALENESS_THRESHOLD_MS;
    }
}

