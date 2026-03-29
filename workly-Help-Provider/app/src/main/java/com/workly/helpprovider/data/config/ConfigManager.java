package com.workly.helpprovider.data.config;

import android.util.Log;
import com.workly.helpprovider.data.model.ConfigResponse;
import com.workly.helpprovider.data.remote.ApiService;
import com.workly.helpprovider.data.remote.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.workly.helpprovider.util.AppLogger;

@Singleton
public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private final ApiService apiService;
    private ConfigResponse cachedConfig;
    private final AppLogger appLogger;

    @Inject
    public ConfigManager(ApiService apiService, AppLogger appLogger) {
        this.apiService = apiService;
        this.appLogger = appLogger;
    }

    public void fetchConfig() {
        apiService.getPublicConfig().enqueue(new Callback<ApiResponse<ConfigResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ConfigResponse>> call, Response<ApiResponse<ConfigResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedConfig = response.body().getData();
                    appLogger.d(TAG, "Config fetched successfully");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ConfigResponse>> call, Throwable t) {
                appLogger.e(TAG, "Failed to fetch config", t);
            }
        });
    }

    public void syncConfig() {
        appLogger.d(TAG, "Syncing config from server...");
        fetchConfig();
    }

    public ConfigResponse getConfig() {
        return cachedConfig;
    }
}
