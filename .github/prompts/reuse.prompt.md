---
description: "Search the codebase for existing utilities, helpers, or patterns that satisfy a requirement — before writing new code."
name: "Reusable Code Finder"
agent: "Reusable Code Finder"
argument-hint: "Describe the functionality you need (e.g., 'retry logic', 'locator healing', 'HTML report generation', 'screenshot capture')"
---
Search the workspace for existing code that already implements the described functionality.

- Parse the requirement to identify key concepts, actions, and domain terms
- Search broadly across `src/` for matching classes, methods, utilities, and patterns
- Report exact file locations and method signatures for any reusable candidates
- Assess fit: full match, partial match, or needs minor adaptation
- Only recommend writing new code if nothing reusable exists

Requirement (if provided by the user): {{REQUIREMENT}}
