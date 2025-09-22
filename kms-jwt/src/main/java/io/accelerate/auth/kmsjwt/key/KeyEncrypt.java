package io.accelerate.auth.kmsjwt.key;

public interface KeyEncrypt {
    byte[] encrypt(byte[] plaintext) throws KeyOperationException;
}
