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

    @Inject
    public JobRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<List<Job>> getAvailableJobs() {
        MutableLiveData<List<Job>> jobsData = new MutableLiveData<>();
        apiService.getAvailableJobs().enqueue(new Callback<ApiResponse<List<Job>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Job>>> call, Response<ApiResponse<List<Job>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    jobsData.setValue(response.body().getData());
                } else {
                    jobsData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Job>>> call, Throwable t) {
                jobsData.setValue(null);
            }
        });
        return jobsData;
    }

    public void acceptJob(String jobId, Callback<ApiResponse<Void>> callback) {
        apiService.acceptJob(jobId).enqueue(callback);
    }

    public void completeJob(String jobId, String otp, Callback<ApiResponse<Void>> callback) {
        java.util.Map<String, String> body = java.util.Collections.singletonMap("otp", otp);
        apiService.completeJob(jobId, body).enqueue(callback);
    }
}
