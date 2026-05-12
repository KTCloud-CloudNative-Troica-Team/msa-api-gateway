# syntax=docker/dockerfile:1.7

# R-27 (a): 호스트의 `./gradlew build`가 이미 테스트 + bootJar 완료 → Docker는
# packaging만 담당 (~1.5분 절약). 기존 3-stage 빌드 (build → layers → runtime)
# 에서 build stage 제거.
#
# 사전 조건: ci.yml의 build-test step이 build/libs/*.jar를 생성 (single module).

# ===== Stage 1: layered jar extraction =====
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ===== Stage 2: runtime =====
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app

COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

EXPOSE 8100
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
