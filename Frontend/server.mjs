import express from 'express';
import https from 'https';
import fs from 'fs';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { spawn } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 4200;
const HTTPS_PORT = 4443;

// Load SSL certificate
const sslOptions = {
  key: fs.readFileSync(path.join(__dirname, 'certs', 'key.pem')),
  cert: fs.readFileSync(path.join(__dirname, 'certs', 'cert.pem'))
};

// Disable X-Powered-By header to prevent information leakage
app.disable('x-powered-by');

// Function to apply security headers to response headers object
const applySecurityHeaders = (headers) => {
  // Check if CSP already exists (case-insensitive check for both variations)
  const existingCSP = headers['content-security-policy'] || headers['Content-Security-Policy'];

  // Debug log
  if (existingCSP) {
    console.log(`  → Existing CSP: ${existingCSP.substring(0, 50)}...`);
  }

  // Determine if this is a restrictive response (404, error pages with default-src 'none')
  if (existingCSP && existingCSP.includes("default-src 'none'")) {
    // For restrictive CSPs (like 404 pages), ensure frame-ancestors and form-action are present
    console.log('  → Applying restrictive CSP with frame-ancestors and form-action');
    const newCSP = "default-src 'none'; frame-ancestors 'none'; form-action 'none'; base-uri 'none';";

    // Delete both possible case variations and set the new one
    delete headers['content-security-policy'];
    delete headers['Content-Security-Policy'];
    headers['content-security-policy'] = newCSP;
  } else {
    // For normal responses, use full CSP with ALL required directives (including non-fallback ones)
    const newCSP = "default-src 'self'; " +
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
      "style-src 'self' 'unsafe-inline'; " +
      "img-src 'self' data: http://localhost:8080 http://localhost:8083; " +
      "font-src 'self' data:; " +
      "connect-src 'self' http://localhost:8080 http://localhost:8081 http://localhost:8082 http://localhost:8083 https://localhost:4443 ws://localhost:4200 wss://localhost:4443; " +
      "media-src 'self'; " +
      "worker-src 'self'; " +
      "child-src 'self'; " +
      "manifest-src 'self'; " +
      "frame-ancestors 'self'; " +
      "form-action 'self'; " +
      "base-uri 'self'; " +
      "object-src 'none';";

    // Delete both possible case variations and set the new one
    delete headers['content-security-policy'];
    delete headers['Content-Security-Policy'];
    headers['content-security-policy'] = newCSP;
  }

  // Set other security headers (ensuring consistent casing)
  headers['x-frame-options'] = 'SAMEORIGIN';
  headers['x-content-type-options'] = 'nosniff';
  headers['x-xss-protection'] = '1; mode=block';
  headers['referrer-policy'] = 'strict-origin-when-cross-origin';
  headers['permissions-policy'] =
    'geolocation=(), microphone=(), camera=(), payment=(), usb=(), ' +
    'magnetometer=(), gyroscope=(), accelerometer=()';

  // Remove X-Powered-By if it exists (both case variations)
  delete headers['x-powered-by'];
  delete headers['X-Powered-By'];
};

