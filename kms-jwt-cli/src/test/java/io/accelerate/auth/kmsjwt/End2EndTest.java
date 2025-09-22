package io.accelerate.auth.kmsjwt;

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
import software.amazon.awssdk.services.kms.model.CreateAliasRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class End2EndTest {
    public static final String TEST_AWS_ENDPOINT = "http://localhost:4566";
    private static final String TEST_AWS_REGION = "eu-west-2"; // LocalStack default
    public static final String TEST_ACCESS_KEY_ID = "test";
    public static final String TEST_SECRET_KEY = "test";

    private static final String ALIAS_NAME = "alias/local-test-key";

    private static String TEST_AWS_KEY_ARN;
    private static KmsClient KMS_CLIENT;

    @BeforeAll
    static void setUp() {
        // Point KMS client at LocalStack
        KMS_CLIENT = KmsClient.builder()
                .endpointOverride(URI.create(TEST_AWS_ENDPOINT))
                .region(Region.of(TEST_AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(TEST_ACCESS_KEY_ID, TEST_SECRET_KEY)))
                .build();

        // Check if alias already exists
        ListAliasesResponse aliases = KMS_CLIENT.listAliases();
        var existing = aliases.aliases().stream()
                .filter(a -> ALIAS_NAME.equals(a.aliasName()))
                .findFirst();

        if (existing.isPresent()) {
            // Reuse existing key
            TEST_AWS_KEY_ARN = existing.get().aliasArn();
        } else {
            // Create a new key
            CreateKeyResponse createKeyResponse = KMS_CLIENT.createKey();
            TEST_AWS_KEY_ARN = createKeyResponse.keyMetadata().arn();

            // Create alias for convenience
            KMS_CLIENT.createAlias(CreateAliasRequest.builder()
                    .aliasName(ALIAS_NAME)
                    .targetKeyId(createKeyResponse.keyMetadata().keyId())
                    .build());
        }
    }

    @AfterAll
    static void tearDown() {
        if (KMS_CLIENT != null) {
            KMS_CLIENT.close();
        }
    }

    @Test
    void sign_token_with_KMS_and_verify() throws Exception {
        // Capture System.out
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            String[] params = {
                    "--region", TEST_AWS_REGION,
                    "--key", TEST_AWS_KEY_ARN,
                    "--endpoint", TEST_AWS_ENDPOINT,
                    "--username", "userXYZ",
                    "--journey", "SUM,UPR",
            };
            GenerateTokenApp.main(params);

            String jwtToken = getTokenFromStdout(outContent.toString());
            System.out.println("jwt: " + jwtToken);

            KMSDecrypt kmsDecrypt = new KMSDecrypt(KMS_CLIENT, Collections.singleton(TEST_AWS_KEY_ARN));
            Claims claims = new JWTDecoder(kmsDecrypt).decodeAndVerify(jwtToken);
            assertThat(claims.get("usr"), is("userXYZ"));
        } finally {
            // Restore System.out
            System.setOut(originalOut);
        }
    }

    private String getTokenFromStdout(String log) {
        return Arrays.stream(log.split("\n"))
                .filter(s -> s.contains("JWT_TOKEN"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Token not found"))
                .split("=")[1].trim();
    }
}
