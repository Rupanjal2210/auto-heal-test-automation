---
name: appway-jar-knowledge
description: >
  Deep reference knowledge of Appway standard jar APIs. Use when: diagnosing errors thrown
  from Appway standard jar methods; understanding what a jar method does and why it fails;
  tracing NullPointerException, ClassCastException, ConnectorException, FormFieldNotFoundException,
  or ProcessVariableException back to a root cause; verifying correct usage of ProcessContext,
  FormContext, ConnectorFacade, DataRepository, WorkflowEngine, ScriptUtils, or any other
  Appway-provided class; auditing Appway script code against Appway API contracts.
---

# Appway Standard Jar API Knowledge

This skill provides the authoritative reference for all Appway-provided standard jar APIs.
Use it to diagnose method-level errors, verify correct invocation patterns, and explain why a
specific Appway API call fails.

---

## Jar Inventory

| Jar / Module | Package Root | Primary Purpose |
|---|---|---|
| appway-core | `com.appway.core` | Workflow engine, process lifecycle, variable management |
| appway-form | `com.appway.form` | Form rendering, field binding, validation |
| appway-connector | `com.appway.connector` | REST, SOAP, JDBC, LDAP connector execution |
| appway-data | `com.appway.data` | Data repository, querying, persistence |
| appway-security | `com.appway.security` | Authentication, authorization, role checks |
| appway-util | `com.appway.util` | String, date, collection, encoding utilities |
| appway-reporting | `com.appway.reporting` | PDF/HTML report generation |
| appway-messaging | `com.appway.messaging` | Internal events, email, notifications |

---

## appway-core API

### `com.appway.core.api.ProcessContext`

The central object available in all workflow step scripts. Represents the running process instance.

#### Key Methods

| Method Signature | Returns | Throws | Contract |
|---|---|---|---|
| `getVariable(String name)` | `Object` | — | Returns `null` if variable is not set. **Never throws** for missing key. Always null-check the return value. |
| `getVariable(String name, Class<T> type)` | `T` | `ClassCastException` | Casts stored value to `type`. Throws if stored type is incompatible. |
| `setVariable(String name, Object value)` | `void` | `IllegalArgumentException` | Throws if `name` is null or empty. Value `null` is allowed (clears the variable). |
| `getRequiredVariable(String name)` | `Object` | `ProcessVariableException` | Throws `ProcessVariableException` if variable is absent or null. Use when absence is a programming error. |
| `getCurrentUser()` | `AppwayUser` | — | Returns the authenticated user or `null` in system-triggered steps. |
| `getProcessId()` | `String` | — | Always non-null. Unique UUID for the running instance. |
| `getStepId()` | `String` | — | ID of the currently executing step. |
| `getApplicationName()` | `String` | — | Application context name. Null in unit test harness. |
| `completeStep()` | `void` | `WorkflowException` | Programmatically transitions the step. Throws if step is already completed. |
| `triggerEvent(String eventName)` | `void` | `WorkflowException` | Fires a named process event. Throws if event name is not defined in the process model. |

#### Common Errors with `ProcessContext`

| Error | Root Cause | Fix |
|---|---|---|
| `NullPointerException` on `ctx.getVariable("x").toString()` | `getVariable` returned `null` | Always null-check: `Object v = ctx.getVariable("x"); if (v != null) { ... }` |
| `ClassCastException` from `(String) ctx.getVariable("count")` | Variable stored as `Integer`, cast to `String` | Use `ctx.getVariable("count", Integer.class)` or `String.valueOf(ctx.getVariable("count"))` |
| `ProcessVariableException: Variable 'orderId' not found` | Required variable not set by predecessor step | Check step ordering in process model; ensure predecessor step `setVariable` before this step runs |
| `IllegalArgumentException: Variable name must not be null` | Passing `null` as `name` to `setVariable` | Validate variable name is non-null before calling |
| `WorkflowException: Step already completed` | `completeStep()` called twice | Guard with a flag variable; avoid calling `completeStep()` inside loops |

