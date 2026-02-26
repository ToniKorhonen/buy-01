package service.order.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import service.order.exceptions.JwtProcessingException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    public String extractUserId(String token) {
        return extractPart(token, 0);
    }

    public String extractRole(String token) {
        return extractPart(token, 2);
    }

    private String extractPart(String token, int index) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length >= 5) {
                String payload = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
                String signature = parts[4];
                String expectedSignature = hmacSha256(payload, secret);
                if (signature.equals(expectedSignature)) {
                    long expirationTimestamp = Long.parseLong(parts[3]);
                    if (System.currentTimeMillis() > expirationTimestamp) {
                        return null; // expired
                    }
                    return parts[index];
                }
            }
            return null;
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException ignored) {
            // Malformed token — treat as invalid (NumberFormatException is a subclass of IllegalArgumentException)
            return null;
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new JwtProcessingException("Failed to compute HMAC", e);
        }
    }
}

