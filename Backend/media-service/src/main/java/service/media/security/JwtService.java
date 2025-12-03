package service.media.security;

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
            if (parts.length >= 5) {
                String payload = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
                String signature = parts[4];
                String expectedSignature = hmacSha256(payload, secret);
                if (signature.equals(expectedSignature)) {
                    // Validate expiration
                    long expirationTimestamp = Long.parseLong(parts[3]);
                    if (System.currentTimeMillis() > expirationTimestamp) {
                        return null; // Token expired
                    }
                    return parts[1]; // email is the second part
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length >= 5) {
                String payload = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
                String signature = parts[4];
                String expectedSignature = hmacSha256(payload, secret);
                if (signature.equals(expectedSignature)) {
                    // Validate expiration
                    long expirationTimestamp = Long.parseLong(parts[3]);
                    if (System.currentTimeMillis() > expirationTimestamp) {
                        return null; // Token expired
                    }
                    return parts[2]; // role is the third part
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUserId(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length >= 5) {
                String payload = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
                String signature = parts[4];
                String expectedSignature = hmacSha256(payload, secret);
                if (signature.equals(expectedSignature)) {
                    // Validate expiration
                    long expirationTimestamp = Long.parseLong(parts[3]);
                    if (System.currentTimeMillis() > expirationTimestamp) {
                        return null; // Token expired
                    }
                    return parts[0]; // userId is the first part
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