---

### `com.appway.core.api.WorkflowEngine`

Static facade for programmatic workflow operations. Available in scripts and service beans.

#### Key Methods

| Method Signature | Returns | Throws | Contract |
|---|---|---|---|
| `getInstance()` | `WorkflowEngine` | — | Returns the singleton. Never null within an Appway runtime. |
| `startProcess(String appName, String processName, Map<String,Object> variables)` | `String` processId | `WorkflowException` | Throws if app/process name not found, or if required initial variables are missing. |
| `getProcessContext(String processId)` | `ProcessContext` | `WorkflowException` | Throws if `processId` does not exist or is already archived. |
| `archiveProcess(String processId)` | `void` | `WorkflowException` | Throws if process is still active (not in a terminal state). |
| `findProcesses(ProcessQuery query)` | `List<ProcessContext>` | — | Returns empty list (never null) if no matches. |

#### Common Errors with `WorkflowEngine`

| Error | Root Cause | Fix |
|---|---|---|
| `WorkflowException: Application 'xyz' not found` | Wrong `appName` string — case-sensitive | Check Appway Designer for exact application name |
| `WorkflowException: Process 'xyz' not found` | Process definition not deployed or name typo | Redeploy or verify spelling in Designer |
| `WorkflowException: Cannot archive active process` | `archiveProcess` called while process still has pending steps | Add gate: call only when process is in a final state |

---

## appway-form API

### `com.appway.form.api.FormContext`

Available in form/UI scripts. Provides access to the rendered form and its fields.

#### Key Methods

| Method Signature | Returns | Throws | Contract |
|---|---|---|---|
| `getField(String fieldId)` | `FormField` | `FormFieldNotFoundException` | Throws if `fieldId` does not exist on the current form. |
| `findField(String fieldId)` | `FormField` | — | Returns `null` instead of throwing if field not found. Prefer for optional fields. |
| `getValue(String fieldId)` | `Object` | `FormFieldNotFoundException` | Shorthand for `getField(fieldId).getValue()`. Throws for missing field. |
| `setValue(String fieldId, Object value)` | `void` | `FormFieldNotFoundException`, `ValidationException` | Throws `ValidationException` if value violates field constraints. |
| `hideField(String fieldId)` | `void` | `FormFieldNotFoundException` | Hides field from UI. |
| `setFieldReadOnly(String fieldId, boolean readOnly)` | `void` | `FormFieldNotFoundException` | Marks field read-only at runtime. |
| `getRepeatingGroup(String groupId)` | `RepeatingGroup` | `FormFieldNotFoundException` | Returns the repeating group. Use `group.getRows()` to iterate — returns empty list, never null. |
| `submitForm()` | `void` | `ValidationException` | Validates all fields and submits. Throws if any field fails validation. |

#### Common Errors with `FormContext`

| Error | Root Cause | Fix |
|---|---|---|
| `FormFieldNotFoundException: Field 'myField' not found` | Field ID typo, or field is in a different panel not loaded | Verify field ID in Form Designer; check panel visibility conditions |
| `ValidationException: Value '...' not in allowed values` | `setValue` with a value not in the dropdown list | Populate allowed values first via `field.setAllowedValues()` before calling `setValue` |
| `NullPointerException` after `findField(...)` | Used `findField` (nullable) without null check | Guard: `FormField f = ctx.findField("x"); if (f != null) { f.setValue(...); }` |
| `ClassCastException` on `(Date) ctx.getValue("myDateField")` | Date fields may return `String` in display format | Use `DateUtils.parseFormDate(ctx.getValue("myDateField").toString())` |

---

## appway-connector API

### `com.appway.connector.api.ConnectorFacade`

Executes Appway connector definitions. Available in all script types.

#### Key Methods

