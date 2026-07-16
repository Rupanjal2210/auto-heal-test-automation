#Requires -Version 5.1
<#
.SYNOPSIS
    Mainframe Veracode Vulnerability Scanner — PowerShell Scan Engine

.DESCRIPTION
    Scans COBOL (.cbl, .cpy, .cob), JCL (.jcl, .proc), Assembler (.asm),
    and BMS (.bms, .map) files for Veracode-aligned security vulnerabilities
    using static pattern matching. Outputs findings to a CSV file.

    This script is the scan engine called by scan-mainframe.bat.
    It is NOT intended to be called directly by the agent — use the .bat wrapper.

.PARAMETER TargetDir
    Root directory to scan recursively. Default: current working directory.

.PARAMETER OutFile
    Full path to the output CSV file. Default: auto-generated with timestamp.

.EXAMPLE
    .\scan-mainframe-core.ps1 -TargetDir ".\src\mainframe" -OutFile ".\output\findings.csv"
#>
param(
    [string]$TargetDir = ".",
    [string]$OutFile   = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Resolve paths ──────────────────────────────────────────────────────────────
$TargetDir = Resolve-Path -LiteralPath $TargetDir -ErrorAction Stop | Select-Object -ExpandProperty Path

if ([string]::IsNullOrWhiteSpace($OutFile)) {
    $stamp   = Get-Date -Format "yyyyMMdd"
    $outDir  = Join-Path $PSScriptRoot "..\..\output"
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
    $OutFile = Join-Path (Resolve-Path $outDir) "mainframe-scan-$stamp.csv"
}

# ── File extensions to scan ────────────────────────────────────────────────────
$MF_EXTENSIONS = @("*.cbl","*.cpy","*.cob","*.jcl","*.proc","*.asm","*.bms","*.map")

# ── Vulnerability rule definitions ────────────────────────────────────────────
#
#   Each rule:
#     Pattern         : PowerShell regex (case-insensitive)
#     CWE             : Veracode/MITRE CWE identifier
#     Severity        : 0–5  (0=Informational … 5=Very High)
#     SeverityLabel   : Human label
#     Category        : Veracode category name
#     Language        : COBOL | JCL | ASM | ANY
#     Description     : What the pattern detects
#     Remediation     : Fix guidance
#     FalsePositiveRisk: HIGH | MEDIUM | LOW
#
$Rules = @(
    # ── COBOL: SQL Injection ────────────────────────────────────────────────
    @{
        Pattern          = 'EXEC\s+SQL\b.*\b(STRING|CONCATENATE|INSPECT\s+CONVERTING|MOVE)'
        CWE              = 'CWE-89'
        Severity         = 4
        SeverityLabel    = 'High'
        Category         = 'SQL Injection'
        Language         = 'COBOL'
        Description      = 'Dynamic SQL construction using COBOL string operations inside EXEC SQL block — may allow SQL injection if input is not parameterized.'
        Remediation      = 'Use SQL host variables (:WS-VARIABLE) for all dynamic values. Never concatenate user-supplied input into SQL text directly.'
        FalsePositiveRisk= 'MEDIUM'
    },
    @{
        Pattern          = 'EXEC\s+SQL\s+(SELECT|INSERT|UPDATE|DELETE|MERGE).*\+\s*'
        CWE              = 'CWE-89'
        Severity         = 5
        SeverityLabel    = 'Very High'
        Category         = 'SQL Injection'
        Language         = 'COBOL'
        Description      = 'Apparent string concatenation (+) within EXEC SQL statement — very high SQL injection risk.'
        Remediation      = 'Replace concatenation with parameterized host variables. Validate all inputs before use.'
        FalsePositiveRisk= 'LOW'
    },

    # ── COBOL: Hardcoded Credentials ───────────────────────────────────────
    @{
        Pattern          = '(PASSWORD|PASSWD|CRED|SECRET|API[-_]KEY|ACCESS[-_]KEY|AUTH[-_]TOKEN)\s+PIC\s+X.+VALUE\s+[''"].{2,}[''"]'
        CWE              = 'CWE-798'
        Severity         = 5
        SeverityLabel    = 'Very High'
        Category         = 'Hardcoded Credentials'
        Language         = 'COBOL'
        Description      = 'Hard-coded credential value detected in WORKING-STORAGE or LOCAL-STORAGE data definition.'
        Remediation      = 'Store credentials in an external vault (e.g., RACF keyring, IBM Secrets Manager). Initialize field from environment at runtime via CICS GET CONTAINER or program parameter.'
        FalsePositiveRisk= 'LOW'
    },
    @{
        Pattern          = '(USER[-_]?ID|LOGON[-_]?ID|USERNAME|OPERATOR[-_]?ID)\s+PIC\s+X.+VALUE\s+[''"][A-Z0-9]{3,}[''"]'
        CWE              = 'CWE-798'
        Severity         = 4
        SeverityLabel    = 'High'
        Category         = 'Hardcoded Credentials'
        Language         = 'COBOL'
        Description      = 'Hard-coded user identifier detected in data definition.'
        Remediation      = 'Remove hard-coded user IDs. Pass identity through runtime parameters or RACF-protected data sets.'
        FalsePositiveRisk= 'MEDIUM'
    },

    # ── COBOL: Cleartext Sensitive Data ────────────────────────────────────
    @{
        Pattern          = 'DISPLAY\s+.*(WS-|WRK-|LS-)?(PASSWORD|PASSWD|SSN|SOC[-_]SEC|ACCT[-_]NUM|CARD[-_]NUM|PAN\b|PIN\b|CVV)'
        CWE              = 'CWE-312'
        Severity         = 4
        SeverityLabel    = 'High'
        Category         = 'Cleartext Exposure'
        Language         = 'COBOL'
        Description      = 'Sensitive field written to DISPLAY (terminal or SYSOUT) in cleartext.'
        Remediation      = 'Mask or suppress sensitive fields before display. Use INSPECT REPLACING or substring to obscure PAN/SSN values.'
        FalsePositiveRisk= 'MEDIUM'
    },
    @{
        Pattern          = 'WRITE\s+\S+-RECORD\s+FROM\s+.*(PASSWORD|PASSWD|SSN|PAN\b|CARD|PIN\b)'
        CWE              = 'CWE-532'
        Severity         = 3
        SeverityLabel    = 'Medium'
        Category         = 'Sensitive Data in Logs'
        Language         = 'COBOL'
        Description      = 'Record containing sensitive field is written to a file — may persist sensitive data in logs or flat files.'
        Remediation      = 'Mask or exclude sensitive fields from file records. Encrypt at-rest storage for files containing PAN/SSN.'
        FalsePositiveRisk= 'MEDIUM'
    },

    # ── COBOL: Improper Input Validation ───────────────────────────────────
    @{
        Pattern          = '^\s{6,}ACCEPT\s+\S+\s*\.'
        CWE              = 'CWE-20'
        Severity         = 2
        SeverityLabel    = 'Low'
        Category         = 'Missing Input Validation'
        Language         = 'COBOL'
        Description      = 'ACCEPT statement with no inline validation. Input accepted from terminal without verified bounds or type check on the same line.'
        Remediation      = 'Follow every ACCEPT with an IF/EVALUATE block validating length, type, and allowed value ranges before use.'
        FalsePositiveRisk= 'HIGH'
    },

    # ── COBOL: Dynamic CALL (Command Injection risk) ────────────────────────
    @{
        Pattern          = 'CALL\s+(WS-|WRK-|LS-|INP-)\S+'
        CWE              = 'CWE-78'
        Severity         = 3
        SeverityLabel    = 'Medium'
        Category         = 'Dynamic Program Call'
        Language         = 'COBOL'
        Description      = 'CALL target is a working-storage variable — program name resolved at runtime. If sourced from unvalidated input, an attacker could redirect execution.'
        Remediation      = 'Validate the program name against an explicit allowlist before calling. Prefer static CALL literals where possible.'
        FalsePositiveRisk= 'MEDIUM'
    },

    # ── COBOL: Insecure Copybook inclusion of sensitive field definitions ───
    @{
        Pattern          = 'COPY\s+(SEC|AUTH|CRED|PASSWD|KEYS)\S*\s*\.'
        CWE              = 'CWE-798'
        Severity         = 2
        SeverityLabel    = 'Low'
        Category         = 'Sensitive Copybook Reference'
        Language         = 'COBOL'
        Description      = 'Copybook with a security-related name is included — review for hardcoded credentials or sensitive data structures.'
        Remediation      = 'Audit the referenced copybook for hardcoded values. Ensure credential copybooks are not distributed with source.'
        FalsePositiveRisk= 'HIGH'
    },

    # ── COBOL: Cleartext network / CICS transmission ───────────────────────
    @{
        Pattern          = 'EXEC\s+CICS\s+SEND\s+.*(PASSWORD|PASSWD|PAN\b|SSN|PIN\b)'
        CWE              = 'CWE-319'
        Severity         = 4
        SeverityLabel    = 'High'
        Category         = 'Cleartext Transmission'
        Language         = 'COBOL'
        Description      = 'CICS SEND containing sensitive field — may transmit PAN/password in cleartext over VTAM or TCP/IP channel.'
        Remediation      = 'Ensure CICS communication uses AT-TLS (Application Transparent TLS) policies. Mask sensitive fields before transmitting to terminals.'
        FalsePositiveRisk= 'LOW'
    },

    # ── COBOL: MOVE overflow risk ───────────────────────────────────────────
    @{
        Pattern          = 'MOVE\s+\S+\s+TO\s+(WS-|WRK-)?[A-Z0-9-]+\s*\(?\d*\)?\s*\.\s*$'
        CWE              = 'CWE-121'
        Severity         = 1
        SeverityLabel    = 'Very Low'
        Category         = 'Potential Buffer Truncation'
        Language         = 'COBOL'
        Description      = 'MOVE statement — verify source field length does not exceed target PIC definition to prevent silent truncation.'
        Remediation      = 'Add length validation before MOVE where source size may vary. Use STRING DELIMITED SIZE for controlled truncation.'
        FalsePositiveRisk= 'HIGH'
    },

    # ── JCL: Hardcoded password in PARM ────────────────────────────────────
    @{
        Pattern          = "PARM\s*=\s*['""]?.*(PASSWORD|PASSWD|PWD|SECRET)\s*=\s*[A-Za-z0-9!@#$%^&*]{3,}"
        CWE              = 'CWE-259'
        Severity         = 5
        SeverityLabel    = 'Very High'
        Category         = 'Hardcoded Password in JCL'
        Language         = 'JCL'
        Description      = 'Password value exposed in JCL PARM field — visible in job log and spool output.'
        Remediation      = 'Replace inline password with a RACF-protected data set or IBM MFA/password phrase mechanism. Never pass credentials via PARM.'
        FalsePositiveRisk= 'LOW'
    },

    # ── JCL: Hardcoded credentials in DSN path ─────────────────────────────
    @{
        Pattern          = 'DSN=[A-Z0-9.]*\.(PASSWD|PASSWORD|SECRET|CREDENTIAL|KEYS)\b'
        CWE              = 'CWE-798'
        Severity         = 3
        SeverityLabel    = 'Medium'
        Category         = 'Sensitive Dataset Name'
        Language         = 'JCL'
        Description      = 'Dataset name suggests it contains credentials or keys. Verify RACF discrete profile protects this DSN.'
        Remediation      = 'Apply RACF DATASET profile with UACC(NONE). Audit who has READ/UPDATE access. Consider IBM ICSF for key storage.'
        FalsePositiveRisk= 'MEDIUM'
    },

    # ── JCL: Sensitive production DSN without RACF indicator ───────────────
    @{
        Pattern          = 'DSN=[A-Z0-9.]*\.(PAYROLL|FINANCE|HR|TAX|PENSION|ACCTG)\b'
        CWE              = 'CWE-732'
        Severity         = 2
        SeverityLabel    = 'Low'
        Category         = 'Sensitive Dataset Access'
        Language         = 'JCL'
        Description      = 'JCL accesses a dataset in a sensitive functional area. Verify RACF profile exists and access is appropriately restricted.'
        Remediation      = 'Confirm RACF DATASET profile exists with UACC(NONE). Review job owner authority. Apply least-privilege access.'
        FalsePositiveRisk= 'MEDIUM'
    },

    # ── JCL: DISP=SHR on potentially sensitive DSN ─────────────────────────
    @{
        Pattern          = 'DISP\s*=\s*\(?SHR\b.+DSN=[A-Z0-9.]*\.(PROD|MASTER|MSTR|CNTL|CRED)'
        CWE              = 'CWE-732'
        Severity         = 2
        SeverityLabel    = 'Low'
        Category         = 'Shared Access to Sensitive Dataset'
        Language         = 'JCL'
        Description      = 'DISP=SHR used on a production or control dataset — allows concurrent read by other jobs. Verify this is intentional.'
        Remediation      = 'Use DISP=OLD for exclusive access to critical datasets during updates. Ensure RACF profile restricts to authorized jobs only.'
        FalsePositiveRisk= 'HIGH'
    },

    # ── JCL: Symbolic parameter substitution into sensitive fields ──────────
    @{
        Pattern          = '&(PASSWORD|PASSWD|SECRET|API_KEY|TOKEN)\b'
        CWE              = 'CWE-259'
        Severity         = 3
        SeverityLabel    = 'Medium'
        Category         = 'JCL Symbolic Parameter — Sensitive Value'
        Language         = 'JCL'
        Description      = 'JCL symbolic (&&VAR) used for a sensitive parameter. Value may be logged in JES2/JES3 job logs and spool.'
        Remediation      = 'Avoid passing passwords via JCL symbolics. Use RACF keyring or IBM ICSF for secret distribution.'
        FalsePositiveRisk= 'LOW'
    },

    # ── Assembler: Cleartext literal in storage ────────────────────────────
    @{
        Pattern          = "DC\s+C['""][^'""]{4,}(PASS|PWD|SECRET|KEY)[^'""]{0,30}['""]"
        CWE              = 'CWE-798'
        Severity         = 4
        SeverityLabel    = 'High'
        Category         = 'Hardcoded Secret in Assembler DC'
        Language         = 'ASM'
        Description      = 'DC constant in Assembler source contains what appears to be a hardcoded credential or key string.'
        Remediation      = 'Remove hardcoded strings. Load sensitive values from a protected VSAM keystore or RACF keyring at program initialization.'
        FalsePositiveRisk= 'MEDIUM'
    },

    # ── Any: Commented-out password ────────────────────────────────────────
    @{
        Pattern          = '[\*\!]\s*(PASSWORD|PASSWD|PWD|SECRET)\s*=\s*\S{3,}'
        CWE              = 'CWE-615'
        Severity         = 1
        SeverityLabel    = 'Very Low'
        Category         = 'Credential in Comment'
        Language         = 'ANY'
        Description      = 'Commented-out line contains what appears to be a password or secret value. Comments are visible to anyone with source access.'
        Remediation      = 'Remove all credential values from comments and source code. Rotate the exposed secret immediately.'
        FalsePositiveRisk= 'LOW'
    }
)

# ── Pre-compile regex patterns and group rules by language ────────────────────
# Compiled regexes are reused across every file — avoids JIT overhead per Select-String call.
$regexOpts = [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor `
             [System.Text.RegularExpressions.RegexOptions]::Compiled
$rulesByLang = @{
    COBOL = [System.Collections.Generic.List[hashtable]]::new()
    JCL   = [System.Collections.Generic.List[hashtable]]::new()
    ASM   = [System.Collections.Generic.List[hashtable]]::new()
    BMS   = [System.Collections.Generic.List[hashtable]]::new()
    ANY   = [System.Collections.Generic.List[hashtable]]::new()
}
foreach ($rule in $Rules) {
    $rule['_Regex'] = [System.Text.RegularExpressions.Regex]::new($rule.Pattern, $regexOpts)
    if ($rulesByLang.ContainsKey($rule.Language)) { $rulesByLang[$rule.Language].Add($rule) }
}

# ── Helper: language detection by extension ────────────────────────────────────
function Get-Language([string]$ext) {
    switch ($ext.ToLower()) {
        '.cbl'  { return 'COBOL' }
        '.cpy'  { return 'COBOL' }
        '.cob'  { return 'COBOL' }
        '.jcl'  { return 'JCL'   }
        '.proc' { return 'JCL'   }
        '.asm'  { return 'ASM'   }
        '.bms'  { return 'BMS'   }
        '.map'  { return 'BMS'   }
        default { return 'UNKNOWN' }
    }
}

# ── Helper: escape CSV field ───────────────────────────────────────────────────
function ConvertTo-CsvField([string]$value) {
    $escaped = $value -replace '"', '""'
    return "`"$escaped`""
}

# ── Discover files ─────────────────────────────────────────────────────────────
Write-Host "[SCAN] Target directory : $TargetDir"

# Single-pass discovery — avoids 8 separate Get-ChildItem calls and O(n²) array growth
$extSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
'.cbl','.cpy','.cob','.jcl','.proc','.asm','.bms','.map' | ForEach-Object { [void]$extSet.Add($_) }

$allFiles = Get-ChildItem -LiteralPath $TargetDir -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $extSet.Contains($_.Extension) }

