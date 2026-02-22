# RaidenPhim — Feature Backlog

## 🔥 Priority 1 — Quick Wins
- [x] #1  Pull-to-Refresh trên Home
- [x] #5  Greeting theo giờ (Chào buổi sáng/tối)
- [x] #6  Movie card hiện năm + chất lượng
- [x] #8  Active filter indicator trên Home
- [x] #14 Expandable description (Detail)
- [x] #30 Next/Prev episode buttons (Player)
- [x] #33 Auto-play next toggle (Settings)
- [x] #38 Clear watch data (Settings)

## ⚡ Priority 2 — Medium
- [x] #2  Skeleton Loading (shimmer)
- [x] #4  Banner auto-scroll + progress indicator
- [x] #7  Horizontal scroll snap
- [x] #9  Search history (15 từ khóa, persistent)
- [x] #20 "Xem từ tập đã dừng" (Detail → Continue button)
- [x] #21 Episode progress bar (Detail)
- [x] #22 Double-tap seek ±10s
- [x] #39 Animated transitions (fade + slide)

## 🔨 Priority 3 — Larger
- [x] #11 Trending searches (keywords gợi ý)
- [x] #12 Filter kết quả search (hiện result count)
- [x] #19 Cast & Director section (Detail)
- [x] #23 Brightness gesture (swipe trái)
- [x] #24 Volume gesture (swipe phải)
- [x] #36 Watch history screen (bottom nav tab)
- [x] #10 Voice search 🎤 — nhận diện giọng nói vi-VN
- [x] #13 Search suggestion autocomplete (history + trending)
- [x] #17 Rating/Score hiện IMDb ⭐ (via OMDB API)
- [x] #27 Multi-Source Vietnamese Subtitle — tự tìm sub Việt cho phim English
  - ✅ Source 1: **Consumet** (sub English/multi-lang có sẵn từ FlixHQ)
  - ✅ Source 2: **SubDL** REST API (API key set)
  - ✅ Source 3: **SubSource** REST API (API key set)
  - ✅ Source 4: **Subscene** HTML scrape (không cần key)
  - ⏳ Source 5: **OpenSubtitles** REST API (cần API key)
  - Flow: Search tên phim → query tất cả source song song → merge → sort Vietnamese first → user chọn [🇻🇳 VI] [🇬🇧 EN]
- [ ] #31 Dark/Light theme toggle
- [x] #34 Notification tập mới
- [x] #40 Season Grouping 📺 — detect multi-season, horizontal scroll chọn phần

- [x] #45 Anime Detail Screen 🎌 — xem detail anime từ Anime47 API (backdrop, badges, episodes)

- [x] #47 Donghua Section 🐉 — mục Hoạt Hình Trung Quốc trên tab Anime (search-based, curated keywords)
- [x] #48 Genre Search 🔍 — bấm genre chip → search API lấy anime theo thể loại
- [x] #49 See More Buttons › — nút "Xem thêm" trên section headers (Trending, Mới Cập Nhật, Sắp Chiếu)
- [x] #50 See More Navigation — điều hướng khi bấm "Xem thêm" (mở list full → CategoryScreen đã có)
- [x] #50b Infinite scroll fix — KKPhim TV Shows home row chỉ hiện 10 item → đã fix: fetch page 1 + page 2 song song → ~20 item. `distinctBy { slug }` dedup
- [ ] #51 Donghua Style Filter — tận dụng API `animeStyles` field để filter chính xác hơn

## 🐛 Bugs Known
- [x] #BUG-1 **Data loss khi update APK** 🔴 — favorites + watch history bị mất khi install over APK ✅ Fixed (Room DB không bị obfuscate)
- [x] #BUG-3 **Widget "Xem tiếp" không cập nhật** — widget hiện "Chưa có phim" dù đang xem. Fix: thêm `notifyWidgetUpdate()` broadcast
- [x] #BUG-4 **Room Migration compile errors** ✅ v1.20.2 — 12 lỗi collectAsState() thiếu `initial`, saveProgress→updateContinue API rename, history.value trên Flow, hiddenCount Int→Flow<Int>

---

