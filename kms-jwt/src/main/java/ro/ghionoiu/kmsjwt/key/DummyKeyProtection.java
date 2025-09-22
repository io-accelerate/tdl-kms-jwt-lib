package ro.ghionoiu.kmsjwt.key;

import java.util.Arrays;

public class DummyKeyProtection implements KeyDecrypt, KeyEncrypt {
    @Override
    public byte[] encrypt(byte[] plaintext) {
        byte[] encryptedBuffer = Arrays.copyOf(plaintext, plaintext.length + 1);
        encryptedBuffer[plaintext.length] = 'X';
        return encryptedBuffer;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return Arrays.copyOf(ciphertext, ciphertext.length - 1);
    }
}
