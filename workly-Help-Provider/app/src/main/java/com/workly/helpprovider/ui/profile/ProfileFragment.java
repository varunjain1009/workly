package com.workly.helpprovider.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.workly.helpprovider.data.model.Profile;
import com.workly.helpprovider.databinding.FragmentProfileBinding;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private Profile currentProfile;

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

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                currentProfile = profile;
                binding.etName.setText(profile.getName());
                if (profile.getSkills() != null) {
                    binding.etSkills.setText(String.join(", ", profile.getSkills()));
                }
                binding.switchAvailability.setChecked(profile.isAvailable());
                if (profile.getMobileNumber() != null) {
                    viewModel.fetchAverageRating(profile.getMobileNumber());
                }
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
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!isLoading);
        });
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> {
            if (currentProfile == null) {
                currentProfile = new Profile();
            }
            currentProfile.setName(binding.etName.getText().toString());
            String skillsStr = binding.etSkills.getText().toString();
            if (!skillsStr.isEmpty()) {
                currentProfile.setSkills(Arrays.stream(skillsStr.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()));
            } else {
                currentProfile.setSkills(Collections.emptyList());
            }

            // Availability is likely handled separately via switch, but let's sync it
            currentProfile.setAvailable(binding.switchAvailability.isChecked());

            viewModel.updateProfile(currentProfile);
        });

        binding.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updateAvailability(isChecked);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
