package com.workly.helpseeker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.workly.helpseeker.databinding.ActivityMainBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Navigation setup will go here once fragments are created
        checkAndRequestPermissions();
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
            new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                if (Boolean.TRUE.equals(isGranted.get(android.Manifest.permission.ACCESS_FINE_LOCATION))) {
                    android.util.Log.d("WorklyPermissions", "Location permission granted");
                }
                if (Boolean.TRUE.equals(isGranted.get(android.Manifest.permission.READ_SMS))) {
                    android.util.Log.d("WorklyPermissions", "SMS permission granted");
                }
                // Handle other permissions if needed
            });
}
