# Use an image with JDK 17 and Android SDK 34
FROM --platform=linux/amd64 mobiledevops/android-sdk-image:34.0.0-jdk17

# Set the working directory
WORKDIR /app

# Ensure we are running as root to change permissions
USER root

# Copy only the gradle files first to cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./
COPY app/build.gradle ./app/

# Download dependencies (this layer will be cached unless dependencies change)
RUN chmod +x ./gradlew
RUN ./gradlew help --no-daemon

# Copy the rest of the source code
COPY . .

# Build the debug APK with build cache enabled
RUN ./gradlew assembleDebug --build-cache --no-daemon

# The output will be in /app/app/build/outputs/apk/debug/
