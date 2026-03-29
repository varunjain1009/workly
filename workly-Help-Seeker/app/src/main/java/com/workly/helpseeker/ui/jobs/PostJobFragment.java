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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

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

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupValidations();

        binding.btnPostJob.setOnClickListener(v -> postJob());
        binding.etDate.setOnClickListener(v -> showDatePicker());
        binding.etTime.setOnClickListener(v -> showTimePicker());

        binding.cgJobType.setEnabled(false); // Disable manual selection
        validateFields(); // Initial validation
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

        boolean hasDate = !date.isEmpty();
        boolean hasTime = !time.isEmpty();

        // Reactive Job Type Logic
        if (hasDate || hasTime) {
            // At least one present: Force Scheduled (User is trying to schedule)
            binding.chipScheduled.setChecked(true);
            binding.chipImmediate.setEnabled(false);
            binding.chipScheduled.setEnabled(false); // Read-only indication
        } else {
            // Both empty: Force Immediate
            binding.chipImmediate.setChecked(true);
            binding.chipImmediate.setEnabled(false);
            binding.chipScheduled.setEnabled(false); // Read-only indication
        }

        boolean isValid = !title.isEmpty() && !description.isEmpty() && !skill.isEmpty() && !radiusStr.isEmpty();

        // Date and Time must be "all or nothing"
        boolean timingValid = (hasDate && hasTime) || (!hasDate && !hasTime);

        // Check scheduling constraints
        if (hasDate && hasTime && !validateScheduling(date, time)) {
            timingValid = false;
            // Optional: Show toast or error on field (difficult in this method structure
            // without spamming)
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
        appLogger.d(TAG, "PostJobFragment: [ENTER] postJob");
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

        Job job = new Job(title, description, skill, new Location(0.0, 0.0, "Current Location"),
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
                    if (response.isSuccessful() && response.body() != null) {
                        appLogger.d(TAG, "Job Posted Successfully. ID: " + response.body().getData().getId());
                        Toast.makeText(getContext(), "Job Posted Successfully", Toast.LENGTH_SHORT).show();

                        // Add to local ViewModel for immediate UI update
                        jobViewModel.addJobLocal(response.body().getData());

                        // For Scheduled jobs, just go back
                        requireActivity().onBackPressed();
                    } else {
                        appLogger.e(TAG, "Failed to post job. Status: " + response.code());
                        Toast.makeText(getContext(), "Failed to post job", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Job>> call, Throwable t) {
                    appLogger.e(TAG, "Network error posting job: " + t.getMessage(), t);
                    Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
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
