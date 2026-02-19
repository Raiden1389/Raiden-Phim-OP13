# Raiden Phim — Project Rules (phimbox-apk)

## 🗣️ XƯNG HÔ
- Gọi user: **sếp** hoặc **đại ca**
- Xưng: **em**

---

## 🚨 BUILD RULES (BẮT BUỘC)

### 0. TUYỆT ĐỐI KHÔNG TỰ BUILD
- **KHÔNG BAO GIỜ tự chạy `gradlew assembleRelease` khi sếp chưa yêu cầu**
- Chỉ build khi sếp nói rõ "build", "tạo APK", hoặc tương đương
- Nếu cần test compile, chỉ chạy `:app:compileReleaseKotlin` (không build APK)

### 1. LUÔN BUMP VERSION TRƯỚC KHI BUILD
- **Mỗi lần build APK PHẢI bump version** — Android reject cài APK cùng versionCode
- Version nằm tại: `app/build.gradle.kts` → `versionCode` + `versionName`
- **versionCode**: tăng 1 mỗi build (integer, bắt buộc tăng)
- **versionName**: semantic versioning `major.minor.patch`
  - `patch` +1 cho bugfix
  - `minor` +1 cho feature mới
  - `major` +1 cho breaking change
- **CHANGELOG.md**: phải update tương ứng

### 2. BUILD COMMAND
```bash
.\gradlew.bat assembleRelease
```
Output: `app/build/outputs/apk/release/RaidenPhim-v{version}.apk`

### 3. SIGNING
- Keystore: `raidenphim.jks` (root dir)
- Alias: `raidenphim`

---

## 📋 PROJECT INFO

### Tech Stack
- **Language**: Kotlin + Jetpack Compose
- **Player**: ExoPlayer (Media3)
- **Network**: Retrofit + OkHttp + Gson
- **Image**: Coil
- **Local Storage**: SharedPreferences (FavoriteManager, WatchHistoryManager, SettingsManager)

### API Sources
| Source | Base URL | Content |
|---|---|---|
| OPhim | `https://ophim1.com/v1/api/` | Phim Việt, TQ, HQ, anime |
| KKPhim | `https://phimapi.com/` | Phim Việt, TQ |
| Consumet/FlixHQ | `https://consumet-api-ten-chi.vercel.app/` | English movies/shows |
| Anime47 | `https://anime47.love/api/` | Anime |

### Consumet FlixHQ Notes
- **Server**: Phải dùng `server=vidcloud` — UpCloud trả URL expired (403)
- Servers available: upcloud (❌ 403), vidcloud (✅ 200), akcloud (❌ invalid in API)
- Stream URL cần Referer header từ API response

### Player Architecture
- **PlayerScreen.kt**: OPhim/KKPhim player — custom OTT controls, gesture, auto-next
- **EnglishPlayerScreen.kt**: Consumet player — ExoPlayer PlayerView, subtitle support
- **Fullscreen**: Nuclear approach = FLAG_FULLSCREEN + WindowCompat + native insetsController
- **enableEdgeToEdge()**: ĐÃ XOÁ — không cần cho app xem phim, gây conflict fullscreen

### Current Version
- **v1.11.0** (versionCode 18)

---

## 🎯 PLAYER IMPROVEMENT BACKLOG (priority order)
1. ~~Chia 3 vùng tap (trái/giữa/phải) + play/pause ở giữa~~ ✅ v1.11.0
2. ~~Cộng dồn seek (+10, +20, +30)~~ ✅ v1.11.0
3. ~~Seek animation overlay (⏩ 20s)~~ ✅ v1.11.0
4. ~~Haptic feedback~~ ✅ v1.11.0
5. ~~Audio focus handling (pause khi gọi điện)~~ ✅ v1.11.0
6. ~~Aspect ratio toggle (Fit/Fill)~~ ✅ v1.11.0
7. ~~PiP (Picture-in-Picture)~~ ✅ v1.11.0
8. Subtitle style settings (font, size, color, background)
9. Customizable subtitle position
10. Long press speed (2x while holding)

---

## ⚠️ KNOWN ISSUES
- Extractor folder (11MB) — vô dụng, có thể xóa
- Consumet trending endpoint đôi khi timeout (Vercel cold start)
- **Data loss khi cập nhật APK** — favorites + watch history bị mất khi install over (đang điều tra)
