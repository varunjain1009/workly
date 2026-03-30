package com.workly.helpprovider.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.workly.helpprovider.data.model.ChatMessage;
import com.workly.helpprovider.data.model.Profile;

@Database(entities = { ChatMessage.class, Profile.class }, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class WorklyProviderDatabase extends RoomDatabase {
    public abstract ChatMessageDao chatMessageDao();
    public abstract ProfileDao profileDao();

    /** Adds the skills JSON column added when @Ignore was removed from Profile.skills */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD COLUMN skills TEXT");
        }
    };
}
