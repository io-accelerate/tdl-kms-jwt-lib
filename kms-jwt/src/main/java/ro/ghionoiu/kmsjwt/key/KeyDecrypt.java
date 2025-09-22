package ro.ghionoiu.kmsjwt.key;

public interface KeyDecrypt {
    byte[] decrypt(byte[] ciphertext) throws KeyOperationException;
}
