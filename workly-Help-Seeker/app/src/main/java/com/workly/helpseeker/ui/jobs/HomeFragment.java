package com.workly.helpseeker.ui.jobs;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.workly.helpseeker.R;
import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.databinding.FragmentHomeBinding;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final String TAG = "WORKLY_DEBUG";
    private FragmentHomeBinding binding;
    private JobAdapter adapter;
    private JobViewModel viewModel;

    @Inject
    ApiService apiService;

    @Inject
    com.workly.helpseeker.util.AppLogger appLogger;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(JobViewModel.class);
        return binding.getRoot();
    }

    private String currentJobType = "active";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        observeViewModel();

        binding.toggleJobType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_active) {
                    currentJobType = "active";
                    binding.tvEmptyState.setText("No active jobs found");
                } else if (checkedId == R.id.btn_past) {
                    currentJobType = "past";
                    binding.tvEmptyState.setText("No past jobs found");
                }
                // Use cache if fresh — ViewModel decides whether to hit network
                viewModel.loadJobs(currentJobType, false);
            }
        });

        // Initial load
        viewModel.loadJobs(currentJobType, false);

        binding.fabPostJob.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_home_to_postJob);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Let ViewModel's staleness threshold decide whether to fetch
        if (viewModel != null) {
            viewModel.loadJobs(currentJobType, false);
        }
    }

    private void observeViewModel() {
        viewModel.getJobs().observe(getViewLifecycleOwner(), jobs -> {
            if (jobs == null || jobs.isEmpty()) {
                appLogger.d(TAG, "No jobs returned from server.");
                binding.rvJobs.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                adapter.setJobs(new java.util.ArrayList<>());
            } else {
                appLogger.d(TAG, "Successfully loaded " + jobs.size() + " jobs.");
                binding.rvJobs.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
                adapter.setJobs(jobs);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Update UI for loading state if needed
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new JobAdapter(job -> {
            Bundle args = new Bundle();
            args.putSerializable("job", job);
            Navigation.findNavController(binding.rvJobs).navigate(R.id.action_home_to_jobDetails, args);
        }, appLogger);
        binding.rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvJobs.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
