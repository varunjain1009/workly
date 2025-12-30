package com.workly.helpseeker.di;

import android.content.Context;
import androidx.room.Room;
import com.workly.helpseeker.data.local.ChatMessageDao;
import com.workly.helpseeker.data.local.WorklyDatabase;
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
    public WorklyDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, WorklyDatabase.class, "workly_db")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public ChatMessageDao provideChatMessageDao(WorklyDatabase database) {
        return database.chatMessageDao();
    }
}
