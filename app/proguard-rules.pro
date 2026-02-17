# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# Gson
-keep class xyz.raidenhub.phim.data.api.models.** { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-dontwarn coil3.**

# Compose
-dontwarn androidx.compose.**

# Coroutines
-dontwarn kotlinx.coroutines.**
