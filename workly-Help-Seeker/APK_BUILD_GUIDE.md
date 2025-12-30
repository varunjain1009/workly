# Workly Help Seeker - Detailed APK Build Guide

This guide provides step-by-step instructions to build the Workly Help Seeker Android application APK from source.

## 1. Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Java Development Kit (JDK) 17**: Required for Spring Boot and Android Gradle compatibility.
- **Android SDK**: You need the SDK platforms and build tools (version 34).
- **Gradle**: If the wrapper (`gradlew`) is missing, you may need a local installation of Gradle (version 8.0+ recommended).

### Environment Variables
Ensure `JAVA_HOME` points to your JDK 17 installation and `ANDROID_HOME` points to your Android SDK directory.

---

## 2. Project Configuration

Before building, verify the backend configuration:

1. Navigate to `app/src/main/assets/config.properties`.
2. Update the `backend.url` to point to your running Workly Server (e.g., `http://192.168.1.5:8080/api/v1`).
3. Ensure `monetisation.enabled` is set correctly for your testing needs.

---

## 3. Building the Debug APK

The debug APK is used for development and local testing. It is automatically signed with a default debug key.

1. Open a terminal in the project root (`workly-Help-Seeker`).
2. Run the following command:
   ```bash
   ./gradlew assembleDebug
   ```
   *Note: If `gradlew` is not executable, run `chmod +x gradlew` first.*

3. **Output Location**:
   The generated APK will be available at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Building a Signed Release APK

For production or distribution, you must build a signed release APK.

### Step A: Generate a Upload Key (if you don't have one)
Run the following command to generate a keystore file:
```bash
keytool -genkey -v -keystore workly-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias workly-alias
```

### Step B: Configure Signing in `app/build.gradle`
Update the `android` block to include `signingConfigs`:

```gradle
android {
    ...
    signingConfigs {
        release {
            storeFile file("path/to/workly-release-key.jks")
            storePassword "your_password"
            keyAlias "workly-alias"
            keyPassword "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            ...
        }
    }
}
```

### Step C: Build the Release APK
Run:
```bash
./gradlew assembleRelease
```

3. **Output Location**:
   The generated APK will be available at:
   `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if configured).

## 5. Building using Docker (Recommended)

Using Docker is an excellent way to build the APK without worrying about local SDK or JDK configurations.

### A. Create the Docker Image
From the project root, build the image:
```bash
docker build -t workly-help-seeker-build .
```

### B. Extract the APK
Once the build is complete, create a temporary container to copy the APK to your host machine:
```bash
docker create --name temp-container workly-help-seeker-build
docker cp temp-container:/app/app/build/outputs/apk/debug/app-debug.apk ./workly-help-seeker-debug.apk
docker rm temp-container
```

---

## 6. Troubleshooting

- **Permissions**: If `./gradlew` fails with Permission Denied, run `chmod +x gradlew`.
- **SDK Missing**: Create a `local.properties` file in the root directory with:
  `sdk.dir=/path/to/your/android/sdk`
- **Memory**: Building Android apps can be resource-intensive. Ensure your system has at least 8GB of RAM.