## 🎯 Planned Features (v1.15+)

### 🏠 Home
- [x] #H-1  **Hero Carousel Filter** — Long press poster trên Hero Carousel → context menu "🚫 Bỏ qua phim này" → ẩn khỏi carousel. Quản lý trong Settings (đếm + reset). `HeroFilterManager.kt`
- [x] #H-6  **Reorder Home Sections** — Nút ↑↓ trong Settings → sắp xếp thứ tự các row (K-Drama, Phim Bộ, Phim Lẻ...) theo sở thích. `SectionOrderManager.kt`
- [x] #H-7  **Quick Play (Long Press)** — Giữ poster bất kỳ trên Home/row → haptic + bắt đầu xem ngay (bỏ qua Detail)
- [x] #H-8  **Relative Timestamp** — Badge "3m trước" / "2h trước" / "2 ngày" trên Continue Watching cards

### 🔍 Search
- [x] #S-1  **In-results Filter** — Sau khi tìm → filter kết quả theo năm / phim bộ / phim lẻ (chip row). Dùng `episodeCurrent` heuristic vì `Movie` model không có field `type`
- [x] #S-2  **Genre Quick Search** — Row chip thể loại trên SearchScreen (🥊 Hành động, 👻 Kinh dị, 🏯 Cổ trang...) → tap → fetch
- [x] #S-3  **Smart Keyword Normalize** — "han quoc" → "Hàn Quốc", "hanh dong" → "Hành động"; áp dụng cả voice search
- [x] #S-4  **Sort Search Results** — Dropdown: 🕒 Mới nhất / 📋 Cũ nhất / 🔤 Tên A-Z
- [ ] #S-5  **Dynamic Trending** — Trending keywords tính từ search history aggregate (top 16 từ khóa phổ biến nhất)
- [ ] #S-6  **Unified Multi-API Search** — Search 1 lần → query OPhim + KKPhim + Anime47 song song → merge + dedup → hiện badge nguồn (🎬 OPhim / 📺 KKPhim / 🎌 Anime47) trên mỗi kết quả. Tab chip filter theo source

### 🎬 Detail
- [x] #D-3  **TMDB Rating** — Fetch song song với IMDb, hiển thị "🍅 TMDB X.X/10" trong info chip row
- [x] #D-5  **Phim liên quan** — Row "Xem thêm như phim này" — search theo thể loại + quốc gia đầu tiên của phim
- [x] #D-6  **Cast Grid** — Danh sách diễn viên từ `actor` field dạng horizontal scroll với avatar placeholder
- [x] #D-7  **Expand/Collapse Plot** — Nội dung phim giới hạn 4 dòng, gradient fade overlay khi thu gọn, tap "Xem thêm ▼" để mở full
- [x] #D-8  **Episode Sort Toggle** — Button đảo thứ tự tập: 1→N hoặc N→1 (phim Trung/Hàn dài muốn xem mới nhất)

### ▶️ Player
- [ ] #52   Subtitle style settings — font, size, color, background opacity
- [ ] #53   Customizable subtitle position — điều chỉnh vị trí sub trên/dưới
- [ ] #54   Long press speed 2x — giữ màn hình để xem 2x, thả về bình thường
- [ ] #P-1  **Subtitle Style** — (alias #52) font, size, màu chữ, màu nền, opacity via Settings
- [ ] #P-2  **Subtitle Position** — (alias #53) slider điều chỉnh offset Y của subtitle
- [ ] #PL-1 **Seekbar Preview Thumbnail** — Kéo seek bar → hiện thumbnail frame tại vị trí (giống YouTube). Tìm đúng cảnh muốn xem lại
- [ ] #PL-2 **A-B Repeat Loop** — Đánh dấu 2 điểm → lặp lại đoạn đó. Cho cảnh hay / nghe nhạc phim
- [ ] #PL-3 **Swipe Horizontal Seek** — Swipe ngang trên player = seek liên tục (giống MX Player). Chính xác hơn double-tap
- [ ] #PL-4 **Remaining Time Toggle** — Tap vào thời lượng → toggle: `1:23:45` (total) ↔ `-0:37:12` (còn lại). Biết còn bao lâu
- [ ] #PL-5 **Smooth Episode Transition** — Hết tập → crossfade 1.5s + hiện tên tập mới dạng cinematic ("Tập 13: Bí Mật"). Binge mượt
- [ ] #PL-6 **Smart Intro Detection (per-country)** — Track vị trí user skip đầu tập theo `countryCode`. Sau 3+ tập cùng quốc gia skip ±same timestamp → hỏi "Phim HQ hay intro ~55s, lưu country default?" → feed vào IntroOutroManager.promoteToCountryDefault(). Learn per 🇰🇷/🇨🇳/🇯🇵 riêng
- [ ] #PL-7 **Smart Episode Notification** — Ưu tiên phim rate 🔥 + xem gần đây. Text thông minh: "Vincenzo có tập 13! Bạn xem đến tập 12 hôm qua 🍿". Không spam phim quên lâu

