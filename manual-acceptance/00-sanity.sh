#!/usr/bin/env bash
source "$(dirname "$0")/env.sh"
source "$(dirname "$0")/common.sh"

capture_and_compare sanity aws sts get-caller-identity --profile "$AWS_PROFILE" --output json
