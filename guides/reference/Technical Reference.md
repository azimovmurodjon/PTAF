# FNB-ETAF Comprehensive Technical Reference

**Audience:** Testers, framework maintainers, developers, reviewers, support teams, and onboarding users.

**Purpose:** This is the canonical long-form reference library for the active FNB-ETAF workspace. It explains how the framework is built, where each automation module lives, how configuration is read, how tests are executed, which reports are produced, and how to trace an observed behavior back to the responsible implementation. The library uses repository-relative source links so every explanation can be verified against the code.

> **Use this library with the source code.** The implementation under [`../../src/`](../../src/) is authoritative. This reference intentionally identifies source-visible limitations and runner/configuration discrepancies instead of hiding them.

## Start with the correct path

Use the chapter that matches the work being performed. Regular browser UI, HTTP/API performance, and `ui_performance` are intentionally different execution paths.

| If the goal is… | Begin here | Then use… |
|---|---|---|
| Build, run, troubleshoot Maven, TestNG, Cucumber, or a runner | [Chapter 01 — Foundation, Build, and Runners](chapters/01-foundation-build-runners.md) | [Appendix A — source and step catalog](appendices/A-source-configuration-step-catalog.md) |
| Change a YAML, XML, `.properties`, environment, locator, or test-data setting | [Chapter 02 — Configuration, YAML, Locators, and Data](chapters/02-configuration-yaml-locators-data.md) | [Appendix B — every current resource setting](appendices/B-configuration-setting-index.md) |
| Build regular Playwright desktop browser automation | [Chapter 03 — Regular Web UI](chapters/03-web-ui-playwright.md) | [Chapter 12 — Reporting and Evidence](chapters/12-reporting-evidence-artifacts.md) |
| Build REST/API functional automation | [Chapter 04 — API Automation](chapters/04-api-automation.md) | [Chapter 10 — HTTP/API Performance](chapters/10-http-api-performance.md) when load testing is needed |
| Build database validation | [Chapter 05 — Database Automation](chapters/05-database-automation.md) | [Appendix B](appendices/B-configuration-setting-index.md) for `config.yml` and `db_queries.yml` |
| Build native Android/iOS Appium automation | [Chapter 06 — Native Mobile Appium](chapters/06-mobile-native-appium.md) | [Chapter 12](chapters/12-reporting-evidence-artifacts.md) for mobile evidence |
| Build mobile-browser automation | [Chapter 07 — Mobile Browser](chapters/07-mobile-browser.md) | [Chapter 06](chapters/06-mobile-native-appium.md) if the browser is Appium/device-driven |
| Validate CSV, XML, TXT, ZIP, or Excel content | [Chapter 08 — Data and File Utilities](chapters/08-data-files-csv-xml-zip-excel.md) | [Appendix B](appendices/B-configuration-setting-index.md) for paths and switches |
| Validate a PDF document | [Chapter 09 — PDF Validation](chapters/09-pdf-validation.md) | [Chapter 12](chapters/12-reporting-evidence-artifacts.md) for output ownership |
| Create HTTP, REST, or GraphQL load tests | [Chapter 10 — HTTP/API Performance](chapters/10-http-api-performance.md) | [Appendix B](appendices/B-configuration-setting-index.md) for `performance-config.yml` |
| Run concurrent, real-browser load/stress/spike/soak tests | [Chapter 11 — `ui_performance`](chapters/11-ui-performance-load-stress.md) | [Appendix B](appendices/B-configuration-setting-index.md) for the isolated `ui_performance` resources |
| Find a report, screenshot, video, download, or owner of a report defect | [Chapter 12 — Reporting, Evidence, and Artifacts](chapters/12-reporting-evidence-artifacts.md) | [Appendix A](appendices/A-source-configuration-step-catalog.md) to locate the class or step |

## Complete chapter library

1. [Foundation, Maven, TestNG, Cucumber, and Runner Reference](chapters/01-foundation-build-runners.md)
2. [Configuration, YAML, Environment, Locator, and Data Source Reference](chapters/02-configuration-yaml-locators-data.md)
3. [Regular Playwright Web UI Automation Reference](chapters/03-web-ui-playwright.md)
4. [API Automation Reference](chapters/04-api-automation.md)
5. [Database Automation Reference](chapters/05-database-automation.md)
6. [Native Mobile Appium Automation Reference](chapters/06-mobile-native-appium.md)
7. [Mobile Browser Automation Reference](chapters/07-mobile-browser.md)
8. [CSV, XML, TXT, ZIP, Excel, and File Utility Reference](chapters/08-data-files-csv-xml-zip-excel.md)
9. [PDF Automation and Validation Reference](chapters/09-pdf-validation.md)
10. [HTTP and GraphQL API Performance Testing Reference](chapters/10-http-api-performance.md)
11. [Concurrent Real-Browser UI Performance Load Testing Reference](chapters/11-ui-performance-load-stress.md)
12. [Reporting, Evidence, Artifacts, and Failure Diagnosis Reference](chapters/12-reporting-evidence-artifacts.md)

## Generated navigation appendices

The appendices are generated from the active source tree. They provide the detailed code and resource inventory that would be difficult and error-prone to maintain by hand.

- [Appendix A — Source, Configuration, Runner, and Step Catalog](appendices/A-source-configuration-step-catalog.md) catalogs production classes, test classes, runners, features, resource files, and every Cucumber annotation discovered in the current tree.
- [Appendix B — Configuration Setting Index](appendices/B-configuration-setting-index.md) lists every checked-in YAML, XML, and property setting with a source line and safe value preview. Sensitive values and private target-looking values are redacted.
- [Appendix C — Java Callable Catalog](appendices/C-java-callable-catalog.md) lists every detected public or protected Java declaration with its source line, signature, nearest Javadoc summary, and source link.

To refresh these indexes after a code or configuration change, run:

```bash
python3 scripts/generate_reference_catalog.py
python3 scripts/generate_configuration_index.py
python3 scripts/generate_java_callable_catalog.py
```

## Documentation boundaries

This library is a **reference**, not a release claim. A source link proves that a class or resource exists in the active workspace. It does not prove that an internal environment is reachable, a credential is valid, an Appium device is connected, or a requested external target is approved for performance load.

The project-level [`ReadMe.md`](../../ReadMe.md) remains the main framework guide and the [`../architecture/`](../architecture/) directory contains the editable architecture assets. This `guides/reference/` library is the detailed, code-oriented canonical path for day-to-day implementation, configuration, troubleshooting, and source navigation. It replaces duplicate short module guides.

## Source references

- **[1]** [`pom.xml`](../../pom.xml) — Maven dependencies, plugins, execution profiles, and suite selection.
- **[2]** [`src/main/java/com/ptaf/`](../../src/main/java/com/ptaf/) — Production framework implementation.
- **[3]** [`src/test/java/com/ptaf/`](../../src/test/java/com/ptaf/) — Runners, step definitions, contract tests, and test support.
- **[4]** [`src/test/resources/`](../../src/test/resources/) — Features, configurations, locators, payloads, and data resources.

## References

[1]: ../../pom.xml "FNB-ETAF Maven build configuration"
[2]: ../../src/main/java/com/ptaf "FNB-ETAF production framework source"
[3]: ../../src/test/java/com/ptaf "FNB-ETAF runners, step definitions, and test support"
[4]: ../../src/test/resources "FNB-ETAF features and configuration resources"
