---
description: "Audit source code for OWASP Top 10 vulnerabilities: injection, hardcoded secrets, insecure config, auth issues, and more."
name: "Security Auditor"
agent: "Security Auditor"
argument-hint: "Scope to audit (e.g., 'audit all src/', 'scan for hardcoded credentials', 'check injection risks in WebPlatformAdapter.java')"
---
Perform a static security audit on the specified scope.

- Identify the language and framework in use
- Scan for OWASP Top 10 (2021) risks: injection, broken auth, sensitive data exposure, insecure config, etc.
- Flag hardcoded credentials, secrets, or API keys
- Check input validation, error handling, and authorization logic
- Report findings by severity with remediation guidance

Do NOT fix or modify any code — audit only.

Scope (if provided by the user): {{SCOPE}}
