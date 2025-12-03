package com.yourname.sensemeclone.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val filePath: String,
    val mood: String? = null  // Will be filled after clustering
)
