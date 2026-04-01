plugins {
    alias(libs.plugins.android.application)
    // ADDED FOR FIREBASE (Removed the duplicate android plugin):
    id("com.google.gms.google-services")
}

android {
    namespace = "com.miniflo.femcare"
    compileSdk = 36 // Fixed syntax

    defaultConfig {
        applicationId = "com.miniflo.femcare"
        minSdk = 24
        targetSdk = 36 // Matched to compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- 1. MVVM & LIFECYCLE ---
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

    // --- 2. ROOM DATABASE (Local Storage) ---
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version") // Required for Java

    // --- 3. FIREBASE (Cloud & Auth) ---
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // --- 4. WORKMANAGER (Background Tasks & Daily Math) ---
    implementation("androidx.work:work-runtime:2.9.0")

    // --- 5. RETROFIT & GSON (REST API) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.android.volley:volley:1.2.1")

    // --- 6. MPANDROIDCHART (For TrackFragment Graphs) ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- 7. GLIDE (Image preview for medical report uploads) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
