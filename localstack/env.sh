#!/usr/bin/env bash
# Common environment for LocalStack scripts

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=eu-west-2
export LOCALSTACK_ENDPOINT="--endpoint-url=http://localhost:4566 --region eu-west-2"
