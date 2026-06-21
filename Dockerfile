# Playwright's official Java image — bundles the browsers and every OS library
# they need, pinned to the SAME Playwright version as pom.xml (1.60.0) so the
# pre-installed browsers match the Java client. (noble = Ubuntu 24.04.)
FROM mcr.microsoft.com/playwright/java:v1.60.0-noble

# The base image ships a JDK + browsers but not Maven — add it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# Keep the Maven cache in a shared, world-readable path so the build (root) and
# the runtime user (pwuser) use the same downloaded dependencies.
ENV MAVEN_REPO=/m2

WORKDIR /app

# Pre-fetch dependencies first, keyed only on pom.xml. This layer is cached and
# only re-runs when pom.xml changes — not on every source edit.
COPY pom.xml ./
RUN mvn -B -q -Dmaven.repo.local=$MAVEN_REPO dependency:go-offline

# Now the sources (changes here don't bust the dependency cache above).
COPY src ./src

# Chromium will not run as root without --no-sandbox, so run as the non-root
# pwuser provided by the base image. Give it ownership of /app (to write
# target/) and read access to the shared Maven cache.
RUN chown -R pwuser:pwuser /app && chmod -R a+rX "$MAVEN_REPO"
USER pwuser

# Default: run the whole suite headless (config.properties sets headless=true).
# Reports are written to /app/target — mount a volume to extract them:
#   docker run --rm -v "$PWD/target:/app/target" mycucumbertest
CMD ["mvn", "-B", "-Dmaven.repo.local=/m2", "test"]