$fileCount = $allFiles.Count
Write-Host "[SCAN] Files found      : $fileCount mainframe files"

if ($fileCount -eq 0) {
    Write-Warning "[SCAN] No mainframe files found in '$TargetDir'. Scan complete with zero findings."
    # Write empty CSV with headers only
    $headers = "File,Line,Severity,SeverityLabel,CWE,Category,Language,Description,CodeSnippet,Remediation,FalsePositiveRisk"
    $headers | Out-File -FilePath $OutFile -Encoding utf8 -Force
    Write-Host "[SCAN] Empty report written: $OutFile"
    exit 0
}

Write-Host "[SCAN] Rules applied    : $($Rules.Count)"

# ── Run scan ───────────────────────────────────────────────────────────────────
# Each file is read once; compiled regexes are applied in-memory instead of
# calling Select-String (which re-opens the file) once per rule.
$findings = [System.Collections.Generic.List[hashtable]]::new()
$sw = [System.Diagnostics.Stopwatch]::StartNew()

foreach ($file in $allFiles) {
    $fileLang = Get-Language $file.Extension
    $relPath  = $file.FullName.Substring($TargetDir.Length).TrimStart([char]'\', [char]'/')

    # Build applicable rule list for this language — skips irrelevant rules per file
    $applicable = [System.Collections.Generic.List[hashtable]]::new()
    if ($rulesByLang.ContainsKey($fileLang)) { $applicable.AddRange($rulesByLang[$fileLang]) }
    if ($rulesByLang['ANY'].Count -gt 0)     { $applicable.AddRange($rulesByLang['ANY']) }
    if ($applicable.Count -eq 0) { continue }

    # Read file once — was N_rules I/O reads per file, now 1
    try {
        $lines = [System.IO.File]::ReadAllLines($file.FullName)
    } catch {
        Write-Warning "[SCAN] Cannot read '$relPath': $_"
        continue
    }

    for ($lineIdx = 0; $lineIdx -lt $lines.Length; $lineIdx++) {
        $rawLine = $lines[$lineIdx]

        # Skip COBOL fixed-format comment lines (column 7 = '*' or '/')
        if ($fileLang -eq 'COBOL' -and $rawLine.Length -ge 7) {
            $col7 = $rawLine[6]
            if ($col7 -eq '*' -or $col7 -eq '/') { continue }
        }

        foreach ($rule in $applicable) {
            if (-not $rule['_Regex'].IsMatch($rawLine)) { continue }

            $snippet = $rawLine.Trim()
            if ($snippet.Length -gt 120) { $snippet = $snippet.Substring(0, 117) + '...' }

            $findings.Add(@{
                File             = $relPath
                Line             = $lineIdx + 1
                Severity         = $rule.Severity
                SeverityLabel    = $rule.SeverityLabel
                CWE              = $rule.CWE
                Category         = $rule.Category
                Language         = $fileLang
                Description      = $rule.Description
                CodeSnippet      = $snippet
                Remediation      = $rule.Remediation
                FalsePositiveRisk= $rule.FalsePositiveRisk
            })
        }
    }
}

$sw.Stop()
$totalFindings = $findings.Count
Write-Host "[SCAN] Findings total   : $totalFindings"
Write-Host "[SCAN] Scan elapsed     : $($sw.Elapsed.TotalSeconds.ToString('F1'))s"

# ── Write CSV ──────────────────────────────────────────────────────────────────
# StreamWriter writes rows directly to disk — avoids buffering the entire CSV in memory.
$writer = [System.IO.StreamWriter]::new($OutFile, $false, [System.Text.Encoding]::UTF8)
try {
    $writer.WriteLine("File,Line,Severity,SeverityLabel,CWE,Category,Language,Description,CodeSnippet,Remediation,FalsePositiveRisk")
    foreach ($f in $findings) {
        $writer.WriteLine((@(
            (ConvertTo-CsvField $f.File),
            $f.Line,
            $f.Severity,
            (ConvertTo-CsvField $f.SeverityLabel),
            (ConvertTo-CsvField $f.CWE),
            (ConvertTo-CsvField $f.Category),
            (ConvertTo-CsvField $f.Language),
            (ConvertTo-CsvField $f.Description),
            (ConvertTo-CsvField $f.CodeSnippet),
            (ConvertTo-CsvField $f.Remediation),
            (ConvertTo-CsvField $f.FalsePositiveRisk)
        ) -join ","))
    }
} finally {
    $writer.Close()
}

Write-Host "[SCAN] Report written   : $OutFile"
Write-Host "[SCAN] Done."
exit 0
