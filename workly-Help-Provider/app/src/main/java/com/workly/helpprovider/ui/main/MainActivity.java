package com.workly.helpprovider.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.messaging.FirebaseMessaging;
import com.workly.helpprovider.data.repository.ProfileRepository;
import com.workly.helpprovider.databinding.ActivityMainBinding;
import com.workly.helpprovider.service.AvailabilityService;
import com.workly.helpprovider.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "WORKLY_DEBUG";
    private ActivityMainBinding binding;
    private MainViewModel viewModel;

    @javax.inject.Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @javax.inject.Inject
    ProfileRepository profileRepository;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                boolean locationGranted = Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                boolean smsGranted = Boolean.TRUE.equals(granted.get(Manifest.permission.RECEIVE_SMS))
                        || Boolean.TRUE.equals(granted.get(Manifest.permission.READ_SMS));
                boolean notificationsGranted = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
                        || Boolean.TRUE.equals(granted.get(Manifest.permission.POST_NOTIFICATIONS));
                appLogger.d(TAG, "Permission result — location=" + locationGranted + " sms=" + smsGranted);
                if (!locationGranted || !smsGranted || !notificationsGranted) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Permissions Required")
                            .setMessage("Location, SMS, and notification permissions are required for Workly to function. Please grant them in Settings.")
                            .setCancelable(false)
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                android.content.Intent intent = new android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton("Quit", (dialog, which) -> finish())
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appLogger.d(TAG, "MainActivity created/recreated.");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestRequiredPermissions();

        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager()
                .findFragmentById(com.workly.helpprovider.R.id.nav_host_fragment);
        androidx.navigation.NavController navController = navHostFragment.getNavController();

        // Setup Bottom Navigation
        androidx.navigation.ui.NavigationUI.setupWithNavController(binding.bottomNav, navController);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        syncCurrentFcmToken();

        // Remove old UI logic from MainActivity (moved to HomeFragment)
        // Check session globally
        viewModel.checkSession();
        setupObservers();
    }

    private void requestRequiredPermissions() {
        List<String> missing = new ArrayList<>();
        List<String> required = new ArrayList<>();
        required.add(Manifest.permission.ACCESS_FINE_LOCATION);
        required.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        required.add(Manifest.permission.RECEIVE_SMS);
        required.add(Manifest.permission.READ_SMS);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }
        if (!missing.isEmpty()) {
            appLogger.d(TAG, "Requesting " + missing.size() + " missing permissions");
            permissionLauncher.launch(missing.toArray(new String[0]));
        } else {
            appLogger.d(TAG, "All required permissions already granted");
        }
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
        appLogger.w(TAG, "Session invalid. Redirecting user to LoginActivity.");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appLogger.d(TAG, "MainActivity entered foreground (onResume).");
        syncCurrentFcmToken();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appLogger.d(TAG, "MainActivity entering background (onPause).");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appLogger.d(TAG, "MainActivity destroyed.");
    }

    private void syncCurrentFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isBlank()) {
                        return;
                    }
                    profileRepository.updateDeviceToken(token);
                })
                .addOnFailureListener(e -> appLogger.e(TAG, "Failed to fetch current FCM token", e));
    }
}
