---
name: mainframe-veracode-scanner
description: >
  Veracode-aligned static vulnerability detection for mainframe source files
  (COBOL .cbl/.cpy, JCL .jcl, Assembler .asm, BMS .bms/.map). Batch-first
  approach: delegates bulk scanning to a PowerShell-backed batch script and reads
  only the resulting CSV report. Optional Veracode REST API integration via curl
  for live platform correlation.
---

# Mainframe Veracode Scanner Skill

---

## Phase 0 — Prerequisite Check

Before running any scan, verify the following using the `execute` tool:

```powershell
# 1. Confirm script files exist
Test-Path ".github/scripts/scan-mainframe.bat"
Test-Path ".github/scripts/scan-mainframe-core.ps1"
Test-Path ".github/scripts/veracode-api.ps1"

# 2. Confirm output directory exists (create if missing)
if (-not (Test-Path ".github/output")) { New-Item -ItemType Directory -Path ".github/output" }

# 3. Check for Veracode API credentials (optional)
$hasVeracodeCredentials = ($env:VERACODE_API_ID -ne $null) -and ($env:VERACODE_API_KEY -ne $null)
Write-Host "Veracode API credentials present: $hasVeracodeCredentials"
```

If any script is missing, stop and report the missing file to the user.

---

## Phase 1 — Mainframe File Discovery

Use `search` to locate all mainframe source files. Report the count of each type before scanning.

File extensions to locate:
| Extension | Type        | Description                  |
|-----------|-------------|------------------------------|
| `.cbl`    | COBOL       | Main program source          |
| `.cpy`    | COBOL Copy  | Copybooks / include files    |
| `.cob`    | COBOL       | Alternate COBOL extension    |
| `.jcl`    | JCL         | Job Control Language         |
| `.proc`   | JCL Proc    | Catalogued procedures        |
| `.asm`    | Assembler   | IBM Assembler source         |
| `.bms`    | BMS         | Basic Mapping Support maps   |
| `.map`    | BMS Map     | Screen map definitions       |

If zero mainframe files are found in the specified scope, report this clearly and stop.
Do **not** attempt to scan non-mainframe files.

---

## Phase 2 — Batch Scan Execution

Run the batch script via the `execute` tool. Pass the target directory as the first argument.

```powershell
# Run from workspace root — script handles all file discovery internally
& ".\.github\scripts\scan-mainframe.bat" "<TARGET_DIR>"
```

Replace `<TARGET_DIR>` with the user-supplied directory (default: `.` for full workspace).

**What the script does** (no tokens spent reading source files):
- Recursively finds all mainframe files matching Phase 1 extensions
- Applies ~20 vulnerability rule patterns against every file using PowerShell `Select-String`
- Writes one CSV row per finding to `.github/output/mainframe-scan-<YYYYMMDD>.csv`

**Expected output message** from the script:
```
[SCAN] Target directory : <path>
[SCAN] Files found      : <N> mainframe files
[SCAN] Rules applied    : 20
[SCAN] Findings total   : <N>
[SCAN] Report written   : .github/output/mainframe-scan-<YYYYMMDD>.csv
[SCAN] Done.
```

If the script exits with a non-zero code, read stderr output and report the failure.
Do NOT attempt to re-run the scan manually by reading source files.

---

## Phase 3 — Report Parsing

Read the CSV output file using the `read` tool:

```
.github/output/mainframe-scan-<YYYYMMDD>.csv
```

**CSV columns:**
```
File, Line, Severity, SeverityLabel, CWE, Category, Language, Description, CodeSnippet, Remediation, FalsePositiveRisk
```

Parse the CSV and group findings by severity (Very High → High → Medium → Low → Very Low → Informational).

### False-Positive Mitigation Rules

Apply these checks before including a finding in the final report:

| Rule | Condition | Action |
|------|-----------|--------|
| FP-1 | `ACCEPT` in COPY member (`.cpy`) with no program logic context | Downgrade severity by 1, flag as `[REVIEW]` |
| FP-2 | `DISPLAY` of variable named `WS-DISPLAY-*` or `WRK-DISPLAY-*` without sensitive keywords in VALUE clause | Flag as `[REVIEW]`, lower severity to Informational |
| FP-3 | `EXEC SQL` in a comment line (starts with `*`) | Skip — not executable code |
| FP-4 | PASSWORD field with `VALUE SPACES` or `VALUE LOW-VALUES` | Skip — field is initialized empty, not hardcoded |
| FP-5 | `CALL` literal string program name (quoted) flagged as dynamic CALL | Skip — static call, not dynamic |

