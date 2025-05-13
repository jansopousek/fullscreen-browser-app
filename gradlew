#!/bin/bash
# gradlew script for Unix-based OS (Linux/macOS)

APP_DIR=$(dirname "$0")
if [ -z "$APP_DIR" ]; then
  APP_DIR="."
fi

if [ -f "$APP_DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -Duser.home="$APP_DIR" -cp "$APP_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
else
  echo "Unable to locate the gradle-wrapper.jar file. Please run 'gradle wrapper' first."
  exit 1
fi