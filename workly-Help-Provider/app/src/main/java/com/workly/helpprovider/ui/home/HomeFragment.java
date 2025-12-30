package com.workly.helpprovider.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.databinding.FragmentHomeBinding;
import com.workly.helpprovider.ui.adapter.JobAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment implements JobAdapter.OnJobClickListener {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private JobAdapter jobAdapter;

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

        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupRecyclerView() {
        jobAdapter = new JobAdapter(this);
        binding.rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvJobs.setAdapter(jobAdapter);
    }

    private void setupObservers() {
        viewModel.getAvailableJobs().observe(getViewLifecycleOwner(), jobs -> {
            binding.progressBar.setVisibility(View.GONE);
            if (jobs != null && !jobs.isEmpty()) {
                jobAdapter.setJobs(jobs);
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvJobs.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvJobs.setVisibility(View.GONE);
            }
        });

        viewModel.getAvailabilityUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (updated) {
                Toast.makeText(getContext(), "Availability updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
        Bundle bundle = new Bundle();
        bundle.putSerializable("job", job);
        androidx.navigation.Navigation.findNavController(requireView())
                .navigate(com.workly.helpprovider.R.id.action_home_to_details, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
