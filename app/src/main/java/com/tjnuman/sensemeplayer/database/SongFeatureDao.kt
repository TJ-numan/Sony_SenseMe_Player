package com.tjnuman.sensemeplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongFeatureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(songFeature: SongFeature)

    @Query("SELECT * FROM song_features")
    suspend fun getAll(): List<SongFeature>
}