// Block access to sensitive directories and files (CRITICAL SECURITY)
app.use((req, res, next) => {
  const sensitivePathPatterns = [
    /^\/\.git/i,        // Git repository
    /^\/\.hg/i,         // Mercurial repository
    /^\/\.svn/i,        // Subversion repository
    /^\/\.bzr/i,        // Bazaar repository
    /^\/\.env/i,        // Environment files
    /^\/\.idea/i,       // JetBrains IDE config
    /^\/\.vscode/i,     // VS Code config
    /^\/\.DS_Store/i,   // macOS metadata
    /^\/node_modules/i, // Node dependencies
    /^\/\.npm/i,        // NPM cache
    /^\/\.gitignore/i,  // Git ignore file
    /^\/\.gitattributes/i, // Git attributes
    /^\/package-lock\.json/i, // NPM lock file
    /^\/yarn\.lock/i,   // Yarn lock file
    /^\/composer\.lock/i, // Composer lock file
    /^\/Gemfile\.lock/i,  // Ruby Gemfile lock
    /^\/\.htaccess/i,   // Apache config
    /^\/\.htpasswd/i,   // Apache password file
    /^\/web\.config/i,  // IIS config
    /^\/backup/i,       // Backup directories
    /^\/\.backup/i,     // Hidden backup directories
    /^\/\.sql/i,        // SQL files
    /^\/\.bak/i,        // Backup files
    /^\/\.swp/i,        // Vim swap files
    /^\/\.swo/i,        // Vim swap files
    /^\/__pycache__/i,  // Python cache
    /^\/\.pytest_cache/i, // Pytest cache
    /^\/\.mypy_cache/i, // Mypy cache
  ];

  // Check if request path matches any sensitive pattern
  const isSensitivePath = sensitivePathPatterns.some(pattern =>
    pattern.test(req.path)
  );

  if (isSensitivePath) {
    console.warn(`🚫 Blocked access to sensitive path: ${req.path}`);

    // Set security headers for error response
    res.setHeader('Content-Security-Policy',
      "default-src 'none'; " +
      "frame-ancestors 'none'; " +
      "form-action 'none'; " +
      "base-uri 'none';"
    );
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Referrer-Policy', 'no-referrer');
    res.setHeader('Permissions-Policy',
      'geolocation=(), microphone=(), camera=(), payment=(), usb=(), ' +
      'magnetometer=(), gyroscope=(), accelerometer=()'
    );

    // Return 403 Forbidden
    return res.status(403).send('Forbidden');
  }

  next();
});

// Security headers middleware for direct responses from Express
app.use((req, res, next) => {
  // Content Security Policy with all required directives (including non-fallback ones)
  res.setHeader('Content-Security-Policy',
    "default-src 'self'; " +
    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
    "style-src 'self' 'unsafe-inline'; " +
    "img-src 'self' data: http://localhost:8080 http://localhost:8083; " +
    "font-src 'self' data:; " +
    "connect-src 'self' http://localhost:8080 http://localhost:8081 http://localhost:8082 http://localhost:8083 https://localhost:4443 ws://localhost:4200 wss://localhost:4443; " +
    "media-src 'self'; " +
    "worker-src 'self'; " +
    "child-src 'self'; " +
    "manifest-src 'self'; " +
    "frame-ancestors 'self'; " +
    "form-action 'self'; " +
    "base-uri 'self'; " +
    "object-src 'none';"
  );

  // X-Frame-Options - legacy protection against clickjacking
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');

  // X-Content-Type-Options - prevents MIME type sniffing
  res.setHeader('X-Content-Type-Options', 'nosniff');

  // X-XSS-Protection - enables XSS filter in older browsers
  res.setHeader('X-XSS-Protection', '1; mode=block');

  // Referrer-Policy - controls referrer information
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');

  // Permissions-Policy - restricts browser features
  res.setHeader('Permissions-Policy',
    'geolocation=(), ' +
    'microphone=(), ' +
    'camera=(), ' +
    'payment=(), ' +
    'usb=(), ' +
    'magnetometer=(), ' +
    'gyroscope=(), ' +
    'accelerometer=()'
  );

  next();
});

// Proxy API requests to backend gateway
app.use('/api', createProxyMiddleware({
  target: 'http://localhost:8080',
  changeOrigin: true,
  onProxyRes: (proxyRes, _req, _res) => {
    // Apply security headers to API responses
    applySecurityHeaders(proxyRes.headers);
  }
}));

// Start Angular dev server as child process
console.log('Starting Angular dev server...');
const ngServe = spawn('npm', ['run', 'ng-serve'], {
  cwd: __dirname,
  stdio: 'inherit',
  shell: true
});

ngServe.on('error', (error) => {
  console.error('Failed to start Angular dev server:', error);
  process.exit(1);
});

