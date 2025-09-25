#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"

echo "Listing KMS keys in LocalStack..."
aws $LOCALSTACK_ENDPOINT kms list-keys
