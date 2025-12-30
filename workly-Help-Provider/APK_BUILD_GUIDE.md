# Workly Help Provider - Detailed APK Build Guide

This guide provides step-by-step instructions to build the Workly Help Provider Android application APK using Docker.

## 1. Prerequisites

- **Docker**: Installed and running on your machine.
- **Backend Access**: Ensure you know the URL of your running Workly Server.

---

## 2. Configuration

Verify the backend settings in `app/src/main/assets/config.properties`:
```properties
backend.url=http://your-ip-address:8080/v1/
```
> [!IMPORTANT]
> Ensure the URL ends with a trailing slash `/` for Retrofit compatibility.

---

## 3. Building the APK using Docker

Using Docker ensures all dependencies (SDK 34, JDK 17) are correctly handled.

### Step A: Build the Docker Image
Run from the root of `workly-Help-Provider`:
```bash
docker build -t workly-help-provider-build .
```

### Step B: Extract the Signed APK
1. Create a temporary container:
   ```bash
   docker create --name provider-temp workly-help-provider-build
   ```
2. Copy the APK to your current directory:
   ```bash
   docker cp provider-temp:/app/app/build/outputs/apk/debug/app-debug.apk ./workly-help-provider-debug.apk
   ```
3. Remove the temporary container:
   ```bash
   docker rm provider-temp
   ```

---

## 4. Troubleshooting

- **Architecture Warning**: If you are on an Apple Silicon Mac, the `Dockerfile` uses `--platform=linux/amd64` for compatibility. This is expected.
- **Installation Error**: If you see "App not installed", ensure any previous version of the app is uninstalled from your phone first.
