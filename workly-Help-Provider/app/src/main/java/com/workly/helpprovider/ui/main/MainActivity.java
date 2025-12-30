package com.workly.helpprovider.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.workly.helpprovider.databinding.ActivityMainBinding;
import com.workly.helpprovider.service.AvailabilityService;
import com.workly.helpprovider.ui.login.LoginActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "WORKLY_DEBUG";
    private ActivityMainBinding binding;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager()
                .findFragmentById(com.workly.helpprovider.R.id.nav_host_fragment);
        androidx.navigation.NavController navController = navHostFragment.getNavController();

        // Setup Bottom Navigation
        androidx.navigation.ui.NavigationUI.setupWithNavController(binding.bottomNav, navController);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Remove old UI logic from MainActivity (moved to HomeFragment)
        // Check session globally
        viewModel.checkSession();
        setupObservers();
    }

    private void setupListeners() {
        // Moved to HomeFragment
    }

    private void setupObservers() {
        viewModel.getIsSessionValid().observe(this, isValid -> {
            if (!isValid) {
                redirectToLogin();
            }
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
