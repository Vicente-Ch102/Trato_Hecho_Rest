#!/usr/bin/env sh
# Lightweight Gradle launcher. Uses installed Gradle or bootstraps the pinned distribution.
set -eu
GRADLE_VERSION="8.14.7"
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/trato-hecho-gradle/gradle-$GRADLE_VERSION"
if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$GRADLE_USER_HOME/trato-hecho-gradle"
  ZIP="$GRADLE_USER_HOME/trato-hecho-gradle/gradle-$GRADLE_VERSION-bin.zip"
  echo "Gradle $GRADLE_VERSION no está instalado; descargándolo desde services.gradle.org..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    echo "Instala Gradle $GRADLE_VERSION o instala curl/wget para descargarlo." >&2
    exit 1
  fi
  command -v unzip >/dev/null 2>&1 || { echo "Se requiere unzip." >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$GRADLE_USER_HOME/trato-hecho-gradle"
fi
exec "$DIST_DIR/bin/gradle" "$@"
