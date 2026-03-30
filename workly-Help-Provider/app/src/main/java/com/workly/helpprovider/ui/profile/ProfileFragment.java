package com.workly.helpprovider.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpprovider.data.model.Profile;
import com.workly.helpprovider.databinding.FragmentProfileBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private Profile currentProfile;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        appLogger.d(TAG, "ProfileFragment(Provider): onViewCreated - ViewModel initialized");

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                appLogger.d(TAG, "ProfileFragment(Provider): Profile data received - name: " + profile.getName());
                currentProfile = profile;
                binding.etName.setText(profile.getName());
                if (profile.getSkills() != null) {
                    binding.etSkills.setText(String.join(", ", profile.getSkills()));
                }
                if (profile.getMobileNumber() != null) {
                    viewModel.fetchAverageRating(profile.getMobileNumber());
                }
            } else {
                appLogger.d(TAG, "ProfileFragment(Provider): Profile data is null");
            }
        });

        viewModel.getAverageRating().observe(getViewLifecycleOwner(), rating -> {
            if (rating != null) {
                binding.tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", rating));
                binding.ratingBar.setRating(rating.floatValue());
            }
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!isLoading);
        });
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> {
            appLogger.d(TAG, "ProfileFragment(Provider): Save button pressed");
            if (currentProfile == null) {
                currentProfile = new Profile();
            }
            currentProfile.setName(binding.etName.getText().toString());
            String skillsStr = binding.etSkills.getText().toString();
            if (!skillsStr.isEmpty()) {
                String[] parts = skillsStr.split(",");
                List<String> skills = new ArrayList<>(parts.length);
                for (String part : parts) {
                    skills.add(part.trim());
                }
                currentProfile.setSkills(skills);
            } else {
                currentProfile.setSkills(Collections.emptyList());
            }

            viewModel.updateProfile(currentProfile);
        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
