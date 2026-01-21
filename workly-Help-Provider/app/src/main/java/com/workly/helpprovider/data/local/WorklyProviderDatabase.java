package com.workly.helpprovider.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.workly.helpprovider.data.model.ChatMessage;
import com.workly.helpprovider.data.model.Profile;

@Database(entities = { ChatMessage.class, Profile.class }, version = 1, exportSchema = false)
public abstract class WorklyProviderDatabase extends RoomDatabase {
    public abstract ChatMessageDao chatMessageDao();
    public abstract ProfileDao profileDao();
}
