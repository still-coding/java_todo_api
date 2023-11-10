package utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "my-very-secret-key";

    private static final int TOKEN_EXPIRATION_MINUTES = 60;

    public static String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRATION_MINUTES * 60000);
        return JWT.create()
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    public static String verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET_KEY)).build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException exception) {
            System.out.println(exception);
            return null;
        }
    }
}