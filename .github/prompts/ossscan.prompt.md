---
description: "Scan all dependencies for CVEs, license risks, and supply chain threats. Writes CSV reports to .github/output/"
name: "OSS Vulnerability Scanner"
agent: "OSS Vulnerability Scanner"
argument-hint: "Scope (e.g., 'scan all dependencies', 'check pom.xml only', 'audit transitive risks')"
---
Run a full OSS vulnerability scan on this workspace.

- Auto-discover all dependency manifests (pom.xml, package.json, requirements.txt, etc.)
- Check every dependency for known CVEs using live advisory databases
- Identify transitive risks, license issues, and supply chain signals
- Write structured findings as CSV files to `.github/output/`
- If file write fails, print the consolidated results as a Markdown table

Scope (if provided by the user): {{SCOPE}}
