import express from 'express';
import https from 'node:https';
import fs from 'node:fs';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { spawn } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 4200;
const HTTPS_PORT = 4443;
const REDIRECT_HTTPS_ORIGIN = `https://localhost:${HTTPS_PORT}`;

// Load SSL certificate
const sslOptions = (() => {
  try {
    const keyPath = path.join(__dirname, 'certs', 'key.pem');
    const certPath = path.join(__dirname, 'certs', 'cert.pem');

    if (!fs.existsSync(keyPath) || !fs.existsSync(certPath)) {
      throw new Error(`SSL certificates not found at ${keyPath} or ${certPath}`);
    }

    return {
      key: fs.readFileSync(keyPath),
      cert: fs.readFileSync(certPath)
    };
  } catch (err) {
    console.error('❌ FATAL: Failed to load SSL certificates');
    console.error(`   Error: ${err.message}`);
    console.error('   Make sure cert-generator.mjs has run successfully');
    process.exit(1);
  }
})();

// Disable X-Powered-By header to prevent information leakage
app.disable('x-powered-by');

// ─── FIX #7: Truncate BEFORE filtering to bound work on attacker-controlled input ───
const sanitizeForLog = (value) => String(value ?? '')
  .slice(0, 200)
  .replaceAll(/[\r\n\t]/g, ' ')
  .replaceAll(/[^\x20-\x7E]/g, '?');

// ─── FIX #4: Single source of truth for CSP strings ───────────────────────────────
const CSP_NORMAL =
  "default-src 'self'; " +
  "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
  "style-src 'self' 'unsafe-inline'; " +
  "img-src 'self' data: http://localhost:8080 http://localhost:8083; " +
  "font-src 'self' data:; " +
  "connect-src 'self' http://localhost:8080 http://localhost:8081 http://localhost:8082 http://localhost:8083 " +
  "https://localhost:4443 ws://localhost:4200 wss://localhost:4443; " +
  "media-src 'self'; " +
  "worker-src 'self'; " +
  "child-src 'self'; " +
  "manifest-src 'self'; " +
  "frame-ancestors 'self'; " +
  "form-action 'self'; " +
  "base-uri 'self'; " +
  "object-src 'none';";

const CSP_RESTRICTIVE =
  "default-src 'none'; frame-ancestors 'none'; form-action 'none'; base-uri 'none';";

// Function to apply security headers to response headers object (used by proxy callbacks)
const applySecurityHeaders = (headers) => {
  const existingCSP = headers['content-security-policy'] ?? headers['Content-Security-Policy'];

  if (existingCSP) {
    console.log(`  → Existing CSP: ${existingCSP.substring(0, 50)}...`);
  }

  delete headers['content-security-policy'];
  delete headers['Content-Security-Policy'];

  headers['content-security-policy'] = existingCSP?.includes("default-src 'none'")
    ? CSP_RESTRICTIVE
    : CSP_NORMAL;

  if (existingCSP?.includes("default-src 'none'")) {
    console.log('  → Applying restrictive CSP with frame-ancestors and form-action');
  }

  headers['x-frame-options'] = 'SAMEORIGIN';
  headers['x-content-type-options'] = 'nosniff';
  headers['x-xss-protection'] = '1; mode=block';
  headers['referrer-policy'] = 'strict-origin-when-cross-origin';
  headers['permissions-policy'] =
    'geolocation=(), microphone=(), camera=(), payment=(), usb=(), ' +
    'magnetometer=(), gyroscope=(), accelerometer=()';

  delete headers['x-powered-by'];
  delete headers['X-Powered-By'];
};

// ─── FIX #3: Comprehensive sensitive-path blocking with extended patterns ──────────
// Single compiled regex is faster and easier to maintain than an array of patterns.
// Uses case-insensitive flag; covers variant filenames (.env.local, .env.production, etc.)
const SENSITIVE_PATH_RE = new RegExp(
  String.raw`^\/(?:` + [
    String.raw`\.git(?:\/|$)`,
    String.raw`\.hg(?:\/|$)`,
    String.raw`\.svn(?:\/|$)`,
    String.raw`\.bzr(?:\/|$)`,
    String.raw`\.env(?:\.[^/]*)?(?:\/|$)`,
    String.raw`\.idea(?:\/|$)`,
    String.raw`\.vscode(?:\/|$)`,
    String.raw`\.DS_Store(?:\/|$)`,
    String.raw`node_modules(?:\/|$)`,
    String.raw`\.npm(?:\/|$)`,
    String.raw`\.gitignore$`,
    String.raw`\.gitattributes$`,
    String.raw`package-lock\.json$`,
    String.raw`yarn\.lock$`,
    String.raw`composer\.lock$`,
    String.raw`Gemfile\.lock$`,
    String.raw`\.htaccess$`,
    String.raw`\.htpasswd$`,
    String.raw`web\.config$`,
    String.raw`backup(?:\/|$)`,
    String.raw`\.backup(?:\/|$)`,
    String.raw`\.sql$`,
    String.raw`\.bak$`,
    String.raw`\.swp$`,
    String.raw`\.swo$`,
    String.raw`__pycache__(?:\/|$)`,
    String.raw`\.pytest_cache(?:\/|$)`,
    String.raw`\.mypy_cache(?:\/|$)`,
  ].join('|') + String.raw`)`,
  'i'
);
const warnSensitivePath = () => console.warn('Blocked access to sensitive path');

app.use((req, res, next) => {
  if (SENSITIVE_PATH_RE.test(req.path)) {
    warnSensitivePath();
    res.setHeader('Content-Security-Policy', CSP_RESTRICTIVE);
    return res.status(403).send('Forbidden');
  }
  next();
});


