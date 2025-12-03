package com.yourname.sensemeclone.database

import androidx.room.*

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<Song>

    @Update
    suspend fun update(song: Song)
}
