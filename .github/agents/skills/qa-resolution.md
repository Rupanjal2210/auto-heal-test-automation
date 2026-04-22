# QA Resolution Recommendations Criteria

This skill file is loaded by the QA Automation Expert agent during Stage 5. Generate actionable resolution recommendations for every item in the refactoring backlog from Stage 4.

---

## Resolution Card Format

For every **P0** and **P1** item, produce a full Resolution Card:

```
### Resolution Card #N — [Short Title]

**Priority**: P0/P1 | **Effort**: XS/S/M/L/XL | **Risk**: Low/Medium/High

**Problem**
One-line summary of the issue with severity tag.

**Root Cause**
Why this issue exists — the underlying design decision or oversight.

**Resolution Steps**
1. Step-by-step instructions to fix the issue
2. Be specific about which class, method, or pattern to change
3. Include what to add, remove, or restructure

**Pattern Example**

_Before:_
```java
// Show the problematic pattern (illustrative, not copy-paste from source)
public void doSomething() {
    Thread.sleep(5000); // hardcoded wait
    driver.findElement(By.xpath("/html/body/div[1]/div[2]/form/input"));
}
```

_After:_
```java
// Show the corrected pattern
public void doSomething() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='search-input']")));
}
```

**Verification**
- Specific test to write or assertion to add to confirm the fix
- Command to run (e.g., `mvn test -pl module -Dtest=ClassName`)
- What success looks like

**Dependencies**
- List other Resolution Cards that must be completed first
- Or state "None — independent item"
```

---

## Condensed Format for P2/P3

For **P2** and **P3** items, provide a one-liner:

| # | Priority | Item | Resolution | Effort |
|---|----------|------|-----------|--------|
| N | P2 | Short title | One-sentence fix description | S |

---

## Grouping by Theme

Group Resolution Cards into themes for clarity:

### Theme Categories
- **Test Architecture**: Page Object Model, test data management, framework structure
- **Security Hardening**: credential removal, configuration security, OWASP fixes
- **Code Quality**: design patterns, complexity reduction, naming, SRP violations
- **Test Coverage Gaps**: missing tests, assertion-free tests, untested production classes
- **Reliability & Stability**: flakiness fixes, wait strategies, environment isolation
- **Maintainability**: dead code removal, documentation, dependency management

---

## Execution Roadmap

After all Resolution Cards, produce a sequenced execution plan:

### Sprint Allocation

```
Sprint 1 — Safety & Security (P0 items)
  ├── Resolution Card #1 — [title]
  ├── Resolution Card #3 — [title]
  └── Resolution Card #5 — [title]

Sprint 2 — Test Foundation (P1 "test first" items)
  ├── Resolution Card #2 — [title] (prerequisite for #7)
  └── Resolution Card #4 — [title]

Sprint 3 — Structural Improvements (P1 remaining)
  ├── Resolution Card #7 — [title] (depends on #2)
  └── Resolution Card #8 — [title]

Sprint 4+ — Polish (P2/P3 items)
  └── See condensed table above
```

### Dependency Graph

Show which cards depend on which:

```
#1 (independent)
#2 → #7 (must complete #2 before #7)
#3 (independent)
#4 → #8 (must complete #4 before #8)
```

### Success Criteria

Define what "done" looks like for the full resolution effort:
- All P0 items resolved and verified
- Test coverage increased to X% (estimated from coverage gaps found)
- Zero `[CRITICAL]` security findings remaining
- All `Thread.sleep()` replaced with explicit waits
- No hardcoded credentials in `src/main/`

---

## Constraints for Pattern Examples

- Pattern examples must be **illustrative** — they demonstrate the correct pattern, NOT copy-paste production code
- Use the detected language and framework (Java/JUnit, Python/pytest, JS/Jest, etc.)
- Keep examples to 5–15 lines — enough to show the pattern, not a full implementation
- Always show both **Before** (problematic) and **After** (corrected) patterns
- Use realistic but generic class/method names in examples
- Never include real credentials, URLs, or PII in examples
