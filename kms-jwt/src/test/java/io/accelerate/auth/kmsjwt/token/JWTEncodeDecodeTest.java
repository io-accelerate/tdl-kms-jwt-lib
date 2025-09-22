package io.accelerate.auth.kmsjwt.token;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.accelerate.auth.kmsjwt.key.DummyKeyProtection;
import io.accelerate.auth.kmsjwt.key.KeyDecrypt;
import io.accelerate.auth.kmsjwt.key.KeyOperationException;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * NOTE: Use https://jwt.io/ to obtain test tokens
 */
class JWTEncodeDecodeTest {
    private static final byte[] SECRET_AS_BYTE_ARRAY =
            "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER_SECRET_AS_BYTE_ARRAY =
            "FEDCBA9876543210FEDCBA9876543210".getBytes(StandardCharsets.UTF_8);
    private static final DummyKeyProtection DUMMY_KEY_PROTECTION = new DummyKeyProtection();

    private JWTDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        jwtDecoder = new JWTDecoder(DUMMY_KEY_PROTECTION);
    }

    @Test
    void encode_and_decode_work_together() throws Exception {
        String jwt = JWTEncoder.builder(DUMMY_KEY_PROTECTION)
                .claim("usr", "friendly_name")
                .compact();

        Claims claims = new JWTDecoder(DUMMY_KEY_PROTECTION).decodeAndVerify(jwt);

        assertThat(claims.get("usr"), is("friendly_name"));
    }

    @Test
    void decode_rejects_empty_jwt() {
        JWTVerificationException ex = assertThrows(
                JWTVerificationException.class,
                () -> jwtDecoder.decodeAndVerify("")
        );
        assertThat(ex.getMessage(), containsString("empty"));
    }

    @Test
    void decode_rejects_invalid_jwt() {
        JWTVerificationException ex = assertThrows(
                JWTVerificationException.class,
                () -> jwtDecoder.decodeAndVerify("X.Y.X")
        );
        assertThat(ex.getMessage(), containsString("Unable to read"));
    }

    @Test
    void decode_rejects_valid_token_without_key_id() {
        String validTokenWithoutKeyId = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                                        +".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9"
                                        +".TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

        JWTVerificationException ex = assertThrows(
                JWTVerificationException.class,
                () -> jwtDecoder.decodeAndVerify(validTokenWithoutKeyId)
        );
        assertThat(ex.getMessage(), containsString("No key ID"));
    }

    @Test
    void decode_rejects_if_key_cannot_be_decoded() throws Exception {
        String tokenWithKeyId = "eyJhbGciOiJIUzI1NiIsImtpZCI6Ik1ERXlNelExTmpjNE9VRkNRMFJGUmpBeE1qTTBOVFkzT0RsQlFrTkVSVVk9In0" +
                                 ".eyJ1c3IiOiJmcmllbmRseV9uYW1lIn0" +
                                 ".wvWzfuRlAa1nKgOsagwq7at-U6zsrMzzdHfxJiV4d_c";
        KeyDecrypt keyDecrypt = mock(KeyDecrypt.class);
        when(keyDecrypt.decrypt(any())).thenThrow(new KeyOperationException("X"));
        jwtDecoder = new JWTDecoder(keyDecrypt);

        JWTVerificationException ex = assertThrows(
                JWTVerificationException.class,
                () -> jwtDecoder.decodeAndVerify(tokenWithKeyId)
        );
        assertThat(ex.getMessage(), containsString("Key decryption failed"));
    }

    @Test
    void decode_uses_key_id_to_obtain_key() throws Exception {
        String validTokenWithBase64KeyIdSecret = "eyJhbGciOiJIUzI1NiIsImtpZCI6Ik1ERXlNelExTmpjNE9VRkNRMFJGUmpBeE1qTTBOVFkzT0RsQlFrTkVSVVk9In0" +
                                                 ".eyJ1c3IiOiJmcmllbmRseV9uYW1lIn0" +
                                                 ".wvWzfuRlAa1nKgOsagwq7at-U6zsrMzzdHfxJiV4d_c";
        KeyDecrypt keyDecrypt = mock(KeyDecrypt.class);
        when(keyDecrypt.decrypt(any())).thenReturn(SECRET_AS_BYTE_ARRAY);
        jwtDecoder = new JWTDecoder(keyDecrypt);

        try {
            jwtDecoder.decodeAndVerify(validTokenWithBase64KeyIdSecret);
        } catch (JWTVerificationException ignored) {}

        verify(keyDecrypt).decrypt(SECRET_AS_BYTE_ARRAY);
    }

    @Test
    void decode_rejects_if_expiration_date_in_the_past() {
        String validKeySignedBySecretWithDateInThePast = "eyJhbGciOiJIUzI1NiIsImtpZCI6Ik1ERXlNelExTmpjNE9VRkNRMFJGUmpBeE1qTTBOVFkzT0RsQlFrTkVSVVk9In0" +
                                                         ".eyJleHAiOjAsInVzciI6ImZyaWVuZGx5X25hbWUifQ" +
                                                         ".nXiyXh-8P23T_5e4Pf5LwyG4QX4-DatK95D3fEQqc-I";
        jwtDecoder = new JWTDecoder(ciphertext -> SECRET_AS_BYTE_ARRAY);

        JWTVerificationException ex = assertThrows(
                JWTVerificationException.class,
                () -> jwtDecoder.decodeAndVerify(validKeySignedBySecretWithDateInThePast)
        );
        assertThat(ex.getMessage(), containsString("expired"));
    }

    @Test
    void decode_rejects_if_keys_do_not_match() {
        String validKeySignedBySecret = "eyJhbGciOiJIUzI1NiIsImtpZCI6Ik1ERXlNelExTmpjNE9VRkNRMFJGUmpBeE1qTTBOVFkzT0RsQlFrTkVSVVk9In0" +
                                        ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9" +
                                        ".IzAFrMM0MbFJB9a35yQcp-jLSk7pBJP036CK5C144cI";
        jwtDecoder = new JWTDecoder(ciphertext -> OTHER_SECRET_AS_BYTE_ARRAY);

        JWTVerificationException ex = assertThrows(
                JWTVerificationException.class,
                () -> jwtDecoder.decodeAndVerify(validKeySignedBySecret)
        );
        assertThat(ex.getMessage(), containsString("should not be trusted"));
    }

    @Test
    void decode_accepts_valid_key() throws Exception {
        String validKeySignedBySecret = "eyJhbGciOiJIUzI1NiIsImtpZCI6Ik1ERXlNelExTmpjNE9VRkNRMFJGUmpBeE1qTTBOVFkzT0RsQlFrTkVSVVk9In0" +
                                        ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9" +
                                        ".IzAFrMM0MbFJB9a35yQcp-jLSk7pBJP036CK5C144cI";
        jwtDecoder = new JWTDecoder(ciphertext -> SECRET_AS_BYTE_ARRAY);

        jwtDecoder.decodeAndVerify(validKeySignedBySecret);
    }
}
