package com.workly.helpprovider.ui.jobs;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.location.LocationServices;
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

        // Enable Complete only after 4-digit OTP is entered
        binding.etOtp.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnCompleteJob.setEnabled(s.length() == 4 && s.toString().matches("\\d{4}"));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnCompleteJob.setOnClickListener(v -> {
            if (job != null) {
                String otp = binding.etOtp.getText().toString();
                appLogger.d(TAG, "JobDetailsFragment(Provider): Completing job " + job.getId() + " with OTP");
                viewModel.completeJob(job.getId(), otp);
            }
        });

        // specific observers for this fragment
        viewModel.getAcceptJobStatus().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Snackbar.make(binding.getRoot(), "Job Accepted!", Snackbar.LENGTH_SHORT).show();
                // Push accepted job into My Jobs list so the tab updates without a refresh
                if (job != null) {
                    job.setStatus(JobStatus.ASSIGNED);
                    new ViewModelProvider(requireActivity()).get(MyJobsViewModel.class).addJobLocal(job);
                }
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

    @SuppressLint("MissingPermission")
    private void displayJobDetails(Job job) {
        binding.tvTitle.setText(job.getTitle());
        binding.tvStatus.setText("Status: " + job.getStatus());
        binding.tvDescription.setText(job.getDescription());
        binding.tvSkill.setText(job.getRequiredSkill());

        if (job.getLocation() != null) {
            binding.tvLocation.setText(job.getLocation().getAddress());

            // Show distance from provider's current location to job
            if (job.getLocation().getLatitude() != 0 || job.getLocation().getLongitude() != 0) {
                LocationServices.getFusedLocationProviderClient(requireContext())
                        .getLastLocation()
                        .addOnSuccessListener(myLoc -> {
                            if (myLoc != null && binding != null) {
                                float[] results = new float[1];
                                android.location.Location.distanceBetween(
                                        myLoc.getLatitude(), myLoc.getLongitude(),
                                        job.getLocation().getLatitude(), job.getLocation().getLongitude(),
                                        results);
                                float distKm = results[0] / 1000f;
                                binding.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km away", distKm));
                                binding.tvDistance.setVisibility(View.VISIBLE);
                            }
                        });
            }
        }

        // Seeker info — always show on My Jobs; hide on available jobs where ASSIGNED hasn't happened
        if (job.getSeekerMobileNumber() != null) {
            binding.tvSeekerLabel.setVisibility(View.VISIBLE);
            binding.tvSeekerPhone.setText("Phone: " + job.getSeekerMobileNumber());
            binding.tvSeekerPhone.setVisibility(View.VISIBLE);
            if (job.getSeekerName() != null && !job.getSeekerName().isEmpty()) {
                binding.tvSeekerName.setText(job.getSeekerName());
                binding.tvSeekerName.setVisibility(View.VISIBLE);
            }
        }

        if (job.getPreferredDateTime() > 0 && job.getStatus() == JobStatus.SCHEDULED) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            binding.tvScheduledTime.setText("Scheduled: " + sdf.format(new Date(job.getPreferredDateTime())));
            binding.tvScheduledTime.setVisibility(View.VISIBLE);
        } else {
            binding.tvScheduledTime.setVisibility(View.GONE);
        }

        // Hide accept button if already accepted or cancelled
        if (job.getStatus() != JobStatus.BROADCASTED
                && job.getStatus() != JobStatus.SCHEDULED
                && job.getStatus() != JobStatus.PENDING_ACCEPTANCE) {
            binding.btnAcceptJob.setVisibility(View.GONE);
        }

        // Show complete + chat section if Accepted/Assigned
        if (job.getStatus() == JobStatus.ASSIGNED) {
            binding.llCompleteJob.setVisibility(View.VISIBLE);
            if (job.getSeekerMobileNumber() != null) {
                binding.btnChatSeeker.setVisibility(View.VISIBLE);
                binding.btnChatSeeker.setOnClickListener(v -> startChat(job.getSeekerMobileNumber()));
            } else {
                binding.btnChatSeeker.setVisibility(View.GONE);
            }
        }
    }

    private void startChat(String seekerMobileNumber) {
        appLogger.d(TAG, "JobDetailsFragment(Provider): Starting chat with seeker: " + seekerMobileNumber);
        Bundle bundle = new Bundle();
        bundle.putString("otherUserId", seekerMobileNumber);
        Navigation.findNavController(requireView())
                .navigate(com.workly.helpprovider.R.id.action_job_details_to_chat, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
