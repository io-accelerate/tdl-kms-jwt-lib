package ro.ghionoiu.kmsjwt.key;

public interface KeyEncrypt {
    byte[] encrypt(byte[] plaintext) throws KeyOperationException;
}
