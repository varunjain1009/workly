package com.workly.helpseeker.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.workly.helpseeker.data.model.ChatMessage;

@Database(entities = { ChatMessage.class }, version = 1, exportSchema = false)
public abstract class WorklyDatabase extends RoomDatabase {
    public abstract ChatMessageDao chatMessageDao();
}
