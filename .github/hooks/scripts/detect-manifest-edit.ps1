# detect-manifest-edit.ps1
# PostToolUse hook — fires after every tool call.
# If the edited file is a dependency manifest, injects a system message
# asking the Dependency Auto-Healer agent to scan and fix it.

# ── Read stdin (JSON payload from the agent runtime) ─────────────────────────
$raw = $null
try {
    $raw = [Console]::In.ReadToEnd()
} catch {
    exit 0   # No stdin — not a tool-use context; exit cleanly
}

if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }

# ── Parse payload ─────────────────────────────────────────────────────────────
$payload = $null
try {
    $payload = $raw | ConvertFrom-Json
} catch {
    exit 0   # Malformed JSON; do nothing
}

# ── Identify the tool and the file path it touched ───────────────────────────
$toolName = $payload.tool_name
$filePath = $payload.tool_input.filePath `
            ?? $payload.tool_input.path `
            ?? $payload.tool_input.file_path `
            ?? ""

# Only act on file-edit tools
$editTools = @(
    "replace_string_in_file",
    "multi_replace_string_in_file",
    "edit_file",
    "create_file",
    "write_file"
)
if ($toolName -notin $editTools) { exit 0 }

# ── Manifest filename patterns ────────────────────────────────────────────────
$manifestPatterns = @(
    "pom\.xml$",
    "build\.gradle(\.kts)?$",
    "package\.json$",
    "requirements\.txt$",
    "pyproject\.toml$",
    "go\.mod$",
    "Cargo\.toml$",
    ".*\.csproj$",
    "composer\.json$"
)

$isManifest = $false
foreach ($pattern in $manifestPatterns) {
    if ($filePath -match $pattern) {
        $isManifest = $true
        break
    }
}

if (-not $isManifest) { exit 0 }

# ── Inject system message to trigger the Dependency Auto-Healer ──────────────
$manifestFile = Split-Path -Leaf $filePath

$output = @{
    systemMessage = @"
A dependency manifest was just modified: ``$manifestFile``

Invoke the **Dependency Auto-Healer** agent now:
1. Re-read ``$filePath`` to detect any newly added or changed dependencies
2. Query OSV.dev / GitHub Advisory Database for CVEs against those dependencies
3. If any are vulnerable, resolve the minimum safe version, apply the fix, and run the build
4. Append new findings to ``.github/output/dependency-heal-report-<YYYYMMDD>.csv``

Do not ask for confirmation — proceed automatically.
"@
} | ConvertTo-Json -Compress

Write-Output $output

exit 0