### ⚙️ Settings
- [x] #SE-1 **Default Playback Quality** — Chọn mặc định: Auto / 360p / 720p / 1080p khi start player
- [x] #SE-6 **Export/Import Backup** — Xuất favorites + watch history ra file JSON → import vào máy khác (share file)

### 🔔 Notifications
- [x] #N-1  **New Episode Notify** — WorkManager periodic check favorites → push notification khi ra tập mới. `EpisodeCheckWorker.kt`
- [x] #N-3  **"Xem tiếp" Widget** — App widget 4x2 hiện danh sách "Xem tiếp" + tap to play. `ContinueWatchingWidget.kt`

### 🗂️ Categories & Discovery
- [x] #C-1  **Year Filter in Categories** — Dropdown năm (2020/2022/2024/Tất cả) trong CategoryScreen
- [x] #C-2  **Genre Hub Page** — Screen thể loại riêng: Hành động / Romance / Kinh dị... → danh sách phim
- [x] #C-4  **"Xem Sau" Watchlist** — Bookmark phim chưa muốn xem ngay (khác Favorites). Icon 🔖 trên Detail
- [x] #C-5  **User Playlists** — Tạo playlist thủ công ("Phim xem cuối tuần", "List Gia Đình")

## 🎮 Player Features (từ backlog nội bộ)
*(đã merge lên section Player ở trên)*

## 🏠 Personal UX (v1.20+)
- [ ] #UX-1  **Smart Home theo ngữ cảnh** — Buổi tối: Continue Watching lên hero to nhất, 1 tap → xem ngay. Buổi sáng: hiện phim mới. Dựa vào giờ + lịch sử xem
- [ ] #UX-2  **Episode Tracker Badge** — Trên mỗi poster phim bộ đang xem: vòng tròn progress + "12/48 tập". Nhìn biết ngay xem được bao nhiêu
- [ ] #UX-3  **Quick Rating (Emoji)** — Xem xong phim/tập → popup nhẹ: 🔥👍😐💤. 1 tap rate. Data feed vào Taste Profile + thống kê

## 🎨 Visual Polish (v1.20+)
- [ ] #VP-1  **Accent Color Picker** — Settings: chọn màu chủ đạo app (6 preset + custom HSL). Giữ dark theme, chỉ đổi accent color (nút, highlight, indicator)
- [ ] #VP-2  **Animated Number Counter** — Detail screen: rating, năm, số tập chạy counter từ 0 (count-up animation). Nhỏ nhưng premium
- [ ] #VP-3  **Category Colors** — Mỗi thể loại có gradient riêng (Hành động = đỏ cam, Kinh dị = tím đen, Tình cảm = hồng). Genre Hub + chips dùng màu tương ứng
- [ ] #VP-4  **Living Wallpaper Home** — Background Home = poster phim đang xem, blur 60% làm nền. Mỗi ngày khác vì đang xem phim khác. App "sống", zero config
- [ ] #VP-5  **Card Shape Variants** — Settings: chọn hình poster card: Bo tròn mềm (iOS) / Bo nhẹ (Android) / Vuông cứng (Cinematic) / Asymmetric (nghệ)

