# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module Gradle build. The `kms-jwt` module contains the core KMS-backed JWT encoder/decoder under `kms-jwt/src/main/java`, with unit tests in `kms-jwt/src/test/java`. The `kms-jwt-cli` module wraps the library in a command-line generator at `kms-jwt-cli/src/main/java/ro/ghionoiu/kmsjwt/GenerateTokenApp.java`, and keeps CLI-specific fixtures in `kms-jwt-cli/src/test`. Shared Gradle logic (Java toolchain, publishing, application wiring) is defined in `build-logic/` scripts and applied by each module.

## Build, Test, and Development Commands
- `./gradlew clean build` — compile all modules, run tests, and assemble jars.
- `./gradlew :kms-jwt:test` — execute the library test suite when iterating on core code.
- `./gradlew :kms-jwt-cli:shadowJar` — generate the shaded CLI jar in `kms-jwt-cli/build/libs/`.
- `./gradlew :kms-jwt-cli:run --args="--region eu-west-2 --key <arn> --username demo --journey SUM"` — try the CLI without packaging.
- `./gradlew publishToMavenLocal` — stage artifacts locally before a release cut.

## Coding Style & Naming Conventions
The build targets Java 21 (see `build-logic/shared.gradle`) and follows four-space indentation with braces on the same line. Use UpperCamelCase for classes, camelCase for methods/fields, and UPPER_SNAKE_CASE for constants. Prefer `final` for dependencies and favour SLF4J parameterized logging. No formatter plugin is enforced; format via your IDE and run `./gradlew check` before pushing.

## Testing Guidelines
JUnit 5 drives the tests, which use the `*Test` suffix. Integration tests expect a KMS endpoint; start Localstack when exercising them:
```sh
docker run -d --rm --name localstack -p 4566:4566 -e SERVICES=kms -e DEBUG=1 localstack/localstack:4.8.1
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=eu-west-2
```
Create a test key via the AWS CLI, then run `./gradlew test -i`. Stop the container afterwards with `docker stop localstack`.

## Commit & Pull Request Guidelines
Recent history uses `type: summary` commit messages (e.g. `feat: move all tests to localstack`); follow that form, keep commits focused, and reference issues when available. Pull requests should describe behaviour changes, list test evidence, and mention any manual AWS/localstack setup required for reviewers.

## Security & Configuration Tips
Do not commit AWS credentials or KMS ARNs; keep secrets in environment variables or `~/.gradle/gradle.properties`. GPG signing keys and Maven Central credentials are read from your Gradle user home when releasing. Review generated artifacts (`kms-jwt/build/libs/`, `kms-jwt-cli/build/libs/`) before publishing to ensure no sensitive data is bundled.
