package com.workly.helpprovider.di;

import android.content.Context;

import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.util.AppLogger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public AuthManager provideAuthManager(@ApplicationContext Context context, AppLogger appLogger) {
        return new AuthManager(context, appLogger);
    }
}
