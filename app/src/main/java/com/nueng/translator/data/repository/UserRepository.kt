package com.nueng.translator.data.repository

import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.entity.User
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager,
    private val firebaseUserRepository: FirebaseUserRepository
) {
    suspend fun register(username: String, password: String, role: String = "user"): Result<User> {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) {
            return Result.failure(Exception("Username already exists"))
        }
        val hash = hashPassword(password)
        val user = User(username = username, passwordHash = hash, role = role)
        val id = userDao.insertUser(user)
        val created = user.copy(id = id)
        preferencesManager.setLoggedInUser(id, isGuest = false)
        // Sync to Firebase
        firebaseUserRepository.registerUser(username, role)
        return Result.success(created)
    }

    suspend fun login(username: String, password: String): Result<User> {
        val hash = hashPassword(password)
        val user = userDao.login(username, hash)
            ?: return Result.failure(Exception("Invalid username or password"))
        userDao.updateLastOnline(user.id)
        preferencesManager.setLoggedInUser(user.id, isGuest = false)
        // Update Firebase last online
        firebaseUserRepository.updateLastOnline(username)
        return Result.success(user)
    }

    suspend fun loginAsGuest(): Result<User> {
        val guestUsername = "guest_${System.currentTimeMillis()}"
        val guest = User(username = guestUsername, passwordHash = "", role = "guest")
        val id = userDao.insertUser(guest)
        preferencesManager.setLoggedInUser(id, isGuest = true)
        return Result.success(guest.copy(id = id))
    }

    suspend fun logout() {
        preferencesManager.clearSession()
    }

    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)

    suspend fun updateLastOnline(userId: Long) {
        userDao.updateLastOnline(userId)
        val user = userDao.getUserById(userId)
        user?.let { firebaseUserRepository.updateLastOnline(it.username) }
    }

    suspend fun cleanupInactiveUsers(): Int {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val cutoff = System.currentTimeMillis() - thirtyDaysMs
        return userDao.deleteInactiveUsers(cutoff)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
