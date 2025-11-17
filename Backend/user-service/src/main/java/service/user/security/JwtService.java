package service.user.security;


import service.user.models.User;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class JwtService {
    private static final String SECRET = "SuperSecretKeyForJwtGeneration123456!";

    public String generateToken(User user) {
        String payload = user.getEmail() + ":" + System.currentTimeMillis();
        String signature = hmacSha256(payload, SECRET);
        String token = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public String extractEmail(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length >= 3) {
                String payload = parts[0] + ":" + parts[1];
                String signature = parts[2];
                String expectedSignature = hmacSha256(payload, SECRET);
                if (signature.equals(expectedSignature)) {
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
            throw new RuntimeException("Error generating token", e);
        }
    }
}
