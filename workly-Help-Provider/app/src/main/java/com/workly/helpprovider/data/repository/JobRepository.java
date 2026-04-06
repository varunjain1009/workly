package com.workly.helpprovider.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.remote.ApiResponse;
import com.workly.helpprovider.data.remote.ApiService;
import com.workly.helpprovider.data.config.ConfigManager;

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
    // Default 5 min; overridden at runtime from remote config
    private static final long DEFAULT_STALENESS_MS = 5 * 60 * 1000L;

    private final ApiService apiService;
    private final ConfigManager configManager;
    private final com.workly.helpprovider.util.AppLogger appLogger;

    // Persistent LiveData — survives across fragment recreations
    private final MutableLiveData<List<Job>> availableJobsData = new MutableLiveData<>();
    // Emits a human-readable throttle message when the user tries to refresh too early
    private final MutableLiveData<String> throttleMessage = new MutableLiveData<>();
    private long lastFetchTime = 0;

    @Inject
    public JobRepository(ApiService apiService, ConfigManager configManager, com.workly.helpprovider.util.AppLogger appLogger) {
        this.apiService = apiService;
        this.configManager = configManager;
        this.appLogger = appLogger;
    }

    /**
     * Returns persistent LiveData for available jobs. Use refreshAvailableJobs() to trigger updates.
     */
    public LiveData<List<Job>> getAvailableJobs() {
        return availableJobsData;
    }

    /** Emits a throttle warning message if the user tries to refresh too early. */
    public LiveData<String> getThrottleMessage() {
        return throttleMessage;
    }

    public void clearThrottleMessage() {
        throttleMessage.setValue(null);
    }

    /**
     * Force-fetches available jobs, bypassing all throttle / staleness checks.
     * Use after a location push so the server's fresh coordinates are always used.
     */
    public void forceRefreshAvailableJobs() {
        appLogger.d(TAG, "JobRepository: forceRefreshAvailableJobs() — bypassing throttle");
        fetchAvailableJobs();
    }

    /**
     * Refreshes available jobs from network, respecting staleness threshold.
     */
    public void refreshAvailableJobs(boolean isManualPull) {
        long intervalMs = getThrottleIntervalMs();
        
        if (isManualPull) {
            // For manual pull, allow fetch unless it's within 10 seconds (spam prevention)
            if (lastFetchTime > 0 && System.currentTimeMillis() - lastFetchTime < 10000) {
                appLogger.d(TAG, "JobRepository: Throttled manual pull.");
                throttleMessage.postValue("Refreshing too fast. Please wait a moment.");
                return;
            }
        } else {
            // If the cache is still fresh based on our interval Config for background automated fetches
            if (isCacheFresh(intervalMs)) {
                appLogger.d(TAG, "JobRepository: Cache fresh, skipping background auto-refresh.");
                return;
            }
        }

        appLogger.d(TAG, "JobRepository: Fetching available jobs from network."
                + (isManualPull ? " (manual pull)" : " (stale/missing)"));
        fetchAvailableJobs();
    }

    private void fetchAvailableJobs() {
        apiService.getAvailableJobs().enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> jobs = response.body().getData();
                    appLogger.d(TAG, "JobRepository: Successfully retrieved " + jobs.size() + " available jobs.");
                    availableJobsData.postValue(jobs);
                    lastFetchTime = System.currentTimeMillis();
                } else {
                    appLogger.e(TAG, "JobRepository: Failed to retrieve jobs. Status code: " + response.code());
                    List<Job> current = availableJobsData.getValue();
                    availableJobsData.postValue(current != null ? current : new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                appLogger.e(TAG, "JobRepository: Network error fetching available jobs: " + t.getMessage(), t);
                List<Job> current = availableJobsData.getValue();
                availableJobsData.postValue(current != null ? current : new ArrayList<>());
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

    private long getThrottleIntervalMs() {
        if (configManager != null && configManager.getConfig() != null) {
            int minutes = configManager.getConfig().getJobRefreshIntervalMinutes();
            if (minutes > 0) return minutes * 60 * 1000L;
        }
        return DEFAULT_STALENESS_MS;
    }

    private boolean isCacheFresh(long intervalMs) {
        if (lastFetchTime == 0) return false;
        if (availableJobsData.getValue() == null) return false;
        return (System.currentTimeMillis() - lastFetchTime) < intervalMs;
    }
}

