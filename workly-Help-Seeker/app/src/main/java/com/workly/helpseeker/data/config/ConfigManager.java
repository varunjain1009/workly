package com.workly.helpseeker.data.config;

import com.workly.helpseeker.data.model.ConfigResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.data.network.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import javax.inject.Singleton;
import android.util.Log;

@Singleton
public class ConfigManager {
    private static final String TAG = "WORKLY_DEBUG";
    private final ApiService apiService;
    private ConfigResponse cachedConfig;
    private final com.workly.helpseeker.util.AppLogger appLogger;

    @Inject
    public ConfigManager(ApiService apiService, com.workly.helpseeker.util.AppLogger appLogger) {
        this.apiService = apiService;
        this.appLogger = appLogger;
    }

    public void fetchConfig() {
        appLogger.d(TAG, "ConfigManager: Dispatching public config network request...");
        apiService.getPublicConfig().enqueue(new Callback<ApiResponse<ConfigResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ConfigResponse>> call, Response<ApiResponse<ConfigResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedConfig = response.body().getData();
                    appLogger.d(TAG, "ConfigManager: Successfully resolved and cached remote configuration payload.");
                } else {
                    appLogger.e(TAG, "ConfigManager: Failed resolving remote config. Status code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ConfigResponse>> call, Throwable t) {
                appLogger.e(TAG, "ConfigManager: Network transport error during config sync: " + t.getMessage(), t);
            }
        });
    }

    public void syncConfig() {
        appLogger.d(TAG, "ConfigManager: Outer wrapper syncConfig() invoked globally.");
        fetchConfig();
    }

    public ConfigResponse getConfig() {
        return cachedConfig;
    }
}
