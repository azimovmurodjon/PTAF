# ScenarioUtil Enhancements — String & Structured Value Reporting
**Date:** 2025-10-09

This document captures all changes we made to `ScenarioUtil` to enable robust string and structured-data reporting **without breaking existing methods and naming**. It also includes copy‑pasteable usage examples.

---

## 1) Overview
We extended `ScenarioUtil` with **non-breaking** helpers to attach and log:
- **Arbitrary strings** (plain text)
- **Element values** (works on page and up to 3 nested iframes; auto-detects input vs. text nodes)
- **JSON** payloads (request/response bodies)
- **Tables** (list of strings → numbered rows)
- **Key–Value** maps (configs, environment, headers)
- **Value + Screenshot** combo for a specific element

All original methods (`handleScenarioTeardown`, `handleScenarioTeardownFailier`, `handleScenarioTeardownLocator`, `reportAllDropdownOptionsMultiline`) remain **unchanged**.

---

## 2) New Methods (Signatures & Purpose)

### 2.1 `reportString`
```java
public static void reportString(Scenario scenario, String title, String value)
```
Attach/log any arbitrary string value as `text/plain`. Useful for environment labels, IDs, debug notes, etc.

---

### 2.2 `captureElementString`
```java
public static String captureElementString(Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator)
```
Safely capture an element's **string**. If `input/textarea` → uses `inputValue()`, else falls back to `innerText()`/`textContent()`. Works on page or nested frames (up to 3). Returns `"<empty>"` for blank, `"<not-found>"` on failure.

---

### 2.3 `reportElementString`
```java
public static void reportElementString(Scenario scenario, Page page,
                                       String iFrame, String iFrame_2, String iFrame_3,
                                       String targetLocator, String label)
```
Convenience wrapper: calls `captureElementString(...)` and then `reportString(...)` to attach/log the value with a readable label.

---

### 2.4 `reportJson`
```java
public static void reportJson(Scenario scenario, String title, String json)
```
Attaches JSON with `application/json` content-type. Uses a lightweight pretty-printer—falls back to raw if needed.

---

### 2.5 `reportTable`
```java
public static void reportTable(Scenario scenario, String title, List<String> rows)
```
Attaches a numbered list as multi-line text. Handy for menus, dropdown options, validation lists, etc.

---

### 2.6 `reportKeyValues`
```java
public static void reportKeyValues(Scenario scenario, String title, Map<String, ?> map)
```
Attaches key–value pairs (configs, env vars, headers).

---

### 2.7 `reportElementStringWithScreenshot`
```java
public static void reportElementStringWithScreenshot(Scenario scenario, Page page,
                                                     String iFrame, String iFrame_2, String iFrame_3,
                                                     String targetLocator, String label)
```
One call to capture a specific element’s **value** and **screenshot** (JPEG bytes attached as `image/png` to retain your existing report viewers' behavior). Works with nested iframes.

---

## 3) Backward Compatibility
- **No existing methods were removed or renamed.**
- **Content types** for screenshot attachments remain `image/png`, exactly as before in your reporting flow.
- The new helpers are **additive** and can be adopted incrementally.

---

## 4) Usage Examples

### 4.1 Report a Plain String
```java
ScenarioUtil.reportString(scenario, "Environment", "QA-US-East");
ScenarioUtil.reportString(scenario, "Generated Order Id", orderId);
```

### 4.2 Capture & Report a Value (Page level)
```java
ScenarioUtil.reportElementString(scenario, page, null, null, null, "#totalAmount", "Total Amount");
```

### 4.3 Capture & Report a Value (Nested iframes)
```java
ScenarioUtil.reportElementString(
    scenario, page,
    "iframe[name='iframeApplicationContent']",
    "iframe[name='iframeContent']",
    null,
    "input[name='accountNumber']",
    "Account Number"
);
```

### 4.4 Attach JSON (Request/Response/Fixture)
```java
String requestJson = "{\"customerId\":\"12345\",\"plan\":\"gold\"}";
ScenarioUtil.reportJson(scenario, "CreateCustomerRequest", requestJson);
```

### 4.5 Attach a Table (List → Multi-line)
```java
List<String> menuItems = List.of("Home", "Accounts", "Payments", "Transfers", "Settings");
ScenarioUtil.reportTable(scenario, "Visible Menu Items", menuItems);
```

### 4.6 Attach Key–Value Pairs (Configs/Headers)
```java
Map<String, Object> config = Map.of(
    "env", "QA",
    "branch", "release/1.4.0",
    "buildNumber", "2025.10.09",
    "retry", 2
);
ScenarioUtil.reportKeyValues(scenario, "Runtime Config", config);
```

### 4.7 Value + Screenshot Combo
```java
// No iframes
ScenarioUtil.reportElementStringWithScreenshot(
    scenario, page, null, null, null, "#totalAmount", "Total Amount");

// With iframes
ScenarioUtil.reportElementStringWithScreenshot(
    scenario, page,
    "iframe[name='iframeApplicationContent']",
    "iframe[name='iframeContent']",
    null,
    "input[name='accountNumber']",
    "Account Number");
```

### 4.8 Dropdown Options (Existing Method)
```java
ScenarioUtil.reportAllDropdownOptionsMultiline(
    scenario, page,
    null, null, null, "select#country"
);
```

---

## 5) Integration Steps
1. Replace your existing `ScenarioUtil` with the updated class file containing the new helpers.
2. No additional dependencies required.
3. Start using the helpers in your step definitions / page methods as shown above.
4. If you later decide to export 2D tables, we can add a CSV/Markdown attachment helper as well.

---

## 6) Troubleshooting & Tips
- If a selector is flaky, consider `waitForSelector(...)` before capture calls (already used internally in capture helpers).
- `captureElementString` returns **`"<empty>"`** for blank text and **`"<not-found>"`** if the element cannot be located/read, making assertions easier to debug.
- Large strings are truncated in logs (not in attachments) to keep logs readable.

---

## 7) Changelog (This Batch)
- **Added** `reportString(...)`
- **Added** `captureElementString(...)`
- **Added** `reportElementString(...)`
- **Added** `reportJson(...)`
- **Added** `reportTable(...)`
- **Added** `reportKeyValues(...)`
- **Added** `reportElementStringWithScreenshot(...)`
- **No changes** to existing methods and names.