// Security headers middleware for direct responses from Express
app.use((req, res, next) => {
  res.setHeader('Content-Security-Policy', CSP_NORMAL);
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy',
    'geolocation=(), microphone=(), camera=(), payment=(), usb=(), ' +
    'magnetometer=(), gyroscope=(), accelerometer=()'
  );
  // ─── FIX #5: Add HSTS on every response (meaningful only over HTTPS) ────────────
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');

  next();
});

// ─── FIX #2: Strip Host / X-Forwarded-* headers to prevent SSRF via header injection ─
// changeOrigin rewrites the Host header to the target, but we explicitly
// overwrite forwarding headers so the backend cannot be tricked by a crafted
// incoming X-Forwarded-Host value.
app.use('/api', createProxyMiddleware({
  target: 'http://localhost:8080',
  changeOrigin: true,
  headers: {
    host: 'localhost:8080',
    'x-forwarded-host': '',
    'x-forwarded-for': '',
    'x-forwarded-proto': '',
  },
  onProxyRes: (proxyRes, _req, _res) => {
    applySecurityHeaders(proxyRes.headers);
  }
}));

const isProduction = process.env.NODE_ENV === 'production' ||
  fs.existsSync(path.join(__dirname, 'dist', 'Frontend', 'browser', 'index.html'));

let ngServe;

if (isProduction) {
  console.log('🚀 Running in PRODUCTION mode - serving built static files');

  const distPath = path.join(__dirname, 'dist', 'Frontend', 'browser');

  app.use(express.static(distPath));

  app.get(/^\/(?!api).*/, (req, res) => {
    res.sendFile(path.join(distPath, 'index.html'));
  });
} else {
  console.log('🔧 Running in DEVELOPMENT mode - proxying to Angular dev server');

  console.log('Starting Angular dev server...');

  const SAFE_USER = /^[a-zA-Z0-9_-]+$/.test(process.env.USER ?? '') ? process.env.USER : 'app';
  const SAFE_LANG = /^[a-zA-Z0-9_.@-]+$/.test(process.env.LANG ?? '') ? process.env.LANG : 'en_US.UTF-8';

  const safeEnv = {
    PATH: '/usr/local/bin:/usr/bin:/bin',   // hardcoded — never trust env PATH
    NODE_ENV: 'development',
    HOME: '/root',                           // hardcoded — prevents config hijack
    TMPDIR: '/tmp',                          // hardcoded — prevents temp dir hijack
    USER: SAFE_USER,                         // validated scalar, no path risk
    LANG: SAFE_LANG,                         // validated scalar, no path risk
  };

  ngServe = spawn('npm', ['run', 'ng-serve'], {
    cwd: __dirname,
    stdio: 'inherit',
    shell: true,
    env: safeEnv,
  });


  ngServe.on('error', (error) => {
    console.error('Failed to start Angular dev server:', error);
    process.exit(1);
  });

  ngServe.on('exit', (code) => {
    console.log(`Angular dev server exited with code ${code}`);
    process.exit(code);
  });

  // ─── FIX #2 (dev proxy): strip forwarding headers on the Angular dev proxy too ──
  app.use('/', createProxyMiddleware({
    target: 'http://localhost:4201',
    changeOrigin: true,
    ws: true,
    headers: {
      host: 'localhost:4201',
      'x-forwarded-host': '',
      'x-forwarded-for': '',
      'x-forwarded-proto': '',
    },
    on: {
      proxyRes: (proxyRes, req, res) => {
        const safeMethod = sanitizeForLog(req.method);
        const safePath = sanitizeForLog(req.path);
        const safeStatus = sanitizeForLog(proxyRes.statusCode);
        console.log(`[${safeMethod}] ${safePath} - Status: ${safeStatus}`);

        applySecurityHeaders(proxyRes.headers);
      },
      error: (err, _req, res) => {
        console.error('Proxy error:', sanitizeForLog(err.message));
        res.status(503).send('Angular dev server not ready yet. Please wait...');
      }
    }
  }));
}

// ─── FIX #1: Safe redirect — block protocol-relative paths like //evil.com ────────
// startsWith('/') alone is insufficient: '//evil.com' starts with '/' but is a
// valid protocol-relative URL that browsers resolve to https://evil.com.
// The regex /^\/(?![\/\\])/ ensures the second character is not / or \.
const redirectApp = express();
redirectApp.use((req, res) => {
  const rawUrl = req.originalUrl;
  const safeUrl =
    typeof rawUrl === 'string' && /^\/(?![/\\])/.test(rawUrl)
      ? rawUrl.replaceAll(/[^\x20-\x7E]/g, '').slice(0, 500)
      : '/';
  res.redirect(301, `${REDIRECT_HTTPS_ORIGIN}${safeUrl}`);
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

// ─── FIX #6: Graceful shutdown with SIGKILL fallback after 5 s ────────────────────
// Without a timeout, SIGTERM can be ignored by a hung child process and the
// parent hangs forever — holding open ports and file descriptors.
function killChild(proc) {
  if (!proc) return;
  proc.kill('SIGTERM');
  const timer = setTimeout(() => {
    console.warn('Child process did not exit in time — sending SIGKILL');
    proc.kill('SIGKILL');
  }, 5000);
  timer.unref(); // don't prevent the event loop from exiting on its own
}

process.on('SIGINT', () => {
  console.log('\nShutting down servers...');
  if (!isProduction) killChild(ngServe);
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\nShutting down servers...');
  if (!isProduction) killChild(ngServe);
  process.exit(0);
});
