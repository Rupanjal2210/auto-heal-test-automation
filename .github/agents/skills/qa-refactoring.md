# QA Refactoring Backlog Criteria

This skill file is loaded by the QA Automation Expert agent during Stage 4. Consume ALL findings from Stages 1, 2, and 3 to build a prioritized refactoring backlog.

---

## Effort Scale

| Size | Criteria | Estimate |
|------|----------|----------|
| XS | Single rename, extract constant, remove dead code | < 30 min |
| S | Extract method, add interface, fix naming across one class | 1–2 hours |
| M | Extract class, introduce dependency injection, add test coverage for one class | Half day |
| L | Redesign a component, break up a god class, add full test suite | 1–2 days |
| XL | Cross-cutting architectural change, introduce new abstraction across packages | 1+ week |

## Risk Levels

- **Low**: change is isolated, well-tested, purely cosmetic
- **Medium**: touches shared utilities or interfaces used by multiple callers
- **High**: modifies core engine logic, public APIs, or areas with few/no tests

## Priority Tiers

| Tier | Criteria |
|------|----------|
| **P0 — Fix Now** | Security vulnerabilities, assertion-free tests, broken logic, data loss risk |
| **P1 — Next Sprint** | High-impact design problems, missing critical test coverage, major flakiness sources |
| **P2 — Planned** | Readability, naming, dead code, moderate complexity reduction |
| **P3 — Backlog** | Nice-to-have cleanups, minor NITs, low-risk cosmetic improvements |

---

## Cross-Cutting Structural Checks

Beyond individual findings, check for these structural issues:
- **God classes**: classes with more than one clear responsibility
- **Duplicate logic**: similar code repeated across two or more classes
- **Missing shared interfaces**: similar patterns without a common abstraction
- **Circular package dependencies**: package A depends on B depends on A
- **Hardcoded instantiation**: `new X()` scattered through logic instead of injected dependencies
- **Missing test prerequisites**: items that require adding tests BEFORE refactoring — flag as "test first"

---

## Output Format

### Refactoring Backlog

| # | Priority | Item | File / Class | Effort | Risk | Source Stage |
|---|----------|------|-------------|--------|------|-------------|
| 1 | P0 | Short title | `ClassName.java` | S | High | Stage 1, 3 |
| 2 | P1 | Short title | `ClassName.java` | M | Medium | Stage 2 |
| … | … | … | … | … | … | … |

### Detailed Plan

For each backlog item:

```
#### Item #N — Short title (Priority, Effort, Risk)
- **Source**: which stage(s) and finding(s) identified this
- **Current state**: what the code does now
- **Target state**: what it should do after refactoring
- **Prerequisites**: other items or tests needed first
- **Impact**: what improves when this is done
```

### Execution Order

Group items into phases respecting dependencies:

1. **Phase 1 — Safety** (P0 items): security fixes, critical test fixes
2. **Phase 2 — Foundation** (P1 items needing "test first"): add test coverage for areas about to be refactored
3. **Phase 3 — Structure** (P1 items): design improvements, major refactoring
4. **Phase 4 — Polish** (P2/P3 items): readability, naming, cleanup

### Effort Summary

| Priority | Count | Total Effort |
|----------|-------|-------------|
| P0 | | |
| P1 | | |
| P2 | | |
| P3 | | |
| **Total** | | |
