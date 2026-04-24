---
description: "Build a prioritized refactoring plan from code review findings or technical debt. Includes effort estimates and an ordered backlog."
name: "Refactoring Planner"
agent: "Refactoring Planner"
argument-hint: "What to plan (e.g., 'plan refactoring based on the last review', 'prioritize security findings in src/', 'create a backlog for AutoHealingEngine.java')"
---
Create a prioritized refactoring plan for the specified scope.

- Collect any existing review findings, audit results, or raw code as input
- Categorize findings by type: design, naming, security, test coverage, performance
- Prioritize by risk and business impact (P1 → P3)
- Estimate effort for each item (S / M / L)
- Output an ordered refactoring backlog ready for sprint planning

Do NOT write or modify any code — plan only.

Scope / input (if provided by the user): {{SCOPE}}
