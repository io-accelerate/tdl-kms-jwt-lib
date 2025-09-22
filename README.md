
A Java library to sign and verify JSON Web Tokens (JWT) using Amazon Key Management Service (KMS)

[![Maven Central](https://img.shields.io/maven-central/v/io.accelerate/kms-jwt)](https://central.sonatype.com/artifact/io.accelerate/kms-jwt)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](build-logic/shared.gradle)
Inspired by **codahale/kmssig** https://github.com/codahale/kmssig

Token generation:
* You construct a JWT with your claims
* The JWT is signed with the **HS256** algorithm using a newly generated symmetric key
* The symmetric key is encrypted using **KMS** and shared with the client as a JWT Header parameter (`kid`)

Token validation:
* The JWT header is read and the encrypted key is extracted from the `kid` parameter
* A call is made to **KMS** to decrypt the encrypted key
* The decrypted key is then used to validate the JWT signature

More info about JWT: https://jwt.io/  
More info about KMS: https://aws.amazon.com/documentation/kms/

## To use as a library

### Add as Maven dependency

Add a dependency to `io.accelerate:kms-jwt` in `compile` scope.
```xml
<dependency>
  <groupId>io.accelerate</groupId>
  <artifactId>kms-jwt</artifactId>
  <version>X.Y.Z</version>
</dependency>
```

### Configure AWS user and KMS key

To run this you need:
* KMS key: http://docs.aws.amazon.com/kms/latest/developerguide/getting-started.html
* IAM user with Encrypt permissions
* IAM user with Decrypt permissions

For more info on IAM policies go to: http://docs.aws.amazon.com/kms/latest/developerguide/iam-policies.html#aws-managed-policies

### To generate token

```java
    try (KmsClient kmsClient = KmsClient.builder()
            .region(Region.of(region))
            .build()) {
        KMSEncrypt kmsEncrypt = new KMSEncrypt(kmsClient, keyARN);

        String jwt = JWTEncoder.builder(kmsEncrypt)
                .claim("usr", username)
                .claim("jrn", journey)
                .compact();
        System.out.println(jwt);
    }
```

### To validate token

```java
    try (KmsClient kmsClient = KmsClient.builder()
            .region(Region.of(region))
            .build()) {
        KMSDecrypt kmsDecrypt = new KMSDecrypt(kmsClient, Collections.singleton(keyARN));

        Claims claims = new JWTDecoder(kmsDecrypt).decodeAndVerify(jwt);
        System.out.println(claims.get("usr"));
    }
```

## Development

Might need Java Cryptography Extension?
https://cwiki.apache.org/confluence/display/STONEHENGE/Installing+Java+Cryptography+Extension+%28JCE%29+Unlimited+Strength+Jurisdiction+Policy+Files+6


### Run tests

Start Localstack as a container
```shell
docker run -d --rm \
  --name localstack \
  -p 4566:4566 \
  -e SERVICES=kms \
  -e DEBUG=1 \
  localstack/localstack:4.8.1
```

The emulated KMS instance exposed by Localstack can be accessed via the normal AWS client
```shell
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=eu-west-2

# Create a test key
aws --endpoint-url=http://localhost:4566 --region eu-west-2 \
    kms create-key \
    --description "LocalStack test key" \
    --key-usage ENCRYPT_DECRYPT \
    --origin AWS_KMS
    
# List keys
aws --endpoint-url=http://localhost:4566 \
    --region eu-west-2 \
    kms list-keys
```

Run the local tests
```
./gradlew test -i
```

### Build and run as command-line app

```bash
./gradlew :kms-jwt-cli:shadowJar
java -Dlogback.configurationFile=`pwd`/logback.xml  \
    -jar ./kms-jwt-cli/build/libs/kms-jwt-cli-0.0.5-all.jar \
    --region eu-west-2 \
    --key arn:aws:kms:eu-west-2:577770582757:key/7298331e-c199-4e15-9138-906d1c3d9363 \
    --username testuser \
    --journey "SUM,UPR"
```


### Problems and solutions

On MAC, if Encoder spends around 5 seconds initialising, have a look at this:
https://stackoverflow.com/questions/25321187/java-mac-getinstance-for-hmacsha1-slow



### Release

Configure the version inside the "gradle.properties" file

Create publishing bundle into Maven Local
```bash
./gradlew publishToMavenLocal
```

Check Maven Local contains release version:
```
CURRENT_VERSION=$(cat gradle.properties | grep version | cut -d "=" -f2)

ls -l $HOME/.m2/repository/io/accelerate/kms-jwt/${CURRENT_VERSION}
```

Publish to Maven Central Staging repo

### Publish to Maven Central - the manual way

At this point publishing to Maven Central from Gradle is only possible manually.
Things might have changed, check this page:
https://central.sonatype.org/publish/publish-portal-gradle/

Generate the Maven Central bundle:
```
./generateMavenCentralBundle.sh
```

Upload the bundle to Maven Central by clicking the "Publish Component" button.
https://central.sonatype.com/publishing

### To build artifacts in Github

Commit all changes then:
```bash
export RELEASE_TAG="v$(cat gradle.properties | cut -d= -f2)"
git tag -a "${RELEASE_TAG}" -m "${RELEASE_TAG}"
git push --tags
git push
```