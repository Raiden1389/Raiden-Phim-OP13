# 🎬 RaidenPhim — Tính Năng

> **Phiên bản:** v1.18.0 · **Build:** 52 · **Cập nhật:** 2026-02-21

---

## 🏠 Màn Hình Chính (Home)

| Tính năng | Mô tả |
|-----------|-------|
| �️ **Hero Carousel** | Banner tự cuộn với indicator chấm tròn. Tap poster → Detail |
| � **Continue Watching** | Row "Xem tiếp" — card landscape 16:9 với play icon overlay, progress bar, chip tập + thời gian tương đối |
| ❤️ **Yêu thích** | Row phim đã yêu thích, truy cập nhanh |
| 🕛 **Lịch sử** | Row lịch sử xem gần đây |
| 📺 **Phim bộ / Lẻ / Hoạt hình** | Các hàng ngang theo danh mục, tự tải từ API|
| 🌅 **Lời chào thông minh** | "Chào buổi sáng / chiều / tối" theo giờ thực tế |
| 🏷️ **Filter đang hoạt động** | Badge hiển thị khi có filter quốc gia / thể loại đang bật |
| 🔄 **Pull-to-Refresh** | Kéo xuống để làm mới nội dung |
| ⏭️ **Nút "Xem thêm"** | Header section có › để mở danh sách đầy đủ |
| ⚡ **Quick Play** | Long-press poster bất kỳ → haptic + launch player ngay, bỏ qua Detail |

---

## 🔍 Tìm Kiếm

| Tính năng | Mô tả |
|-----------|-------|
| 🎤 **Voice Search** | Nhấn mic → nhận diện giọng nói tiếng Việt (vi-VN), tự động chuẩn hoá từ khoá |
| 📝 **Lịch sử tìm kiếm** | Lưu 15 từ khóa gần nhất, tap để tìm lại |
| 💡 **Autocomplete** | Gợi ý từ lịch sử khi gõ |
| 🔥 **Trending Keywords** | 16 từ khóa phổ biến hiển thị khi chưa tìm |
| 🎥 **Genre Quick Search** | 10 chip thể loại nổi bật (🥊 Hành động / 👻 Kinh dị / 🚀 Viễn tưởng...) khi chưa gõ gì |
| 🏷️ **Filter kết quả** | Chip Phim bộ / Phim lẻ + chip năm sau khi tìm được kết quả |
| � **Sắp xếp** | Dropdown Mới nhất / Cũ nhất / Tên A-Z khi có kết quả |
| �🔢 **Số kết quả** | Hiển thị "X kết quả" + bộ lọc đang active khi có kết quả tìm kiếm |
| ⏱️ **Debounce 400ms** | Tự động tìm sau khi ngừng gõ, tránh spam API |

---

## 🎬 Chi Tiết Phim (Detail)

| Tính năng | Mô tả |
|-----------|-------|
| 🖼️ **Parallax Backdrop** | Poster cuộn parallax 0.5x speed với scale-up depth, fade-out, gradient overlay cinematic |
| 🎨 **Dynamic Color** | Trích xuất dominant color từ poster → tint nút Play + badge chất lượng, animated transition |
| ✨ **Entrance Animation** | Fade + scale animation khi mở Detail — hiệu ứng card → full-screen |
| ⭐ **IMDb Rating** | Điểm số thực từ OMDB API (nếu phim có trên IMDb) |
| 🍅 **TMDB Rating** | Điểm số từ TMDB API, hiển thị song song với IMDb |
| 📖 **Mô tả mở rộng** | Giới hạn 4 dòng, gradient fade khi thu gọn, tap "Xem thêm ▼" để expand |
| 🏷️ **Badge thông tin** | Năm / Quốc gia / Thể loại / Chất lượng |
| 🎭 **Diễn viên** | Ảnh diễn viên thật từ TMDB Credits API, circular crop, fallback emoji |
| 📺 **Chọn Server** | Dropdown chọn nguồn phát (nhiều server dự phòng) |
| 🔀 **Sắp xếp tập** | Toggle 1→N / N→1 cho phim nhiều tập |
| 📊 **Episode Progress** | Progress bar trên nút tập đã xem |
| ▶️ **Tiếp tục xem** | Nút "Xem tiếp từ Tập X" nếu đang xem dở |
| 📋 **Season Grouping** | Phát hiện phim nhiều mùa (Phần 1/2/3), horizontal selector |
| 🔖 **Xem Sau (Watchlist)** | Nút bookmark để lưu vào danh sách Xem Sau |
| ➕ **Thêm vào Playlist** | Chọn playlist thủ công để thêm phim |
| 🗂️ **Phim liên quan** | Row "Có thể bạn thích" — phim cùng thể loại |

---

## ▶️ Trình Phát (Player)

