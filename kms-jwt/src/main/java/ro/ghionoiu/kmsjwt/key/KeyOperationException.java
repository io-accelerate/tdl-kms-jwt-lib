package ro.ghionoiu.kmsjwt.key;

public class KeyOperationException extends Exception {
    public KeyOperationException(String message) {
        super(message);
    }

    public KeyOperationException(String message, Exception e) {
        super(message, e);
    }
}