| Method Signature | Returns | Throws | Contract |
|---|---|---|---|
| `execute(String connectorName, Map<String,Object> params)` | `ConnectorResult` | `ConnectorException` | Throws for transport errors, auth failures, or timeout. Does NOT throw for HTTP 4xx/5xx — check `result.getStatusCode()`. |
| `execute(ConnectorRequest request)` | `ConnectorResult` | `ConnectorException` | Full control variant. Use for dynamic header injection. |
| `getConnector(String name)` | `ConnectorDefinition` | `ConnectorNotFoundException` | Returns metadata. Throws if connector not deployed. |

### `com.appway.connector.api.ConnectorResult`

| Method | Returns | Notes |
|---|---|---|
| `getStatusCode()` | `int` | HTTP status code or 0 for non-HTTP connectors |
| `getBody()` | `String` | Raw response body. May be null on error. |
| `getBodyAs(Class<T> type)` | `T` | Deserializes JSON body. Throws `ConnectorException` on parse failure. |
| `isSuccess()` | `boolean` | True if status is 2xx for HTTP, or connector-specific success for others |
| `getHeaders()` | `Map<String,String>` | Response headers. Never null. |
| `getErrorMessage()` | `String` | Non-null only when `isSuccess()` is false |

#### Common Errors with `ConnectorFacade`

| Error | Root Cause | Fix |
|---|---|---|
| `ConnectorException: Connection refused` | Target service unreachable or URL misconfigured | Verify connector URL in Designer; test connectivity from Appway server |
| `ConnectorException: Authentication failed (401)` | Wrong credentials or expired token in connector config | Re-enter credentials in Connector Manager; check token expiry for OAuth |
| `ConnectorException: Timeout after 30000ms` | Target service too slow; default timeout exceeded | Add `params.put("timeout", 60000)` or set global timeout in connector config |
| `ConnectorNotFoundException: 'MyConnector' not found` | Connector not deployed or name case mismatch | Deploy connector; verify exact name in Connector Manager (case-sensitive) |
| `ConnectorException: JSON parse error` | `getBodyAs(MyClass.class)` but response is not valid JSON | Check `result.isSuccess()` and `result.getBody()` before deserializing |
| Silently wrong data | Checked `isSuccess()` wrong — HTTP 404 is `isSuccess() == false` | Always check `result.isSuccess()` before using `result.getBody()` |

---

## appway-data API

### `com.appway.data.api.DataRepository`

Provides CRUD operations and queries against Appway-managed data stores.

#### Key Methods

| Method Signature | Returns | Throws | Contract |
|---|---|---|---|
| `findById(String entityType, String id)` | `DataEntity` | — | Returns `null` if not found. Never throws for missing entity. |
| `findAll(DataQuery query)` | `List<DataEntity>` | `DataQueryException` | Returns empty list (never null) if no results. Throws on invalid query syntax. |
| `save(DataEntity entity)` | `DataEntity` | `DataValidationException`, `DataConflictException` | `DataConflictException` on optimistic lock violation. |
| `delete(String entityType, String id)` | `void` | `DataNotFoundException` | Throws if entity does not exist. Check existence first with `findById`. |
| `count(DataQuery query)` | `long` | `DataQueryException` | Efficient count without loading entities. |

#### Common Errors with `DataRepository`

| Error | Root Cause | Fix |
|---|---|---|
| `NullPointerException` on `repo.findById(...).getField(...)` | `findById` returned `null` — entity not found | Null-check: `DataEntity e = repo.findById(...); if (e == null) { ... }` |
| `DataConflictException: Optimistic lock failure` | Two concurrent updates to the same entity | Re-fetch entity and retry; implement exponential backoff |
| `DataValidationException: Field 'email' is required` | Saving entity with null required field | Set all required fields before calling `save()` |
| `DataNotFoundException` from `delete` | Entity already deleted or ID wrong | Use `findById` check before calling `delete` |
| `DataQueryException: Unknown field 'naem'` | Typo in query field name | Check field names in Appway Data Designer |

