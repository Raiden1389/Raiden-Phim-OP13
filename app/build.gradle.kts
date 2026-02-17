plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "xyz.raidenhub.phim"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.raidenhub.phim"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "1.7.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../raidenphim.jks")
            storePassword = "raiden123"
            keyAlias = "raidenphim"
            keyPassword = "raiden123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "RaidenPhim-v${variant.versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "RaidenPhim-v${variant.versionName}.apk"
        }
    }
}

dependencies {
    // ═══ Compose BOM (2026.02.00 — Latest) ═══
    val composeBom = platform("androidx.compose:compose-bom:2026.02.00")
    implementation(composeBom)

    // ═══ Compose Core ═══
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // ═══ Navigation ═══
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // ═══ ExoPlayer (Media3) ═══
    implementation("androidx.media3:media3-exoplayer:1.9.2")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.2")
    implementation("androidx.media3:media3-datasource:1.9.2")
    implementation("androidx.media3:media3-ui:1.9.2")

    // ═══ Coil 3 — Image loading ═══
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // ═══ Retrofit + OkHttp ═══
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ═══ DataStore ═══
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // ═══ Core ═══
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.2.0")

    // ═══ Coroutines ═══
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // ═══ Debug ═══
    debugImplementation("androidx.compose.ui:ui-tooling")
}
