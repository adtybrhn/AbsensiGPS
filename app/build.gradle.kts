plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.absensigps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.absensigps"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Rekomendasi untuk compileSdk 35
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17" // Harus sama dengan sourceCompatibility
    }
}

dependencies {
    // Library Utama
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Google Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase (Menggunakan BOM adalah cara terbaik)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Versi stabil terbaru
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore") // TAMBAHKAN INI agar firestore dikenali!

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}