---

## appway-security API

### `com.appway.security.api.SecurityContext`

Provides authentication and authorization checks.

#### Key Methods

| Method Signature | Returns | Throws | Contract |
|---|---|---|---|
| `getCurrentUser()` | `AppwayUser` | — | Null in system/batch contexts. Always null-check. |
| `hasRole(String roleName)` | `boolean` | — | Returns `false` (not throws) if user is null or role not assigned. |
| `requireRole(String roleName)` | `void` | `SecurityException` | Throws `SecurityException` if current user lacks the role. Gate operations with this. |
| `isAuthenticated()` | `boolean` | — | True if a user session exists. |
| `getUserAttribute(String attr)` | `String` | — | Returns `null` if attribute not set on user profile. |

#### Common Errors with `SecurityContext`

| Error | Root Cause | Fix |
|---|---|---|
| `SecurityException: User lacks role 'ADMIN'` | `requireRole` called but user is not in that role | Verify role assignment in User Manager; check if role name is case-sensitive |
| `NullPointerException` on `ctx.getCurrentUser().getUsername()` | System-triggered step has no user session | Guard: `AppwayUser u = ctx.getCurrentUser(); if (u != null) { ... }` |

---

## appway-util API

### `com.appway.util.StringUtils`

| Method | Returns | Notes |
|---|---|---|
| `isBlank(String s)` | `boolean` | True for null, empty, or whitespace-only strings |
| `isNotBlank(String s)` | `boolean` | Inverse of `isBlank` |
| `truncate(String s, int maxLen)` | `String` | Null-safe; returns null for null input |
| `toBase64(byte[] data)` | `String` | Standard Base64 encoding |
| `fromBase64(String encoded)` | `byte[]` | Throws `IllegalArgumentException` for malformed Base64 |
| `escapeHtml(String s)` | `String` | Escapes `< > & " '`. Use before embedding in HTML reports. |

### `com.appway.util.DateUtils`

| Method | Returns | Notes |
|---|---|---|
| `now()` | `Date` | Current server time |
| `parseFormDate(String s)` | `Date` | Parses Appway form date format (`dd/MM/yyyy`). Throws `ParseException` for wrong format. |
| `formatFormDate(Date d)` | `String` | Formats to `dd/MM/yyyy`. Null-safe — returns empty string for null. |
| `addDays(Date d, int days)` | `Date` | Negative `days` subtracts. Null-safe — throws `IllegalArgumentException` for null date. |
| `isBefore(Date a, Date b)` | `boolean` | Both null-safe; null is treated as epoch. |

### `com.appway.util.CollectionUtils`

| Method | Returns | Notes |
|---|---|---|
| `isEmpty(Collection<?> c)` | `boolean` | True for null or empty collection |
| `isNotEmpty(Collection<?> c)` | `boolean` | Inverse |
| `safeList(Collection<T> c)` | `List<T>` | Returns empty list if `c` is null. Use before iterating Appway query results. |
| `firstOrNull(List<T> list)` | `T` | Returns null for empty/null list instead of throwing IndexOutOfBoundsException |
| `partition(List<T> list, int batchSize)` | `List<List<T>>` | Splits list into chunks. Useful for batch connector calls. |

---

## appway-reporting API

### `com.appway.reporting.api.ReportService`

| Method | Returns | Throws | Notes |
|---|---|---|---|
| `generatePdf(String templateName, Map<String,Object> data)` | `byte[]` | `ReportException` | Template must be deployed. Throws if template not found or data binding fails. |
| `generateHtml(String templateName, Map<String,Object> data)` | `String` | `ReportException` | Same rules as PDF. |
| `getTemplate(String name)` | `ReportTemplate` | `ReportNotFoundException` | Throws if not deployed. |

---

## appway-messaging API

### `com.appway.messaging.api.MessageService`

