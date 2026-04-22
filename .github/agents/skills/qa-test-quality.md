# QA Test Quality Criteria

This skill file is loaded by the QA Automation Expert agent during Stage 2. Apply to every test file in scope.

---

## Step 1 — Coverage Map

Before reviewing individual tests, build a coverage map:

1. List all test files in `src/test/` (or equivalent).
2. Map each test file to its corresponding production class in `src/main/`.
3. Flag production classes with **no corresponding test file** as `[UNTESTED]`.

Output the coverage map using this template:

| Production Class | Test File | Status |
|-----------------|-----------|--------|
| `ClassName.java` | `ClassNameTest.java` | Covered / Missing / Partial |

---

## Step 2 — Review Dimensions

Evaluate every test file against these 7 dimensions:

### 1. Naming
- Method name clearly describes **what** is tested and **expected outcome**?
- Good: `methodName_condition_expectedResult` (e.g., `heal_whenLocatorStale_returnsAlternative`)
- Bad: `test1()`, `testHealing()`, `shouldWork()`
- Test class names suffixed with `Test` or `Tests`?

### 2. Behavior Coverage
- **Happy path** (expected success), **sad path** (expected failures), and **edge cases** all tested?
- Edge cases: null inputs, empty collections, boundary values, concurrent access, timeouts
- Exception paths explicitly tested (`assertThrows`)?
- All public methods of the production class exercised?

### 3. Assertion Quality
- Every test has at least one meaningful assertion? (No assertion = `[CRITICAL]`)
- Assertions specific (`assertEquals` not just `assertNotNull`)?
- Failure messages present for self-explanatory failures?
- Multiple related assertions grouped with `assertAll` (JUnit 5)?
- Tests assert **behavior** (what the system does) not **implementation** (how it does it)?

### 4. Independence
- Each test sets up its own state via `@BeforeEach` / fixtures?
- No shared mutable static state causing flakiness?
- Mocks/stubs reset between tests?
- No reliance on test execution order?

### 5. AAA Readability
- Arrange / Act / Assert structure clear and separated?
- Test under ~30 lines?
- Magic numbers or strings replaced with named constants or descriptive variables?

### 6. Mock / Stub Quality
- Mocks used only to isolate the unit under test — not mocking the class being tested?
- `verify()` used to assert meaningful interactions?
- No over-specified mocks that break on refactoring?
- No unnecessary mocks where a real object would be simpler?

### 7. Test Smells
Flag explicitly if present:
- **Assertion-free test**: no assertion → always `[CRITICAL]`
- **God test**: one method covering many unrelated behaviors
- **Flaky test**: relies on timing, random data, or external state
- **Dead test**: `@Disabled` / `@Ignore` without explanation or expiry
- **Duplicate test**: identical logic under different names
- **Excessive setup**: `@BeforeEach` longer than the tests themselves

---

## QA Automation-Specific Checks

### Flakiness Indicators
- `Thread.sleep()` in tests → `[MAJOR]` — use waits or polling
- Assertions on UI timing or animation state → `[MAJOR]`
- Tests dependent on network availability or external services without mocking → `[MAJOR]`
- Non-deterministic test data (random IDs, timestamps) without seeding → `[MINOR]`

### Test Data Management
- Hardcoded test data inline vs parameterized → flag opportunities for `@ParameterizedTest` / `@CsvSource`
- Test data builders or factories used?
- Test data cleanup in `@AfterEach` to prevent pollution?

### Environment Isolation
- Tests coupling to specific environments (hardcoded URLs, paths, ports)?
- Missing abstractions for environment configuration?
- Tests that only pass on a specific OS or locale?

### Reporting Integration
- Do test failures produce actionable output (screenshots, logs, DOM snapshots)?
- Are test results machine-readable (JUnit XML, JSON reports)?
- Custom reporters wired correctly?

### Test Pyramid Compliance
- What is the ratio of unit / integration / e2e tests?
- Over-reliance on UI/e2e tests when unit tests would suffice → `[MAJOR]`
- Missing integration tests for critical cross-component flows → `[MINOR]`

---

## Severity Tags

- `[CRITICAL]` — Assertion-free tests, tests that always pass, broken test logic
- `[MAJOR]` — Missing edge cases, over-mocking, flakiness indicators, test ordering dependency
- `[MINOR]` — Naming, readability, AAA structure, missing parameterization
- `[NIT]` — Minor style, redundant comments, trivial improvements

---

## Output Format

For each test file:

```
#### `TestFileName.java`
**Summary**: One sentence on overall quality.

**[SEVERITY] Category — Short title**
- **Test method**: method name (or "class-level")
- **Issue**: what is wrong and why it matters
- **Suggestion**: what to do instead (describe, do not write code)
```

### Test Quality Scorecard

| Dimension | Rating (1–5) | Notes |
|-----------|-------------|-------|
| Naming clarity | | |
| Behavior coverage | | |
| Assertion quality | | |
| Test independence | | |
| Readability (AAA) | | |
| Mock quality | | |
| Test smells | | |
| **QA Automation** | | |
| **Overall** | | |

1 = Critical issues, 5 = Excellent
