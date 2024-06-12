package com.khw.boxgame

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query(
        "SELECT *, rowid AS rank FROM user WHERE length = :length ORDER BY time, count"
    )
    fun getRank(length: Int): Flow<List<User>>

    @Insert
    fun insertAll(vararg users: User)

    @Delete
    fun delete(user: User)
}