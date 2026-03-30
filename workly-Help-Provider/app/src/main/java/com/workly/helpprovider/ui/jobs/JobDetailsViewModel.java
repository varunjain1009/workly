package com.workly.helpprovider.ui.jobs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.workly.helpprovider.data.repository.JobRepository;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class JobDetailsViewModel extends ViewModel {

    private final JobRepository jobRepository;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private final MutableLiveData<Boolean> acceptJobStatus = new MutableLiveData<>();
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public JobDetailsViewModel(JobRepository jobRepository, com.workly.helpprovider.util.AppLogger appLogger) {
        this.jobRepository = jobRepository;
        this.appLogger = appLogger;
    }

    public LiveData<Boolean> getAcceptJobStatus() {
        return acceptJobStatus;
    }

    public void acceptJob(String jobId) {
        appLogger.d(TAG, "JobDetailsViewModel(Provider): [ENTER] acceptJob - jobId: " + jobId);
        jobRepository.acceptJob(jobId, new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
            @Override
            public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                    retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    appLogger.d(TAG, "JobDetailsViewModel(Provider): Job " + jobId + " accepted successfully");
                    jobRepository.removeJobLocal(jobId);
                    acceptJobStatus.setValue(true);
                } else {
                    appLogger.e(TAG, "JobDetailsViewModel(Provider): Accept failed. Code: " + response.code());
                    acceptJobStatus.setValue(false);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                    Throwable t) {
                appLogger.e(TAG, "JobDetailsViewModel(Provider): Network error accepting job: " + t.getMessage(), t);
                acceptJobStatus.setValue(false);
            }
        });
    }

    private final MutableLiveData<Boolean> completeJobStatus = new MutableLiveData<>();

    public LiveData<Boolean> getCompleteJobStatus() {
        return completeJobStatus;
    }

    public void completeJob(String jobId, String otp) {
        appLogger.d(TAG, "JobDetailsViewModel(Provider): [ENTER] completeJob - jobId: " + jobId);
        jobRepository.completeJob(jobId, otp,
                new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            appLogger.d(TAG, "JobDetailsViewModel(Provider): Job " + jobId + " completed successfully");
                            completeJobStatus.setValue(true);
                        } else {
                            appLogger.e(TAG, "JobDetailsViewModel(Provider): Complete failed. Code: " + response.code());
                            completeJobStatus.setValue(false);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            Throwable t) {
                        appLogger.e(TAG, "JobDetailsViewModel(Provider): Network error completing job: " + t.getMessage(), t);
                        completeJobStatus.setValue(false);
                    }
                });
    }

    private final MutableLiveData<Boolean> cancelJobStatus = new MutableLiveData<>();

    public LiveData<Boolean> getCancelJobStatus() {
        return cancelJobStatus;
    }

    public void cancelJob(String jobId) {
        appLogger.d(TAG, "JobDetailsViewModel(Provider): [ENTER] cancelJob - jobId: " + jobId);
        jobRepository.cancelJob(jobId, new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
            @Override
            public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                    retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    appLogger.d(TAG, "JobDetailsViewModel(Provider): Job " + jobId + " cancelled successfully");
                    cancelJobStatus.setValue(true);
                } else {
                    appLogger.e(TAG, "JobDetailsViewModel(Provider): Cancel failed. Code: " + response.code());
                    cancelJobStatus.setValue(false);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                    Throwable t) {
                appLogger.e(TAG, "JobDetailsViewModel(Provider): Network error cancelling job: " + t.getMessage(), t);
                cancelJobStatus.setValue(false);
            }
        });
    }
}
