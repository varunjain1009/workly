# Workly Help Provider - Build & Configuration

Instructions for building and configuring the Workly Worker App.

## 🏗 Building the APK

### Debug Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## ⚙️ Configuration
Manage environment settings in `app/src/main/assets/config.properties`.

```properties
api.base_url=http://10.0.2.2:8080/api/v1/
chat.url=ws://10.0.2.2:8082/ws/chat
app.debug_enabled=true
auth.otp.resend_delay_seconds=300
```

## 📦 Features
*   **Job Dashboard**: View assigned and available jobs.
*   **OTP Verification**: Secure start/end of jobs.
*   **Chat**: Real-time communication with Customer.
*   **Wallet**: Track earnings (Future).
