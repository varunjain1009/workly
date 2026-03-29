package com.workly.helpseeker.ui.jobs;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.data.model.JobStatus;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.databinding.FragmentJobDetailsBinding;

import java.util.Properties;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class JobDetailsFragment extends Fragment {

    private static final String TAG = "WORKLY_DEBUG";
    private FragmentJobDetailsBinding binding;
    private Job job;

    @Inject
    ApiService apiService;

    @Inject
    com.workly.helpseeker.util.AppLogger appLogger;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentJobDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnSubmitReview.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // For now, we assume the job is passed via arguments or a ViewModel
        // Since we are migrating, we'll implement the logic to display details
        if (getArguments() != null) {
            job = (Job) getArguments().getSerializable("job");
            displayJobDetails();
        }

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
        binding.btnCancel.setOnClickListener(v -> cancelJob());
        binding.btnReschedule.setOnClickListener(v -> rescheduleJob());
    }

    private void displayJobDetails() {
        if (job == null)
            return;
        appLogger.d(TAG, "JobDetailsFragment(Seeker): displayJobDetails - jobId: " + job.getId() + ", status: " + job.getStatus());

        binding.tvTitle.setText(job.getTitle());
        binding.tvDescription.setText(job.getDescription());
        binding.tvStatus.setText(job.getStatus().name());

        // Check if Scheduled Job
        if (job.getJobType() == com.workly.helpseeker.data.model.JobType.SCHEDULED
                && job.getStatus() == JobStatus.SCHEDULED) {
            binding.llActions.setVisibility(View.VISIBLE);
            if (job.getPreferredDateTime() > 0) {
                binding.tvScheduledTime.setVisibility(View.VISIBLE);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm",
                        java.util.Locale.getDefault());
                binding.tvScheduledTime.setText(sdf.format(new java.util.Date(job.getPreferredDateTime())));
            }
        } else {
            binding.llActions.setVisibility(View.GONE);
            binding.tvScheduledTime.setVisibility(View.GONE);
        }

        if (job.getStatus() == JobStatus.ASSIGNED) {
            binding.cvOtp.setVisibility(View.VISIBLE);
            binding.tvOtp.setText(job.getCompletionOtp() != null ? job.getCompletionOtp() : "----");

            if (job.getWorkerId() != null) {
                binding.btnChat.setVisibility(View.VISIBLE);
                binding.btnChat.setOnClickListener(v -> startChat());
            }
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            binding.llReview.setVisibility(View.VISIBLE);
        }
    }

    private void cancelJob() {
        appLogger.d(TAG, "JobDetailsFragment(Seeker): [ENTER] cancelJob - jobId: " + job.getId());
        apiService.updateJobStatus(job.getId(), "CANCELLED")
                .enqueue(new Callback<com.workly.helpseeker.data.network.ApiResponse<Job>>() {
                    @Override
                    public void onResponse(Call<com.workly.helpseeker.data.network.ApiResponse<Job>> call,
                            Response<com.workly.helpseeker.data.network.ApiResponse<Job>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Job Cancelled", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        } else {
                            Toast.makeText(getContext(), "Failed to cancel", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.workly.helpseeker.data.network.ApiResponse<Job>> call, Throwable t) {
                        Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rescheduleJob() {
        appLogger.d(TAG, "JobDetailsFragment(Seeker): [ENTER] rescheduleJob - jobId: " + job.getId());
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            new android.app.TimePickerDialog(getContext(), (tView, hourOfDay, minute) -> {
                calendar.set(year, month, dayOfMonth, hourOfDay, minute);
                long newTime = calendar.getTimeInMillis();

                // Reuse validation logic if possible or simple check
                long minDiff = 2 * 60 * 60 * 1000L; // Hardcoded default for now as fragment doesn't inject properties
                                                    // easily
                if (newTime - System.currentTimeMillis() < minDiff) {
                    Toast.makeText(getContext(), "Must schedule at least 2 hours in advance", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                Job update = new Job();
                update.setPreferredDateTime(newTime);

                apiService.updateJob(job.getId(), update)
                        .enqueue(new Callback<com.workly.helpseeker.data.network.ApiResponse<Job>>() {
                            @Override
                            public void onResponse(Call<com.workly.helpseeker.data.network.ApiResponse<Job>> call,
                                    Response<com.workly.helpseeker.data.network.ApiResponse<Job>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Toast.makeText(getContext(), "Job Rescheduled", Toast.LENGTH_SHORT).show();
                                    job = response.body().getData();
                                    displayJobDetails();
                                } else {
                                    Toast.makeText(getContext(), "Failed to reschedule", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<com.workly.helpseeker.data.network.ApiResponse<Job>> call,
                                    Throwable t) {
                                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                            }
                        });

            }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show();
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void startChat() {
        appLogger.d(TAG, "Navigating to ChatFragment for worker: " + job.getWorkerId());
        Bundle bundle = new Bundle();
        bundle.putString("otherUserId", job.getWorkerId());
        androidx.navigation.Navigation.findNavController(requireView())
                .navigate(com.workly.helpseeker.R.id.action_jobDetails_to_chatFragment, bundle);
    }

    private void submitReview() {
        float rating = binding.sliderRating.getValue();
        String comment = binding.etComment.getText().toString();

        com.workly.helpseeker.data.network.ReviewRequest request = new com.workly.helpseeker.data.network.ReviewRequest(
                job.getId(), (int) rating, comment);

        apiService.submitReview(request).enqueue(new Callback<com.workly.helpseeker.data.network.ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<com.workly.helpseeker.data.network.ApiResponse<Void>> call,
                    Response<com.workly.helpseeker.data.network.ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Review Submitted", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                } else {
                    appLogger.e(TAG, "Failed to submit review. Status: " + response.code());
                    Toast.makeText(getContext(), "Failed to submit review", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.workly.helpseeker.data.network.ApiResponse<Void>> call, Throwable t) {
                appLogger.e(TAG, "Network error submitting review: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
