# QA Security Audit Criteria

This skill file is loaded by the QA Automation Expert agent during Stage 3. Apply systematically to all source files in scope.

---

## Step 1 — Credential & Secret Scan (Always Run First)

Search the entire `src/` tree for hardcoded secrets BEFORE any other analysis.

**Search patterns:**
- Hardcoded passwords: `password`, `passwd`, `pwd`, `secret`, `apiKey`, `api_key`, `token`, `AUTH_TOKEN`
- URLs with embedded credentials: `://user:pass@`
- Base64-looking strings assigned to credential variables
- Private keys or certificates inline in code
- Connection strings: `jdbc:`, `mongodb://`, `redis://`, `amqp://`
- Test credentials in production paths (`src/main/`)

Flag every match as `[CRITICAL]`. Mask values in output — show only first 2 characters (e.g., `pa****`).

---

## Step 2 — OWASP Top 10 (2021) Scan

For each file read, evaluate all applicable categories:

### A01 — Broken Access Control
- Authorization checks before sensitive operations?
- Direct object references without ownership validation?
- Admin/privileged paths protected?

### A02 — Cryptographic Failures
- Sensitive data transmitted or stored in plaintext?
- Weak algorithms: `MD5`, `SHA1`, `DES`, `RC4`, `ECB` mode?
- Passwords hashed with proper algorithm (`bcrypt`, `Argon2`, `PBKDF2`)?
- Cryptographically secure RNG for security values?

### A03 — Injection
- **SQL**: queries built with string concatenation instead of parameterized statements?
- **Command**: user input to `Runtime.exec()`, `ProcessBuilder`, `os.system()`, `eval()`?
- **Log**: unsanitized user input written directly to logs?
- **XPath / HQL / JPQL**: query languages constructed with user input?

### A04 — Insecure Design
- Missing rate-limiting, throttling, or lockout?
- Security bolted on as afterthought rather than designed in?

### A05 — Security Misconfiguration
- Debug modes, verbose errors, or stack traces exposed?
- Default credentials or configurations still in place?
- Unnecessary features or endpoints enabled?

### A06 — Vulnerable and Outdated Components
- Note dependency versions in `pom.xml` / `package.json` / `requirements.txt`
- Flag `@SuppressWarnings("deprecation")` — underlying APIs may carry security implications
- Flag dependencies with known CVEs if identifiable from version

### A07 — Identification and Authentication Failures
- Session tokens with sufficient entropy?
- Passwords stored securely (not reversible or plaintext)?
- Brute-force protection present?
- JWT tokens validated (signature, expiry, algorithm)?

### A08 — Software and Data Integrity Failures
- Deserialization of untrusted data (`ObjectInputStream`, `pickle`, `yaml.load()` without safe loader)?
- Integrity checks for downloaded or external data?

### A09 — Security Logging and Monitoring Failures
- Security events logged (failed logins, access denials)?
- Sensitive data (passwords, tokens, PII) being logged?
- Log entries include timestamps and user context?

### A10 — Server-Side Request Forgery (SSRF)
- User-controlled input used to construct fetched URLs?
- Allowlists for permitted URL schemes or destinations?
- Internal network addresses blocked from user-supplied URLs?

---

## QA Automation-Specific Security Checks

### Test Credentials in Production Code
- Any credentials (even "test" ones) in `src/main/` → `[CRITICAL]`
- `.properties` or `.yml` config files with plaintext passwords in committed code → `[CRITICAL]`

### Screenshots and Reports
- Do test reports or screenshots capture sensitive data (PII, tokens, passwords visible in UI)?
- Are report output directories excluded from version control (`.gitignore`)?

### Insecure WebDriver / Client Configuration
- Certificate validation disabled (`acceptInsecureCerts`, `--ignore-certificate-errors`) → `[MEDIUM]`
- Permissive CORS or proxy settings in test configs → `[LOW]`
- Browser running with elevated privileges unnecessarily → `[LOW]`

### Test Data Exposure
- Real user data used in test environments → `[HIGH]`
- Test databases with production data copies → `[HIGH]`
- API keys for production services used in tests → `[CRITICAL]`

---

## Severity Tags

- `[CRITICAL]` — Directly exploitable, data breach or system takeover risk
- `[HIGH]` — Significant risk requiring prompt attention
- `[MEDIUM]` — Risk present but requires specific conditions to exploit
- `[LOW]` — Defense-in-depth gap, missing monitoring, informational

---

## Output Format

### Credential Scan Results

List every finding:
- **File**: relative path (line number)
- **Finding**: what was found (masked value)
- **Risk**: why this is dangerous

If none: "No hardcoded credentials detected."

### OWASP Findings

For each vulnerability:

```
**[SEVERITY] OWASP Axx — Short title**
- **File**: relative path (line number)
- **Vulnerability**: what the code does that is insecure
- **Attack scenario**: how an attacker could exploit this
- **Remediation**: what class of fix is needed (do not write code)
```

### Security Scorecard

| OWASP Category | Risk Level | Findings |
|----------------|-----------|----------|
| A01 Broken Access Control | | |
| A02 Cryptographic Failures | | |
| A03 Injection | | |
| A04 Insecure Design | | |
| A05 Security Misconfiguration | | |
| A06 Vulnerable Components | | |
| A07 Authentication Failures | | |
| A08 Data Integrity Failures | | |
| A09 Logging & Monitoring | | |
| A10 SSRF | | |
| **Hardcoded Credentials** | | |
| **QA-Specific Security** | | |
