#!/usr/bin/env bash
set -euo pipefail

docker stop localstack || true
echo "LocalStack stopped"
