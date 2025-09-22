package ro.ghionoiu.kmsjwt.key;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KMSEncryptDecryptTest {
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
    void encrypt_decrypt_work_together() throws Exception {
        KMSEncrypt kmsEncrypt = new KMSEncrypt(KMS_CLIENT, TEST_AWS_KEY_ARN);
        KMSDecrypt kmsDecrypt = new KMSDecrypt(KMS_CLIENT, Collections.singleton(TEST_AWS_KEY_ARN));
        String originalCleartext = "secret";

        String base64CipherText = Base64.getEncoder()
                .encodeToString(kmsEncrypt.encrypt(originalCleartext.getBytes()));
        System.out.println("ciphertext: " + base64CipherText);

        String plaintext = new String(
                kmsDecrypt.decrypt(Base64.getDecoder().decode(base64CipherText))
        );
        System.out.println("plaintext: " + plaintext);

        assertThat(plaintext, is(originalCleartext));
    }

    @Test
    void decrypt_should_reject_ciphertext_with_unrecognised_key() throws Exception {
        byte[] ciphertext = new KMSEncrypt(KMS_CLIENT, TEST_AWS_KEY_ARN).encrypt("secret".getBytes());
        KMSDecrypt kmsDecrypt = new KMSDecrypt(KMS_CLIENT, Collections.singleton("SOME_OTHER_KEY"));

        KeyOperationException ex = assertThrows(
                KeyOperationException.class,
                () -> kmsDecrypt.decrypt(ciphertext)
        );
        assertThat(ex.getMessage(), containsString("signed by unexpected key"));
    }

    @Test
    void decrypt_should_reject_ciphertext_if_KMS_returns_exception() {
        KMSDecrypt kmsDecrypt = new KMSDecrypt(KMS_CLIENT, Collections.singleton(TEST_AWS_KEY_ARN));

        KeyOperationException ex = assertThrows(
                KeyOperationException.class,
                () -> kmsDecrypt.decrypt(new byte[0])
        );
        assertThat(ex.getMessage(), containsString("validation error"));
    }

    @Test
    void encrypt_should_stop_encryption_if_KMS_returns_exception() {
        KMSEncrypt kmsEncrypt = new KMSEncrypt(KMS_CLIENT, "SOME_OTHER_KEY");

        KeyOperationException ex = assertThrows(
                KeyOperationException.class,
                () -> kmsEncrypt.encrypt("secret".getBytes())
        );
        assertThat(ex.getMessage(), containsString("Invalid keyId"));
    }
}
