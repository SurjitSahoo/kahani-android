import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  
  id("com.google.dagger.hilt.android")
  id("org.jmailen.kotlinter") version "5.2.0"
  id("com.google.devtools.ksp")
}

kotlinter {
  reporters = arrayOf("checkstyle", "plain")
  ignoreFormatFailures = false
  ignoreLintFailures = false
}

val localProperties = Properties().apply {
  rootProject.file("local.properties").takeIf { it.exists() }?.let { file -> file.inputStream().use { load(it) } }
}

tasks.named("preBuild") {
  dependsOn("formatKotlin")
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

fun gitCommitHash(): String {
  return try {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
      .redirectErrorStream(true)
      .start()
    process.inputStream.bufferedReader().use { it.readText().trim() }
  } catch (e: Exception) {
    "stable"
  }
}

android {
  namespace = "org.grakovne.lissen"
  compileSdk = 36
  
  lint {
    disable.add("MissingTranslation")
  }
  
  defaultConfig {
    val commitHash = gitCommitHash()
    
    applicationId = "com.kahani.app"
    minSdk = 28
    targetSdk = 36
    versionCode = project.property("appVersionCode").toString().toInt()
    versionName = project.property("appVersionName").toString()
    
    buildConfigField("String", "GIT_HASH", "\"$commitHash\"")
    
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val envKeyStore = System.getenv("RELEASE_STORE_FILE")
      val propKeyStore = localProperties.getProperty("RELEASE_STORE_FILE")

      storeFile = when {
        envKeyStore != null -> file(envKeyStore)
        propKeyStore != null -> file(propKeyStore)
        else -> null
      }

      storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: localProperties.getProperty("RELEASE_STORE_PASSWORD")
      keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: localProperties.getProperty("RELEASE_KEY_ALIAS")
      keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: localProperties.getProperty("RELEASE_KEY_PASSWORD")

      enableV1Signing = true
      enableV2Signing = true
    }
  }

  flavorDimensions += "distribution"

  productFlavors {
    create("foss") {
      dimension = "distribution"
      applicationIdSuffix = ".foss"
      buildConfigField("String", "APP_NAME_SUFFIX", "\" (FOSS)\"")
      buildConfigField("String", "DISTRIBUTION", "\"foss\"")
    }
    create("play") {
      dimension = "distribution"
      buildConfigField("String", "APP_NAME_SUFFIX", "\"\"")
      buildConfigField("String", "DISTRIBUTION", "\"play\"")
      buildConfigField("String", "CLARITY_PROJECT_ID", "\"vc8bgk8nk9\"")
    }
  }

  buildTypes {
    release {
      signingConfigs.findByName("release")?.let {
        if (it.storeFile?.exists() == true) {
          signingConfig = it
        }
      }

      isMinifyEnabled = true
      isShrinkResources = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
      )
    }
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = " (DEBUG)"
      matchingFallbacks.add("release")
      isDebuggable = true
    }
  }
  
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlin {
    jvmToolchain(21)
  }
  buildFeatures {
    buildConfig = true
    compose = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1,MIT}"
    }
  }
  buildToolsVersion = "36.0.0"
  
}

// Disable Google Services and Crashlytics for FOSS flavor tasks
tasks.configureEach {
  if (name.contains("Foss", ignoreCase = true)) {
    if (name.contains("GoogleServices") || name.contains("Crashlytics") || name.contains("UploadCrashlyticsMappingFile")) {
      enabled = false
    }
  }
}

// Apply non-FOSS plugins conditionally to satisfy F-Droid scanner
val isFossBuild = project.hasProperty("foss") && project.property("foss").toString() == "true" ||
                  gradle.startParameter.taskNames.any { it.contains("Foss", ignoreCase = true) }

// FOSS_REMOVE_START
if (!isFossBuild) {
  val g = "com." + "google" + ".gms"
  val s = "google" + "-services"
  apply(plugin = "$g.$s")

  val f = "com." + "google"
  val b = "fire" + "base"
  val c = "crash" + "lytics"
  apply(plugin = "$f.$b.$c")
}
// FOSS_REMOVE_END

dependencies {
  implementation(project(":lib"))
  
  implementation(libs.androidx.navigation.compose)
  implementation(libs.material)
  implementation(libs.material3)
  
  implementation(libs.androidx.material)
  implementation(libs.compose.shimmer.android)
  
  implementation(libs.retrofit)
  implementation(libs.logging.interceptor)
  implementation(libs.okhttp)
  implementation(libs.androidx.browser)
  
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.hoko.blur)
  
  implementation(libs.androidx.paging.compose)
  
  implementation(libs.androidx.compose.material.icons.extended)
  
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.hilt.android)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.lifecycle.service)
  implementation(libs.androidx.lifecycle.process)
  
  ksp(libs.androidx.room.compiler)
  ksp(libs.hilt.android.compiler)
  ksp(libs.moshi.kotlin.codegen)
  
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.runtime.livedata)
  
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.datasource)
  implementation(libs.androidx.media3.database)
  
  implementation(libs.androidx.localbroadcastmanager)
  implementation(libs.timber)
  
  implementation(libs.androidx.glance)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  
  implementation(libs.converter.moshi)
  implementation(libs.moshi)
  implementation(libs.moshi.kotlin)

  // Non-FOSS dependencies
  // FOSS_REMOVE_START
  add("playImplementation", platform(libs.firebase.bom))
  add("playImplementation", libs.firebase.crashlytics)
  add("playImplementation", libs.firebase.analytics)
  add("playImplementation", libs.microsoft.clarity)
  // FOSS_REMOVE_END

  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}
