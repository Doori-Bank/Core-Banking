# ========== Build Stage ==========
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사
COPY src src

# Gradle 빌드 실행 (의존성 다운로드 및 빌드)
RUN chmod +x ./gradlew && \
    ./gradlew clean bootJar -x test --no-daemon

# ========== Runtime Stage ==========
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 타임존 설정
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# JVM 옵션 (메모리 및 GC 설정)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Build Stage에서 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# non-root 사용자 생성 (보안)
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

