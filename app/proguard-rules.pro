# NuengTranslator ProGuard Rules

# Keep Room entities
-keep class com.nueng.translator.data.local.entity.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep data classes used with Room
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
