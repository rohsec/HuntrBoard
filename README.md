# HuntrBoard

HuntrBoard is a polished Burp Suite dashboard extension for bug hunters. It adds a custom Burp tab with a bounty progress cockpit, persistent notes workspace, and a compact local MP3 player.

## What it does

- Yearly target tracker with editable monthly values for all 12 months
- Auto-calculated total achieved, remaining target, percent complete, average per month, and best/worst month highlights
- Lightweight custom visuals: monthly progress bar chart and yearly completion ring
- Persistent global notes with multiple note documents, save/rename/delete flows, and session survival
- Compact JukeBox MP3 player with playlist management and transport controls

## Build

```bash
./gradlew jar
```

The built JAR is created under:

```bash
build/libs/HuntrBoard-1.0.0.jar
```

## Load into Burp

1. Build the extension with `./gradlew jar`
2. Open Burp Suite
3. Go to Extensions > Installed > Add
4. Choose the generated JAR from `build/libs/`
5. Confirm the extension loads as `HuntrBoard`

## JukeBox / MP3 player notes

- Local MP3 files only
- Add tracks from disk into the saved playlist
- Playlist and current volume persist across Burp sessions
- Playback is implemented with a lightweight Java MP3 library bundled into the JAR

## Project structure

- `src/main/java/com/rohsec/huntrboard/HuntrBoardExtension.java` - Montoya entry point
- `model/` - persisted state and dashboard models
- `persistence/` - Montoya preferences-backed state repository
- `service/` - tracker calculations and music playback manager
- `ui/` - main tab UI, notes panel, music panel, charts, and shared styling components

## Tech choices

- Java 21
- Burp Montoya API
- Gradle Kotlin DSL
- Burp Suite custom tab via `registerSuiteTab`
- Montoya preferences for state persistence
