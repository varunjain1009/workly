package com.workly.helpseeker.ui.jobs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class JobViewModel extends ViewModel {

    private static final String TAG = "WORKLY_DEBUG";
    private static final long STALENESS_THRESHOLD_MS = 60_000; // 60 seconds

    private final MutableLiveData<List<Job>> jobs = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ApiService apiService;
    private final com.workly.helpseeker.util.AppLogger appLogger;

    // Per-type cache: "active" -> list, "past" -> list
    private final Map<String, List<Job>> jobCache = new HashMap<>();
    private final Map<String, Long> lastFetchTime = new HashMap<>();
    private String currentType = "active";

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
        currentType = type;

        // Check if we can serve from cache
        if (!forceRefresh && isCacheFresh(type)) {
            List<Job> cached = jobCache.get(type);
            appLogger.d(TAG, "Serving " + type + " jobs from cache. Count: "
                    + (cached != null ? cached.size() : 0)
                    + " (age: " + getCacheAgeSeconds(type) + "s)");
            jobs.setValue(cached);
            return;
        }

        appLogger.d(TAG, "Fetching " + type + " jobs from network."
                + (forceRefresh ? " (forced)" : " (stale/missing)"));
        isLoading.setValue(true);
        apiService.getJobs(type).enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> data = response.body().getData();
                    appLogger.d(TAG, "Successfully loaded " + type + " jobs. Count: " + data.size());
                    jobCache.put(type, data);
                    lastFetchTime.put(type, System.currentTimeMillis());
                    // Only update LiveData if user is still viewing this type
                    if (type.equals(currentType)) {
                        jobs.setValue(data);
                    }
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

    /**
     * Replace an existing job in the cache with an updated version (e.g. after reschedule).
     * Emits the refreshed list immediately so the UI reflects the change without a network call.
     */
    public void updateJobLocal(Job updatedJob) {
        appLogger.d(TAG, "Locally updating job in cache: " + updatedJob.getId());
        for (Map.Entry<String, List<Job>> entry : jobCache.entrySet()) {
            List<Job> cached = new ArrayList<>(entry.getValue());
            for (int i = 0; i < cached.size(); i++) {
                if (updatedJob.getId() != null && updatedJob.getId().equals(cached.get(i).getId())) {
                    cached.set(i, updatedJob);
                    jobCache.put(entry.getKey(), cached);
                    if (entry.getKey().equals(currentType)) {
                        jobs.setValue(cached);
                    }
                    return;
                }
            }
        }
    }

    public void addJobLocal(Job job) {
        appLogger.d(TAG, "Locally adding new job to active cache: " + job.getTitle());
        List<Job> activeJobs = jobCache.get("active");
        if (activeJobs == null) {
            activeJobs = new ArrayList<>();
        } else {
            activeJobs = new ArrayList<>(activeJobs);
        }
        activeJobs.add(0, job); // Add to top
        jobCache.put("active", activeJobs);
        lastFetchTime.put("active", System.currentTimeMillis()); // Mark as fresh

        // Update LiveData if currently viewing active jobs
        if ("active".equals(currentType)) {
            jobs.setValue(activeJobs);
        }
    }

    /**
     * Invalidate cache for a specific type, forcing next load to hit network.
     */
    public void invalidateCache(String type) {
        lastFetchTime.remove(type);
        jobCache.remove(type);
    }

    private boolean isCacheFresh(String type) {
        Long fetchTime = lastFetchTime.get(type);
        if (fetchTime == null) return false;
        List<Job> cached = jobCache.get(type);
        if (cached == null) return false;
        return (System.currentTimeMillis() - fetchTime) < STALENESS_THRESHOLD_MS;
    }

    private long getCacheAgeSeconds(String type) {
        Long fetchTime = lastFetchTime.get(type);
        if (fetchTime == null) return -1;
        return (System.currentTimeMillis() - fetchTime) / 1000;
    }
}
