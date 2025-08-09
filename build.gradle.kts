// build.gradle.kts (niveau projet)

plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1") // ✅ dernière version stable recommandée

    }

}
