package com.yourname.sensemeclone.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Song::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
