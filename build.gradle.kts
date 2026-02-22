// Top-level build file — Raiden Phim Phone (same stack as TV)
plugins {
    id("com.android.application") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    // KSP — Symbol Processor cho Room (Kotlin 2.x phải dùng KSP, không dùng KAPT)
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
}
