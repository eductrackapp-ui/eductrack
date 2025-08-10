plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Obligatoire pour Firebase
}

android {
    namespace = "com.equipe7.eductrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.equipe7.eductrack"
        minSdk = 26
        targetSdk = 35
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ✅ Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    // ✅ Firebase BoM - manages all Firebase dependency versions
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    
    // ✅ Firebase dependencies (versions managed by BoM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics") // Optional: for analytics

    // ✅ HTTP Client (keeping for other potential uses)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // ✅ JavaMail API for reliable email sending
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
