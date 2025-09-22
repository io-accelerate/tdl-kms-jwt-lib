package ro.ghionoiu.kmsjwt.key;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;

import java.nio.ByteBuffer;

public class KMSEncrypt implements KeyEncrypt {
    private final AWSKMS kmsClient;
    private final String keyARN;

    public KMSEncrypt(AWSKMS kmsClient, String keyARN) {
        this.kmsClient = kmsClient;
        this.keyARN = keyARN;
    }

    public byte[] encrypt(byte[] plaintext) throws KeyOperationException {
        EncryptRequest req = new EncryptRequest().withKeyId(keyARN)
                .withPlaintext(ByteBuffer.wrap(plaintext));

        EncryptResult encrypt;
        try {
            encrypt = kmsClient.encrypt(req);
        } catch (Exception e) {
            throw new KeyOperationException(e.getMessage(), e);
        }

        return encrypt.getCiphertextBlob().array();
    }
}
