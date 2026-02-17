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
- [ ] #10 Voice search
- [ ] #13 Search suggestion autocomplete (API)
- [ ] #17 Rating/Score hiện IMDb
- [ ] #27 Multi-Source Vietnamese Subtitle — tự tìm sub Việt cho phim English
  - Source 1: **Consumet** (sub English/multi-lang có sẵn từ FlixHQ)
  - Source 2: **OpenSubtitles.com** REST API (free 20 dl/ngày, 200 VIP)
  - Source 3: **Subdl.com** REST API (miễn phí, có sub Việt)
  - Source 4: **Subscene** scrape (kho sub Việt lớn nhất)
  - Source 5: **Podnapisi.net** API (backup)
  - Flow: Search tên phim + năm → query tất cả source → merge → user chọn [🇬🇧 EN] [🇻🇳 VI]
  - ExoPlayer: load .srt/.vtt → SubtitleConfiguration
- [ ] #31 Dark/Light theme toggle
- [ ] #34 Notification tập mới
- [ ] #40 Season Grouping — Gộp multi-season (VD: Supernatural 15 phần → 1 trang, Season Picker tabs)
  - Parse pattern `(Phần X)` / `(Season X)` từ tên phim
  - Search API tìm tất cả season cùng tên gốc
  - UI: Season tabs `[S1] [S2] ... [S15]` + episode list tương ứng
- [x] #41 English Tab (Consumet/FlixHQ) — Tab 🍿 English với Trending, Recent Movies, Recent Shows
- [ ] #42 Consumet Player Integration — stream M3U8 + subtitle cho phim English
