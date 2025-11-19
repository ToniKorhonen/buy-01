# JWT Secret Externalization - Security Guide

## Overview

The JWT secret has been externalized from the codebase to enhance security. **Never commit the `.env` file to version control.**

## Quick Start

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Generate a secure JWT secret:**
   ```bash
   openssl rand -base64 64
   ```

3. **Update `.env` file with your generated secret:**
   ```bash
   nano .env
   # Replace JWT_SECRET value with the generated secret
   ```

4. **Start services with environment variables:**
   ```bash
   ./start-services.sh
   ```

## Security Best Practices

### ✅ DO:
- Use the provided `.env` file for local development
- Generate a new, unique JWT secret for each environment (dev, staging, production)
- Use at least 256 bits (32 bytes) for JWT secrets - we use 512 bits (64 bytes)
- Store production secrets in secure secret management systems (HashiCorp Vault, AWS Secrets Manager, etc.)
- Rotate JWT secrets periodically (every 90 days recommended)
- Keep `.env` file permissions restricted: `chmod 600 .env`

### ❌ DON'T:
- Never commit `.env` to version control (already in `.gitignore`)
- Never use default or example secrets in production
- Never share JWT secrets via email, chat, or insecure channels
- Never log JWT secrets in application logs
- Never use simple/predictable secrets

## Environment Variables

### Required Variables

- **JWT_SECRET**: The secret key used to sign JWT tokens
  - Must be set (no default value)
  - Minimum 256 bits recommended, we use 512 bits
  - Generated using: `openssl rand -base64 64`

- **JWT_EXPIRATION**: Token expiration time in milliseconds
  - Default: 3600000 (1 hour)
  - Adjust based on your security requirements

### Optional Variables (for future use)

- **MONGODB_HOST**: MongoDB server host (default: localhost)
- **MONGODB_PORT**: MongoDB server port (default: 27017)
- **MONGODB_USERNAME**: MongoDB authentication username
- **MONGODB_PASSWORD**: MongoDB authentication password

## Service Configuration

The following services use JWT authentication:

1. **user-service** (port 8081)
   - Generates JWT tokens on login
   - Configuration: `Backend/user-service/src/main/resources/application.properties`

2. **product-service** (port 8082)
   - Validates JWT tokens for protected endpoints
   - Configuration: `Backend/product-service/src/main/resources/application.properties`

Both services **require** the `JWT_SECRET` environment variable to be set. The application will fail to start if it's missing.

## Production Deployment

### Docker
```dockerfile
# In your Dockerfile or docker-compose.yml
environment:
  - JWT_SECRET=${JWT_SECRET}
  - JWT_EXPIRATION=3600000
```

### Kubernetes
```yaml
# Use Kubernetes Secrets
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
type: Opaque
data:
  JWT_SECRET: <base64-encoded-secret>
```

### Systemd Service
```ini
[Service]
EnvironmentFile=/etc/buy01/secrets.env
ExecStart=/path/to/application
```

### Cloud Platforms

**AWS:**
- Use AWS Secrets Manager or Parameter Store
- Reference in ECS task definitions or Lambda environment variables

**Google Cloud:**
- Use Secret Manager
- Reference in Cloud Run or GKE deployments

**Azure:**
- Use Key Vault
- Reference in App Service configuration

## Verification

Test that the JWT secret is properly loaded:

```bash
# Run the start script
./start-services.sh

# You should see:
# Loading environment variables from .env file...
# ✓ Environment variables loaded
# JWT_SECRET: [REDACTED - 88 characters]
```

## Troubleshooting

### Application fails to start with "JWT_SECRET" error
- Ensure `.env` file exists in the project root
- Verify `JWT_SECRET` is set in `.env`
- Check file permissions: `ls -la .env`

### "Invalid token" errors
- Ensure all services use the **same** JWT_SECRET
- Verify the secret hasn't been modified
- Check token expiration time

### Environment variables not loading
- Ensure you're using `./start-services.sh` to start services
- Or manually export: `export $(cat .env | xargs)`
- For Maven: Add to your IDE run configuration

## Security Incident Response

If you suspect the JWT secret has been compromised:

1. **Immediately** generate a new secret
2. Update the secret in all environments
3. Restart all services
4. Invalidate all existing tokens (users must re-login)
5. Audit logs for suspicious activity
6. Review access controls and secret management procedures

## Additional Resources

- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [JWT.io](https://jwt.io/) - JWT Debugger
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)

---

**Last Updated:** 2025-11-19  
**Security Level:** HIGH - Treat JWT secrets as sensitive credentials

