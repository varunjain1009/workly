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

@Singleton
public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private final ApiService apiService;
    private ConfigResponse cachedConfig;

    @Inject
    public ConfigManager(ApiService apiService) {
        this.apiService = apiService;
    }

    public void fetchConfig() {
        apiService.getPublicConfig().enqueue(new Callback<ApiResponse<ConfigResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ConfigResponse>> call, Response<ApiResponse<ConfigResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedConfig = response.body().getData();
                    Log.d(TAG, "Config fetched successfully");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ConfigResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch config", t);
            }
        });
    }

    public void syncConfig() {
        android.util.Log.d(TAG, "Syncing config from server...");
        fetchConfig();
    }

    public ConfigResponse getConfig() {
        return cachedConfig;
    }
}
