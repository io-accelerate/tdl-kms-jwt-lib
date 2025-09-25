#!/usr/bin/env bash
set -euo pipefail

ARTIFACTS_DIR="$(dirname "$0")/artifacts"
SNAPSHOTS_DIR="$(dirname "$0")/snapshots"
mkdir -p "$ARTIFACTS_DIR" "$SNAPSHOTS_DIR"

timestamp() {
  date -u +"%Y%m%dT%H%M%SZ"
}

normalize() {
  sed -E \
    -e 's/^[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3} \[[^]]+\] ([A-Z]+[[:space:]]+)/\1/' \
    -e 's/[0-9]{12,}//g' \
    -e 's/[A-Za-z0-9+/=]{20,}/<REDACTED>/g'
}

capture_and_compare() {
  local name="$1"; shift
  local ts="$(timestamp)"
  local out="$ARTIFACTS_DIR/${ts}_${name}.out"

  local exit_code=0
  if ! "$@" >"$out"; then
    exit_code=$?
    echo "[WARN] Command exited with status ${exit_code}" >&2
  fi

  normalize <"$out" >"$out.norm"

  local snap="$SNAPSHOTS_DIR/${name}.snap"
  if [[ "${UPDATE_SNAPSHOTS:-0}" == "1" ]]; then
    cp "$out.norm" "$snap"
    echo "Updated snapshot $snap"
  else
    if ! diff -u "$snap" "$out.norm"; then
      echo "❌ Snapshot mismatch for $name" >&2
      exit 1
    else
      echo "✅ $name matches snapshot"
    fi
  fi

  return ${exit_code}
}