## ⚡ Micro-UX (v1.20+)
- [ ] #MU-1  **Swipe chuyển tab** — HorizontalPager cho bottom nav 5 tab, swipe trái/phải chuyển tab. Dùng 1 tay thoải mái
- [ ] #MU-2  **Double-tap Poster Info** — Double-tap poster bất kỳ → popup card (rating, năm, số tập, nút Play). Không cần vào Detail
- [ ] #MU-3  **Thống kê xem phim** — Screen trong Settings: tổng giờ xem, top phim, top thể loại, streak ngày. Kiểu Spotify Wrapped cho phim

## � Interaction (v1.20+)
- [ ] #IA-1  **Long Press Context Menu** — Long press poster bất kỳ → rich menu nổi: ▶️ Play / 🔖 Watchlist / ❤️ Favorite / 📋 Add Playlist / 🚫 Ẩn. 1 điểm làm mọi thứ
- [ ] #IA-2  **Swipe Card Actions** — Continue Watching: swipe trái = "Đã xem xong, ẩn". Swipe phải = "Pin lên đầu". Quản lý nhanh không cần menu

## �💎 Cá Nhân Hoá (v1.20+)
- [ ] #CN-1  **Custom Home Layout** — Chọn style Home: Card lớn (1 cột poster to) / Grid chặt (3 cột) / List (info text). Tuỳ gu nhìn
- [ ] #CN-2  **"My Theater" Splash** — Splash screen custom: tên riêng ("Raiden's Theater 🎬") + quote phim ngẫu nhiên. Cảm giác mở rạp riêng
- [ ] #CN-3  **Poster Art Mode** — Trong Watchlist/Favorites: toggle "Gallery" — poster full-width, không text/badge, chỉ ảnh. Bộ sưu tập tranh

## 🗑️ Tech Debt
### 🔴 P0 — Blocking
- [x] #TD-1 **Xóa Extractor folder** — đã xóa 11MB Node.js dead code ở root project
- [x] #TD-2 **Room DB migration** ✅ v1.20.2 — migrate toàn bộ 9 Managers (Favorite, WatchHistory, SearchHistory, Watchlist, Playlist, HeroFilter, SectionOrder, IntroOutro, Settings) sang Room DB. 9 DAOs + 9 Entities + AppDatabase. Backward-compat aliases giữ UI nguyên
- [x] #TD-3 **Coil cache tuning** — 200MB disk cache + 50MB memory cache + hardware bitmaps (GPU) trong `App.kt`
- [x] #TD-4 **God Screen Split** ✅ v1.19.2 — PlayerScreen (1298L), DetailScreen (827L), HomeScreen (798L), SearchScreen (538L) → tách thành Screen + ViewModel + Components riêng (13 files)
### 🟡 P1 — Important
- [ ] #TD-5 **Hilt DI** — Thay 8 `object + init(context)` singleton bằng Hilt @Inject. ViewModel dùng @HiltViewModel. Testable + clean
- [ ] #TD-7 **Error Handling Strategy** — Phân biệt NetworkError/ApiError/ParseError. Retry chỉ cho network. Thêm Crashlytics free tier cho release build
- [ ] #TD-9 **Offline Mode** — Cache last-loaded Home data vào Room. Mất mạng → hiện data cũ + banner "Đang offline". Continue Watching vẫn hoạt động
### 🟢 P2 — Nice to have
- [ ] #TD-8  **API Key Security** — Move TMDB/OMDB key sang local.properties + BuildConfig. Không hardcode trong source
- [ ] #TD-10 **ProGuard Precision** — Thay `-keep class **$* { *; }` bằng rules chính xác cho data/api/models + data/local. APK nhỏ hơn
- [ ] #TD-11 **Compose Recomposition** — `remember` callbacks, `@Stable` annotations cho data class, `derivedStateOf` cho computed state. Giảm unnecessary recomposition
- [ ] #TD-12 **KotlinX Serialization** — Thay Gson bằng kotlinx-serialization. Compile-time safe, nhanh hơn ~30%, nhẹ hơn ~300KB
- [ ] #TD-13 **Gradle Multi-Module** — Split app thành :core, :data, :player, :ui modules. Parallel build + incremental compile

