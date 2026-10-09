# Feature Evaluation - OptiCast Roadmap

**Current:** v2.6.120 (169) - F-Droid MR !50919 awaiting merge
**Date:** 2026-10-09
**Principle:** Offline-first, private, no ads, phone-only, open source, open source

---

## Evaluated Features

### 1. Auto Rotate based on device orientation
- **Fits?** Yes
- **Value:** High - expected behavior
- **Effort:** Low (1 day)
- **Verdict:** Must-have - Fix existing
- **Current:** We have `autoLandscape` boolean, basic. MX/VLC have Auto / Portrait / Landscape / Sensor + per-video lock.
- **Better:** Settings → Orientation: Auto (sensor) / Portrait / Landscape / Locked + remember per video + lock button in player. Add sensor listener.

### 2. Show Battery Percentage and time like MX player
- **Fits?** Yes
- **Value:** Very High - #1 MX requested feature
- **Effort:** Low (2-3h)
- **Verdict:** Must-have - v2.6.121 first
- **Notes:** Duplicate of #5 in list. 100% offline, no permission, privacy-safe. BatteryManager + time ticker every 60s. Show in top bar chip when controls visible. Toggle in Settings → Player → Show battery & clock (default ON).

### 3. Capture frame as image like MX player
- **Fits?** Yes
- **Value:** Medium
- **Effort:** Low (2h)
- **Verdict:** Nice-to-have - Quick win
- **Notes:** mpv `screenshot` command. Save to Pictures/OptiCast. Add button in More controls. F-Droid OK.

### 4. Queue / Playlist management like MX player
- **Fits?** Yes
- **Value:** High - core for TV shows
- **Effort:** Medium (2-3 days)
- **Verdict:** Must-have - Core
- **Notes:** We have Continue Watching but no queue. Add: Long-press → Play next / Add to queue / Add to playlist + Queue screen with reorder, shuffle, save. Needs DB table.

### 5. Battery+time like MX (duplicate)
- **Verdict:** Skip duplicate - same as #2

### 6. A-B Loop
- **Fits?** Yes
- **Value:** Medium-Niche (10-15% users, but loved by those)
- **Effort:** Medium (1 day)
- **Verdict:** Nice-to-have - Learning niche
- **Notes:** Great for language/dance/music. mpv `ab-loop-a/b`. Add as advanced button in player. Not must-have first.

### 7. Auto Crop black bars
- **Fits?** Yes
- **Value:** Medium
- **Effort:** Low (3h)
- **Verdict:** Nice-to-have
- **Notes:** mpv `video-crop` auto. Toggle in aspect menu: Auto Crop On/Off. Useful for old 4:3 videos.

### 8. Smart Collections like Infuse
- **Fits?** Yes
- **Value:** Very High - differentiator
- **Effort:** Medium (3-4 days)
- **Verdict:** Must-have
- **Notes:** Auto: Unwatched, By Genre, By Year, By Director, 4K, Shorts (<20min), Long, Recently Added. Uses existing movie info, no internet. Makes F-Droid listing stand out: "Only local player with smart collections offline".

### 9. Storage Analyzer like Nova
- **Fits?** Yes
- **Value:** High - practical for 64GB phones
- **Effort:** Medium (2 days)
- **Verdict:** Must-have
- **Notes:** Show: Movies 32GB, TV 12GB, bar chart + biggest files. 100% local, privacy-safe. Add in Settings → Storage.

### 10. Trash / Recently Deleted like Infuse
- **Fits?** Yes
- **Value:** Very High - safety, #1 support issue prevention
- **Effort:** Low-Med (1-2 days)
- **Verdict:** Must-have - Safety first
- **Notes:** Delete → moves to `.trash` folder for 30 days → auto delete. Restore. Easy DB + file move. Must have before any file actions.

### 11. Dialogue Boost like Infuse
- **Fits?** Yes
- **Value:** Medium
- **Effort:** Low (2h)
- **Verdict:** Nice-to-have - Audio
- **Notes:** mpv `af=dialoguenorm` or `lavfi=dynaudnorm`. Boost voices for night. Toggle in audio menu.

### 12. Volume normalization across videos
- **Fits?** Yes
- **Value:** Medium - polish
- **Effort:** Medium (1-2 days)
- **Verdict:** Nice-to-have
- **Notes:** One video loud, next quiet. Store per-video volume or use replaygain. Can wait after dialogue boost.

### 13. Gesture customization like VLC
- **Fits?** Yes
- **Value:** High - power users
- **Effort:** Medium (2-3 days)
- **Verdict:** Must-have
- **Notes:** VLC lets remap left/right swipe, double-tap duration. Add Settings → Gestures → Customize. Keep simple defaults, allow power users.

### 14. Smart File Butler - REWORKED (see detailed design below)
- **Fits?** ⚠️ Risky if auto
- **Value:** Very High - most unique, no player does it
- **Effort:** High (1 week if safe design)
- **Verdict:** Must-have but REWORKED - Suggest-only, never auto, with safe design
- **Notes:** Auto-rename/move is dangerous, can cause data loss, F-Droid users hate. Must be reworked to safe design below.

---

## Recommended Implementation Order

