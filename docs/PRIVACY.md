# Privacy Policy - OptiCast Video Player

**Version:** v2.6.120 (169)
**Last updated:** 2026-10-06

### Summary
OptiCast is private, with no ads, no tracking, and open source. Your videos stay on your device.

### What we collect
Nothing - no analytics, no tracking, no ads.

- **Your videos stay on your device:** Finds videos on your phone, fetches movie information only when needed
- **Works offline:** Fully functional without internet after initial setup
- **Update check:** Only checks version number when online, uses minimal data, no personal information
- **No accounts:** No login, no cloud, no sync

### Permissions - only what's needed

- **Internet** - To fetch movie information and subtitles, and check for updates
- **Network state** - To check internet availability before update checks
- **Video library access** - To find your video files
- **Notifications** - To show playback controls
- **Background playback** - For background play and picture-in-picture
- **Install updates** - To install app updates inside the app
- **Keep screen awake** - To keep screen on while watching (if enabled)

No location, no contacts, no microphone, no camera.

### Offline-first

- **Library:** Found on device, saved locally, no cloud
- **Posters:** Saved for offline viewing
- **Subtitles:** Saved on device for offline viewing, supports dual subtitles
- **Movie information:** Saved for offline use

### Optional services

- **Movie information:** Optional, fetches posters and details via secure proxy
- **Subtitles:** Optional subtitle search and download

All optional services have their own terms.

### How movie info works - proxy details

To keep the app key-free and F-Droid compliant, movie info requests go through a secure proxy instead of direct TMDB calls:

- **What is sent:** Only movie title, year, or TMDB ID - e.g., `Dune Part Two 2024` - no personal data, no device ID, no location
- **What is NOT sent:** Your videos, file paths, contacts, or any identifier
- **Server protection:** Requests protected by secret header, rate limiting, and CORS - abuse blocked by IP
- **Server logs:** IP temporarily used for rate limiting (to prevent abuse), not tracked, logs deleted after 24 hours, no analytics
- **Offline-first:** After first fetch, posters and details are saved locally and work offline. Proxy only used when you need new metadata

This is why v2.6.105 says "no API key required" - key stays server-side, not in app.

### Updates

- Checks for updates only when online, uses minimal data
- Downloads inside app and installs with system confirmation
- Clear changelog for each version

### Children

No data collection, no ads, safe for all ages. Plays only your own videos.

### Changes

Any changes will be posted on GitHub and the official website.

### Contact

opticastproject@gmail.com and https://github.com/opticastplayer-dev/opticast/issues

### License

GPL-3.0-only - source code available on GitHub.

**Compatibility:** Android 8 and newer, phones only, offline-first
