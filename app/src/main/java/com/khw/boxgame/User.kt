package com.khw.boxgame

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "length") val length: Int?,
    @ColumnInfo(name = "time") val time: String?,
    @ColumnInfo(name = "count") val count: Int?
)