ngServe.on('exit', (code) => {
  console.log(`Angular dev server exited with code ${code}`);
  process.exit(code);
});

// Proxy all other requests to Angular dev server
app.use('/', createProxyMiddleware({
  target: 'http://localhost:4201',
  changeOrigin: true,
  ws: true, // Enable WebSocket proxy for hot reload
  on: {
    proxyRes: (proxyRes, req, res) => {
      console.log(`[${req.method}] ${req.path} - Status: ${proxyRes.statusCode}`);

      // Get existing CSP to determine if it's a 404/error page
      const existingCSP = proxyRes.headers['content-security-policy'];

      if (existingCSP && existingCSP.includes("default-src 'none'")) {
        // For 404 and error pages with restrictive CSP, add missing directives
        console.log('  → Modifying 404/error CSP to include frame-ancestors and form-action');
        proxyRes.headers['content-security-policy'] = "default-src 'none'; frame-ancestors 'none'; form-action 'none'; base-uri 'none';";
      } else {
        // For normal pages, set full CSP with ALL required directives (including non-fallback ones)
        proxyRes.headers['content-security-policy'] =
          "default-src 'self'; " +
          "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
          "style-src 'self' 'unsafe-inline'; " +
          "img-src 'self' data: http://localhost:8080 http://localhost:8083; " +
          "font-src 'self' data:; " +
          "connect-src 'self' http://localhost:8080 http://localhost:8081 http://localhost:8082 http://localhost:8083 https://localhost:4443 ws://localhost:4200 wss://localhost:4443; " +
          "media-src 'self'; " +
          "worker-src 'self'; " +
          "child-src 'self'; " +
          "manifest-src 'self'; " +
          "frame-ancestors 'self'; " +
          "form-action 'self'; " +
          "base-uri 'self'; " +
          "object-src 'none';";
      }

      // Set other security headers
      proxyRes.headers['x-frame-options'] = 'SAMEORIGIN';
      proxyRes.headers['x-content-type-options'] = 'nosniff';
      proxyRes.headers['x-xss-protection'] = '1; mode=block';
      proxyRes.headers['referrer-policy'] = 'strict-origin-when-cross-origin';
      proxyRes.headers['permissions-policy'] =
        'geolocation=(), microphone=(), camera=(), payment=(), usb=(), ' +
        'magnetometer=(), gyroscope=(), accelerometer=()';

      // Remove X-Powered-By
      delete proxyRes.headers['x-powered-by'];
    },
    error: (err, _req, res) => {
      console.error('Proxy error:', err.message);
      res.status(503).send('Angular dev server not ready yet. Please wait...');
    }
  }
}));

// Create HTTP to HTTPS redirect server
const redirectApp = express();
redirectApp.use((req, res) => {
  res.redirect(301, `https://localhost:${HTTPS_PORT}${req.url}`);
});

redirectApp.listen(PORT, () => {
  console.log(`\n🔀 HTTP redirect server running on http://localhost:${PORT}`);
  console.log(`   Redirecting all traffic to https://localhost:${HTTPS_PORT}`);
});

// Start the HTTPS security proxy server
https.createServer(sslOptions, app).listen(HTTPS_PORT, () => {
  console.log(`\n🔒 HTTPS security proxy server running on https://localhost:${HTTPS_PORT}`);
  console.log('📡 Proxying Angular dev server from port 4201');
  console.log('🔐 All security headers are being applied');
  console.log('🔐 SSL/TLS encryption enabled');
  console.log('✅ X-Powered-By header disabled');
  console.log(`\n💡 Access the application at: https://localhost:${HTTPS_PORT}`);
  console.log(`   (HTTP requests to port ${PORT} will be redirected to HTTPS)\n`);
});

// Handle graceful shutdown
process.on('SIGINT', () => {
  console.log('\nShutting down servers...');
  ngServe.kill();
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\nShutting down servers...');
  ngServe.kill();
  process.exit(0);
});
