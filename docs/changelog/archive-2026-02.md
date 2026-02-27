# 📦 Raiden Phim — Changelog Archive (2026-02)

---

## v1.22.1 — 2026-02-27 (FFmpeg Audio + Player Polish)

**Session**: FFmpeg Audio Decoder + Player Polish
**Objective**: Fix silent MKV playback (EAC3 unsupported), clean episode names, redesign subtitle dialog

### Added
- FFmpeg audio decoder via `nextlib-media3ext:1.9.1-0.11.0` — AC3, EAC3, DTS, TrueHD software decode
- `NextRenderersFactory` + `EXTENSION_RENDERER_MODE_PREFER`
- `cleanEpName()` + `smartEpLabel()` utility functions

### Changed
- Media3 1.9.2 → 1.9.1 (match nextlib)
- Subtitle dialog → glassmorphism overlay (match Audio TrackSelectionDialog)
- Split PlayerSubtitleDialog → PlayerSubtitleDialog + PlayerOnlineSubtitles
- Disabled R8 minify + shrink resources (faster builds)

### Fixed
- Episode name "Tập 5 . 1080 3,3 GB" → "Tập 5"
- Double "Tập Tập 5" → "Tập 5"
- Auto-play — removed duplicate AudioFocusEffect (ExoPlayer handles internally)

### Technical Notes
- Native lib is `libmedia3ext.so` not `libnextlib.so`
- AAR bundles: libavcodec, libavutil, libmedia3ext, libswresample, libswscale
- `setAudioAttributes(attrs, handleAudioFocus=true)` makes ExoPlayer manage focus — never duplicate

### Files
| File | Change |
|---|---|
| build.gradle.kts | v1.22.1, nextlib dep, Media3 1.9.1, R8 off |
| PlayerScreen.kt | NextRenderersFactory + diagnostic |
| PlayerViewModel.kt | cleanEpName + smartEpLabel |
| PlayerTopBar.kt | smartEpLabel |
| PlayerBottomActions.kt | smartEpLabel |
| PlayerEpisodeSheet.kt | cleanEpName |
| PlayerSessionEffects.kt | cleanEpName, removed AudioFocusEffect |
| PlayerSourceLoader.kt | debug log |
| PlayerSubtitleDialog.kt | rewritten glassmorphism |
| PlayerOnlineSubtitles.kt | NEW — online search |

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