---

## Phase 4 — Veracode API Correlation (Optional)

**Skip this phase unless:**
- The user explicitly requested API correlation, OR
- `VERACODE_API_ID` and `VERACODE_API_KEY` environment variables are both set

If skipping, add this note to the report header:
```
Veracode API phase skipped — no credentials found or not requested.
Static scan results are standalone findings.
```

### When to run the API:

```powershell
# Execute veracode-api.ps1 — it detects credentials automatically
& ".\.github\scripts\veracode-api.ps1" -Action "list-apps"
```

Available actions:
| Action          | API Endpoint                                              | Purpose                                 |
|-----------------|-----------------------------------------------------------|-----------------------------------------|
| `list-apps`     | `GET /appsec/v1/applications`                             | List all Veracode application profiles  |
| `get-flaws`     | `GET /appsec/v2/applications/{guid}/findings`             | Fetch existing flaws for an app         |
| `policy-status` | `GET /appsec/v1/applications/{guid}/policy_compliance`    | Check policy pass/fail for an app       |

### Correlation logic:

After fetching API results, match against static findings by:
1. **CWE match**: If a static finding's CWE matches a Veracode flaw CWE → mark as `[CONFIRMED]`
2. **Mitigated flaws**: If Veracode shows the flaw as `MITIGATED` → flag static finding as `[POSSIBLY MITIGATED — verify]`
3. **New findings**: Static findings with no API counterpart → mark as `[NEW — not yet in Veracode]`

---

## Phase 5 — Consolidated Report Output

After all phases, produce a structured Markdown summary in the chat response.

### Report format:

```
# Mainframe Veracode Scan Report
**Scan Date**: <date>
**Target**: <directory>
**Files Scanned**: <N>
**Veracode API**: Used | Not used (reason)

---

## Summary
| Severity     | Count |
|--------------|-------|
| Very High    | N     |
| High         | N     |
| Medium       | N     |
| Low          | N     |
| Very Low     | N     |
| Informational| N     |
| **TOTAL**    | **N** |

---

## Findings

### [VERY HIGH] CWE-XXX — Category Name
**File**: `path/to/file.cbl` **Line**: NN
**Code**: `<flagged snippet>`
**Why it matters**: ...
**Remediation**: ...
**Veracode API status**: CONFIRMED | NEW | POSSIBLY MITIGATED | N/A

[... repeat per finding, grouped by severity ...]

---

## False Positives Dismissed
| File | Line | Rule | Reason |
|------|------|------|--------|

---

## Recommendations
1. Prioritize Very High and High findings for immediate remediation
2. ...
```

---

## CWE Reference — Mainframe-Specific

| CWE     | Name                                      | Common COBOL/JCL Pattern                         |
|---------|-------------------------------------------|--------------------------------------------------|
| CWE-89  | SQL Injection                             | `EXEC SQL` with concatenated host variables       |
| CWE-798 | Hardcoded Credentials                     | `PIC X VALUE 'password'` in WORKING-STORAGE       |
| CWE-259 | Hard-coded Password                       | `PARM='PASSWORD=secret'` in JCL                  |
| CWE-20  | Improper Input Validation                 | `ACCEPT` without subsequent IF/EVALUATE           |
| CWE-312 | Cleartext Storage of Sensitive Info       | `DISPLAY WS-PASSWORD` or `DISPLAY WS-SSN`        |
| CWE-532 | Sensitive Info in Log Files               | `WRITE` record containing PASSWORD/SSN fields     |
| CWE-78  | OS Command Injection                      | `CALL` using variable program name                |
| CWE-732 | Incorrect Permission Assignment           | JCL DSN with no RACF profile on sensitive data   |
| CWE-121 | Stack-based Buffer Overflow               | `MOVE` to shorter PIC field without length check |
| CWE-319 | Cleartext Transmission of Sensitive Info  | CICS SEND/RECEIVE without TLS indicators         |

---

## Veracode API — Is It Necessary?

| Scenario                                                         | API Needed? |
|------------------------------------------------------------------|-------------|
| First-time scan with no existing Veracode platform scans         | No          |
| Team uses Veracode SAST and wants to cross-reference results     | Yes         |
| Checking if a flaw is already acknowledged or mitigated          | Yes         |
| Checking policy compliance status                                | Yes         |
| Pure static analysis of COBOL files with no Veracode license     | No          |

**Default recommendation**: Run static scan only. Add API if the team is already on the Veracode platform.
