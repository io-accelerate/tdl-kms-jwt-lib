package ro.ghionoiu.kmsjwt.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.security.MacAlgorithm;
import ro.ghionoiu.kmsjwt.key.KeyEncrypt;
import ro.ghionoiu.kmsjwt.key.KeyOperationException;

import javax.crypto.SecretKey;
import java.util.Base64;

public final class JWTEncoder {

    private JWTEncoder() { }

    public static JwtBuilder builder(KeyEncrypt keyEncrypt) throws KeyOperationException {
        MacAlgorithm alg = Jwts.SIG.HS256; // new algorithm API
        SecretKey secretKey = alg.key().build(); // generate a suitable HMAC key

        byte[] encryptedKey = keyEncrypt.encrypt(secretKey.getEncoded());

        return Jwts.builder()
                .header().add("kid", Base64.getEncoder().encodeToString(encryptedKey)).and()
                .signWith(secretKey, alg);
    }
}
