plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  // Reads google-services.json at build time (Cloud Account setup).
  alias(libs.plugins.google.services)
}

// Cloud Account setup: the google-services plugin fails the build by default when
// app/google-services.json is absent. WARN lets the app build before that one-time setup,
// and AccountManager reports "not configured" at runtime instead of crashing.
googleServices {
  missingGoogleServicesStrategy =
    com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
}

// The Gemini API key must reach the app as BuildConfig.GEMINI_API_KEY, otherwise the
// declared MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API is never actually used.
// Resolution order: Gradle property -> environment variable -> .env file (AI Studio).
val geminiApiKey: String = run {
  fun sanitize(raw: String?): String? = raw?.trim()?.trim('"', '\'')?.takeIf { it.isNotBlank() }

  val fromDotEnv = rootProject.file(".env").takeIf { it.exists() }?.let { envFile ->
    envFile.readLines()
      .map { it.trim() }
      .firstOrNull { it.startsWith("GEMINI_API_KEY") }
      ?.substringAfter("=", "")
  }

  sanitize(providers.gradleProperty("GEMINI_API_KEY").orNull)
    ?: sanitize(providers.environmentVariable("GEMINI_API_KEY").orNull)
    ?: sanitize(fromDotEnv)
    ?: ""
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.ahexstreak.rxmpb"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField(
      "String",
      "GEMINI_API_KEY",
      "\"${geminiApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
  }

  val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
  val releaseStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
  val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
  val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
  val releaseSigningConfigured = !releaseKeystorePath.isNullOrBlank() &&
    file(releaseKeystorePath!!).exists() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

  signingConfigs {
    create("release") {
      if (releaseSigningConfigured) {
        storeFile = file(releaseKeystorePath!!)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (releaseSigningConfigured) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  // Never allow a release artifact to be produced without an explicitly configured
  // production keystore. In particular, do not fall back to the public debug key.
  gradle.taskGraph.whenReady {
    if (allTasks.any { it.name.contains("Release", ignoreCase = true) } && !releaseSigningConfigured) {
      throw GradleException("Release signing is not configured. Set KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD.")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Keep the dependency surface minimal. Add a dependency only with a tested feature need.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // Cloud accounts & library sync (Firebase Auth + Firestore)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)

  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.mlkit.face.detection)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
