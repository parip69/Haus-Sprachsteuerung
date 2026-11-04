import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val projectSigning = Properties().apply {
    rootProject.file("signing/signing.properties").inputStream().use { load(it) }
}
val iconVariant = providers.gradleProperty("iconVariant").orElse("mikrofon").get()
require(iconVariant in setOf("mikrofon", "sprachwelle")) { "iconVariant muss mikrofon oder sprachwelle sein" }
android {
    namespace = "de.parip69.homesprachsteuerung"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.parip69.homesprachsteuerung"
        minSdk = 26
        targetSdk = 35
        versionCode = 25
        versionName = "1.1.14-auto-test"
    }
    sourceSets.getByName("main").res.srcDir("src/icons/$iconVariant/res")
    buildFeatures { viewBinding = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        create("projectKey") {
            storeFile = rootProject.file(projectSigning.getProperty("storeFile"))
            storePassword = projectSigning.getProperty("storePassword")
            keyAlias = projectSigning.getProperty("keyAlias")
            keyPassword = projectSigning.getProperty("keyPassword")
            storeType = "JKS"
        }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.getByName("projectKey") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("projectKey")
        }
    }
}
dependencies {
    // Official template API and Android Auto host connection.
    implementation("androidx.car.app:app:1.7.0")
    implementation("androidx.car.app:app-projected:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Java MQTT client needs no separate Paho Android service. CaptureService owns the recording.
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
}
