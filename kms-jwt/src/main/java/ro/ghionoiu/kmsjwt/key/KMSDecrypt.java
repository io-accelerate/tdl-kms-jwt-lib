package ro.ghionoiu.kmsjwt.key;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import java.util.Set;

public class KMSDecrypt implements KeyDecrypt {
    private final KmsClient kmsClient;
    private final Set<String> supportedKeyARNs;

    public KMSDecrypt(KmsClient kmsClient, Set<String> supportedKeyARNs) {
        this.kmsClient = kmsClient;
        this.supportedKeyARNs = supportedKeyARNs;
    }

    public byte[] decrypt(byte[] ciphertext) throws KeyOperationException {
        DecryptRequest req = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertext))
                .build();

        DecryptResponse decrypt;
        try {
            decrypt = kmsClient.decrypt(req);
        } catch (SdkException e) {
            throw new KeyOperationException(e.getMessage(), e);
        }

        String keyId = decrypt.keyId();
        if (!supportedKeyARNs.contains(keyId)){
            throw new KeyOperationException("Ciphertext signed by unexpected key");
        }

        return decrypt.plaintext().asByteArray();
    }

}
