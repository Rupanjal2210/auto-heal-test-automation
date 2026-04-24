---
description: "Run a full 5-stage QA audit: code review, test quality, security scan, refactoring backlog, and resolution recommendations — all in one unified report."
name: "QA Automation Expert"
agent: "QA Automation Expert"
argument-hint: "Scope (e.g., 'full audit of src/', 'quality check on the adapters package', 'review test quality in src/test/')"
---
Run the full 5-stage QA automation pipeline on the specified scope.

Stages (executed in sequence):
1. **Code Review** — Google standards: naming, design, readability
2. **Test Quality** — coverage, assertions, naming, behavior vs. implementation
3. **Security Audit** — OWASP Top 10 static analysis
4. **Refactoring Backlog** — prioritized technical debt from stages 1–3
5. **Resolution Recommendations** — actionable fix plan with effort estimates

Produce a single unified report at the end.

Do NOT write or modify any code — review, audit, and recommend only.

Scope (if provided by the user): {{SCOPE}}