### v2.6.121 (170) - Quick wins, high value, low risk
1. Battery % + time (#2)
2. Auto Rotate improved (#1)
3. Capture frame (#3)
4. Auto Crop (#7)
5. Dialogue Boost (#11)

### v2.6.122 (171) - Core library upgrade
6. Trash / Recently Deleted (#10) - safety first, required before file ops
7. Queue / Playlist (#4)
8. Smart Collections (#8)
9. Storage Analyzer (#9)

### v2.6.123 (172) - Power users
10. Gesture customization (#13)
11. Volume normalization (#12)
12. A-B Loop (#6)
13. Smart File Butler - safe version (#14) - beta, behind feature flag

---

## #14 Smart File Butler - Better Safe Design

### Problem with naive auto-rename/move
- Auto file operations = data loss risk, breaks user organization, F-Droid users will 1-star
- Android scoped storage makes moves expensive and sometimes fails
- TV show detection can be wrong: `video.mp4` != `Show S01E01.mp4`

### Better Way: Organize Assistant - Virtual First, Suggest Only, Never Auto

#### Principle: Never touch files without explicit preview + approval + undo

**Phase 0: Virtual Organization (Zero Risk, Default)**
- Don't move files physically. Like Infuse/Plex, show organized view inside app only:
  - Library already shows Movies / TV Shows virtually, files stay where they are
  - This is already our current behavior - keep it as default
- Benefit: User gets clean library without any file system changes

**Phase 1: Health Report (Read-Only Analysis)**
- New screen: Settings → Tools → Organize Assistant → Scan
- Scan is manual, never during library scan
- Shows read-only report:
  ```
  Health Report:
  • 42 files with messy names (e.g., movie_2023_1080p_x264_YIFY.mp4)
  • 12 possible duplicates (same size/hash)
  • 5 empty folders
  • 3 videos in Download that look like Movies
  • 8 TV episodes not in Show/Season folders
  ```
- No changes yet, just report

**Phase 2: Suggest Mode with Preview & Selective Approval**
- Tap category → list of cards, each with checkbox (default unchecked):
  ```
  [ ] Before: /Download/movie_2023_1080p_x264_YIFY.mp4 (1.2GB)
      After:  /Movies/Movie (2023)/Movie (2023).mp4
      Reason: Clean name, move to Movies folder
      [Preview] [Ignore]

  [ ] Before: /Movies/show_s01e01.mp4
      After:  /TV Shows/Show/Season 01/Show S01E01.mp4
      Reason: TV show detected S01E01
  ```
- User checks only what they want
- Group by: Rename only (same folder) / Move to Movies / Move to TV Shows / Duplicates
- Search + filter: Show only renames, only moves, only >1GB

**Phase 3: Safe Execution (Copy-Verify-Trash, Not Move)**

1. **Dry Run First:** Always first run is dry-run, shows what would happen, no changes
2. **Copy Then Verify:** For moves, copy file to new location, verify size/hash matches, then move old to Trash (not delete)
3. **All Through Trash:** Old files go to Trash / Recently Deleted (feature #10) for 30 days, not immediate delete
4. **Log + Undo:** Create `organize_log.json` in app files:
   ```json
   { "timestamp": "...", "operations": [{ "from": "...", "to": "...", "type": "rename" }] }
   ```
   One-tap Undo button in notification and in Organize Assistant → Undo last
5. **One-by-one with progress:** Process one file at a time, show progress, allow cancel anytime
6. **Failure safe:** If copy fails, stop, keep original, show error

**Phase 4: User Control Levels (Settings)**

- **Level 1 - Suggest Only (Default):** Only show suggestions, no file operations allowed. User sees clean virtual library.
- **Level 2 - Rename in Same Folder Only:** Allow cleaning filename in same folder: `movie_2023...mp4` → `Movie (2023).mp4` in same folder. Safer, no move.
- **Level 3 - Allow Organized Moves (Advanced):** Allow move to Movies/TV Shows folders. Requires extra confirmation dialog with warning + type "MOVE" to confirm. Behind toggle "Enable move operations (advanced)".

**Phase 5: Safety Guards**

- **Never Auto:** Never run during scan, never auto-rename, only manual Tools → Organize Assistant
- **Preserve Structure Option:** Toggle "Keep my folder structure" - only clean filenames, don't move
- **TV Show Safety:** Only suggest TV move if filename contains S01E01 / 1x01 pattern with high confidence, else skip
- **Backup Index:** Before any operation, create `file_index_backup.json` with all file paths
- **Storage Check:** Check free space before copy, abort if <2x file size free
- **Ignore List:** User can tap "Ignore" on file → never suggest again, stored in settings

**Phase 6: Alternative - Export Script Instead of Direct Ops**

- For power users: Button "Export rename script" → generates `organize.sh` bash script with `mv` commands
- User can review script on computer, run manually
- Zero risk from app side

**UI Flow:**

```
Settings → Tools → Organize Assistant
→ [Scan Library] (manual)
→ Health Report (read-only)
→ Tap "42 messy names" → list with checkboxes (all unchecked by default)
→ User checks 5 files → [Preview 5 changes]
→ Preview screen shows before/after + total space + warning
→ [Dry Run] → shows would succeed
→ [Apply 5 changes] → requires Level 2/3 + confirm
→ Copy → Verify → Move old to Trash → Show notification with Undo
→ Log saved
```

**Why this is better:**

- **Zero risk by default:** Virtual organization only, no file touch
- **User in control:** Every file unchecked by default, explicit approval
- **Recoverable:** All through Trash + log + undo
- **Transparent:** Dry run, preview, health report first
- **open source:** No hidden file operations, all explicit, open source log
- **Unique:** No player has safe organize assistant with virtual-first + trash + undo + levels

**Implementation steps:**

1. Build virtual organization (already have) - 0 days
2. Build health report scanner (read-only) - 1 day
3. Build suggest UI with checkboxes - 2 days
4. Build safe copy-verify-trash + log + undo - 2 days
5. Add levels + settings + dry run - 1 day
6. Beta behind feature flag `organizeAssistantEnabled` - default off for v2.6.123, on for v2.6.124 after testing

**This turns dangerous auto-butler into safe, user-respecting assistant - unique to OptiCast, no other player has this level of safety.**

---

**Saved:** 2026-10-09 - Ready for implementation after F-Droid live
