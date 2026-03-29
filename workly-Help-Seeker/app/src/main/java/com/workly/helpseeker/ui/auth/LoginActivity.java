package com.workly.helpseeker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        // Disable UI until config is loaded
        binding.btnSendOtp.setEnabled(false);
        // Show loading if needed, or just wait locally

        configManager.fetchConfig();
        // Give it a moment or update UI when config is ready?
        // For simplicity, we assume network is fast or defaults are used.
        // But better is to observe. ConfigManager assumes a simple fetch.
        // We will delay slightly or just proceed with defaults if not ready.
        // Ideally we should have a callback/observable.
        // Let's modify ConfigManager to accept a callback or use LiveData?
        // Keeping it simple: We trigger fetch here. We use values if available.

        // Give it a moment or update UI when config is ready?
        // Let's make ConfigManager have a callback for "onFetchComplete"

        resendDelayMs = Long.parseLong(properties.getProperty("auth.otp.resend_delay_seconds", "300")) * 1000;

        if (authManager.isLoggedIn()) {
            performAutoLogin();
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
                        if (response.isSuccessful()) {
                            appLogger.d(TAG, "OTP Requested Successfully for: " + phone);
                            binding.tilOtp.setVisibility(View.VISIBLE);
                            binding.btnVerifyOtp.setVisibility(View.VISIBLE);
                            binding.btnVerifyOtp.setEnabled(binding.etOtp.getText().length() == 4);
                            startResendTimer();
                            Toast.makeText(LoginActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                        } else {
                            appLogger.e(TAG, "Failed to request OTP. Status: " + response.code());
                            Toast.makeText(LoginActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        appLogger.e(TAG, "Network error during Requesting OTP: " + t.getMessage(), t);
                        Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
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
                    if (response.isSuccessful() && response.body() != null) {
                        String token = response.body().getData().getToken();
                        appLogger.d(TAG, "Login Successful! Token received.");
                        authManager.saveToken(token);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        appLogger.e(TAG, "Login failed. Status: " + response.code());
                        Toast.makeText(LoginActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                    appLogger.e(TAG, "Network error during Login: " + t.getMessage(), t);
                    Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
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
        appLogger.d(TAG, "Attempting auto-login with existing token...");

        // Show progress or splash-like state if needed, here we just call the API
        apiService.refresh().enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    appLogger.d(TAG, "Auto-login successful! Token refreshed.");
                    authManager.saveToken(response.body().getData().getToken());
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    appLogger.w(TAG, "Auto-login failed. Token may be expired.");
                    authManager.clearToken();
                    // Stay on login screen
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                appLogger.e(TAG, "Network error during auto-login: " + t.getMessage());
                // In case of network error, we might want to let user retry manually or stay on
                // login
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
