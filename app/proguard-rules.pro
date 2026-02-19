# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# Gson — API models
-keep class xyz.raidenhub.phim.data.api.models.** { *; }
# Gson — Local data (FavoriteItem, ContinueItem, SeriesConfig, etc.)
-keep class xyz.raidenhub.phim.data.local.** { *; }
-keep class xyz.raidenhub.phim.data.local.**$* { *; }
-keepclassmembers class xyz.raidenhub.phim.data.local.** { *; }
-keepclassmembers class xyz.raidenhub.phim.data.local.**$* { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# Gson TypeToken — R8 full mode strips generic signatures
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-dontwarn coil3.**

# Compose
-dontwarn androidx.compose.**

# Coroutines
-dontwarn kotlinx.coroutines.**
