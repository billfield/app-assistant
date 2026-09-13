#!/bin/sh
# Gradle wrapper script (fallback when gradle-wrapper.jar is not yet present)
# See gradlew.bat for initialization instructions.

if ! command -v gradle >/dev/null 2>&1; then
    echo ""
    echo "[ERROR] gradle command not found"
    echo ""
    echo "Please initialize Gradle Wrapper first:"
    echo "  1. Download https://services.gradle.org/distributions/gradle-8.7-bin.zip"
    echo "  2. Extract and add bin/ to PATH"
    echo "  3. Run: gradle wrapper --gradle-version 8.7"
    echo ""
    exit 1
fi
exec gradle "$@"
