---
description: "Diagnose errors from Appway standard jar APIs: ProcessContext, FormContext, ConnectorFacade, WorkflowEngine, DataRepository, and more."
name: "Appway Jar Debugger"
agent: "Appway Jar Debugger"
argument-hint: "Paste the stack trace or describe the error (e.g., 'NullPointerException in ProcessContext.getVariable', 'ConnectorException from ConnectorFacade.execute')"
---
Diagnose and fix the specified Appway jar API error.

- Identify which Appway jar class and method is throwing the exception
- Determine whether the root cause is a jar-level contract violation or a script-level misuse
- Explain exactly what the failing method requires, what was provided, and why it failed
- Provide a corrected usage pattern with a clear explanation

Stack trace / error (if provided by the user): {{ERROR}}
