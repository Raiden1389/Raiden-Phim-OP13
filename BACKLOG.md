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
- [x] #BUG-1 **Data loss khi update APK** 🔴 — favorites + watch history bị mất khi install over APK ✅ Fixed
- [x] #BUG-3 **Widget "Xem tiếp" không cập nhật** — widget hiện "Chưa có phim" dù đang xem. Fix: thêm `notifyWidgetUpdate()` broadcast

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

## 🗑️ Tech Debt
- [x] #TD-1 **Xóa Extractor folder** — đã xóa 11MB Node.js dead code ở root project
- [ ] #TD-2 **Room DB migration** — thay SharedPreferences bằng Room DB cho WatchHistory + Favorites (query nhanh hơn, type-safe)
- [x] #TD-3 **Coil cache tuning** — 200MB disk cache + 50MB memory cache + hardware bitmaps (GPU) trong `App.kt`
