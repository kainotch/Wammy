// Created by Notch
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.wammy"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.wammy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // Automatically sign release builds with debug key for easy testing
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }


}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("androidx.webkit:webkit:1.11.0-alpha02")
    implementation("io.github.dokar3:quickjs-kt:1.0.0-alpha13")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("org.jsoup:jsoup:1.17.2")
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended:1.6.0")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Wammy Added Dependencies
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.telephoto.zoomable.coil)
  
  implementation("io.github.ireaderorg:source-api:1.5.1")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:okhttp-brotli:4.11.0")
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.jsoup)
  implementation("com.squareup.okio:okio:3.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.8.0")

}
dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("androidx.webkit:webkit:1.11.0-alpha02")
    implementation("io.github.dokar3:quickjs-kt:1.0.0-alpha13")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-okhttp:2.3.11")
}

base {
    archivesName.set("wammy")
}
