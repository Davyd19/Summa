plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // HAPUS: id("kotlin-kapt")
    // GANTI DENGAN KSP:
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt) // Gunakan alias dari TOML
    id("com.google.gms.google-services")
}

android {
    namespace = "com.app.summa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.summa"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    composeOptions {
        // HAPUS BARIS INI: kotlinCompilerExtensionVersion = "1.5.15"
        // (Alasannya: Plugin kotlin-compose otomatis menangani versi compiler di Kotlin 2.0)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// HAPUS BLOK KAPT:
// kapt { correctErrorTypes = true }

dependencies {
    // Core Android (Gunakan libs. dari TOML agar rapi, atau biarkan hardcode tapi konsisten)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // --- ROOM (MIGRASI KE KSP & TOML) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // GANTI kapt JADI ksp
    ksp(libs.androidx.room.compiler)

    // --- HILT (MIGRASI KE KSP & TOML) ---
    // Pastikan menggunakan versi dari TOML (2.51.1) bukan hardcode 2.48
    implementation(libs.hilt.android)
    // GANTI kapt JADI ksp
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Accompanist
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")
}