package com.workly.helpseeker.ui.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpseeker.MainActivity;
import com.workly.helpseeker.data.auth.AuthManager;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.data.network.AuthResponse;
import com.workly.helpseeker.data.network.LoginRequest;
import com.workly.helpseeker.data.network.OtpRequest;
import com.workly.helpseeker.databinding.ActivityLoginBinding;

import java.util.Properties;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "WORKLY_DEBUG";
    private ActivityLoginBinding binding;

    @Inject
    ApiService apiService;

    @Inject
    AuthManager authManager;

    @Inject
    Properties properties;

    @Inject
    com.workly.helpseeker.data.config.ConfigManager configManager;

    @Inject
    com.workly.helpseeker.util.AppLogger appLogger;

    private CountDownTimer countDownTimer;
    private long resendDelayMs = 300000; // Default 5 mins

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                boolean locationGranted = Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                boolean smsGranted = Boolean.TRUE.equals(granted.get(Manifest.permission.RECEIVE_SMS))
                        || Boolean.TRUE.equals(granted.get(Manifest.permission.READ_SMS));
                android.util.Log.d(TAG, "LoginActivity(Seeker): permission result — location=" + locationGranted + " sms=" + smsGranted);
                if (!locationGranted || !smsGranted) {
                    showPermissionDeniedDialog();
                }
            });

    private void requestRequiredPermissions() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }
        if (!missing.isEmpty()) {
            android.util.Log.d(TAG, "LoginActivity(Seeker): requesting " + missing.size() + " missing permissions");
            permissionLauncher.launch(missing.toArray(new String[0]));
        } else {
            android.util.Log.d(TAG, "LoginActivity(Seeker): all required permissions already granted");
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Location and SMS permissions are required for Workly to function. Please grant them in Settings.")
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Quit", (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestRequiredPermissions();

        // Disable UI and show overlay until config is loaded
        binding.btnSendOtp.setEnabled(false);
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        configManager.fetchConfig();
        configManager.getOnConfigLoaded().observe(this, config -> {
            android.util.Log.d(TAG, "LoginActivity(Seeker): config callback fired — config=" + config);
            binding.loadingOverlay.setVisibility(View.GONE);
            binding.btnSendOtp.setEnabled(binding.etPhone.getText().length() == 10);
        });

        resendDelayMs = Long.parseLong(properties.getProperty("auth.otp.resend_delay_seconds", "300")) * 1000;

        appLogger.d(TAG, "Checking Auto-Login status...");
        boolean loggedIn = authManager.isLoggedIn();
        appLogger.d(TAG, "isLoggedIn: " + loggedIn);

        if (loggedIn) {
            performAutoLogin();
        } else {
            appLogger.d(TAG, "Not logged in. UI should be showing Login fields.");
            binding.btnSendOtp.setText("GET OTP");
            binding.getRoot().setBackgroundColor(android.graphics.Color.LTGRAY);
        }

        binding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnSendOtp.setEnabled(s.length() == 10 && s.toString().matches("\\d{10}"));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.etOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnVerifyOtp.setEnabled(s.length() == 4 && s.toString().matches("\\d{4}"));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnSendOtp.setOnClickListener(v -> {
            String phone = binding.etPhone.getText().toString();
            android.util.Log.d(TAG, "LoginActivity(Seeker): GET OTP tapped — phone=" + phone
                    + " overlayVisible=" + (binding.loadingOverlay.getVisibility() == View.VISIBLE));
            if (phone.matches("\\d{10}")) {
                binding.loadingOverlay.setVisibility(View.GONE); // safety: ensure overlay never blocks
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnSendOtp.setEnabled(false);
                apiService.requestOtp(new OtpRequest(phone)).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        android.util.Log.d(TAG, "LoginActivity(Seeker): OTP response HTTP " + response.code());
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            appLogger.d(TAG, "OTP Requested Successfully for: " + phone);
                            binding.tilOtp.setVisibility(View.VISIBLE);
                            binding.btnVerifyOtp.setVisibility(View.VISIBLE);
                            binding.btnVerifyOtp.setEnabled(binding.etOtp.getText().length() == 4);
                            startResendTimer();
                            Snackbar.make(binding.getRoot(), "OTP Sent", Snackbar.LENGTH_SHORT).show();
                        } else {
                            binding.btnSendOtp.setEnabled(true);
                            appLogger.e(TAG, "Failed to request OTP. Status: " + response.code());
                            Snackbar.make(binding.getRoot(), "Failed to send OTP", Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        android.util.Log.e(TAG, "LoginActivity(Seeker): OTP network failure — " + t.getMessage(), t);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSendOtp.setEnabled(true);
                        appLogger.e(TAG, "Network error during Requesting OTP: " + t.getMessage(), t);
                        Snackbar.make(binding.getRoot(), "Network Error", Snackbar.LENGTH_LONG).show();
                    }
                });
            } else {
                binding.tilPhone.setError("Invalid mobile number");
            }
        });

        binding.btnVerifyOtp.setOnClickListener(v -> {
            String phone = binding.etPhone.getText().toString();
            String otp = binding.etOtp.getText().toString();
            appLogger.d(TAG, "Verifying OTP for: " + phone);

            apiService.login(new LoginRequest(phone, otp)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<AuthResponse>> call,
                        Response<ApiResponse<AuthResponse>> response) {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        String token = response.body().getData().getToken();
                        appLogger.d(TAG, "Login Successful! Token received.");
                        authManager.saveToken(token);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        appLogger.e(TAG, "Login failed. Status: " + response.code());
                        Snackbar.make(binding.getRoot(), "Invalid OTP", Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                    if (isFinishing() || isDestroyed()) return;
                    appLogger.e(TAG, "Network error during Login: " + t.getMessage(), t);
                    Snackbar.make(binding.getRoot(), "Network Error", Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    private void startResendTimer() {
        if (countDownTimer != null)
            countDownTimer.cancel();

        binding.btnSendOtp.setEnabled(false);
        countDownTimer = new CountDownTimer(resendDelayMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.btnSendOtp.setText(String.format("RESEND IN %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                binding.btnSendOtp.setText("RESEND OTP");
                binding.btnSendOtp.setEnabled(binding.etPhone.getText().toString().matches("\\d{10}"));
            }
        }.start();
    }

    private void performAutoLogin() {
        appLogger.d(TAG, "Auto-login started. If the screen is stuck here, check network/timeout.");
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.refresh().enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    appLogger.d(TAG, "Auto-login successful! Token refreshed.");
                    authManager.saveToken(response.body().getData().getToken());
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    appLogger.w(TAG, "Auto-login failed. Token may be expired.");
                    authManager.clearToken();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);
                appLogger.e(TAG, "Auto-login failed: " + t.getMessage());
                authManager.clearToken();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null)
            countDownTimer.cancel();
        super.onDestroy();
    }
}
