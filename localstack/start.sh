#!/usr/bin/env bash
set -euo pipefail

SERVICES="iam,sts,s3,kms"

docker run -d --rm \
  --name localstack \
  -p 4566:4566 \
  -e SERVICES=$SERVICES \
  -e DEBUG=1 \
  localstack/localstack:4.8.1

echo "LocalStack started with services: $SERVICES on http://localhost:4566"