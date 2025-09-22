package io.accelerate.auth.kmsjwt.key;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class KMSDecrypt implements KeyDecrypt {
    private final KmsClient kmsClient;
    private final Set<String> providedKeyIdentifiers;
    private final Set<String> resolvedKeyArns;

    public KMSDecrypt(KmsClient kmsClient, Set<String> supportedKeyARNs) {
        this.kmsClient = kmsClient;
        this.providedKeyIdentifiers = Set.copyOf(supportedKeyARNs);
        this.resolvedKeyArns = resolveKeyArns(this.providedKeyIdentifiers);
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
        if (!providedKeyIdentifiers.contains(keyId) && !resolvedKeyArns.contains(keyId)){
            throw new KeyOperationException("Ciphertext signed by unexpected key");
        }

        return decrypt.plaintext().asByteArray();
    }

    private Set<String> resolveKeyArns(Set<String> keyIdentifiers) {
        Set<String> resolved = new HashSet<>();
        for (String keyIdentifier : keyIdentifiers) {
            resolved.add(resolveKeyArn(keyIdentifier));
        }
        return Collections.unmodifiableSet(resolved);
    }

    private String resolveKeyArn(String keyIdentifier) {
        try {
            return kmsClient.describeKey(DescribeKeyRequest.builder().keyId(keyIdentifier).build())
                    .keyMetadata()
                    .arn();
        } catch (SdkException e) {
            return keyIdentifier;
        }
    }

}
