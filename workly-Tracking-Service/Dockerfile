FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Cache Gradle distribution as a Docker layer (re-downloaded only when wrapper files change)
COPY gradlew gradlew
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew --no-daemon --version

COPY . .
RUN sed -i "s/org.gradle.jvmargs=.*/org.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8/" gradle.properties && \
    echo "org.gradle.daemon=false" >> gradle.properties && \
    ./gradlew :tracking-service:bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/workly-Tracking-Service/build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
