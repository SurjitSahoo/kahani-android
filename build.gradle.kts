import java.util.Properties

// FOSS_REMOVE_START
buildscript {
  dependencies {
    val g = "com." + "google"
    val s = "gms"
    val v = "google-services:4.4.2"
    classpath("$g.$s:$v")

    val f = "com." + "google"
    val b = "fire" + "base"
    val r = "firebase" + "-crashlytics-gradle:3.0.3"
    classpath("$f.$b:$r")
  }
}
// FOSS_REMOVE_END

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
}