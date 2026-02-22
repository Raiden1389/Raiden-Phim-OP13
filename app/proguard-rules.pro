# ═══════════════════════════════════════════════════════════════
# ProGuard Rules — RaidenPhim
# Updated v1.20.3: TD-10 precision cleanup after Room migration
# ═══════════════════════════════════════════════════════════════

# ══ Retrofit ══
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# ══ Gson — Retrofit API response models (GsonConverterFactory) ══
# Only API models use @SerializedName — local/ managers use Room now
-keep class xyz.raidenhub.phim.data.api.models.** { *; }
-keepclassmembers class xyz.raidenhub.phim.data.api.models.** { *; }

# Gson TypeToken — R8 full mode strips generic signatures
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson @SerializedName fields (covers API models + widget legacy format)
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# ══ Widget — ContinueWatchingWidget (Gson deserialization from SharedPrefs) ══
-keep class xyz.raidenhub.phim.widget.** { *; }
-keepclassmembers class xyz.raidenhub.phim.widget.** { *; }

# ══ OkHttp ══
-dontwarn okhttp3.**
-dontwarn okio.**

# ══ Coil ══
-dontwarn coil3.**

# ══ Compose ══
-dontwarn androidx.compose.**

# ══ Coroutines ══
-dontwarn kotlinx.coroutines.**

# ══ Room DB ══
# Room generates _Impl classes via KSP — keep entity fields + DAO implementations
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep Room entity classes — R8 full mode strips @ColumnInfo fields
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Room entity + DAO inner members
-keepclassmembers class xyz.raidenhub.phim.data.db.entity.** { *; }
-keepclassmembers class xyz.raidenhub.phim.data.db.dao.** { *; }

# Room schema annotations must survive
-keepattributes *Annotation*

# SettingKeys object (referenced by string, must not be renamed)
-keep class xyz.raidenhub.phim.data.db.entity.SettingKeys { *; }

# ══ DataStore ══
-dontwarn androidx.datastore.**

# ══ WorkManager ══
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class xyz.raidenhub.phim.worker.** { *; }

# ══ Glance (Widget) ══
-dontwarn androidx.glance.**
