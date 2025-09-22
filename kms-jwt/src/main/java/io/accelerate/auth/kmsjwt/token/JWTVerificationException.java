package io.accelerate.auth.kmsjwt.token;

public class JWTVerificationException extends Exception {
    JWTVerificationException(String message, Exception e) {
        super(message, e);
    }
}
