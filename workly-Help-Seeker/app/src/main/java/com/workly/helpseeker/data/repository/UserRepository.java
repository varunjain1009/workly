package com.workly.helpseeker.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.workly.helpseeker.data.model.User;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.util.AppLogger;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Single source of truth for the Seeker's own User profile.
 *
 * Caching strategy:
 *  1. On cold boot, reads from SharedPreferences (survives app kills).
 *  2. If the prefs store is empty (user cleared app data), a backend API call
 *     is triggered automatically to re-populate the cache.
 *  3. On a successful backend fetch, data is written back to SharedPreferences.
 */
@Singleton
public class UserRepository {

    private static final String TAG = "WORKLY_DEBUG";
    private static final String PREF_NAME = "workly_user_cache";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_ID = "user_id";
    private static final String KEY_IMAGE = "user_image";
    private static final String KEY_MONETIZATION = "user_monetization";
    private static final String KEY_SUBSCRIPTION = "user_subscription";
    private static final String KEY_LAST_FETCH = "user_last_fetch_ms";
    private static final long STALENESS_MS = 5 * 60 * 1000L; // 5 min

    private final ApiService apiService;
    private final AppLogger appLogger;
    private final SharedPreferences prefs;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();

    @Inject
    public UserRepository(ApiService apiService, AppLogger appLogger,
                          @ApplicationContext Context context) {
        this.apiService = apiService;
        this.appLogger = appLogger;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns LiveData for the Seeker User.
     * Immediately emits a cached User from SharedPreferences if available,
     * then refreshes from backend if the cache is stale or empty.
     */
    public LiveData<User> getUser() {
        // Serve from local prefs immediately (no wait for network)
        User cached = loadFromPrefs();
        if (cached != null) {
            userLiveData.setValue(cached);
            appLogger.d(TAG, "UserRepository: Served user from local cache. name=" + cached.getName());
        }

        // If cache is missing or stale, hit the network
        if (cached == null || isCacheStale()) {
            appLogger.d(TAG, "UserRepository: Cache miss or stale — fetching from backend.");
            fetchFromBackend();
        }

        return userLiveData;
    }

    /**
     * Force-refreshes the user profile from the backend and updates the cache.
     */
    public void refresh() {
        appLogger.d(TAG, "UserRepository: Force refresh triggered.");
        fetchFromBackend();
    }

    private void fetchFromBackend() {
        apiService.getSeekerProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call,
                                   Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    User user = response.body().getData();
                    appLogger.d(TAG, "UserRepository: Fetched user from backend. name=" + user.getName());
                    saveToPrefs(user);
                    userLiveData.postValue(user);
                } else {
                    appLogger.e(TAG, "UserRepository: Backend returned error. code=" + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                appLogger.e(TAG, "UserRepository: Network error fetching user profile: " + t.getMessage(), t);
            }
        });
    }

    private User loadFromPrefs() {
        String name = prefs.getString(KEY_NAME, null);
        String phone = prefs.getString(KEY_PHONE, null);
        if (name == null && phone == null) return null;

        User user = new User();
        user.setName(name);
        user.setPhoneNumber(phone);
        user.setId(prefs.getString(KEY_ID, null));
        user.setProfileImageUrl(prefs.getString(KEY_IMAGE, null));
        user.setMonetizationEnabled(prefs.getBoolean(KEY_MONETIZATION, false));
        user.setSubscriptionStatus(prefs.getString(KEY_SUBSCRIPTION, null));
        return user;
    }

    private void saveToPrefs(User user) {
        prefs.edit()
                .putString(KEY_NAME, user.getName())
                .putString(KEY_PHONE, user.getPhoneNumber())
                .putString(KEY_ID, user.getId())
                .putString(KEY_IMAGE, user.getProfileImageUrl())
                .putBoolean(KEY_MONETIZATION, user.isMonetizationEnabled())
                .putString(KEY_SUBSCRIPTION, user.getSubscriptionStatus())
                .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                .apply();
        appLogger.d(TAG, "UserRepository: User data saved to SharedPreferences.");
    }

    private boolean isCacheStale() {
        long lastFetch = prefs.getLong(KEY_LAST_FETCH, 0);
        return (System.currentTimeMillis() - lastFetch) > STALENESS_MS;
    }
}
