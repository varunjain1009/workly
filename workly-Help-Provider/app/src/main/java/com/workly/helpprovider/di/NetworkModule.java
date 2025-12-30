package com.workly.helpprovider.di;

import android.content.Context;
import android.util.Log;

import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.data.network.AuthInterceptor;
import com.workly.helpprovider.data.remote.ApiService;

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
        String baseUrl = properties.getProperty("backend.url", "https://api.workly.com/v1/");

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
