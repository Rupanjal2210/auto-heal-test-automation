#!/usr/bin/env bash
# detect-manifest-edit.sh
# PostToolUse hook — fires after every tool call.
# If the edited file is a dependency manifest, injects a system message
# asking the Dependency Auto-Healer agent to scan and fix it.

# ── Read stdin (JSON payload from the agent runtime) ─────────────────────────
raw=$(cat 2>/dev/null)
[ -z "$raw" ] && exit 0

# ── Parse with python (available on all macOS/Linux) ─────────────────────────
result=$(python3 - <<'PYEOF' "$raw"
import sys, json

raw = sys.argv[1] if len(sys.argv) > 1 else ""
try:
    payload = json.loads(raw)
except Exception:
    sys.exit(0)

tool_name  = payload.get("tool_name", "")
tool_input = payload.get("tool_input", {})
file_path  = (
    tool_input.get("filePath") or
    tool_input.get("path") or
    tool_input.get("file_path") or ""
)

EDIT_TOOLS = {
    "replace_string_in_file",
    "multi_replace_string_in_file",
    "edit_file",
    "create_file",
    "write_file",
}
if tool_name not in EDIT_TOOLS:
    sys.exit(0)

import re
MANIFEST_PATTERNS = [
    r"pom\.xml$",
    r"build\.gradle(\.kts)?$",
    r"package\.json$",
    r"requirements\.txt$",
    r"pyproject\.toml$",
    r"go\.mod$",
    r"Cargo\.toml$",
    r".*\.csproj$",
    r"composer\.json$",
]
if not any(re.search(p, file_path) for p in MANIFEST_PATTERNS):
    sys.exit(0)

import os
manifest_file = os.path.basename(file_path)
from datetime import date
today = date.today().strftime("%Y%m%d")

msg = (
    f"A dependency manifest was just modified: `{manifest_file}`\n\n"
    "Invoke the **Dependency Auto-Healer** agent now:\n"
    f"1. Re-read `{file_path}` to detect any newly added or changed dependencies\n"
    "2. Query OSV.dev / GitHub Advisory Database for CVEs against those dependencies\n"
    "3. If any are vulnerable, resolve the minimum safe version, apply the fix, and run the build\n"
    f"4. Append new findings to `.github/output/dependency-heal-report-{today}.csv`\n\n"
    "Do not ask for confirmation — proceed automatically."
)
print(json.dumps({"systemMessage": msg}))
PYEOF
)

[ -n "$result" ] && echo "$result"
exit 0
