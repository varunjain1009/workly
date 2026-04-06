#!/usr/bin/env bash
# Fast local APK build — uses host Gradle/Android SDK cache, no Docker overhead.
# First run: ~2-3 min (downloads deps). Subsequent incremental builds: ~15-30s.
set -euo pipefail
cd "$(dirname "$0")"
./gradlew assembleDebug
echo ""
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
