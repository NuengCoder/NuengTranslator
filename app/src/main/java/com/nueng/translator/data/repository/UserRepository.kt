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
    private val firebaseUserRepository: FirebaseUserRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userDataRepository: UserDataRepository
) {

    // ── Register ─────────────────────────────────────────────────────────
    suspend fun register(username: String, password: String, role: String = "user"): Result<User> {
        // 1. Check local Room first (fast path)
        val localExisting = userDao.getUserByUsername(username)
        if (localExisting != null) {
            return Result.failure(Exception("Username already exists"))
        }

        // 2. Check Firebase (cross-device uniqueness check)
        val fbExists = firebaseUserRepository.checkUsernameExists(username)
        if (fbExists) {
            return Result.failure(Exception("Username already taken on another device"))
        }

        val hash = hashPassword(password)
        val user = User(username = username, passwordHash = hash, role = role)
        val id   = userDao.insertUser(user)
        val created = user.copy(id = id)

        preferencesManager.setLoggedInUser(id, isGuest = false)

        // 3. Push to Firebase (both public users/ and private accounts/)
        firebaseUserRepository.registerUser(username, role)
        firebaseUserRepository.saveAccountCredentials(username, hash, role)

        // 4. Create online profile keyed by USERNAME
        ensureOnlineProfile(username, role)

        return Result.success(created)
    }

    // ── Login ─────────────────────────────────────────────────────────────
    suspend fun login(username: String, password: String): Result<User> {
        val hash = hashPassword(password)

        // 1. Try local Room first
        var user = userDao.login(username, hash)

        // 2. If not in Room, try pulling from Firebase (new device)
        if (user == null) {
            val fbAccount = firebaseUserRepository.getAccountFromFirebase(username)
                ?: return Result.failure(Exception("Invalid username or password"))

            // Verify password hash matches what's on Firebase
            if (fbAccount.passwordHash != hash) {
                return Result.failure(Exception("Invalid username or password"))
            }

            // Create local Room row from Firebase data
            val newUser = User(
                username     = fbAccount.username,
                passwordHash = fbAccount.passwordHash,
                role         = fbAccount.role,
                createdAt    = fbAccount.createdAt,
                lastOnline   = System.currentTimeMillis()
            )
            val newId = userDao.insertUser(newUser)
            user = newUser.copy(id = newId)
        }

        userDao.updateLastOnline(user.id)
        preferencesManager.setLoggedInUser(user.id, isGuest = false)
        firebaseUserRepository.updateLastOnline(username)

        // 3. Ensure online profile exists (keyed by username)
        ensureOnlineProfile(username, user.role)

        // 4. Pull user notes/dirs from Firebase -> local Room
        if (user.role != "admin") {
            userDirectoryRepository.pullFromFirebase(user.id, username)
            userDataRepository.pullFromFirebase(user.id, username)
        }

        return Result.success(user)
    }

    // ── Guest ─────────────────────────────────────────────────────────────
    suspend fun loginAsGuest(): Result<User> {
        val guestUsername = "guest_${System.currentTimeMillis()}"
        val guest = User(username = guestUsername, passwordHash = "", role = "guest")
        val id = userDao.insertUser(guest)
        preferencesManager.setLoggedInUser(id, isGuest = true)
        return Result.success(guest.copy(id = id))
    }

    suspend fun logout() { preferencesManager.clearSession() }

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

    // ── Ensure online profile node exists, keyed by USERNAME ─────────────
    private fun ensureOnlineProfile(username: String, role: String) {
        if (username.isBlank()) return
        val db  = com.google.firebase.database.FirebaseDatabase.getInstance(
            "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
        val rank = if (role == "admin" && username == "NuengAdmin") "DevAdmin" else "Normal"
        val ref  = db.getReference("online_profiles").child(username)
        ref.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any>(
                "username" to username,
                "rank"     to rank
            )
            if (!snapshot.hasChild("nickname"))             updates["nickname"]             = ""
            if (!snapshot.hasChild("bio"))                  updates["bio"]                  = ""
            if (!snapshot.hasChild("nickname_last_changed"))updates["nickname_last_changed"] = 0L
            ref.updateChildren(updates)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
