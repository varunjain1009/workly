package com.workly.helpprovider.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.databinding.FragmentHomeBinding;
import com.workly.helpprovider.ui.adapter.JobAdapter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment implements JobAdapter.OnJobClickListener {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private JobAdapter jobAdapter;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        appLogger.d(TAG, "HomeFragment(Provider): onViewCreated - ViewModel bound");

        setupRecyclerView();
        setupObservers();
        setupListeners();

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshJobs());
    }

    private void setupRecyclerView() {
        jobAdapter = new JobAdapter(this);
        binding.rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvJobs.setAdapter(jobAdapter);
    }

    private void setupObservers() {
        viewModel.getAvailableJobs().observe(getViewLifecycleOwner(), jobs -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.swipeRefresh.setRefreshing(false);
            if (jobs != null && !jobs.isEmpty()) {
                appLogger.d(TAG, "HomeFragment(Provider): Loaded " + jobs.size() + " available jobs");
                jobAdapter.submitList(jobs);
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvJobs.setVisibility(View.VISIBLE);
            } else {
                appLogger.d(TAG, "HomeFragment(Provider): No available jobs returned");
                jobAdapter.submitList(null);
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvJobs.setVisibility(View.GONE);
            }
        });

        viewModel.getAvailabilityUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (updated) {
                Snackbar.make(binding.getRoot(), "Availability updated", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appLogger.d(TAG, "HomeFragment(Provider): Availability toggled to: " + isChecked);
            viewModel.setAvailability(isChecked);
            if (isChecked) {
                binding.tvStatusLabel.setText("Status: Available");
            } else {
                binding.tvStatusLabel.setText("Status: Not Available");
            }
        });
    }

    @Override
    public void onJobClick(Job job) {
        appLogger.d(TAG, "HomeFragment(Provider): Job clicked - ID: " + job.getId() + ", title: " + job.getTitle());
        Bundle bundle = new Bundle();
        bundle.putSerializable("job", job);
        Navigation.findNavController(requireView()).navigate(
                com.workly.helpprovider.R.id.action_home_to_jobDetails, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
