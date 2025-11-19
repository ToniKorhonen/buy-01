package service.product.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public String extractEmail(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length >= 3) {
                String payload = parts[0] + ":" + parts[1];
                String signature = parts[2];
                String expectedSignature = hmacSha256(payload, secret);
                if (signature.equals(expectedSignature)) {
                    // Validate expiration
                    long expirationTimestamp = Long.parseLong(parts[1]);
                    if (System.currentTimeMillis() > expirationTimestamp) {
                        return null; // Token expired
                    }
                    return parts[0]; // email is the first part
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error validating token", e);
        }
    }
}

