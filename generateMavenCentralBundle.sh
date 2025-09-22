#!/usr/bin/env bash

# Set the vars
# shellcheck disable=SC2002
CURRENT_VERSION=$(cat gradle.properties | grep version | cut -d "=" -f2)
ARTIFACT_NAME=kms-jwt
ARTIFACT_PATH=io/accelerate/${ARTIFACT_NAME}
ARTIFACT_PATH_WITH_VERSION=${ARTIFACT_PATH}/${CURRENT_VERSION}

# Publish to Maven Local
./gradlew publishToMavenLocal

# Prep the publish folder
rm -rf build/publish
mkdir -p build/publish/${ARTIFACT_PATH}

# Copy from Maven Local
cp -R "$HOME/.m2/repository/${ARTIFACT_PATH_WITH_VERSION}"  build/publish/${ARTIFACT_PATH}

# MD5 and SHA1
# shellcheck disable=SC2044
for file in $(find "build/publish/${ARTIFACT_PATH_WITH_VERSION}" -type f); do
    md5sum "$file" | cut -d ' ' -f 1 > "$file.md5"
    shasum -a 1 "$file" | cut -d ' ' -f 1  > "$file.sha1"
done

(cd build/publish/ && zip -r ${ARTIFACT_NAME}.zip io/)
echo "---------------------------------------------------"
echo "Now you need to upload the build/publish/${ARTIFACT_NAME}.zip file to the Maven Central Repository"
echo "https://central.sonatype.com/publishing"