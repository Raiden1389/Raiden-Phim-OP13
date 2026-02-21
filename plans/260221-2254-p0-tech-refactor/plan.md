# Plan: P0 Tech Refactor — God Screen Split + Room DB
Created: 2026-02-21T22:54:00+07:00
Status: 🟡 Planning

## Overview
Refactor 2 blocking tech debts để unblock toàn bộ backlog v1.20+:
1. **TD-4** — Tách 4 God Screens thành Screen + ViewModel + Components
2. **TD-2** — Migrate 7 SharedPreferences Managers sang Room DB

**Tại sao P0?**
- Mọi feature mới (UX-1, PL-6, S-6...) đều chạm vào PlayerScreen/HomeScreen/SearchScreen
- File 800-1300 dòng = rủi ro regression cao khi edit
- SharedPreferences parse JSON on main thread = jank khi data lớn
- Room mở cánh cửa cho: offline mode, query, migration, type-safe

## Tech Stack
- Room 2.7.0 (KSP, no KAPT)
- Existing: Kotlin 2.2.20, Compose BOM 2026.02.00, AGP 8.10.0

## Nguyên Tắc
1. **Không đổi UI** — user mở app thấy y hệt. Chỉ refactor code bên trong
2. **Migrate từng Manager 1** — không big-bang. Mỗi Manager = 1 commit riêng
3. **Data migration** — SharedPrefs data cũ phải import sang Room lần đầu
4. **Test sau mỗi phase** — build APK + verify manually

## Phases

| Phase | Name | Status | Tasks | Est. |
|-------|------|--------|-------|------|
| 01 | God Screen Split | ⬜ Pending | 14 | 1-2 sessions |
| 02 | Room DB Setup + Core Entities | ⬜ Pending | 12 | 1-2 sessions |
| 03 | Manager Migration + Data Import | ⬜ Pending | 10 | 1 session |

## Quick Commands
- Start Phase 1: `/code phase-01`
- Check progress: `/next`
- Save context: Memory Keeper auto-save
