package com.workly.helpseeker.di;

import android.content.Context;
import android.util.Log;

import com.workly.helpseeker.data.auth.AuthManager;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.data.network.AuthInterceptor;

import java.io.InputStream;
import java.util.Properties;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String TAG = "WORKLY_DEBUG";

    @Provides
    @Singleton
    public Properties provideProperties(@ApplicationContext Context context) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load config.properties", e);
        }
        return properties;
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(Properties properties, AuthInterceptor authInterceptor) {
        boolean debugEnabled = Boolean.parseBoolean(properties.getProperty("app.debug_enabled", "false"));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
            if (debugEnabled)
                Log.d(TAG, message);
        });
        logging.setLevel(debugEnabled ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Properties properties) {
        String baseUrl = com.workly.helpseeker.BuildConfig.SERVER_URL;
        boolean debugEnabled = Boolean.parseBoolean(properties.getProperty("app.debug_enabled", "false"));
        if (debugEnabled)
            Log.d(TAG, "Initializing Retrofit with Base URL: " + baseUrl);

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
