---
description: "Auto-fix vulnerable dependencies: upgrade to safe versions, apply patches, and verify the build passes. Renovate-style dependency healing."
name: "Dependency Auto-Healer"
agent: "Dependency Auto-Healer"
argument-hint: "Scope (e.g., 'fix all vulnerable dependencies', 'patch pom.xml CVEs and rebuild', 'auto-heal package.json vulnerabilities')"
---
Run automated dependency healing on this workspace.

- Discover all dependency manifests and scan for CVEs
- Upgrade vulnerable packages to the latest safe versions
- Apply patches directly to the manifest files
- Verify the build passes after each change
- Write a structured remediation report to `.github/output/`

Scope (if provided by the user): {{SCOPE}}
