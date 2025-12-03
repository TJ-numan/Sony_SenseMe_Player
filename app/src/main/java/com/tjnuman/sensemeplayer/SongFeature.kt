package com.tjnuman.sensemeplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_features")
data class SongFeature(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val mfcc: String // store as JSON string
)
