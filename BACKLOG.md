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
- [ ] #34 Notification tập mới
- [x] #40 Season Grouping 📺 — detect multi-season, horizontal scroll chọn phần
- [x] #41 English Tab (Consumet/FlixHQ) — Tab 🍿 English với Trending, Recent Movies, Recent Shows
- [x] #42 Consumet Player Integration — stream M3U8 + subtitle picker cho phim English
- [ ] #43 OpenSubtitles API Key — đăng ký opensubtitles.com để kích hoạt nguồn sub thứ 5
- [ ] #44 English Search — tìm kiếm phim English bằng Consumet search API
- [x] #45 Anime Detail Screen 🎌 — xem detail anime từ Anime47 API (backdrop, badges, episodes)
- [ ] #46 Subtitle Format Support — hỗ trợ .srt/.ass ngoài .vtt (convert on-the-fly)
- [x] #47 Donghua Section 🐉 — mục Hoạt Hình Trung Quốc trên tab Anime (search-based, curated keywords)
- [x] #48 Genre Search 🔍 — bấm genre chip → search API lấy anime theo thể loại
- [x] #49 See More Buttons › — nút "Xem thêm" trên section headers (Trending, Mới Cập Nhật, Sắp Chiếu)
- [ ] #50 See More Navigation — điều hướng khi bấm "Xem thêm" (mở list full cho mỗi section)
- [ ] #51 Donghua Style Filter — tận dụng API `animeStyles` field để filter chính xác hơn
