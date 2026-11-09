# GT TV Player 2.0 — Satellite Edition

Native lightweight Android TV player for a GT television running Android 14 with a Mali-450 GPU.

## Channel lineup

001 MBC 1
002 MBC 4
003 MBC العراق
004 MBC مصر
005 MBC دراما
006 الحدث
007 العربية
008 MBC Bollywood
009 Spacetoon
010 MBC مصر دراما

The previous 288p MBC Masr Drama URL was replaced by the new supplied MBC Drama and MBC Masr Drama streams. Streams are third-party services and may have geo/copyright restrictions; the player does not circumvent restrictions.

## Television remote behavior

- The app **starts MBC 1 instantly** on every launch.
- In fullscreen, CH+/CH− or D-pad Up/Down changes channels.
- OK opens a numbered channel list; Up/Down selects; OK tunes; Back closes.
- Left opens channel list, Right opens status/player controls.
- MENU/GUIDE opens list; INFO opens controls.
- Digits 1–9 directly select their channel; enter **1 then 0** to select channel 10.
- Info panel offers auto/480p/720p **maximum quality caps**, live reconnect and previous channel.
- Back closes overlays; press Back twice from fullscreen to exit.
- 28-second loading timeout with an error banner and manual retry.

Channels can be edited in app/src/main/assets/channels.json without altering the player.

## APK

[GT TV Player v2 release](https://github.com/MuhammadMajeed1/Apk/releases/tag/gt-tv-v2)

This is a GitHub Actions **debug-signed** build. The original v1 is signed using a different ephemeral GitHub runner certificate, so updating in place may fail with a signature mismatch. If so, **uninstall v1** then install v2. Android 14 is supported.

The app is tested for build success using GitHub Actions, not on the physical GT television.