| Tính năng | Mô tả |
|-----------|-------|
| 🎥 **Fullscreen native** | Separate Activity, không conflict với navigation bars |
| 🎬 **Gradient Scrims** | Top/bottom gradient overlay cho controls — cảm giác cinematic premium |
| 📋 **Episode Bottom Sheet** | Nút "Tập X" mở ModalBottomSheet grid tất cả tập, dark theme |
| ⏮️⏭️ **Tập Trước / Tiếp** | Nút chuyển tập ngay trong player |
| ⏩ **Auto-play next** | Tự động chuyển tập tiếp theo sau khi kết thúc (bật/tắt trong Settings) |
| 👆 **Gesture seek** | Double-tap trái/phải ±10 giây |
| 🔆 **Gesture brightness** | Vuốt dọc trái → điều chỉnh độ sáng |
| 🔊 **Gesture volume** | Vuốt dọc phải → điều chỉnh âm lượng |
| 🔡 **Subtitle Picker** | Chọn phụ đề (VI / EN / JP...) từ nhiều nguồn |
| 🕐 **Intro/Outro Skip** | Đánh dấu + tự động bỏ qua đoạn intro và outro |
| 🌏 **Per-country Defaults** | Mark intro/outro 1 phim Hàn → áp dụng mặc định cho tất cả phim Hàn |
| 🖥️ **Keep Screen On** | Màn hình không tắt trong lúc xem |
| 💾 **Lưu tiến độ** | Ghi lại vị trí dừng để "Xem tiếp" sau |

---

## 🗂️ Danh Mục & Khám Phá

| Tính năng | Mô tả |
|-----------|-------|
| 🗺️ **Genre Hub** | Screen thể loại với icon grid — Hành động / Kinh dị / Tình cảm / Cổ trang... |
| 🌍 **Country Filter** | Chip lọc theo quốc gia trong CategoryScreen |
| 📅 **Year Filter** | Chip lọc theo năm (Tất cả / 2025~2018) trong CategoryScreen |
| ♾️ **Infinite Scroll** | Tự tải thêm khi cuộn đến cuối, loading spinner |
| 🎌 **Anime Tab** | Nội dung anime từ Anime47 API, genre chips, Donghua section |

---

## 🔖 Thư Viện Cá Nhân

### Xem Sau (Watchlist)
- Bookmark phim bất kỳ từ Detail screen (icon 🔖)
- Grid xem tất cả phim đã bookmark với ảnh bìa + timestamp
- Tap xem, giữ / tap ✕ để xóa

### Playlist Thủ Công
- **Tạo playlist** đặt tên tùy ý ("Xem Cuối Tuần", "List Gia Đình"...)
- **Thêm phim** từ Detail screen → bottom sheet chọn playlist
- **Xem playlist** toàn bộ phim theo từng danh sách
- **Xóa item** khỏi playlist riêng lẻ
- **Xóa playlist** với confirm dialog

### Lịch Sử Xem
- Tự động ghi lại khi xem phim (tên, tập, thời điểm)
- Screen lịch sử đầy đủ với timestamp
- Xóa toàn bộ lịch sử từ Settings

### Yêu Thích
- Toggle ❤️ bất kỳ phim → lưu persistent
- Row yêu thích trên HomeScreen

---

## ⚙️ Cài Đặt

| Tính năng | Mô tả |
|-----------|-------|
| 🎯 **Chất lượng mặc định** | Auto / 360p / 480p / 720p / 1080p khi bắt đầu phát |
| ⏭️ **Auto-play tiếp** | Bật/tắt tự động qua tập sau khi kết thúc |
| 🌍 **Filter quốc gia** | Chọn quốc gia ưa thích cho nội dung Home |
| 🎭 **Filter thể loại** | Chọn thể loại ưa thích cho nội dung Home |
| 🗑️ **Xóa lịch sử** | Xóa toàn bộ watch history |
| 💔 **Xóa yêu thích** | Clear toàn bộ danh sách yêu thích |
| 💾 **Export Backup** | Xuất toàn bộ dữ liệu (favorites, history, watchlist, playlists) ra file JSON |
| 📥 **Import Backup** | Import file JSON từ thiết bị khác (confirm trước khi ghi đè) |
| ℹ️ **Version info** | Hiển thị version name + build number tự động |

---

## 📱 Kỹ Thuật

| Item | Chi tiết |
|------|----------|
| **Min Android** | Android 7.0 (API 24) |
| **Target Android** | Android 15 (API 35) |
| **Language** | Kotlin + Jetpack Compose |
| **Architecture** | MVVM · ViewModel · StateFlow |
| **Image loading** | Coil 3 + wsrv.nl image proxy (resize + compress server-side) |
| **Video** | ExoPlayer (Media3) |
| **Navigation** | Compose Navigation + Separate Activity cho Player |
| **Storage** | SharedPreferences (settings, favorites, history, watchlist, playlists) |
| **Networking** | Retrofit 2 + OkHttp 4 + HTTP cache 50MB |
| **Data sources** | KKPhim API + Ophim API + Anime47 API + OMDB API + TMDB API |

---

## 🗺️ Roadmap

### Đang lên kế hoạch:
- **#N-1** Thông báo tập mới (WorkManager periodic check)
- **#N-3** Widget "Xem tiếp" (4×2 home screen widget)
- **#P-1** Subtitle Style (font, size, màu, opacity)
- **#P-2** Subtitle Position (slider Y)
- **#S-5** Dynamic Trending — từ khóa trending tính từ search history aggregate
- **#TD-1** Xóa Extractor folder (giảm ~11MB APK)
- **#TD-2** Room DB (thay SharedPreferences cho history/favorites)
- **#B-3** Shared Element Transition (full Navigation 2.8+ API)

---

*Tài liệu này được cập nhật tự động theo mỗi release.*
