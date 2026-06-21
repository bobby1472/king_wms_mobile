import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.king.wms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.king.wms"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }

        // Base URL of the kingonesystem backend (Express, port 4000, prefix /api/v1).
        // MUST end with a trailing slash. This is the server PC's LAN IP — it works
        // from BOTH a real device on the WiFi and the emulator (as long as port 4000
        // is reachable on the LAN / not blocked by Windows Firewall).
        buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.150:4000/api/v1/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Uses the LAN IP above. If the emulator can't reach it (e.g. Windows
            // Firewall blocks inbound 4000), use the emulator's host-loopback alias:
            // buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:4000/api/v1/\"")
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.150:4000/api/v1/\"")
        }
    }

    // Stamp every build with version + date-time so each APK is a NEW file in the output
    // folder — older builds are kept, not overwritten. Example:
    //   KingWMS-v1.0(1)-debug-20260621-164530.apk
    // NOTE: "Clean Project" / `gradlew clean` wipes the whole build/ folder, so copy any
    // APKs you want to keep long-term into a separate archive folder.
    val buildStamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "KingWMS-v${variant.versionName}(${variant.versionCode})-${variant.buildType.name}-$buildStamp.apk"
        }
        // After each build, copy the APK into a permanent archive folder OUTSIDE build/
        // (<project root>/apk-archive/) so your version history survives a Clean.
        variant.assembleProvider.configure {
            doLast {
                copy {
                    from(variant.outputs.map { it.outputFile })
                    into(rootProject.file("apk-archive"))
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // Compose BOM keeps all compose versions aligned
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose UI + Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit + OkHttp + kotlinx.serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room (offline cache)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (token storage)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ML Kit barcode scanning + CameraX
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
}
