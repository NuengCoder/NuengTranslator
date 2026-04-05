package com.nueng.translator

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NuengTranslatorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // PERF: Enable disk persistence once, before any other Firebase call.
        //
        // What this does:
        //   - Caches all Firebase reads to local disk automatically
        //   - App works offline — reads return cached data instantly
        //   - Writes queue locally and sync when connection restores
        //   - Dramatically reduces repeat downloads on re-open
        //   - language_words, online_profiles, etc. are served from cache
        //
        // !! Must be called ONCE before any database reference is created !!
        FirebaseDatabase.getInstance(
            "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).setPersistenceEnabled(true)
    }
}
