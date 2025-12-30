# Workly Help Seeker - Build & Icon Documentation

This document provides instructions on how to build the APK and manage assets for the Workly Help Seeker app.

## How to Build the APK

### Debug APK
To build a debug APK for testing:
1. Open the project in Android Studio.
2. Open the terminal and run:
   ```bash
   ./gradlew assembleDebug
   ```
3. The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
To build a release APK:
1. Ensure you have a signing key configured in `build.gradle`.
2. Run:
   ```bash
   ./gradlew assembleRelease
   ```
3. The APK will be generated at:
   `app/build/outputs/apk/release/app-release.apk`

## App Icons

Icons should be placed in the `app/src/main/res/mipmap-*` directories.

| Density | Folder | Icon Size (px) |
| --- | --- | --- |
| MDPI | `mipmap-mdpi` | 48 x 48 |
| HDPI | `mipmap-hdpi` | 72 x 72 |
| XHDPI | `mipmap-xhdpi` | 96 x 96 |
| XXHDPI | `mipmap-xxhdpi` | 144 x 144 |
| XXXHDPI | `mipmap-xxxhdpi` | 192 x 192 |

**Round Icons**: Place corresponding round versions in the same folders as `ic_launcher_round.png`.

## Configuration
The app's backend URL and features are managed via:
`app/src/main/assets/config.properties`

```properties
backend.url=http://10.0.2.2:8080/api/v1/
chat.url=ws://10.0.2.2:8082/ws/chat
job.max-radius-km=100
assignment.mode=FIRST_ACCEPT
monetisation.enabled=true
monetisation.model=PER_JOB
monetisation.allow_browse_without_payment=true
```
