package io.accelerate.auth.kmsjwt.key;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

public class KMSEncrypt implements KeyEncrypt {
    private final KmsClient kmsClient;
    private final String keyARN;

    public KMSEncrypt(KmsClient kmsClient, String keyARN) {
        this.kmsClient = kmsClient;
        this.keyARN = keyARN;
    }

    public byte[] encrypt(byte[] plaintext) throws KeyOperationException {
        EncryptRequest req = EncryptRequest.builder()
                .keyId(keyARN)
                .plaintext(SdkBytes.fromByteArray(plaintext))
                .build();

        EncryptResponse encrypt;
        try {
            encrypt = kmsClient.encrypt(req);
        } catch (SdkException e) {
            throw new KeyOperationException(e.getMessage(), e);
        }

        return encrypt.ciphertextBlob().asByteArray();
    }
}
