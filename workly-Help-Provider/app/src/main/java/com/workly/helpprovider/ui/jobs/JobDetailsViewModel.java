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
    private final MutableLiveData<Boolean> acceptJobStatus = new MutableLiveData<>();

    @Inject
    public JobDetailsViewModel(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public LiveData<Boolean> getAcceptJobStatus() {
        return acceptJobStatus;
    }

    public void acceptJob(String jobId) {
        // Implement access to repository
        // For now, assume success or mock
        // Need to add acceptJob to JobRepository
        jobRepository.acceptJob(jobId, new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
            @Override
            public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                    retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    acceptJobStatus.setValue(true);
                } else {
                    acceptJobStatus.setValue(false);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                    Throwable t) {
                acceptJobStatus.setValue(false);
            }
        });
    }

    private final MutableLiveData<Boolean> completeJobStatus = new MutableLiveData<>();

    public LiveData<Boolean> getCompleteJobStatus() {
        return completeJobStatus;
    }

    public void completeJob(String jobId, String otp) {
        jobRepository.completeJob(jobId, otp,
                new retrofit2.Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            retrofit2.Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            completeJobStatus.setValue(true);
                        } else {
                            completeJobStatus.setValue(false);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            Throwable t) {
                        completeJobStatus.setValue(false);
                    }
                });
    }
}
