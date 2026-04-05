import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}
val localProps = Properties()
localProps.load((rootProject.file("local.properties").inputStream()))
android {
    namespace = "com.nueng.translator"
    compileSdk = 36
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.nueng.translator"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "IMGBB_API_KEY", "\"${localProps["imgbb.api.key"]}\"")
        buildConfigField("String", "ADMIN_USERNAME", "\"${localProps["admin.username"]}\"")
        buildConfigField("String", "ADMIN_PASSWORD", "\"${localProps["admin.password"]}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/nueng-translator.jks")
            storePassword = "NuengTranslator2026"
            keyAlias = "nuengtranslator"
            keyPassword = "NuengTranslator2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation.core)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room (SQLite)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt (DI)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ML Kit
    implementation(libs.mlkit.digital.ink)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // QR Code generate + scan
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0@aar") { isTransitive = true }

    // Pinyin4j — offline Chinese pinyin lookup
    implementation("com.github.houbb:pinyin:0.4.0")

    // Coil — async image loading from URL
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
}
