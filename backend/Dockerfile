# 사용법:
#   dev  빌드: docker build --build-arg SPRING_PROFILE=dev  -t watchtower-backend-dev:tag  .
#   prod 빌드: docker build --build-arg SPRING_PROFILE=prod -t watchtower-backend-prod:tag .

FROM gradle:jdk25-corretto AS build
WORKDIR /app

# 소스 변경 시 의존성 다운로드 레이어 재사용을 위한 분리
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 실제 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre AS runtime

# 없으면 헬스체크 안됨
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# 기본 프로필은 dev
ARG SPRING_PROFILE=dev
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
ENV TZ=Asia/Seoul

# 비루트 사용자로 실행 (보안 권장 사항)
RUN groupadd -r watchtower && useradd -r -g watchtower watchtower

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R watchtower:watchtower /app
USER watchtower

EXPOSE 8080

# docker inspect 상태 확인용 컨테이너 자체 헬스체크
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]