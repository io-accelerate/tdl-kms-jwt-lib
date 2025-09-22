package ro.ghionoiu.kmsjwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ro.ghionoiu.kmsjwt.key.KMSDecrypt;
import ro.ghionoiu.kmsjwt.token.JWTDecoder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class End2EndTest {
    private static final String TEST_AWS_REGION = Optional.ofNullable(System.getenv("TEST_AWS_REGION"))
            .orElse("eu-west-2");

    private static final String TEST_AWS_KEY_ARN = Optional.ofNullable(System.getenv("TEST_AWS_KEY_ARN"))
            .orElse("arn:aws:kms:eu-west-2:577770582757:key/7298331e-c199-4e15-9138-906d1c3d9363");

    private static KmsClient KMS_CLIENT;

    @BeforeAll
    static void setUp() {
        KMS_CLIENT = KmsClient.builder()
                .region(Region.of(TEST_AWS_REGION))
                .build();
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
