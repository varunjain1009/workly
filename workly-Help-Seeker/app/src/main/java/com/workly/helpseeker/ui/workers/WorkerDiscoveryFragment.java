package com.workly.helpseeker.ui.workers;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpseeker.data.model.Worker;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.databinding.FragmentWorkerDiscoveryBinding;

import java.util.ArrayList;
import java.util.List;
import com.workly.helpseeker.ui.jobs.JobViewModel;
import java.util.Properties;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class WorkerDiscoveryFragment extends Fragment {

    private static final String TAG = "WORKLY_DEBUG";
    private FragmentWorkerDiscoveryBinding binding;
    private WorkerAdapter adapter;

    @Inject
    ApiService apiService;

    @Inject
    com.workly.helpseeker.util.AppLogger appLogger;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerDiscoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private com.workly.helpseeker.data.model.Job pendingJob;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupRecyclerView();

        if (getArguments() != null) {
            if (getArguments().containsKey("job")) {
                pendingJob = (com.workly.helpseeker.data.model.Job) getArguments().getSerializable("job");
                if (pendingJob != null) {
                    appLogger.d(TAG, "Loaded Pending Job: " + pendingJob.getTitle());
                    searchWorkers(pendingJob.getRequiredSkill(), pendingJob.getSearchRadiusKm());
                }
            } else {
                // Fallback / Backward compatibility
                String skill = getArguments().getString("skill");
                int radius = getArguments().getInt("radius", 10);
                if (skill != null) {
                    appLogger.d(TAG, "Searching for: " + skill + " within " + radius + "km");
                    searchWorkers(skill, radius);
                } else {
                    loadDummyWorkers();
                }
            }
        } else {
            loadDummyWorkers();
        }
    }

    private void searchWorkers(String skill, int radius) {
        // Using dummy lat/lon for now as per user GPS location requirement (to be
        // implemented)
        apiService.searchWorkers(skill, 0.0, 0.0, radius).enqueue(new Callback<ApiResponse<List<Worker>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Worker>>> call, Response<ApiResponse<List<Worker>>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Worker> workers = response.body().getData();
                    appLogger.d(TAG, "Found " + (workers != null ? workers.size() : 0) + " workers");

                    if (workers == null || workers.isEmpty()) {
                        // No workers found
                        binding.rvWorkers.setVisibility(View.GONE);
                        binding.layoutNoWorkers.setVisibility(View.VISIBLE);

                        // Set current value to slider if needed (only once or always?)
                        // binding.sliderRadius.setValue((float) radius);
                        // Updating label
                        binding.tvRadiusLabel.setText("Search Radius: " + radius + " km");
                    } else {
                        // Workers found
                        binding.rvWorkers.setVisibility(View.VISIBLE);
                        binding.layoutNoWorkers.setVisibility(View.GONE);
                        adapter.submitList(workers);
                    }
                } else {
                    appLogger.e(TAG, "Search failed. Status: " + response.code());
                    Snackbar.make(binding.getRoot(), "Failed to find workers", Snackbar.LENGTH_LONG).show();
                    // Optional: Show empty state on error too?
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Worker>>> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                appLogger.e(TAG, "Network error during search: " + t.getMessage(), t);
                Snackbar.make(binding.getRoot(), "Network error", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new WorkerAdapter(worker -> {
            if (pendingJob != null) {
                confirmAndPostJob(worker);
            } else {
                Snackbar.make(binding.getRoot(), "Selected: " + worker.getName(), Snackbar.LENGTH_SHORT).show();
            }
        }, appLogger);
        binding.rvWorkers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvWorkers.setAdapter(adapter);

        // Setup Slider and Button for No Workers State
        binding.sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            binding.tvRadiusLabel.setText("Search Radius: " + (int) value + " km");
        });

        binding.btnSearchAgain.setOnClickListener(v -> {
            int newRadius = (int) binding.sliderRadius.getValue();
            String skill = "";

            if (pendingJob != null) {
                skill = pendingJob.getRequiredSkill();
                pendingJob.setSearchRadiusKm(newRadius);
            } else if (getArguments() != null) {
                skill = getArguments().getString("skill", "");
            }

            if (!skill.isEmpty()) {
                searchWorkers(skill, newRadius);
            }
        });
    }

    private void confirmAndPostJob(Worker worker) {
        appLogger.d(TAG, "WorkerDiscoveryFragment: [ENTER] confirmAndPostJob - workerId: " + worker.getId() + ", jobTitle: " + pendingJob.getTitle());
        pendingJob.setWorkerId(worker.getId());
        pendingJob.setAssignmentMode(com.workly.helpseeker.data.model.AssignmentMode.MANUAL_SELECT);

        apiService.postJob(pendingJob).enqueue(new Callback<ApiResponse<com.workly.helpseeker.data.model.Job>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.workly.helpseeker.data.model.Job>> call,
                    Response<ApiResponse<com.workly.helpseeker.data.model.Job>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    Snackbar.make(binding.getRoot(), "Job Sent to " + worker.getName(), Snackbar.LENGTH_SHORT).show();

                    // Add to local ViewModel cache to avoid re-fetch on back navigation
                    JobViewModel jobViewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(JobViewModel.class);
                    jobViewModel.addJobLocal(response.body().getData());

                    requireActivity().onBackPressed();
                } else {
                    Snackbar.make(binding.getRoot(), "Failed to create job", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.workly.helpseeker.data.model.Job>> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                Snackbar.make(binding.getRoot(), "Error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void loadDummyWorkers() {
        List<Worker> dummyWorkers = new ArrayList<>();
        // Add dummy data for testing UI
        adapter.submitList(dummyWorkers);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
