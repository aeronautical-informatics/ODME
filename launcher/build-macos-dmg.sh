#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
STAGE_DIR="${REPO_ROOT}/build/jpackage-input"
MAVEN_REPO_DIR="${REPO_ROOT}/build/.m2/repository"
DIST_DIR="${REPO_ROOT}/dist"

PROJECT_VERSION="$(sed -n '0,/<version>/{s:.*<version>\(.*\)<\/version>.*:\1:p}' "${REPO_ROOT}/pom.xml")"
APP_VERSION="${PROJECT_VERSION%-SNAPSHOT}"
JPACKAGE_SIGN_ARGS=()

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This script must be run on macOS."
  exit 1
fi

if [[ -n "${MACOS_SIGNING_IDENTITY:-}" ]]; then
  if [[ -z "${MACOS_PACKAGE_IDENTIFIER:-}" ]]; then
    echo "MACOS_PACKAGE_IDENTIFIER must be set when MACOS_SIGNING_IDENTITY is provided."
    exit 1
  fi
  JPACKAGE_SIGN_ARGS+=(
    --mac-sign
    --mac-package-identifier "${MACOS_PACKAGE_IDENTIFIER}"
    --mac-signing-key-user-name "${MACOS_SIGNING_IDENTITY}"
  )
  if [[ -n "${MACOS_KEYCHAIN:-}" ]]; then
    JPACKAGE_SIGN_ARGS+=(--mac-signing-keychain "${MACOS_KEYCHAIN}")
  fi
fi

mkdir -p "${MAVEN_REPO_DIR}"

echo "Building application jar..."
(
  cd "${REPO_ROOT}"
  mvn -q "-Dmaven.repo.local=${MAVEN_REPO_DIR}" -DskipTests package
)

TARGET_JAR="$(find "${REPO_ROOT}/target" -maxdepth 1 -type f -name 'SESEditor-*.jar' ! -name 'original-*' | sort | tail -n 1)"

if [[ -z "${TARGET_JAR}" || ! -f "${TARGET_JAR}" ]]; then
  echo "Expected packaged jar not found under ${REPO_ROOT}/target/SESEditor-*.jar"
  exit 1
fi

echo "Preparing jpackage input..."
rm -rf "${STAGE_DIR}"
mkdir -p "${STAGE_DIR}"
cp "${TARGET_JAR}" "${STAGE_DIR}/"

echo "Creating macOS dmg installer..."
jpackage \
  --type dmg \
  --name ODME \
  --app-version "${APP_VERSION}" \
  --input "${STAGE_DIR}" \
  --main-jar "$(basename "${TARGET_JAR}")" \
  --main-class odme.odmeeditor.Main \
  --dest "${DIST_DIR}" \
  --vendor "DLR SES" \
  --description "Operation Domain Modeling Environment" \
  "${JPACKAGE_SIGN_ARGS[@]}"

echo
echo "macOS installer created in:"
echo "  ${DIST_DIR}"
echo
echo "Open the generated .dmg and drag ODME.app into Applications."
