---
description: "Review test quality: coverage completeness, assertion strength, naming conventions, and whether tests verify behavior not implementation."
name: "Test Quality Reviewer"
agent: "Test Quality Reviewer"
argument-hint: "Test file path, class name, or scope (e.g., 'review AutoHealingEngineTest.java', 'review all tests in adapters package', 'review all test files')"
---
Perform a test quality review on the specified scope.

- Assess coverage completeness and identify missing test cases
- Evaluate assertion quality — check for weak, missing, or incorrect assertions
- Review test naming for clarity and intent
- Verify tests check behavior, not implementation details
- Flag brittle tests, over-mocking, and test isolation issues

Do NOT write or modify any test code — review only.

Scope (if provided by the user): {{SCOPE}}
