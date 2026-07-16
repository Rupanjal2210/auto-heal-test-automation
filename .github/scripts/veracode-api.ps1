#Requires -Version 5.1
<#
.SYNOPSIS
    Veracode REST API Client — curl-based, HMAC-SHA256 authenticated

.DESCRIPTION
    Calls the Veracode REST API v2 using curl (with HMAC-SHA256 auth header
    generated in PowerShell). No web browser tool required.

    IMPORTANT — This script is OPTIONAL. It is only needed when:
      - Your team already has application profiles on the Veracode platform, AND
      - You want to correlate static scan findings with Veracode platform results.

    For pure static scanning of COBOL/JCL files, use scan-mainframe.bat instead.

.PREREQUISITES
    - VERACODE_API_ID  : Set as environment variable (your Veracode API ID)
    - VERACODE_API_KEY : Set as environment variable (your Veracode API key, hex string)
    - curl must be on PATH (curl.exe ships with Windows 10 1803+)

.PARAMETER Action
    The API action to perform:
      list-apps     — List all application profiles (GET /appsec/v1/applications)
      get-flaws     — Fetch findings for an app   (GET /appsec/v2/applications/{guid}/findings)
      policy-status — Policy compliance status     (GET /appsec/v1/applications/{guid}/policy_compliance)

.PARAMETER AppGuid
    Required for get-flaws and policy-status. The Veracode application GUID
    (obtain from list-apps first).

.PARAMETER SeverityFilter
    For get-flaws: filter by numeric severity (0–5). Default: all severities.

.PARAMETER OutputFile
    Optional. Save raw JSON response to this file for offline analysis.

.EXAMPLE
    # Step 1 — list all apps to get GUIDs
    .\veracode-api.ps1 -Action list-apps

    # Step 2 — get findings for a specific app
    .\veracode-api.ps1 -Action get-flaws -AppGuid "abc123de-f456-..." -SeverityFilter 4

    # Step 3 — check policy compliance
    .\veracode-api.ps1 -Action policy-status -AppGuid "abc123de-f456-..."
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("list-apps","get-flaws","policy-status")]
    [string]$Action,

    [string]$AppGuid        = "",
    [int]   $SeverityFilter = -1,       # -1 = no filter
    [string]$OutputFile     = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Constants ──────────────────────────────────────────────────────────────────
$VERACODE_BASE_URL = "https://api.veracode.com"
$API_VERSION_TAG   = "vcode_request_version_1"

# ── Credential check ───────────────────────────────────────────────────────────
$apiId  = $env:VERACODE_API_ID
$apiKey = $env:VERACODE_API_KEY

