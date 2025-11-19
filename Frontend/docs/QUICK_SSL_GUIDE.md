# Generate SSL Certificate - Quick Guide

## 🚀 One-Line Command

```bash
cd Frontend && mkdir -p certs && cd certs && openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost" && cd ..
```

**That's it!** Now start the server:
```bash
npm start
```

Access your app at: **https://localhost:4443**

---

## 📝 Step-by-Step (if you prefer)

### 1. Go to Frontend folder
```bash
cd Frontend
```

### 2. Create certificates directory
```bash
mkdir -p certs
cd certs
```

### 3. Generate the certificate
```bash
openssl req -x509 -newkey rsa:4096 \
  -keyout key.pem \
  -out cert.pem \
  -days 365 \
  -nodes \
  -subj "/CN=localhost"
```

### 4. Go back and start server
```bash
cd ..
npm start
```

### 5. Open browser
Go to: **https://localhost:4443**

---

## ⚠️ Browser Warning

You'll see a security warning because it's a self-signed certificate. This is **normal for development**.

**Chrome/Edge:** Click "Advanced" → "Proceed to localhost"  
**Firefox:** Click "Advanced" → "Accept the Risk and Continue"  
**Safari:** Click "Show Details" → "visit this website"

---

## ✅ Verify It Worked

Check files were created:
```bash
ls -lh Frontend/certs/
# You should see: cert.pem and key.pem
```

Check certificate expiration:
```bash
openssl x509 -in Frontend/certs/cert.pem -noout -dates
```

---

## 🔄 Regenerate Certificate (if expired or corrupted)

```bash
cd Frontend/certs
rm cert.pem key.pem
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost"
```

---

## 🆘 Troubleshooting

**"openssl: command not found"**
```bash
# Ubuntu/Debian
sudo apt-get install openssl

# macOS
brew install openssl
```

**Port 4443 already in use**
```bash
kill -9 $(lsof -ti:4443)
```

**Certificate still showing errors**
- Clear browser cache
- Restart browser
- Regenerate certificate

---

## 📚 Need More Details?

See the full guide: `Frontend/SSL_CERTIFICATE_GUIDE.md`

---

**Quick Tip:** Certificates are valid for 365 days. Regenerate yearly or when you see expiration warnings.

