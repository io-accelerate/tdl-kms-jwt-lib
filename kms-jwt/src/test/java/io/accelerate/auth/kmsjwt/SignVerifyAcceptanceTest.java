package io.accelerate.auth.kmsjwt;

import io.accelerate.auth.kmsjwt.key.KMSEncrypt;
import io.accelerate.auth.kmsjwt.token.JWTEncoder;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.accelerate.auth.kmsjwt.key.KMSDecrypt;
import io.accelerate.auth.kmsjwt.token.JWTDecoder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SignVerifyAcceptanceTest {
    public static final String TEST_AWS_ENDPOINT = "http://localhost:4566";
    private static final String TEST_AWS_REGION = "eu-west-2"; // LocalStack default
    public static final String TEST_ACCESS_KEY_ID = "test";
    public static final String TEST_SECRET_KEY = "test";
    public static final Integer TEST_DAYS_TO_EXPIRE = 2;
    public static final String CLAIM_USR = "usernameX";
    public static final String CLAIM_JOURNEY = "SUM,HLO";

    private static final String ALIAS_NAME = "alias/local-test-key";

    private static String TEST_AWS_KEY_ARN;
    private static KmsClient KMS_CLIENT;

    @BeforeAll
    static void setUp() {
        // Build KMS client for LocalStack
        KMS_CLIENT = KmsClient.builder()
                .endpointOverride(URI.create(TEST_AWS_ENDPOINT))
                .region(Region.of(TEST_AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(TEST_ACCESS_KEY_ID, TEST_SECRET_KEY)))
                .build();

        // Create a new key
        CreateKeyResponse createKeyResponse = KMS_CLIENT.createKey();
        TEST_AWS_KEY_ARN = createKeyResponse.keyMetadata().arn();
        System.out.println("Created test key: " + TEST_AWS_KEY_ARN);
    }

    @AfterAll
    static void tearDown() {
        if (KMS_CLIENT != null) {
            KMS_CLIENT.close();
        }
    }

    @Test
    void sign_token_with_KMS_and_verify() throws Exception {
        KMSEncrypt kmsEncrypt = new KMSEncrypt(KMS_CLIENT, TEST_AWS_KEY_ARN);
        Date expiryDate = new Date(Instant.now().plus(TEST_DAYS_TO_EXPIRE, ChronoUnit.DAYS).toEpochMilli());
        String jwtToken = JWTEncoder.builder(kmsEncrypt)
                .expiration(expiryDate)
                .claim("usr", CLAIM_USR)
                .claim("jrn", CLAIM_JOURNEY
                )
                .compact();
        System.out.println("jwt: " + jwtToken);

        KMSDecrypt kmsDecrypt = new KMSDecrypt(KMS_CLIENT, Collections.singleton(TEST_AWS_KEY_ARN));
        Claims claims = new JWTDecoder(kmsDecrypt).decodeAndVerify(jwtToken);
        assertThat(claims.get("usr"), is(CLAIM_USR));
    }
}
