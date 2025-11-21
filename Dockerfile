FROM gradle:8.7-jdk21 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
COPY db ./db
RUN gradle --no-daemon clean installDist

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/install/trailglass-backend ./bundle
COPY --from=build /workspace/db ./db
ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 CMD curl -f http://localhost:${PORT}/health || exit 1
ENTRYPOINT ["./bundle/bin/trailglass-backend"]
