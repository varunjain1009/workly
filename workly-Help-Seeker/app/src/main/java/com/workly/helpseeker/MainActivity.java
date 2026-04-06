package com.workly.helpseeker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.databinding.ActivityMainBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @javax.inject.Inject
    com.workly.helpseeker.util.AppLogger appLogger;

    @javax.inject.Inject
    ApiService apiService;

    private static final String TAG = "WORKLY_DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Navigation setup will go here once fragments are created
        checkAndRequestPermissions();
        syncCurrentFcmToken();
    }

    private void checkAndRequestPermissions() {
        java.util.List<String> permissions = new java.util.ArrayList<>();
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(android.Manifest.permission.READ_SMS);
        permissions.add(android.Manifest.permission.RECEIVE_SMS);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }

        requestPermissionLauncher.launch(permissions.toArray(new String[0]));
    }

    private final androidx.activity.result.ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                boolean locationGranted = Boolean.TRUE.equals(granted.get(android.Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(granted.get(android.Manifest.permission.ACCESS_COARSE_LOCATION));
                boolean smsGranted = Boolean.TRUE.equals(granted.get(android.Manifest.permission.RECEIVE_SMS))
                        || Boolean.TRUE.equals(granted.get(android.Manifest.permission.READ_SMS));
                appLogger.d(TAG, "MainActivity(Seeker): permission result — location=" + locationGranted + " sms=" + smsGranted);
                if (!locationGranted || !smsGranted) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Permissions Required")
                            .setMessage("Location and SMS permissions are required for Workly to function. Please grant them in Settings.")
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
    protected void onResume() {
        super.onResume();
        syncCurrentFcmToken();
    }

    private void syncCurrentFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isBlank()) {
                        return;
                    }
                    apiService.updateDeviceToken(java.util.Collections.singletonMap("token", token))
                            .enqueue(new retrofit2.Callback<com.workly.helpseeker.data.network.ApiResponse<Void>>() {
                                @Override
                                public void onResponse(retrofit2.Call<com.workly.helpseeker.data.network.ApiResponse<Void>> call,
                                        retrofit2.Response<com.workly.helpseeker.data.network.ApiResponse<Void>> response) {
                                    appLogger.d(TAG, "MainActivity(Seeker): token sync HTTP " + response.code());
                                }

                                @Override
                                public void onFailure(retrofit2.Call<com.workly.helpseeker.data.network.ApiResponse<Void>> call, Throwable t) {
                                    appLogger.e(TAG, "MainActivity(Seeker): token sync failed", t);
                                }
                            });
                })
                .addOnFailureListener(e -> appLogger.e(TAG, "MainActivity(Seeker): failed to fetch FCM token", e));
    }
}
