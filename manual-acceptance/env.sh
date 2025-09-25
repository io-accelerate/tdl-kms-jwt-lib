#!/usr/bin/env bash
# Environment for manual acceptance tests against AWS sandbox

export AWS_PROFILE=default
export AWS_DEFAULT_REGION=eu-west-2

export APIGW_URL="https://xxxx.execute-api.eu-west-2.amazonaws.com/prod/credentials"
export SOURCE_BUCKET="sandbox-source-bucket"
export ALLOWED_PREFIX="uploads/test-user/"

export KMS_JWT_CLI_REGION="eu-west-2"
export KMS_JWT_CLI_KEY_ARN="arn:aws:kms:eu-west-2:577770582757:key/7298331e-c199-4e15-9138-906d1c3d9363"
export KMS_JWT_CLI_USERNAME="testuser"
export KMS_JWT_CLI_JOURNEY="SUM,UPR"
