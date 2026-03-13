package com.nueng.translator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nueng.translator.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND password_hash = :passwordHash LIMIT 1")
    suspend fun login(username: String, passwordHash: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM users ORDER BY last_online DESC")
    suspend fun getAllUsers(): List<User>

    @Query("UPDATE users SET last_online = :timestamp WHERE id = :userId")
    suspend fun updateLastOnline(userId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM users WHERE role != 'guest' AND last_online < :cutoffTime")
    suspend fun getInactiveUsers(cutoffTime: Long): List<User>

    @Query("DELETE FROM users WHERE role != 'admin' AND role != 'guest' AND last_online < :cutoffTime")
    suspend fun deleteInactiveUsers(cutoffTime: Long): Int
}
