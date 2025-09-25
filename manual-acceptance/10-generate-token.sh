#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

source "${SCRIPT_DIR}/env.sh"
source "${SCRIPT_DIR}/common.sh"

KMS_JWT_CLI_JAR="$(find "${REPO_ROOT}/kms-jwt-cli/build/libs" -maxdepth 1 -name 'kms-jwt-cli-*-all.jar' -print 2>/dev/null | sort | head -n1 || true)"
if [[ -z "${KMS_JWT_CLI_JAR}" ]]; then
  echo "[ERROR] Shaded CLI jar not found. Build it with ./gradlew :kms-jwt-cli:shadowJar" >&2
  exit 1
fi

if [[ -n "${KMS_JWT_CLI_LOGBACK:-}" && ! -f "${KMS_JWT_CLI_LOGBACK}" ]]; then
  echo "[ERROR] Logback configuration not found at ${KMS_JWT_CLI_LOGBACK}." >&2
  exit 1
fi

run_cli() {
  local command_name="$1"
  shift

  local java_cmd=(java)
  if [[ -n "${KMS_JWT_CLI_LOGBACK:-}" ]]; then
    java_cmd+=("-Dlogback.configurationFile=${KMS_JWT_CLI_LOGBACK}")
  fi

  java_cmd+=(-jar "${KMS_JWT_CLI_JAR}")

  if [[ -n "${KMS_JWT_CLI_ENDPOINT:-}" ]]; then
    java_cmd+=(--endpoint "${KMS_JWT_CLI_ENDPOINT}")
  fi

  java_cmd+=("${command_name}")
  java_cmd+=("$@")

  {
    printf '[RUN]';
    for arg in "${java_cmd[@]}"; do
      printf ' %q' "$arg";
    done;
    printf '\n';
  } >&2

  "${java_cmd[@]}"
}

generate_and_validate() {
  local generate_output
  generate_output="$(run_cli generate \
    --region "${KMS_JWT_CLI_REGION}" \
    --key "${KMS_JWT_CLI_KEY_ARN}" \
    --username "${KMS_JWT_CLI_USERNAME}" \
    --journey "${KMS_JWT_CLI_JOURNEY}")"

  local token
  token="$(printf '%s\n' "${generate_output}" | sed -n 's/^JWT_TOKEN=//p' | head -n1)"

  if [[ -z "${token}" ]]; then
    echo "[ERROR] Unable to extract JWT token from generate command output" >&2
    printf '%s\n' "${generate_output}" >&2
    exit 1
  fi

  run_cli validate \
    --region "${KMS_JWT_CLI_REGION}" \
    --key "${KMS_JWT_CLI_KEY_ARN}" \
    --token "${token}"
}

capture_and_compare kms-jwt-cli generate_and_validate
