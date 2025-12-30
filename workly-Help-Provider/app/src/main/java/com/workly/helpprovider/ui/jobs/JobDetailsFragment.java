package com.workly.helpprovider.ui.jobs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.model.JobStatus;
import com.workly.helpprovider.databinding.FragmentJobDetailsBinding;

import dagger.hilt.android.AndroidEntryPoint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@AndroidEntryPoint
public class JobDetailsFragment extends Fragment {

    private FragmentJobDetailsBinding binding;
    private JobDetailsViewModel viewModel;
    private Job job;

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
                displayJobDetails(job);
            }
        }

        binding.btnAcceptJob.setOnClickListener(v -> {
            if (job != null) {
                viewModel.acceptJob(job.getId());
            }
        });

        binding.btnCompleteJob.setOnClickListener(v -> {
            if (job != null) {
                String otp = binding.etOtp.getText().toString();
                if (otp.length() == 4) {
                    viewModel.completeJob(job.getId(), otp);
                } else {
                    Toast.makeText(getContext(), "Please enter a valid 4-digit OTP", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // specific observers for this fragment
        viewModel.getAcceptJobStatus().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Job Accepted Successfully!", Toast.LENGTH_SHORT).show();
                // Navigate back
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            } else {
                Toast.makeText(getContext(), "Failed to accept job", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getCompleteJobStatus().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Job Completed Successfully!", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            } else {
                Toast.makeText(getContext(), "Failed to complete job. Invalid OTP?", Toast.LENGTH_SHORT).show();
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
            //     binding.btnChatSeeker.setVisibility(View.VISIBLE);
            //     binding.btnChatSeeker.setOnClickListener(v -> startChat());
            // }
        }
    }

    private void startChat() {
        Bundle bundle = new Bundle();
        // TODO: Fix - bundle.putString("otherUserId", job.getSeekerId());
            // TODO: Fix navigation
            // androidx.navigation.Navigation.findNavController(requireView()).navigate(com.workly.helpprovider.R.id.nav_chat, bundle);
    }
    */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
