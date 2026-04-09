#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
STAGE_DIR="${REPO_ROOT}/build/jpackage-input"
MAVEN_REPO_DIR="${REPO_ROOT}/build/.m2/repository"
DIST_DIR="${REPO_ROOT}/dist"
PACKAGE_TYPE="${1:-deb}"

PROJECT_VERSION="$(grep -m1 '<version>' "${REPO_ROOT}/pom.xml" | sed -E 's|.*<version>([^<]+)</version>.*|\1|')"
APP_VERSION="${PROJECT_VERSION%-SNAPSHOT}"

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "This script must be run on Linux."
  exit 1
fi

if [[ "${PACKAGE_TYPE}" != "deb" && "${PACKAGE_TYPE}" != "rpm" ]]; then
  echo "Usage: ./launcher/build-linux-installer.sh [deb|rpm]"
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

echo "Creating Linux ${PACKAGE_TYPE} installer..."
jpackage \
  --type "${PACKAGE_TYPE}" \
  --name ODME \
  --app-version "${APP_VERSION}" \
  --input "${STAGE_DIR}" \
  --main-jar "$(basename "${TARGET_JAR}")" \
  --main-class odme.odmeeditor.Main \
  --dest "${DIST_DIR}" \
  --vendor "DLR SES" \
  --description "Operation Domain Modeling Environment" \
  --install-dir /opt/odme

echo
echo "Linux installer created in:"
echo "  ${DIST_DIR}"
echo
echo "Example:"
if [[ "${PACKAGE_TYPE}" == "deb" ]]; then
  echo "  sudo dpkg -i ${DIST_DIR}/odme_${APP_VERSION}-1_amd64.deb"
else
  echo "  sudo rpm -i ${DIST_DIR}/odme-${APP_VERSION}-1.x86_64.rpm"
fi
