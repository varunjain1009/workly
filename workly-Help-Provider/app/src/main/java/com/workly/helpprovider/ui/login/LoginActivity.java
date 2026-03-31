package com.workly.helpprovider.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import com.workly.helpprovider.databinding.ActivityLoginBinding;
import com.workly.helpprovider.ui.main.MainActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "WORKLY_DEBUG";
    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private CountDownTimer countDownTimer;
    // private final long resendDelayMs = 300000; // Removed final/hardcoded

    @javax.inject.Inject
    com.workly.helpprovider.data.config.ConfigManager configManager;

    @javax.inject.Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Disable UI and show overlay until config is loaded
        binding.btnSendOtp.setEnabled(false);
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        appLogger.d(TAG, "LoginActivity started. No saved session — showing login fields.");
        binding.btnSendOtp.setText("READY - ENTER PHONE");
        binding.getRoot().setBackgroundColor(android.graphics.Color.LTGRAY);

        configManager.fetchConfig();
        configManager.getOnConfigLoaded().observe(this, config -> {
            appLogger.d(TAG, "Config loaded, enabling UI.");
            binding.loadingOverlay.setVisibility(View.GONE);
            binding.btnSendOtp.setEnabled(binding.etPhone.getText().length() == 10);
        });

        setupListeners();
        observeViewModel();
    }

    // Helper to get delay dynamically
    private long getResendDelayMs() {
        if (configManager.getConfig() != null) {
            return configManager.getConfig().getOtpResendDelaySeconds() * 1000L;
        }
        return 300000; // Default
    }

    private void setupListeners() {
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
            if (phone.matches("\\d{10}")) {
                viewModel.requestOtp(phone);
            } else {
                binding.tilPhone.setError("Invalid mobile number");
            }
        });

        binding.btnVerifyOtp.setOnClickListener(v -> {
            String phone = binding.etPhone.getText().toString();
            String otp = binding.etOtp.getText().toString();
            viewModel.verifyOtp(phone, otp);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSendOtp.setEnabled(!isLoading && binding.etPhone.getText().toString().matches("\\d{10}"));
            binding.btnVerifyOtp.setEnabled(!isLoading && binding.etOtp.getText().toString().matches("\\d{4}"));
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                appLogger.e(TAG, "Login Error: " + error);
            }
        });

        viewModel.getOtpSent().observe(this, sent -> {
            if (sent) {
                binding.tilOtp.setVisibility(View.VISIBLE);
                binding.btnVerifyOtp.setVisibility(View.VISIBLE);
                // Enable verify button if OTP length matches (unlikely to have 4 chars
                // immediately, but logic is safe)
                binding.btnVerifyOtp.setEnabled(binding.etOtp.getText().length() == 4);
                startResendTimer();
                Snackbar.make(binding.getRoot(), "OTP Sent", Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }

    private void startResendTimer() {
        if (countDownTimer != null)
            countDownTimer.cancel();

        binding.btnSendOtp.setEnabled(false);
        countDownTimer = new CountDownTimer(getResendDelayMs(), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.btnSendOtp.setText(String.format("RESEND IN %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                binding.btnSendOtp.setText("RESEND OTP");
                String phone = binding.etPhone.getText().toString();
                binding.btnSendOtp.setEnabled(phone.matches("\\d{10}"));
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null)
            countDownTimer.cancel();
        super.onDestroy();
    }
}
