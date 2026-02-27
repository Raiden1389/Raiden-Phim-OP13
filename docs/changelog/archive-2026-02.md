# 📦 Raiden Phim — Changelog Archive (2026-02)

---

## v1.22.0 — 2026-02-27 (Fshare HD + Player Refactor)

**Session**: Fshare HD Integration + Player Refactor
**Objective**: Integrate Fshare HD content on HomeScreen, fix URL routing bug, refactor PlayerScreen from 1540 to 210 LOC

### Added
- **Fshare HD HomeScreen rows** — Phim Lẻ & Phim Bộ từ ThuVienCine, poster + quality badge + năm
- **FshareCategoryScreen** — grid listing với infinite scroll pagination
- **FshareDetailScreen** — poster scraping, episode listing cho folder, playback integration
- **Ẩn/hiện rows trong Settings** — toggle cho mỗi section

### Changed
- **Player Refactor** — PlayerScreen.kt 1540 → ~210 LOC, tách thành 18 files single-responsibility
  - PlayerControlsOverlay → PlayerTopBar + PlayerTransportControls + PlayerSeekSection + PlayerBottomActions + PlayerGestureIndicators
  - PlayerSessionEffects, PlayerSourceLoader, PlayerAutoNextEffects, PlayerUiState, PlayerGestureLayer
- **Unified AudioFocusEffect** — gộp 2 audio focus paths thành 1 composable
- **Safe Activity cast** — `context as? Activity` thay vì hard cast

### Fixed
- **Fshare 404 episode listing** — ThuVienCine URL bị gửi nhầm tới Fshare API, fix enrichedSlug logic
- **Fshare login 405** — sai User-Agent, đổi sang `kodivietmediaf-K58W6U`
- **Listener leak** — STATE_ENDED listener không có onDispose → chuyển sang DisposableEffect
- **Unsafe activity cast** — crash khi preview/wrapper context

### Technical Notes
- GestureState class wraps MutableState references cho gesture layer
- PlayerUiStateHolder data class bundles tất cả mutable UI state
- computeEffectiveSlug() extracted as pure function

### Files
| File | Change |
|---|---|
| PlayerScreen.kt | Rewrite → thin wiring shell |
| PlayerControlsOverlay.kt | Rewrite → layout shell |
| 15 new Player* files | Extracted composables |
| FshareDetailScreen.kt | enrichedSlug URL fix |
| FsharePlayerLoader.kt | URL validation gate |
| build.gradle.kts | 1.21.0 → 1.22.0 |
| CHANGELOG.md | v1.22.0 entry |

---

## v1.20.8 — 2026-02-23 (Player UX + Episode Badge)

**Session**: Player gestures, episode tracking, Fshare HomeScreen checkpoint
**Objective**: Add player gestures (swipe seek, remaining time, seekbar tooltip), episode tracker badge, begin Fshare integration

### Added
- Swipe Horizontal Seek (MX Player style)
- Remaining Time Toggle (tap time display)
- Seekbar Time Tooltip (red pill on drag)
- Episode Tracker Badge (progress bar + "12/48" badge)
- Fshare HomeScreen rows (checkpoint)

### Changed
- SectionOrderManager with visibility toggle
- HomeScreen row ordering & hide/show

---

> Older versions: see [CHANGELOG.md](../../CHANGELOG.md)
