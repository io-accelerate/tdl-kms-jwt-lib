package ro.ghionoiu.kmsjwt.key;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;

import java.nio.ByteBuffer;
import java.util.Set;

public class KMSDecrypt implements KeyDecrypt {
    private final AWSKMS kmsClient;
    private final Set<String> supportedKeyARNs;

    public KMSDecrypt(AWSKMS kmsClient, Set<String> supportedKeyARNs) {
        this.kmsClient = kmsClient;
        this.supportedKeyARNs = supportedKeyARNs;
    }

    public byte[] decrypt(byte[] ciphertext) throws KeyOperationException {
        DecryptRequest req = new DecryptRequest()
                .withCiphertextBlob(ByteBuffer.wrap(ciphertext));

        DecryptResult decrypt;
        try {
            decrypt = kmsClient.decrypt(req);
        } catch (Exception e) {
            throw new KeyOperationException(e.getMessage(), e);
        }

        String keyId = decrypt.getKeyId();
        if (!supportedKeyARNs.contains(keyId)){
            throw new KeyOperationException("Ciphertext signed by unexpected key");
        }

        return decrypt.getPlaintext().array();
    }

}
