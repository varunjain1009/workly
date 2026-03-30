package com.workly.helpprovider.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.workly.helpprovider.data.model.Profile;

@Dao
public interface ProfileDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    LiveData<Profile> getProfile();

    @Query("SELECT * FROM profiles LIMIT 1")
    Profile getProfileSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProfile(Profile profile);

    @Query("UPDATE profiles SET available = :available")
    void updateAvailability(boolean available);

    @Query("DELETE FROM profiles")
    void clearProfile();
}
