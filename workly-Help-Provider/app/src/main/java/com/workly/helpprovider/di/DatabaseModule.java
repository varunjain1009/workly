package com.workly.helpprovider.di;

import android.content.Context;
import androidx.room.Room;
import com.workly.helpprovider.data.local.ChatMessageDao;
import com.workly.helpprovider.data.local.WorklyProviderDatabase;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public WorklyProviderDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, WorklyProviderDatabase.class, "workly_provider_db")
                .addMigrations(WorklyProviderDatabase.MIGRATION_1_2)
                .build();
    }

    @Provides
    public ChatMessageDao provideChatMessageDao(WorklyProviderDatabase database) {
        return database.chatMessageDao();
    }

    @Provides
    public com.workly.helpprovider.data.local.ProfileDao provideProfileDao(WorklyProviderDatabase database) {
        return database.profileDao();
    }
}
