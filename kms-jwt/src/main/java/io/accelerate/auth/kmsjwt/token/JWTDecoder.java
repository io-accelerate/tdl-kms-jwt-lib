package io.accelerate.auth.kmsjwt.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.security.Keys;
import io.accelerate.auth.kmsjwt.key.KeyDecrypt;
import io.accelerate.auth.kmsjwt.key.KeyOperationException;

import java.security.Key;
import java.util.Base64;

public class JWTDecoder {
    private final JwtParser jwtParser;

    public JWTDecoder(KeyDecrypt keyDecrypt) {
        this.jwtParser = Jwts.parser()
                .clockSkewSeconds(60)
                .keyLocator(new DecryptKeyViaKid(keyDecrypt))
                .build();
    }

    public Claims decodeAndVerify(String jwt) throws JWTVerificationException {
        if (jwt == null || jwt.isBlank()) {
            throw new JWTVerificationException("JWT value is empty", null);
        }
        try {
            return jwtParser.parseSignedClaims(jwt).getPayload();
        } catch (IllegalArgumentException e) {
            throw new JWTVerificationException(e.getMessage(), e);
        } catch (JwtException e) {
            throw new JWTVerificationException("Unable to read JSON Web Token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new JWTVerificationException(e.getMessage(), e);
        }
    }

    private static final class DecryptKeyViaKid implements Locator<Key> {
        private final KeyDecrypt keyDecrypt;

        DecryptKeyViaKid(KeyDecrypt keyDecrypt) {
            this.keyDecrypt = keyDecrypt;
        }

        @Override
        public Key locate(Header header) {
            Object kidObj = header.get("kid");
            if (!(kidObj instanceof String kid) || kid.isEmpty()) {
                throw new IllegalArgumentException("No key ID has been found in the JWT header");
            }
            try {
                byte[] decrypted = keyDecrypt.decrypt(Base64.getDecoder().decode(kid));
                return Keys.hmacShaKeyFor(decrypted);
            } catch (KeyOperationException e) {
                throw new IllegalArgumentException("Key decryption failed", e);
            }
        }
    }
}
