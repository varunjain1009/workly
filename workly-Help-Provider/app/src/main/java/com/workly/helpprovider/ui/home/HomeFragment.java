package com.workly.helpprovider.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

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
    private FusedLocationProviderClient fusedLocationClient;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    private final android.content.BroadcastReceiver jobsUpdatedReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if ("PROVIDER_AVAILABLE_JOBS_UPDATED".equals(intent.getAction())) {
                appLogger.d(TAG, "HomeFragment(Provider): notification-triggered refresh");
                refreshJobsWithLocation();
            }
        }
    };

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        appLogger.d(TAG, "HomeFragment(Provider): onViewCreated - ViewModel bound");

        setupRecyclerView();
        setupObservers();
        setupSwitchListener();

        binding.swipeRefresh.setOnRefreshListener(this::refreshJobsWithLocation);
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
                viewModel.clearAvailabilityUpdated();
            }
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                appLogger.d(TAG, "HomeFragment(Provider): Profile state loaded: " + profile.isAvailable());
                binding.switchAvailability.setOnCheckedChangeListener(null);
                binding.switchAvailability.setChecked(profile.isAvailable());

                if (profile.isAvailable()) {
                    binding.tvStatusLabel.setText("Status: Available");
                    // Re-login: service may not be running even though profile says available.
                    ensureAvailabilityServiceRunning();
                } else {
                    binding.tvStatusLabel.setText("Status: Not Available");
                }

                // Always push location and refresh jobs when profile loads, regardless
                // of availability — the provider should always be able to browse nearby jobs.
                refreshJobsWithLocation();

                setupSwitchListener();
            }
        });

        // Throttle message: shown when the user tries to refresh too soon
        viewModel.getThrottleMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                binding.swipeRefresh.setRefreshing(false);
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
                viewModel.clearThrottleMessage();
            }
        });
    }

    private void setupSwitchListener() {
        binding.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // Block programmatic triggers
            appLogger.d(TAG, "HomeFragment(Provider): Availability toggled to: " + isChecked);
            viewModel.setAvailability(isChecked);
            Intent serviceIntent = new Intent(requireContext(), com.workly.helpprovider.service.AvailabilityService.class);
            if (isChecked) {
                binding.tvStatusLabel.setText("Status: Available");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(serviceIntent);
                } else {
                    requireActivity().startService(serviceIntent);
                }
            } else {
                binding.tvStatusLabel.setText("Status: Not Available");
                requireActivity().stopService(serviceIntent);
            }
        });
    }

    /**
     * Gets the device's current GPS location and pushes it to the server before
     * fetching matching jobs. Falls back to refreshJobs() (no location) if GPS
     * is unavailable or permission is denied — better than showing a stale empty list.
     */
    @SuppressLint("MissingPermission")
    private void refreshJobsWithLocation() {
        android.util.Log.d(TAG, "HomeFragment(Provider): refreshJobsWithLocation() called");

        boolean hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        android.util.Log.d(TAG, "HomeFragment(Provider): ACCESS_FINE_LOCATION=" + hasFine + " ACCESS_COARSE_LOCATION=" + hasCoarse);

        if (!hasFine && !hasCoarse) {
            android.util.Log.w(TAG, "HomeFragment(Provider): no location permission — refreshing jobs without location");
            viewModel.refreshJobs();
            return;
        }

        android.util.Log.d(TAG, "HomeFragment(Provider): calling getLastLocation() via FusedLocationClient");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        android.util.Log.d(TAG, "HomeFragment(Provider): GPS fix — lat=" + location.getLatitude()
                                + " lon=" + location.getLongitude() + " accuracy=" + location.getAccuracy() + "m");
                        viewModel.refreshJobsWithLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        android.util.Log.w(TAG, "HomeFragment(Provider): getLastLocation() null (cold GPS cache) — trying network provider");
                        android.location.LocationManager lm = (android.location.LocationManager)
                                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
                        android.location.Location netLoc = lm != null
                                ? lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) : null;
                        if (netLoc != null) {
                            android.util.Log.d(TAG, "HomeFragment(Provider): network/WiFi fix — lat=" + netLoc.getLatitude()
                                    + " lon=" + netLoc.getLongitude() + " accuracy=" + netLoc.getAccuracy() + "m");
                            viewModel.refreshJobsWithLocation(netLoc.getLatitude(), netLoc.getLongitude());
                        } else {
                            android.util.Log.e(TAG, "HomeFragment(Provider): GPS and network both null — refreshing without location");
                            viewModel.refreshJobs();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "HomeFragment(Provider): getLastLocation() failed: " + e.getMessage());
                    viewModel.refreshJobs();
                });
    }

    private void ensureAvailabilityServiceRunning() {
        Intent serviceIntent = new Intent(requireContext(), com.workly.helpprovider.service.AvailabilityService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(serviceIntent);
        } else {
            requireActivity().startService(serviceIntent);
        }
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

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                jobsUpdatedReceiver, new android.content.IntentFilter("PROVIDER_AVAILABLE_JOBS_UPDATED"));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(jobsUpdatedReceiver);
    }
}
