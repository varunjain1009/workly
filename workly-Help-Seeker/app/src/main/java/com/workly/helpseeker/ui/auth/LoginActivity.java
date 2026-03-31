package com.workly.helpseeker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Disable UI and show overlay until config is loaded
        binding.btnSendOtp.setEnabled(false);
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        configManager.fetchConfig();
        configManager.getOnConfigLoaded().observe(this, config -> {
            appLogger.d(TAG, "Config loaded, enabling UI.");
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
            binding.btnSendOtp.setText("READY - ENTER PHONE");
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
            appLogger.d(TAG, "Login Attempt - Requesting OTP");
            if (phone.matches("\\d{10}")) {
                apiService.requestOtp(new OtpRequest(phone)).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful()) {
                            appLogger.d(TAG, "OTP Requested Successfully for: " + phone);
                            binding.tilOtp.setVisibility(View.VISIBLE);
                            binding.btnVerifyOtp.setVisibility(View.VISIBLE);
                            binding.btnVerifyOtp.setEnabled(binding.etOtp.getText().length() == 4);
                            startResendTimer();
                            Snackbar.make(binding.getRoot(), "OTP Sent", Snackbar.LENGTH_SHORT).show();
                        } else {
                            appLogger.e(TAG, "Failed to request OTP. Status: " + response.code());
                            Snackbar.make(binding.getRoot(), "Failed to send OTP", Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
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
