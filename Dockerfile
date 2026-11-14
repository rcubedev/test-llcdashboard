# syntax=docker/dockerfile:1
# builds and runs the app: docker build -t limelight-dashboard . && docker run -p 8080:8080 --env-file .env limelight-dashboard
# (vercel and similar hosts build this file themselves)

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
# tests run in github actions, this only builds the jar (gradle also downloads tailwind here)
# the cache mount keeps gradle's downloads between builds where the builder supports it
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon --console=plain

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/build/libs/dashboard.jar dashboard.jar
USER app
# small free instances have about 512 MB: let the jvm use most of it, with the lightest garbage collector
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dashboard.jar"]
