# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# Gson — API models
-keep class xyz.raidenhub.phim.data.api.models.** { *; }
# Gson — Local data (FavoriteItem, ContinueItem, SeriesConfig, etc.)
# NOTE: Sau Phase 03 migration, các class này sẽ xoá → rules này sẽ xoá theo
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

# ═══ Room DB (Phase 02) ═══
# Room generates _Impl classes via KSP — keep entity fields + DAO implementations
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep Room entity classes — R8 full mode strips @ColumnInfo fields
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Room entity inner classes (data classes with companion)
-keepclassmembers class xyz.raidenhub.phim.data.db.entity.** { *; }
-keepclassmembers class xyz.raidenhub.phim.data.db.dao.** { *; }

# Room schema annotations must survive
-keepattributes *Annotation*

# SettingKeys object (referenced by string, must not be renamed)
-keep class xyz.raidenhub.phim.data.db.entity.SettingKeys { *; }

