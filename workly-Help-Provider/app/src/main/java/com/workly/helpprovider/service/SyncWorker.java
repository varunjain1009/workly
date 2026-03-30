package com.workly.helpprovider.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.data.remote.ApiService;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

@HiltWorker
public class SyncWorker extends Worker {

    private static final String TAG = "WORKLY_DEBUG";

    private final AuthManager authManager;
    private final ApiService apiService;

    @AssistedInject
    public SyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters workerParams,
            AuthManager authManager,
            ApiService apiService) {
        super(context, workerParams);
        this.authManager = authManager;
        this.apiService = apiService;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!authManager.isLoggedIn()) {
            Log.w(TAG, "SyncWorker: User not logged in, skipping sync.");
            return Result.failure();
        }

        // Implementation for periodic sync (e.g., fetching latest job requests or
        // profile updates) using this.apiService
        Log.d(TAG, "SyncWorker: Periodic sync started.");

        return Result.success();
    }
}
