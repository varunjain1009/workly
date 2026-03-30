package com.workly.helpprovider.ui.jobs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.model.JobStatus;
import com.workly.helpprovider.databinding.FragmentJobDetailsBinding;

import dagger.hilt.android.AndroidEntryPoint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

@AndroidEntryPoint
public class JobDetailsFragment extends Fragment {

    private FragmentJobDetailsBinding binding;
    private JobDetailsViewModel viewModel;
    private Job job;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentJobDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JobDetailsViewModel.class);

        if (getArguments() != null) {
            job = (Job) getArguments().getSerializable("job");
            if (job != null) {
                appLogger.d(TAG, "JobDetailsFragment(Provider): Displaying job: " + job.getId() + " - " + job.getTitle());
                displayJobDetails(job);
            }
        }

        binding.btnAcceptJob.setOnClickListener(v -> {
            if (job != null) {
                appLogger.d(TAG, "JobDetailsFragment(Provider): Accepting job: " + job.getId());
                viewModel.acceptJob(job.getId());
            }
        });

        binding.btnCompleteJob.setOnClickListener(v -> {
            if (job != null) {
                String otp = binding.etOtp.getText().toString();
                if (otp.length() == 4 && otp.matches("\\d{4}")) {
                    appLogger.d(TAG, "JobDetailsFragment(Provider): Completing job " + job.getId() + " with OTP");
                    viewModel.completeJob(job.getId(), otp);
                } else {
                    appLogger.d(TAG, "JobDetailsFragment(Provider): Invalid OTP: " + otp.length() + " chars");
                    Snackbar.make(binding.getRoot(), "Please enter a valid 4-digit OTP", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        // specific observers for this fragment
        viewModel.getAcceptJobStatus().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Snackbar.make(binding.getRoot(), "Job Accepted Successfully!", Snackbar.LENGTH_SHORT).show();
                // Navigate back
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            } else {
                Snackbar.make(binding.getRoot(), "Failed to accept job", Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getCompleteJobStatus().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Snackbar.make(binding.getRoot(), "Job Completed Successfully!", Snackbar.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            } else {
                Snackbar.make(binding.getRoot(), "Failed to complete job. Invalid OTP?", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void displayJobDetails(Job job) {
        binding.tvTitle.setText(job.getTitle());
        binding.tvStatus.setText("Status: " + job.getStatus());
        binding.tvDescription.setText(job.getDescription());
        binding.tvSkill.setText(job.getRequiredSkill());
        if (job.getLocation() != null) {
            binding.tvLocation.setText(job.getLocation().getAddress());
        }
        if (job.getBudget() != null) {
            binding.tvBudget.setText(String.format("$%.2f", job.getBudget()));
        }

        if (job.getPreferredDateTime() > 0 && job.getStatus() == JobStatus.SCHEDULED) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            binding.tvScheduledTime.setText("Scheduled: " + sdf.format(new Date(job.getPreferredDateTime())));
            binding.tvScheduledTime.setVisibility(View.VISIBLE);
        } else {
            binding.tvScheduledTime.setVisibility(View.GONE);
        }

        // Hide accept button if already accepted or cancelled
        if (job.getStatus() != JobStatus.BROADCASTED && job.getStatus() != JobStatus.SCHEDULED) {
            binding.btnAcceptJob.setVisibility(View.GONE);
        }

        // Show complete section if Accepted/Assigned
        if (job.getStatus() == JobStatus.ASSIGNED) {
            binding.llCompleteJob.setVisibility(View.VISIBLE);
            // TODO: Re-enable chat when Job model has seekerId
            // if (job.getSeekerId() != null) {
            // binding.btnChatSeeker.setVisibility(View.VISIBLE);
            // binding.btnChatSeeker.setOnClickListener(v -> startChat());
            // }
        }
    }

    /*
     * private void startChat() {
     * Bundle bundle = new Bundle();
     * // TODO: Fix - bundle.putString("otherUserId", job.getSeekerId());
     * // TODO: Fix navigation
     * //
     * androidx.navigation.Navigation.findNavController(requireView()).navigate(com.
     * workly.helpprovider.R.id.nav_chat, bundle);
     * }
     */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
