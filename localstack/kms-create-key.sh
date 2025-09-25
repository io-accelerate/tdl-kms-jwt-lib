#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"

echo "Creating KMS test key in LocalStack..."
aws $LOCALSTACK_ENDPOINT kms create-key \
  --description "LocalStack test key" \
  --key-usage ENCRYPT_DECRYPT \
  --origin AWS_KMS