if ([string]::IsNullOrWhiteSpace($apiId) -or [string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Error @"
[VERACODE-API] Credentials not found.

Set the following environment variables before running this script:
  `$env:VERACODE_API_ID  = "your-api-id"
  `$env:VERACODE_API_KEY = "your-api-key-hex-string"

These are available in the Veracode Platform under:
  My Profile > API Credentials > Generate API Credentials

NOTE: The Veracode API is optional for mainframe static scanning.
If you only need static COBOL/JCL analysis, use scan-mainframe.bat instead.
"@
    exit 1
}

# ── curl availability check ────────────────────────────────────────────────────
if (-not (Get-Command "curl" -ErrorAction SilentlyContinue)) {
    Write-Error "[VERACODE-API] curl not found on PATH. curl.exe ships with Windows 10 1803+. Install from https://curl.se if missing."
    exit 1
}

# ── HMAC-SHA256 helper ─────────────────────────────────────────────────────────
function Invoke-HmacSha256 {
    param([byte[]]$Key, [byte[]]$Data)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($Key)
    return $hmac.ComputeHash($Data)
}

# ── Convert hex string to byte array ──────────────────────────────────────────
function ConvertFrom-HexString {
    param([string]$HexStr)
    $bytes = [byte[]]::new($HexStr.Length / 2)
    for ($i = 0; $i -lt $HexStr.Length; $i += 2) {
        $bytes[$i / 2] = [Convert]::ToByte($HexStr.Substring($i, 2), 16)
    }
    return $bytes
}

# ── Convert byte array to lowercase hex string ────────────────────────────────
function ConvertTo-HexString {
    param([byte[]]$Bytes)
    return -join ($Bytes | ForEach-Object { $_.ToString("x2") })
}

# ── Build Veracode HMAC-SHA256 Authorization header ───────────────────────────
#
#   Algorithm (per official Veracode docs):
#     1. Generate 16 random bytes as nonce
#     2. Get UTC milliseconds timestamp as string
#     3. Build signing_data string
#     4. Derive key chain: apiKey → key_nonce → key_date → key_sig → signature
#     5. Format header value
#
function New-VeracodeAuthHeader {
    param(
        [string]$ApiId,
        [string]$ApiKey,     # hex-encoded 128-char key
        [string]$Url,
        [string]$Method = "GET"
    )

    $uri      = [Uri]$Url
    $host     = $uri.Host
    $urlPath  = $uri.PathAndQuery

    # Random nonce (16 bytes)
    $nonceBytes = [byte[]]::new(16)
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($nonceBytes)
    $nonceHex = ConvertTo-HexString $nonceBytes

    # Millisecond-precision UTC timestamp
    $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()

    # Signing data string
    $signingData = "id=$ApiId&host=$host&url=$urlPath&method=$($Method.ToUpper())"

    # Key derivation chain
    $apiKeyBytes = ConvertFrom-HexString $ApiKey
    $keyNonce    = Invoke-HmacSha256 -Key $apiKeyBytes                                               -Data $nonceBytes
    $keyDate     = Invoke-HmacSha256 -Key $keyNonce                                                  -Data ([Text.Encoding]::UTF8.GetBytes($timestamp))
    $keySig      = Invoke-HmacSha256 -Key $keyDate                                                   -Data ([Text.Encoding]::UTF8.GetBytes($API_VERSION_TAG))
    $signature   = Invoke-HmacSha256 -Key $keySig                                                    -Data ([Text.Encoding]::UTF8.GetBytes($signingData))

    $sigHex = ConvertTo-HexString $signature

    return "VERACODE-HMAC-SHA-256 id=$ApiId,ts=$timestamp,nonce=$nonceHex,sig=$sigHex"
}

# ── curl invocation wrapper ────────────────────────────────────────────────────
function Invoke-VeracodeApi {
    param(
        [string]$Url,
        [string]$Method    = "GET",
        [string]$QueryDesc = ""
    )

    Write-Host "[VERACODE-API] $QueryDesc"
    Write-Host "[VERACODE-API] $Method $Url"

    $authHeader = New-VeracodeAuthHeader -ApiId $apiId -ApiKey $apiKey -Url $Url -Method $Method

    # Build curl arguments
    $curlArgs = @(
        "-s",                                   # silent mode (no progress meter)
        "-X", $Method,
        $Url,
        "-H", "Authorization: $authHeader",
        "-H", "Content-Type: application/json",
        "-H", "Accept: application/json",
        "--max-time", "30",                     # 30s timeout
        "-w", "\n[HTTP_STATUS:%{http_code}]"   # append status code to output
    )

    $rawOutput = & curl @curlArgs 2>&1
    $exitCode  = $LASTEXITCODE

    if ($exitCode -ne 0) {
        Write-Error "[VERACODE-API] curl failed with exit code $exitCode. Check network connectivity and credentials."
        return $null
    }

    # Extract HTTP status code from appended marker
    $statusMatch = [regex]::Match($rawOutput, '\[HTTP_STATUS:(\d+)\]$')
    $httpStatus  = if ($statusMatch.Success) { [int]$statusMatch.Groups[1].Value } else { 0 }
    $jsonBody    = $rawOutput -replace '\[HTTP_STATUS:\d+\]\s*$', ''

    Write-Host "[VERACODE-API] HTTP $httpStatus"

    if ($httpStatus -eq 401) {
        Write-Error "[VERACODE-API] HTTP 401 Unauthorized — check VERACODE_API_ID and VERACODE_API_KEY values."
        return $null
    }
    if ($httpStatus -eq 403) {
        Write-Error "[VERACODE-API] HTTP 403 Forbidden — your API credentials do not have permission for this endpoint."
        return $null
    }
    if ($httpStatus -lt 200 -or $httpStatus -ge 300) {
        Write-Warning "[VERACODE-API] Unexpected HTTP $httpStatus — response: $jsonBody"
        return $null
    }

    # Optionally save raw JSON
    if (-not [string]::IsNullOrWhiteSpace($OutputFile)) {
        $jsonBody | Out-File -FilePath $OutputFile -Encoding utf8 -Force
        Write-Host "[VERACODE-API] Raw response saved to: $OutputFile"
    }

    try {
        return $jsonBody | ConvertFrom-Json
    } catch {
        Write-Warning "[VERACODE-API] Response is not valid JSON. Raw output saved if -OutputFile was specified."
        return $jsonBody
    }
}

# ── Actions ────────────────────────────────────────────────────────────────────

switch ($Action) {

    # ── list-apps ───────────────────────────────────────────────────────────
    "list-apps" {
        $url  = "$VERACODE_BASE_URL/appsec/v1/applications?size=500"
        $data = Invoke-VeracodeApi -Url $url -QueryDesc "Listing all Veracode application profiles"

        if ($null -eq $data) { exit 1 }

        $apps = $data._embedded.applications
        if ($null -eq $apps -or $apps.Count -eq 0) {
            Write-Host "[VERACODE-API] No application profiles found on this account."
            exit 0
        }

        Write-Host "`n[VERACODE-API] Found $($apps.Count) application(s):`n"
        $apps | ForEach-Object {
            $guid    = $_.guid
            $name    = $_.profile.name
            $policy  = $_.profile.policies[0].name
            $lastScan = if ($_.last_completed_scan_date) { $_.last_completed_scan_date } else { "Never" }
            Write-Host "  GUID  : $guid"
            Write-Host "  Name  : $name"
            Write-Host "  Policy: $policy"
            Write-Host "  Last Scan: $lastScan"
            Write-Host "  ---"
        }
        Write-Host "[VERACODE-API] Use one of the GUIDs above with -Action get-flaws or policy-status"
    }

    # ── get-flaws ───────────────────────────────────────────────────────────
    "get-flaws" {
        if ([string]::IsNullOrWhiteSpace($AppGuid)) {
            Write-Error "[VERACODE-API] -AppGuid is required for action 'get-flaws'. Run -Action list-apps first to get the GUID."
            exit 1
        }

        $url = "$VERACODE_BASE_URL/appsec/v2/applications/$AppGuid/findings?size=500&violates_policy=true"
        if ($SeverityFilter -ge 0) {
            $url += "&severity=$SeverityFilter"
            Write-Host "[VERACODE-API] Filtering by severity: $SeverityFilter"
        }

        $data = Invoke-VeracodeApi -Url $url -QueryDesc "Fetching findings for app GUID: $AppGuid"
        if ($null -eq $data) { exit 1 }

        $findings = $data._embedded.findings
        if ($null -eq $findings -or $findings.Count -eq 0) {
            Write-Host "[VERACODE-API] No findings returned for app GUID $AppGuid (with current filters)."
            exit 0
        }

        Write-Host "`n[VERACODE-API] $($findings.Count) finding(s) returned:`n"

        $findings | Sort-Object { $_.finding_details.severity } -Descending | ForEach-Object {
            $sev       = $_.finding_details.severity
            $cwe       = $_.finding_details.cwe.id
            $cweName   = $_.finding_details.cwe.name
            $category  = $_.finding_details.finding_category.name
            $file      = $_.finding_details.file_name
            $lineNum   = $_.finding_details.file_line_number
            $status    = $_.finding_status.status
            $mitStatus = $_.finding_status.mitigation_review_status

            Write-Host "  Severity : $sev | CWE-$cwe ($cweName)"
            Write-Host "  Category : $category"
            Write-Host "  File     : $file : $lineNum"
            Write-Host "  Status   : $status | Mitigation: $mitStatus"
            Write-Host "  ---"
        }
    }

    # ── policy-status ───────────────────────────────────────────────────────
    "policy-status" {
        if ([string]::IsNullOrWhiteSpace($AppGuid)) {
            Write-Error "[VERACODE-API] -AppGuid is required for action 'policy-status'. Run -Action list-apps first."
            exit 1
        }

        $url  = "$VERACODE_BASE_URL/appsec/v1/applications/$AppGuid/policy_compliance"
        $data = Invoke-VeracodeApi -Url $url -QueryDesc "Checking policy compliance for app GUID: $AppGuid"
        if ($null -eq $data) { exit 1 }

        $compliance = $data.policy_compliance_status
        $policyName = $data.policy_name
        $scanDate   = $data.last_policy_evaluation_date

        Write-Host "`n[VERACODE-API] Policy Compliance Result"
        Write-Host "  Policy    : $policyName"
        Write-Host "  Status    : $compliance"
        Write-Host "  Evaluated : $scanDate"

        if ($compliance -eq "PASS") {
            Write-Host "  Result    : PASS — Application meets Veracode policy requirements."
        } else {
            Write-Host "  Result    : FAIL — Application does NOT meet policy. Review findings above."
        }
    }
}

exit 0
