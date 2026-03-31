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
import java.util.HashSet;
import java.util.Set;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import com.workly.helpprovider.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private Profile currentProfile;
    private static final String TAG = "WORKLY_DEBUG";
    private final Set<String> cachedSkills = new HashSet<>();

    @Inject
    ApiService apiService;

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
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        appLogger.d(TAG, "ProfileFragment(Provider): onViewCreated - ViewModel initialized");

        setupObservers();
        setupListeners();
        setupSkillAutocomplete();
    }

    private void setupSkillAutocomplete() {
        binding.etSkills.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        binding.etSkills.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!binding.etSkills.hasFocus()) return;
                
                if (s == null || s.toString().trim().isEmpty()) return;
                
                // Get the current word being typed (after the last comma)
                String[] parts = s.toString().split(",");
                String query = parts[parts.length - 1].trim();
                
                if (query.isEmpty()) return;

                // Check local cache first
                List<String> cachedSuggestions = new ArrayList<>();
                for (String skill : cachedSkills) {
                    if (skill.toLowerCase().contains(query.toLowerCase())) {
                        cachedSuggestions.add(skill);
                    }
                }

                if (!cachedSuggestions.isEmpty()) {
                    updateSkillAdapter(cachedSuggestions);
                    return;
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
            public void afterTextChanged(Editable s) {
                if (s == null || s.toString().trim().isEmpty()) {
                    binding.tilSkills.setHint("Skills (comma separated)");
                } else {
                    binding.tilSkills.setHint("Skills");
                }
            }
        });
    }

    private void updateSkillAdapter(List<String> suggestions) {
        if (getContext() == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, suggestions);
        binding.etSkills.setAdapter(adapter);
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
                viewModel.clearStatusMessage();
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
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        skills.add(trimmed);
                    }
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
