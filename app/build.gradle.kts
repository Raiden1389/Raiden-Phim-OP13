plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")   // Room annotation processor
}

// ═══ Load local.properties (API keys) ═══
fun localProp(key: String): String {
    val props = org.jetbrains.kotlin.konan.properties.Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().buffered().use(props::load)
    return props.getProperty(key, "")
}

android {
    namespace = "xyz.raidenhub.phim"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.raidenhub.phim"
        minSdk = 24
        targetSdk = 35
        versionCode = 69
        versionName = "1.23.0"

        // ═══ API Keys từ local.properties (không hardcode trong source) ═══
        buildConfigField("String", "TMDB_API_KEY", "\"${localProp("tmdb.api.key")}\"")
        buildConfigField("String", "FEBBOX_COOKIE", "\"${localProp("febbox.cookie")}\"")

        // ═══ Fshare credentials ═══
        buildConfigField("String", "FSHARE_EMAIL", "\"${localProp("fshare.email")}\"")
        buildConfigField("String", "FSHARE_PASSWORD", "\"${localProp("fshare.password")}\"")
        buildConfigField("String", "FSHARE_APP_KEY", "\"${localProp("fshare.app.key")}\"")
        buildConfigField("String", "FSHARE_USER_AGENT", "\"${localProp("fshare.user.agent")}\"")
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
        debug {
            applicationIdSuffix = ".debug"  // Cài song song với bản release, không đè
        }
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
        // Auto-copy release APK to Google Drive
        if (variant.buildType.name == "release") {
            variant.assembleProvider.get().doLast {
                val apkFile = variant.outputs.first().outputFile
                val destDir = file("H:/My Drive/Raiden APK")
                if (destDir.exists()) {
                    copy {
                        from(apkFile)
                        into(destDir)
                    }
                    println("✅ APK copied to: ${destDir}/${apkFile.name}")
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room schema export — lưu JSON schema vào project, cần cho Room auto-migration
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
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
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // ═══ Navigation ═══
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // ═══ ExoPlayer (Media3 1.9.1 — matches nextlib-media3ext) ═══
    implementation("androidx.media3:media3-exoplayer:1.9.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.1")
    implementation("androidx.media3:media3-datasource:1.9.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.9.1")
    implementation("androidx.media3:media3-ui:1.9.1")

    // ═══ FFmpeg decoder extension — AC3/EAC3/DTS software decode for MKV ═══
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.9.1-0.11.0")

    // ═══ Coil 3 — Image loading ═══
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // ═══ Retrofit + OkHttp ═══
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ═══ Room DB ═══
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")          // Coroutine + Flow support
    ksp("androidx.room:room-compiler:$roomVersion")                 // KSP code generation

    // ═══ DataStore ═══
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // ═══ Core ═══
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.2.0")

    // ═══ Coroutines ═══
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // ═══ WorkManager (#34 — Episode notifications) ═══
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // ═══ Glance (N-3 — Continue Watching widget) ═══
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // ═══ Fshare — HTML scraping + encrypted credentials ═══
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ═══ Debug ═══
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ═══ Tests ═══
    testImplementation("junit:junit:4.13.2")
}
