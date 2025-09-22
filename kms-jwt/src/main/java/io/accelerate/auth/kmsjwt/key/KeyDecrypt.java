package io.accelerate.auth.kmsjwt.key;

public interface KeyDecrypt {
    byte[] decrypt(byte[] ciphertext) throws KeyOperationException;
}