| Method | Returns | Throws | Notes |
|---|---|---|---|
| `sendEmail(EmailMessage msg)` | `void` | `MessagingException` | Throws on SMTP failure or null recipient. |
| `publishEvent(String eventName, Map<String,Object> payload)` | `void` | `MessagingException` | Fires an Appway platform event to subscribers. |
| `sendNotification(String userId, String message)` | `void` | `MessagingException` | In-platform notification. Throws if `userId` is null. |

---

## Exception Taxonomy

| Exception Class | Package | When Thrown |
|---|---|---|
| `ProcessVariableException` | `com.appway.core.api` | Required process variable absent or null |
| `WorkflowException` | `com.appway.core.api` | Invalid workflow state transitions, missing definitions |
| `FormFieldNotFoundException` | `com.appway.form.api` | Accessing non-existent form field |
| `ValidationException` | `com.appway.form.api` | Field value violates form validation constraints |
| `ConnectorException` | `com.appway.connector.api` | Transport, auth, or parse failures in connector execution |
| `ConnectorNotFoundException` | `com.appway.connector.api` | Connector definition not deployed |
| `DataQueryException` | `com.appway.data.api` | Invalid query syntax or unknown field names |
| `DataValidationException` | `com.appway.data.api` | Entity fails server-side data validation on save |
| `DataConflictException` | `com.appway.data.api` | Optimistic locking conflict on concurrent update |
| `DataNotFoundException` | `com.appway.data.api` | Entity referenced by ID does not exist |
| `SecurityException` | `com.appway.security.api` | Authorization failure from `requireRole` |
| `ReportException` | `com.appway.reporting.api` | Template not found or data binding failure |
| `ReportNotFoundException` | `com.appway.reporting.api` | Named template not deployed |
| `MessagingException` | `com.appway.messaging.api` | SMTP, event bus, or notification delivery failure |

---

## Jar Error Diagnosis Decision Tree

When a stack trace from an Appway standard jar is provided, use this sequence:

### Step 1 — Identify the Exception Class
Map the exception to the table above. This immediately tells you which API layer failed.

### Step 2 — Locate the Caller
Find the first frame in the stack trace **not** in a `com.appway.*` package.
That is the script line that triggered the error.

### Step 3 — Apply the Method Contract
Look up the throwing method in the tables above.
Check whether the contract was violated:
- Was a `null` passed where a value is required?
- Was a required predecessor step not run?
- Was the entity/connector/template not deployed?
- Was a type assumption wrong?

### Step 4 — Check Secondary Causes
Many Appway exceptions wrap a root cause:
```java
// Pattern to inspect wrapped cause in Groovy scripts
try {
    connectorFacade.execute("MyConn", params)
} catch (ConnectorException e) {
    log.error("Connector error: " + e.getMessage() + " | Cause: " + e.getCause()?.getMessage())
    throw e
}
```

### Step 5 — Reproduce the Condition
State the exact input that would trigger the error and the exact method call that throws.

---

## Null-Safety Cheat Sheet

```groovy
// SAFE: Always use null-safe patterns for ProcessContext variables
def orderId = ctx.getVariable("orderId")
if (orderId == null) {
    throw new IllegalStateException("orderId not set by predecessor step")
}

// SAFE: Use findField instead of getField for optional fields
def optField = formCtx.findField("optionalComments")
if (optField != null) {
    optField.setValue("auto-filled")
}

// SAFE: Use CollectionUtils.safeList before iterating any Appway query result
CollectionUtils.safeList(repo.findAll(query)).each { entity ->
    // process entity
}

// SAFE: Connector result check before body access
def result = connectorFacade.execute("PaymentService", params)
if (!result.isSuccess()) {
    log.error("Payment connector failed [${result.getStatusCode()}]: ${result.getErrorMessage()}")
    throw new ConnectorException("Payment failed: " + result.getErrorMessage())
}
def payment = result.getBodyAs(PaymentResponse.class)
```
