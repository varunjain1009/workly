package com.workly.helpseeker.ui.jobs;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import android.widget.ArrayAdapter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpseeker.R;
import com.workly.helpseeker.data.model.AssignmentMode;
import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.data.model.JobType;
import com.workly.helpseeker.data.model.Location;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.databinding.FragmentPostJobBinding;

import java.util.Calendar;
import java.util.Properties;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class PostJobFragment extends Fragment {

    private static final String TAG = "WORKLY_DEBUG";
    private FragmentPostJobBinding binding;
    private JobViewModel jobViewModel;
    private final Set<String> cachedSkills = new HashSet<>();
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Inject
    ApiService apiService;

    @Inject
    Properties properties;

    @Inject
    com.workly.helpseeker.util.AppLogger appLogger;

    @Inject
    com.workly.helpseeker.data.config.ConfigManager configManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPostJobBinding.inflate(inflater, container, false);
        jobViewModel = new ViewModelProvider(requireActivity()).get(JobViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                appLogger.d(TAG, "Location permission granted.");
                postJobWithLocation();
            } else {
                appLogger.e(TAG, "Location permission denied! Using default 0.0");
                Snackbar.make(binding.getRoot(), "Location permission denied. Using default location.", Snackbar.LENGTH_LONG).show();
                postJobWithCoordinates(0.0, 0.0);
            }
        });

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupValidations();

        binding.btnPostJob.setOnClickListener(v -> postJob());
        binding.etDate.setOnClickListener(v -> showDatePicker());
        binding.etTime.setOnClickListener(v -> showTimePicker());

        binding.llDateTimeContainer.setVisibility(View.GONE);
        binding.cgJobType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean isScheduled = !checkedIds.isEmpty() && checkedIds.get(0) == R.id.chip_scheduled;
            binding.llDateTimeContainer.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
            validateFields();
        });

        validateFields(); // Initial validation
        setupSkillAutocomplete();
    }

    private void setupSkillAutocomplete() {
        binding.etSkill.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null || s.toString().trim().isEmpty()) return;
                String query = s.toString().trim();
                
                // First check local cache
                List<String> cachedSuggestions = new java.util.ArrayList<>();
                for (String skill : cachedSkills) {
                    if (skill.toLowerCase().contains(query.toLowerCase())) {
                        cachedSuggestions.add(skill);
                    }
                }
                
                if (!cachedSuggestions.isEmpty()) {
                    updateSkillAdapter(cachedSuggestions);
                    return; // Skip network call if we have cached matches
                }

                // Call API
                apiService.getSkillSuggestions(query).enqueue(new Callback<List<String>>() {
                    @Override
                    public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null) {
                            List<String> suggestions = response.body();
                            if (suggestions != null) {
                                cachedSkills.addAll(suggestions);
                                updateSkillAdapter(suggestions);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<String>> call, Throwable t) {
                        appLogger.e(TAG, "Failed to load skill suggestions", t);
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateSkillAdapter(List<String> suggestions) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, suggestions);
        binding.etSkill.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            binding.etDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
            validateFields();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            binding.etTime.setText(String.format("%02d:%02d", hourOfDay, minute));
            validateFields();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void setupValidations() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        binding.etTitle.addTextChangedListener(validationWatcher);
        binding.etDescription.addTextChangedListener(validationWatcher);
        binding.etSkill.addTextChangedListener(validationWatcher);
        binding.etRadius.addTextChangedListener(validationWatcher);
        binding.etDate.addTextChangedListener(validationWatcher);
        binding.etTime.addTextChangedListener(validationWatcher);
    }

    private void validateFields() {
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String skill = binding.etSkill.getText().toString().trim();
        String radiusStr = binding.etRadius.getText().toString().trim();
        String date = binding.etDate.getText().toString().trim();
        String time = binding.etTime.getText().toString().trim();

        boolean isScheduled = binding.chipScheduled.isChecked();
        boolean hasDate = !date.isEmpty();
        boolean hasTime = !time.isEmpty();

        boolean isValid = !title.isEmpty() && !description.isEmpty() && !skill.isEmpty() && !radiusStr.isEmpty();

        boolean timingValid = true;
        if (isScheduled) {
            timingValid = hasDate && hasTime;
            if (timingValid && !validateScheduling(date, time)) {
                timingValid = false;
            }
        }

        isValid = isValid && timingValid;

        // Additional radius check
        if (!radiusStr.isEmpty()) {
            try {
                int r = Integer.parseInt(radiusStr);
                if (r <= 0)
                    isValid = false;
            } catch (NumberFormatException e) {
                isValid = false;
            }
        }

        binding.btnPostJob.setEnabled(isValid);
    }

    private boolean validateScheduling(String date, String time) {
        if (date.isEmpty() || time.isEmpty())
            return true; // Immediate or incomplete

        try {
            int minHours = 2;
            if (configManager.getConfig() != null) {
                minHours = configManager.getConfig().getJobMinAdvanceHours();
            } else {
                try {
                    minHours = Integer.parseInt(properties.getProperty("job.scheduling.min_hours_advance", "2"));
                } catch (Exception e) {
                    /* use default 2 */ }
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",
                    java.util.Locale.getDefault());
            java.util.Date scheduledDate = sdf.parse(date + " " + time);

            if (scheduledDate == null)
                return false;

            long diff = scheduledDate.getTime() - System.currentTimeMillis();
            long minDiff = minHours * 60 * 60 * 1000L;

            if (diff < minDiff) {
                // Ideally show an error to user, for now validation fails
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void postJob() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            postJobWithLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void postJobWithLocation() {
        boolean hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        android.util.Log.d(TAG, "PostJobFragment(Seeker): postJobWithLocation — fine=" + hasFine + " coarse=" + hasCoarse);

        if (!hasFine && !hasCoarse) {
            android.util.Log.w(TAG, "PostJobFragment(Seeker): no location permission — posting with 0,0");
            postJobWithCoordinates(0.0, 0.0);
            return;
        }

        android.util.Log.d(TAG, "PostJobFragment(Seeker): calling getLastLocation() via FusedLocationClient");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        android.util.Log.d(TAG, "PostJobFragment(Seeker): GPS fix — lat=" + location.getLatitude()
                                + " lon=" + location.getLongitude() + " accuracy=" + location.getAccuracy() + "m");
                        postJobWithCoordinates(location.getLatitude(), location.getLongitude());
                    } else {
                        android.util.Log.w(TAG, "PostJobFragment(Seeker): getLastLocation() null — trying network provider");
                        android.location.LocationManager lm = (android.location.LocationManager)
                                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
                        android.location.Location netLoc = lm != null
                                ? lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) : null;
                        if (netLoc != null) {
                            android.util.Log.d(TAG, "PostJobFragment(Seeker): network/WiFi fix — lat=" + netLoc.getLatitude()
                                    + " lon=" + netLoc.getLongitude() + " accuracy=" + netLoc.getAccuracy() + "m");
                            postJobWithCoordinates(netLoc.getLatitude(), netLoc.getLongitude());
                        } else {
                            android.util.Log.e(TAG, "PostJobFragment(Seeker): GPS and network both null — posting with 0,0");
                            postJobWithCoordinates(0.0, 0.0);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "PostJobFragment(Seeker): getLastLocation() failed: " + e.getMessage());
                    postJobWithCoordinates(0.0, 0.0);
                });
    }

    private void postJobWithCoordinates(double lat, double lon) {
        appLogger.d(TAG, "PostJobFragment: [ENTER] postJobWithCoordinates lat: " + lat + " lon: " + lon);
        String title = binding.etTitle.getText().toString();
        String description = binding.etDescription.getText().toString();
        String skill = binding.etSkill.getText().toString();
        String radiusStr = binding.etRadius.getText().toString();

        int defaultRadius = 10;
        if (configManager.getConfig() != null) {
            defaultRadius = configManager.getConfig().getJobMaxRadiusKm();
        }

        int radius = radiusStr.isEmpty() ? defaultRadius : Integer.parseInt(radiusStr);

        AssignmentMode mode = binding.chipManualSelect.isChecked() ? AssignmentMode.MANUAL_SELECT
                : AssignmentMode.FIRST_ACCEPT;
        JobType type = binding.chipScheduled.isChecked() ? JobType.SCHEDULED : JobType.IMMEDIATE;

        long preferredTime = System.currentTimeMillis();
        if (type == JobType.SCHEDULED) {
            String date = binding.etDate.getText().toString();
            String time = binding.etTime.getText().toString();
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",
                        java.util.Locale.getDefault());
                java.util.Date scheduledDate = sdf.parse(date + " " + time);
                if (scheduledDate != null) {
                    preferredTime = scheduledDate.getTime();
                }
            } catch (Exception e) {
                appLogger.e(TAG, "PostJobFragment: Error parsing scheduled date: " + e.getMessage(), e);
            }
        }

        Job job = new Job(title, description, skill, new Location(lat, lon, "Current Location"),
                radius, preferredTime, type, mode);

        if (type == JobType.IMMEDIATE) {
            appLogger.d(TAG, "PostJobFragment: IMMEDIATE job - navigating to WorkerDiscovery for skill: " + skill);
            // Defer posting, navigate to Worker Discovery with the Job object
            Bundle args = new Bundle();
            args.putSerializable("job", job);
            Navigation.findNavController(requireView()).navigate(R.id.action_postJob_to_workerDiscovery, args);
        } else {
            // Scheduled Job - Post Immediately
            apiService.postJob(job).enqueue(new Callback<ApiResponse<Job>>() {
                @Override
                public void onResponse(Call<ApiResponse<Job>> call, Response<ApiResponse<Job>> response) {
                    if (!isAdded() || binding == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        appLogger.d(TAG, "Job Posted Successfully. ID: " + response.body().getData().getId());
                        Snackbar.make(binding.getRoot(), "Job Posted Successfully", Snackbar.LENGTH_SHORT).show();

                        // Add to local ViewModel for immediate UI update
                        jobViewModel.addJobLocal(response.body().getData());

                        // For Scheduled jobs, just go back
                        requireActivity().onBackPressed();
                    } else {
                        appLogger.e(TAG, "Failed to post job. Status: " + response.code());
                        Snackbar.make(binding.getRoot(), "Failed to post job", Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Job>> call, Throwable t) {
                    if (!isAdded() || binding == null) return;
                    appLogger.e(TAG, "Network error posting job: " + t.getMessage(), t);
                    Snackbar.make(binding.getRoot(), "Network Error", Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
