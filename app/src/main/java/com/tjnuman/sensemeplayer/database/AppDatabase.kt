package com.tjnuman.sensemeplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SongFeature::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songFeatureDao(): SongFeatureDao
}
