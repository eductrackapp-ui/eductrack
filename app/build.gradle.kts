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

    // ✅ Firebase (avec versions explicites)
    implementation("com.google.firebase:firebase-auth:21.2.0")
    implementation("com.google.firebase:firebase-database:20.2.2")
    implementation("com.google.firebase:firebase-firestore:24.6.1")
    implementation("com.google.firebase:firebase-storage:20.2.1")

    // ✅ EmailJS
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
