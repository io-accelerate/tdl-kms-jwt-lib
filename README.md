[![Java Version](http://img.shields.io/badge/Java-1.8-blue.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
[![Download](https://api.bintray.com/packages/julianghionoiu/maven/kms-jwt/images/download.svg)](https://bintray.com/julianghionoiu/maven/kms-jwt/_latestVersion)
[![Codeship Status for julianghionoiu/kms-jwt](https://img.shields.io/codeship/5a667980-2af8-0135-70bf-3ade48bf5979/master.svg)](https://codeship.com/projects/224001)
[![Coverage Status](https://coveralls.io/repos/github/julianghionoiu/kms-jwt/badge.svg?branch=master)](https://coveralls.io/github/julianghionoiu/kms-jwt?branch=master)

A Java library to sign and verify JSON Web Tokens (JWT) using Amazon Key Management Service (KMS)
Inspired by **codahale/kmssig** https://github.com/codahale/kmssig

Token generation:
* You construct a JWT with your claims
* The JWT is signed with the **HS256** algorithm using a newly generated symmetric key  
* The symmetric is encrypted using **KMS** and shared with the client as a JWT Header parameter (`kid`)

Token validation:
* The header file is read and the encrypted key is extracted from the `kid` parameter
* A call is made to **KMS** to decrypt the encrypted key
* The decrypted key is then used to validate the JWT signature

More info about JWT: https://jwt.io/  
More info about KMS: https://aws.amazon.com/documentation/kms/

## To use as a library

### Add as Maven dependency

Add a dependency to `ro.ghionoiu:kms-jwt` in `compile` scope. See `bintray` shield for latest release number.
```xml
<dependency>
  <groupId>ro.ghionoiu</groupId>
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

### Build and run as command-line app
```bash
./gradlew shadowJar
java -Dlogback.configurationFile=`pwd`/logback.xml  \
    -jar ./build/libs/kms-jwt-0.0.3-SNAPSHOT-all.jar \
    --region eu-west-2 \
    --key arn:aws:kms:eu-west-2:577770582757:key/7298331e-c199-4e15-9138-906d1c3d9363 \
    --username testuser \
    --journey "SUM,UPR"
```


### Problems and solutions

On MAC, if Encoder spends around 5 seconds initialising, have a look at this:
https://stackoverflow.com/questions/25321187/java-mac-getinstance-for-hmacsha1-slow
