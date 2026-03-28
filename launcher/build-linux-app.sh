#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
STAGE_DIR="${REPO_ROOT}/build/jpackage-input"
MAVEN_REPO_DIR="${REPO_ROOT}/build/.m2/repository"
DIST_DIR="${REPO_ROOT}/dist"
APP_DIR="${DIST_DIR}/ODME"

PROJECT_VERSION="$(sed -n '0,/<version>/{s:.*<version>\(.*\)<\/version>.*:\1:p}' "${REPO_ROOT}/pom.xml")"
APP_VERSION="${PROJECT_VERSION%-SNAPSHOT}"

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "This script must be run on Linux."
  exit 1
fi

mkdir -p "${MAVEN_REPO_DIR}"

echo "Building application jar..."
(
  cd "${REPO_ROOT}"
  mvn -q "-Dmaven.repo.local=${MAVEN_REPO_DIR}" "-Dmaven.test.skip=true" package
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

rm -rf "${APP_DIR}"
mkdir -p "${DIST_DIR}"

echo "Creating Linux app image..."
jpackage \
  --type app-image \
  --name ODME \
  --app-version "${APP_VERSION}" \
  --input "${STAGE_DIR}" \
  --main-jar "$(basename "${TARGET_JAR}")" \
  --main-class odme.odmeeditor.Main \
  --dest "${DIST_DIR}" \
  --vendor "DLR SES" \
  --description "Operation Domain Modeling Environment"

if [[ ! -x "${APP_DIR}/bin/ODME" ]]; then
  echo "Executable was not created: ${APP_DIR}/bin/ODME"
  exit 1
fi

echo
echo "Linux app image created:"
echo "  ${APP_DIR}/bin/ODME"
echo
echo "Launch it by running the ODME binary. Keep the whole ODME folder together."
