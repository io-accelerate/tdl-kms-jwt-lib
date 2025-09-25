#!/usr/bin/env bash
set -euo pipefail

DIR="$(dirname "$0")"
cd "$DIR"

TESTS=("$@")
if [ ${#TESTS[@]} -eq 0 ]; then
  TESTS=(00-sanity.sh 10-generate-token.sh)
fi

for t in "${TESTS[@]}"; do
  echo "=== Running $t ==="
  "./$t"
done
