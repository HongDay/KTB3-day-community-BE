#### 빌드 스테이지 ###
#
## 빌드 환경까지 포함된 jdk 이미지
#FROM gradle:8.7-jdk21 AS builder
#WORKDIR /app
#
## 캐시 극대화를 위해 gradle 빌드 관련 파일만 먼저 복사
#COPY build.gradle settings.gradle gradlew gradlew.bat ./
#COPY gradle ./gradle
#
## 캐시 극대화를 위해 Gradle 의존성만 먼저 다운로드
#RUN ./gradlew dependencies --no-daemon || true
#
#COPY . .
#
## Spring Boot jar 빌드
#RUN ./gradlew bootJar --no-daemon
#

### 런타임 스테이지 ###
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 결과물 복사
COPY *.jar app.jar

# 컨테이너에서 빌드파일 실행
CMD ["java", "-jar", "app.jar"]