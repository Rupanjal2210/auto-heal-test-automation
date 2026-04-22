# QA Code Review Criteria

This skill file is loaded by the QA Automation Expert agent during Stage 1. Apply every dimension below to each source file in scope.

---

## Dimensions

### 1. Design
- Single responsibility? Right abstraction for the problem?
- Dependencies injected or hardcoded (`new X()` inside methods)?
- Testability considered — can each class be unit tested in isolation?
- SOLID principles followed?

### 2. Functionality
- Does the code do what its Javadoc/comments claim?
- Are nulls, empty collections, and boundary values handled?
- Are exceptions caught purposefully or silently swallowed?

### 3. Complexity
- Methods doing more than one thing?
- Nesting deeper than 3 levels?
- Could logic be simplified or broken into smaller methods?

### 4. Tests
- Are there tests? Do they cover the main behaviors and edge cases?
- Are tests readable and well-named?
- Do tests verify behavior, not just that code runs?

### 5. Naming
- Descriptive, self-documenting names?
- Follows language conventions (lowerCamelCase for Java methods, snake_case for Python)?
- Booleans phrased as questions (`isEnabled`, `hasResults`)?

### 6. Comments & Documentation
- Javadocs/docstrings on all public APIs?
- Comments explain *why*, not *what*?
- Stale, misleading, or redundant comments?

### 7. Style
- Magic numbers or strings that should be named constants?
- Import ordering per style guide?
- Consistent formatting and line length?

### 8. Security (Surface Scan)
- String-concatenated queries?
- User input passed to exec/logs?
- Hardcoded credentials?

---

## QA Automation-Specific Checks

Apply these additional checks to test automation code:

### Test Design Patterns
- **Page Object Model**: Are page interactions abstracted into page objects, or are locators/actions scattered in test methods?
- **Factory / Builder for test data**: Is test data created via builders/factories, or hardcoded inline?
- **Screenplay pattern**: For complex flows, is the Screenplay pattern considered?

### Driver / Client Lifecycle
- Is WebDriver (or equivalent client) created in setup and quit in teardown?
- Are there resource leaks — drivers opened but never closed on failure paths?
- Is `WebDriverManager` or equivalent used for driver binary management?

### Wait Strategy
- `Thread.sleep()` → always flag as `[MAJOR]` — use explicit or fluent waits instead
- Implicit waits mixed with explicit waits → flag as `[MAJOR]` (causes unpredictable timeouts)
- Missing waits before element interaction → flag as `[MINOR]`
- Hardcoded timeout values that should be configurable → flag as `[MINOR]`

### Locator Strategy
- Fragile absolute XPaths (`/html/body/div[1]/div[2]/...`) → flag as `[MAJOR]`
- Preference order: `data-testid` > `id` > `CSS selector` > relative XPath > absolute XPath
- Locators with dynamic indices or generated IDs → flag as `[MINOR]`

### Configuration Externalization
- Hardcoded URLs, base paths, or environment-specific values in code → flag as `[MINOR]`
- Hardcoded timeouts that should come from config → flag as `[NIT]`
- Credentials in code (even test credentials in `src/main/`) → flag as `[CRITICAL]`

---

## Severity Tags

- `[CRITICAL]` — Broken logic, security vulnerability, data loss risk
- `[MAJOR]` — Design flaw, reliability risk, significantly impacts maintainability
- `[MINOR]` — Readability, naming, moderate improvement opportunity
- `[NIT]` — Cosmetic, style preference, low-impact suggestion

Cite the applicable Google style guide rule per finding where possible.

---

## Output Format

For each file reviewed, produce:

```
#### `FileName.java`
**Summary**: One sentence on overall quality.

**[SEVERITY] Category — Short title**
- **Line**: approximate line number or method name
- **Issue**: what is wrong and why it matters
- **Standard**: cite Google style guide rule if applicable
- **Suggestion**: what to do instead (describe, do not write code)
```

### Code Review Scorecard

| Dimension | Rating (1–5) | Notes |
|-----------|-------------|-------|
| Design | | |
| Functionality | | |
| Complexity | | |
| Tests | | |
| Naming | | |
| Comments & Docs | | |
| Style | | |
| Security | | |
| **QA Patterns** | | |
| **Overall** | | |

1 = Critical issues, 5 = Excellent
