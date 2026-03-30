package com.workly.helpprovider.data.repository;

import androidx.lifecycle.LiveData;
import com.workly.helpprovider.data.local.ProfileDao;
import com.workly.helpprovider.data.model.Profile;
import com.workly.helpprovider.data.remote.ApiService;
import com.workly.helpprovider.data.remote.ApiResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import android.content.Context;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ProfileRepository {
    private final ProfileDao profileDao;
    private final ApiService apiService;
    private final ExecutorService executorService;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private static final String TAG = "WORKLY_DEBUG";
    private static final long PROFILE_STALENESS_THRESHOLD_MS = 300_000; // 5 minutes
    private android.content.SharedPreferences prefs;


    @Inject
    public ProfileRepository(ProfileDao profileDao, ApiService apiService, com.workly.helpprovider.util.AppLogger appLogger, @ApplicationContext Context context) {
        this.profileDao = profileDao;
        this.apiService = apiService;
        this.executorService = Executors.newSingleThreadExecutor();
        this.appLogger = appLogger;
        // The Application Context holds shared preferences securely
        this.prefs = context.getSharedPreferences("workly_prefs", Context.MODE_PRIVATE);
    }

    public LiveData<Profile> getProfile() {
        executorService.execute(() -> {
            Profile localProfile = profileDao.getProfileSync();
            if (localProfile == null) {
                appLogger.d(TAG, "ProfileRepository: Local DB is completely empty (wiped). Forcing network sync.");
                refreshProfile();
            } else if (!isProfileFresh()) {
                appLogger.d(TAG, "ProfileRepository: Profile cache is stale. Triggering background refresh.");
                refreshProfile();
            } else {
                appLogger.d(TAG, "ProfileRepository: Profile cache is fresh & DB valid, serving from local DB skip network sync.");
            }
        });
        return profileDao.getProfile();
    }

    private boolean isProfileFresh() {
        long lastProfileFetchTime = prefs.getLong("last_profile_fetch_time", 0);
        if (lastProfileFetchTime == 0) return false;
        return (System.currentTimeMillis() - lastProfileFetchTime) < PROFILE_STALENESS_THRESHOLD_MS;
    }

    private void refreshProfile() {
        appLogger.d(TAG, "ProfileRepository: Requesting refresh of provider profile from API...");
        apiService.getProfile().enqueue(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<Profile>> call, Response<ApiResponse<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    appLogger.d(TAG, "ProfileRepository: Successfully retrieved profile from network. Updating local DB.");
                    Profile p = response.body().getData();
                    // Sync skills list back to expertise string
                    if (p.getSkills() != null && !p.getSkills().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < p.getSkills().size(); i++) {
                            sb.append(p.getSkills().get(i));
                            if (i < p.getSkills().size() - 1) {
                                sb.append(", ");
                            }
                        }
                        p.setExpertise(sb.toString());
                    }
                    executorService.execute(() -> profileDao.insertProfile(p));
                    prefs.edit().putLong("last_profile_fetch_time", System.currentTimeMillis()).apply();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Profile>> call, Throwable t) {
                appLogger.e(TAG, "ProfileRepository: Network error refreshing profile data: " + t.getMessage(), t);
            }
        });
    }

    public void updateProfile(Profile profile) {
        appLogger.d(TAG, "ProfileRepository: Dispatching Provider Profile Update payload.");
        apiService.updateProfile(profile).enqueue(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<Profile>> call, Response<ApiResponse<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Profile p = response.body().getData();
                    // Sync skills list back to expertise string
                    if (p.getSkills() != null && !p.getSkills().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < p.getSkills().size(); i++) {
                            sb.append(p.getSkills().get(i));
                            if (i < p.getSkills().size() - 1) {
                                sb.append(", ");
                            }
                        }
                        p.setExpertise(sb.toString());
                    }
                    executorService.execute(() -> profileDao.insertProfile(p));
                    prefs.edit().putLong("last_profile_fetch_time", System.currentTimeMillis()).apply();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Profile>> call, Throwable t) {
                appLogger.e(TAG, "ProfileRepository: Network error updating profile data: " + t.getMessage(), t);
            }
        });
    }

    public void updateAvailability(boolean isAvailable, Callback<ApiResponse<Void>> callback) {
        appLogger.d(TAG, "ProfileRepository: Toggling provider availability: " + isAvailable);
        apiService.updateAvailability(isAvailable).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    executorService.execute(() -> profileDao.updateAvailability(isAvailable));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void updateDeviceToken(String token) {
        appLogger.d(TAG, "ProfileRepository: Dispatching new Device Token to backend mapping.");
        java.util.Map<String, String> tokenMap = java.util.Collections.singletonMap("token", token);
        apiService.updateDeviceToken(tokenMap)
                .enqueue(new Callback<com.workly.helpprovider.data.remote.ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            Response<com.workly.helpprovider.data.remote.ApiResponse<Void>> response) {
                        // Token updated
                    }

                    @Override
                    public void onFailure(Call<com.workly.helpprovider.data.remote.ApiResponse<Void>> call,
                            Throwable t) {
                        appLogger.e(TAG, "ProfileRepository: Failed to dispatch Device Token: " + t.getMessage(), t);
                    }
                });
    }

    public void getWorkerAverageRating(String mobileNumber, Callback<ApiResponse<Double>> callback) {
        apiService.getAverageRating(mobileNumber).enqueue(callback);
    }
}
