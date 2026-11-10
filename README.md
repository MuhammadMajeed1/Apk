# GT TV Satellite v3.0

Android 14 GT television edition, optimized for remote use with native ExoPlayer (HLS and DASH).
It automatically starts MBC 1 with adaptive video quality and no browser.

## What changed
- Simple TV/play icon and TV launcher banner.
- 16 channels in app/src/main/assets/channels.json (previous MBC Egypt, Al Hadath and Spacetoon retained).
- User-supplied new MBC stream URLs, four DASH sources with local ClearKey pairs.
- DASH support uses Android Media3 DRM session management. It works only if the manifest and device support ClearKey; Widevine-only streams may not play.
- No manually forced 480p/720p: automatic video adaptation is always active.
- Improved number entry: channels 1..16, or 001..016 with remote number buttons.
- CH+/CH− and arrows zap, OK opens channel guide, BACK closes.
- Graceful timeout and manual retry.
- Version 3 has its own application ID, so it installs next to v2 without uninstalling the working previous release.

[Download v3 APK](https://github.com/MuhammadMajeed1/Apk/releases/tag/gt-tv-v3)

Please only use streams and decryption keys you are authorized to access. Broadcast availability depends on the upstream provider. The GitHub build verifies compilation but not playback on the physical GT TV.
