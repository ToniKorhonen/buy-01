# SSL Certificate Quick Guide - Development HTTPS

## Quick Start (TL;DR)

Generate a self-signed certificate for local development:

```bash
cd Frontend
mkdir -p certs
cd certs
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/C=US/ST=State/L=City/O=YourOrg/CN=localhost"
```

**Done!** Your certificate is ready. Restart the frontend server and access: `https://localhost:4443`

---

## Step-by-Step Guide

### Prerequisites

- OpenSSL installed (check with: `openssl version`)
- Terminal/command line access

### Step 1: Navigate to Frontend Directory

```bash
cd /home/student/ZONE01/JAVA/buy-01/Frontend
```

### Step 2: Create Certs Directory

```bash
mkdir -p certs
cd certs
```

### Step 3: Generate Self-Signed Certificate

**Basic Command:**
```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost"
```

**Interactive (you'll be prompted for details):**
```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes
```

You'll be asked:
- Country Name (2 letter code): `US`
- State or Province Name: `YourState`
- Locality Name (city): `YourCity`
- Organization Name: `YourOrganization`
- Common Name (hostname): `localhost` ⚠️ **Important: Must be "localhost"**

**Recommended (with full details):**
```bash
openssl req -x509 -newkey rsa:4096 \
  -keyout key.pem \
  -out cert.pem \
  -days 365 \
  -nodes \
  -subj "/C=US/ST=California/L=SanFrancisco/O=MyCompany/OU=Development/CN=localhost"
```

### Step 4: Verify Certificate Files

```bash
ls -lh
# You should see:
# cert.pem (certificate)
# key.pem  (private key)
```

Check certificate details:
```bash
openssl x509 -in cert.pem -text -noout | head -20
```

### Step 5: Start the Server

```bash
cd ..  # Back to Frontend directory
npm start
```

You should see:
```
🔀 HTTP redirect server running on http://localhost:4200
🔒 HTTPS security proxy server running on https://localhost:4443
```

### Step 6: Access the Application

Open your browser to: **https://localhost:4443**

---

## Command Options Explained

| Option | Description |
|--------|-------------|
| `-x509` | Create a self-signed certificate (not a certificate request) |
| `-newkey rsa:4096` | Generate a new 4096-bit RSA private key |
| `-keyout key.pem` | Save private key to this file |
| `-out cert.pem` | Save certificate to this file |
| `-days 365` | Certificate valid for 365 days (1 year) |
| `-nodes` | Don't encrypt the private key (no password required) |
| `-subj "..."` | Set certificate subject (owner info) without prompts |

### Subject Field Components

```
/C=US                    # Country (2 letters)
/ST=California           # State/Province
/L=SanFrancisco         # City/Locality
/O=MyCompany            # Organization
/OU=Development         # Organizational Unit
/CN=localhost           # Common Name (MUST match hostname)
```

⚠️ **Critical: CN (Common Name) must be "localhost"** for local development.

---

## Browser Certificate Warnings

Since this is a **self-signed certificate**, browsers will show a security warning:

### Chrome/Chromium/Edge

1. You'll see: **"Your connection is not private"**
2. Click **"Advanced"**
3. Click **"Proceed to localhost (unsafe)"**

**Permanent Solution (Chrome):**
```bash
# Import certificate to trusted store
chrome://settings/certificates
# Click "Authorities" > "Import" > Select cert.pem
```

### Firefox

1. You'll see: **"Warning: Potential Security Risk Ahead"**
2. Click **"Advanced"**
3. Click **"Accept the Risk and Continue"**

**Permanent Solution (Firefox):**
```bash
# Go to: about:preferences#privacy
# Click "View Certificates" > "Servers" > "Add Exception"
# Enter: https://localhost:4443
# Click "Get Certificate" > "Confirm Security Exception"
```

### Safari

1. Safari will block the page
2. Click **"Show Details"**
3. Click **"visit this website"**

**Permanent Solution (macOS):**
```bash
# Add to Keychain
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain certs/cert.pem
```

---

## Advanced Options

### 1. Longer Validity Period (2 years)

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 730 -nodes -subj "/CN=localhost"
```

### 2. Different Key Sizes

**2048-bit (faster, less secure):**
```bash
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost"
```

**8192-bit (slower, more secure):**
```bash
openssl req -x509 -newkey rsa:8192 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost"
```

### 3. Multiple Hostnames (SAN - Subject Alternative Names)

Create a config file `san.cnf`:
```ini
[req]
default_bits = 4096
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = req_ext

[dn]
CN = localhost

[req_ext]
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = *.localhost
DNS.3 = 127.0.0.1
IP.1 = 127.0.0.1
IP.2 = ::1
```

Generate certificate:
```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -config san.cnf
```

### 4. Password-Protected Private Key

```bash
# Remove -nodes to require password
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -subj "/CN=localhost"
# You'll be prompted to enter a password

# Update server.mjs to include passphrase:
const sslOptions = {
  key: fs.readFileSync(path.join(__dirname, 'certs', 'key.pem')),
  cert: fs.readFileSync(path.join(__dirname, 'certs', 'cert.pem')),
  passphrase: 'your-password-here'
};
```

---

## Troubleshooting

### Error: "openssl: command not found"

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install openssl
```

**macOS:**
```bash
brew install openssl
```

**Windows:**
- Download from: https://slproweb.com/products/Win32OpenSSL.html
- Or use Git Bash (includes OpenSSL)

### Error: "Permission denied" when creating files

```bash
# Make sure you have write permissions
chmod 755 Frontend/certs
cd Frontend/certs
# Try again
```

### Error: "Address already in use" (port 4443)

```bash
# Find process using port 4443
lsof -ti:4443

# Kill the process
kill -9 $(lsof -ti:4443)
```

### Certificate Expired

```bash
# Check expiration date
openssl x509 -in cert.pem -noout -enddate

# Regenerate if expired
cd Frontend/certs
rm cert.pem key.pem
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost"
```

### Browser Still Shows Warning After Adding Exception

```bash
# Clear browser cache and SSL state
# Chrome: chrome://settings/clearBrowserData (check "Cached images and files")
# Firefox: about:preferences#privacy > Clear Data
# Then restart browser
```

---

## Security Best Practices

### ✅ DO:
- Use self-signed certificates **only for development**
- Store certificates in the `certs/` directory (already in .gitignore)
- Use at least 2048-bit keys (we recommend 4096-bit)
- Regenerate certificates periodically (every 6-12 months)
- Keep private keys (key.pem) secure

### ❌ DON'T:
- **Never commit certificates to version control** (especially private keys!)
- Never use self-signed certificates in production
- Never share private key files
- Never use weak key sizes (<2048 bits)
- Never set CN to anything other than your actual hostname

---

## Production Certificates

For production, use a trusted Certificate Authority (CA):

### Free Options:

**1. Let's Encrypt (Recommended)**
```bash
# Install certbot
sudo apt-get install certbot

# Generate certificate (requires public domain)
sudo certbot certonly --standalone -d yourdomain.com
```

**2. ZeroSSL**
- https://zerossl.com
- Free 90-day certificates
- Web-based management

### Paid Options:

- **DigiCert**: Enterprise-grade, $200+/year
- **GlobalSign**: Trusted worldwide, $250+/year
- **Comodo/Sectigo**: Budget-friendly, $50+/year

### Cloud Provider Certificates:

- **AWS Certificate Manager**: Free with AWS services
- **Google Cloud Certificate Manager**: Free with GCP
- **Azure Key Vault Certificates**: Free with Azure
- **Cloudflare**: Free SSL/TLS with their CDN

---

## Verification Checklist

After generating your certificate:

- [ ] Files exist: `Frontend/certs/cert.pem` and `Frontend/certs/key.pem`
- [ ] Server starts without errors
- [ ] Can access `https://localhost:4443` (even with browser warning)
- [ ] HTTP `http://localhost:4200` redirects to HTTPS
- [ ] API calls work (check browser console for CORS errors)
- [ ] WebSocket connections work (check for wss:// connections)
- [ ] Certificate not committed to git (`git status` shouldn't show certs/)

---

## Quick Reference Card

```bash
# Generate certificate (one command)
cd Frontend && mkdir -p certs && cd certs && \
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem \
  -days 365 -nodes -subj "/CN=localhost"

# Start server
cd .. && npm start

# Access application
# https://localhost:4443

# Check certificate expiration
openssl x509 -in certs/cert.pem -noout -dates

# View certificate details
openssl x509 -in certs/cert.pem -text -noout

# Verify certificate and key match
openssl x509 -noout -modulus -in certs/cert.pem | openssl md5
openssl rsa -noout -modulus -in certs/key.pem | openssl md5
# (Both MD5 hashes should match)
```

---

## Additional Resources

- [OpenSSL Documentation](https://www.openssl.org/docs/)
- [Let's Encrypt](https://letsencrypt.org/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [SSL Labs Server Test](https://www.ssllabs.com/ssltest/)
- [Certificate Transparency Log](https://crt.sh/)

---

**Created:** 2025-11-19  
**For:** Development HTTPS Setup  
**Security Level:** Development Only - Not for Production

