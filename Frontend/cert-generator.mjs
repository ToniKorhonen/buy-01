/**
 * Certificate Generator
 *
 * Generates self-signed SSL/TLS certificates if they don't exist.
 * Supports both mounted certificates (Jenkins/production) and runtime generation (local development).
 *
 * Usage: node cert-generator.mjs
 *
 * Behavior:
 * 1. Checks if certs/cert.pem and certs/key.pem exist and are valid
 * 2. If they exist and are valid, skips generation
 * 3. If they don't exist or are invalid, generates new self-signed certificates
 * 4. Properly sets file permissions for security
 *
 * Environment Variables:
 * - CERT_COUNTRY (default: US)
 * - CERT_STATE (default: State)
 * - CERT_LOCALITY (default: Locality)
 * - CERT_ORG (default: Organization)
 * - CERT_COMMON_NAME (default: localhost)
 * - CERT_DAYS (default: 365)
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const CERTS_DIR = path.join(__dirname, 'certs');
const CERT_FILE = path.join(CERTS_DIR, 'cert.pem');
const KEY_FILE = path.join(CERTS_DIR, 'key.pem');

// Certificate configuration from environment or defaults
const CERT_COUNTRY = process.env.CERT_COUNTRY || 'US';
const CERT_STATE = process.env.CERT_STATE || 'State';
const CERT_LOCALITY = process.env.CERT_LOCALITY || 'Locality';
const CERT_ORG = process.env.CERT_ORG || 'Organization';
const CERT_COMMON_NAME = process.env.CERT_COMMON_NAME || 'localhost';
const CERT_DAYS = process.env.CERT_DAYS || 365;

/**
 * Check if certificate files exist and are valid
 */
function certificatesExist() {
  if (!fs.existsSync(CERT_FILE) || !fs.existsSync(KEY_FILE)) {
    return false;
  }

  try {
    const cert = fs.readFileSync(CERT_FILE, 'utf8');
    const key = fs.readFileSync(KEY_FILE, 'utf8');

    // Basic validation: check for PEM headers
    const hasCertHeader = cert.includes('-----BEGIN CERTIFICATE-----') && cert.includes('-----END CERTIFICATE-----');
    const hasKeyHeader = key.includes('-----BEGIN PRIVATE KEY-----') && key.includes('-----END PRIVATE KEY-----') ||
                         key.includes('-----BEGIN RSA PRIVATE KEY-----') && key.includes('-----END RSA PRIVATE KEY-----');

    return hasCertHeader && hasKeyHeader;
  } catch (err) {
    console.error(`❌ Error reading certificate files: ${err.message}`);
    return false;
  }
}

/**
 * Generate self-signed certificate
 */
function generateCertificate() {
  return new Promise((resolve, reject) => {
    try {
      // Ensure certs directory exists
      if (!fs.existsSync(CERTS_DIR)) {
        fs.mkdirSync(CERTS_DIR, { mode: 0o755, recursive: true });
        console.log(`📁 Created certificates directory: ${CERTS_DIR}`);
      }

      const subject = `/C=${CERT_COUNTRY}/ST=${CERT_STATE}/L=${CERT_LOCALITY}/O=${CERT_ORG}/CN=${CERT_COMMON_NAME}`;

      console.log('🔐 Generating self-signed SSL/TLS certificate...');
      console.log(`   Subject: ${subject}`);
      console.log(`   Valid for: ${CERT_DAYS} days`);

      // Use spawn with arguments array to avoid shell injection
      const openssl = spawn('openssl', [
        'req',
        '-x509',
        '-newkey', 'rsa:2048',
        '-keyout', KEY_FILE,
        '-out', CERT_FILE,
        '-days', String(CERT_DAYS),
        '-nodes',
        '-subj', subject,
      ]);

      let errorOutput = '';

      openssl.stderr.on('data', (data) => {
        errorOutput += data.toString();
      });

      openssl.on('close', (code) => {
        if (code !== 0) {
          reject(new Error(`OpenSSL failed with code ${code}: ${errorOutput}`));
          return;
        }

        try {
          // Set proper file permissions
          fs.chmodSync(KEY_FILE, 0o400);  // Read-only for owner (more restrictive for private key)
          fs.chmodSync(CERT_FILE, 0o440); // Read-only for owner and group (restrict world access)

          console.log(`✅ Certificate generated successfully!`);
          console.log(`   Key: ${KEY_FILE} (0400)`);
          console.log(`   Cert: ${CERT_FILE} (0440)`);
          resolve();
        } catch (err) {
          reject(err);
        }
      });

      openssl.on('error', (err) => {
        reject(err);
      });
    } catch (err) {
      reject(err);
    }
  });
}

/**
 * Main function
 */
async function main() {
  console.log('🔑 Certificate Manager Starting...');

  if (certificatesExist()) {
    console.log('✅ Valid certificates already exist at:');
    console.log(`   Key: ${KEY_FILE}`);
    console.log(`   Cert: ${CERT_FILE}`);
    console.log('   Skipping certificate generation\n');
    return;
  }

  console.log('⚠️  Certificates not found or invalid');
  try {
    await generateCertificate();
    console.log('');
  } catch (err) {
    console.error(`❌ Error generating certificate: ${err.message}`);
    console.error('   Make sure OpenSSL is installed on your system');
    process.exit(1);
  }
}

main();

