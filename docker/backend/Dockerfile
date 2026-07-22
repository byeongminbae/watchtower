# 사용법:
#   dev  빌드: docker build --build-arg SPRING_PROFILE=dev  -t watchtower-backend-dev:tag  .
#   prod 빌드: docker build --build-arg SPRING_PROFILE=prod -t watchtower-backend-prod:tag .

# 1단계: 빌드 스테이지
FROM gradle:jdk25-corretto AS build

WORKDIR /app

# 의존성 캐시 최적화: 빌드 스크립트 먼저 복사 후 의존성 다운로드
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x ./gradlew

# 오프라인 캐시 워밍업 (소스 없이 의존성만 먼저 받아 레이어 캐싱)
RUN ./gradlew dependencies --no-daemon || true

# 실제 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행 스테이지
FROM amazoncorretto:25-jdk AS runtime

# 기본 프로필은 dev
ARG SPRING_PROFILE=dev
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
ENV TZ=Asia/Seoul

# HEALTHCHECK 용 curl 설치
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 비루트 사용자로 실행 (보안 권장 사항)
RUN groupadd -r watchtower && useradd -r -g watchtower watchtower

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R watchtower:watchtower /app
USER watchtower

EXPOSE 8080

# 컨테이너 자체 헬스체크 (docker inspect 로도 상태 확인 가능하도록)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -f http://localhost:8080/api/status || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]