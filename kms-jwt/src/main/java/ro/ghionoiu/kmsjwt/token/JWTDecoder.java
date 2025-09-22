package ro.ghionoiu.kmsjwt.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.security.Keys;
import ro.ghionoiu.kmsjwt.key.KeyDecrypt;
import ro.ghionoiu.kmsjwt.key.KeyOperationException;

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
        try {
            return jwtParser.parseSignedClaims(jwt).getPayload();
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
                // Create HS256 verification key from raw bytes (non-deprecated API)
                return Keys.hmacShaKeyFor(decrypted);
            } catch (KeyOperationException e) {
                throw new IllegalArgumentException("Key decryption failed", e);
            }
        }
    }
}
