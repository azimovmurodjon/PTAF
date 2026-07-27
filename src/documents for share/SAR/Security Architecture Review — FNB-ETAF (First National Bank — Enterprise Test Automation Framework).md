# Security Architecture Review — FNB-ETAF (First National Bank — Enterprise Test Automation Framework)

---

## System Overview

### 1. What is the system name?

**FNB-ETAF — First National Bank Enterprise Test Automation Framework**

---

### 2. Provide a description of the system and what it does.

FNB-ETAF is an enterprise-grade, in-house test automation framework developed and maintained by the First National Bank Quality Assurance Engineering team. It is designed to serve as the single, standardised automated testing platform for all FNB digital applications and systems across the entire organisation.

The framework provides a unified, reusable testing capability across six testing domains, enabling FNB QA engineers to write, execute, manage, and report on automated test scenarios for any FNB application:

- **UI Automation:** End-to-end browser-based testing of web applications using Microsoft Playwright. Supports Chrome, Microsoft Edge, Firefox, WebKit, and mobile browser viewport simulation across all FNB web-facing and internal applications.
- **Mobile Native Application Automation:** Native iOS and Android application testing using Appium, supporting both real physical devices and cloud-based emulators/simulators.
- **Mobile Native Browser Automation:** Browser automation on real Android (Chrome) and iOS (Safari) devices using Appium, enabling testing of mobile web experiences on actual device hardware.
- **API Testing:** REST API validation supporting JSON and XML response assertions, authentication header injection, schema validation, and full request/response lifecycle testing for any FNB internal or external-facing API.
- **Performance Testing:** Load and stress testing using the Apache JMeter Java DSL library, executed programmatically within the framework to validate application performance under expected and peak load conditions.
- **Database Testing:** SQL query execution and result validation using JDBC, supporting SQL Server and PostgreSQL, enabling data integrity verification across FNB database systems.
- **XML and CSV Validation:** Structured data validation for XML and CSV files, both from the filesystem and from UI elements rendered within applications.

FNB-ETAF uses a Behaviour-Driven Development (BDD) approach with Cucumber, enabling test scenarios to be written in plain English (Gherkin syntax) that is readable by both technical and non-technical stakeholders. All test scenarios, configurations, and reports are managed within FNB's internal infrastructure.

The framework generates rich HTML and Glass-style PDF test reports using the Extent Reports library, with embedded screenshots, video capture evidence, per-scenario step detail, and per-feature individual reports. Reports serve as audit evidence for quality gates, compliance testing, and release approvals.

FNB-ETAF operates exclusively within FNB's internal network. It does not host any externally accessible services, APIs, or web interfaces. It is a tooling platform used by FNB staff to validate FNB systems — it is not a customer-facing application and has no customer data interaction.

---

### 3. Is this a new software implementation or a software upgrade?

This is a **new software implementation**. FNB-ETAF is a newly developed in-house enterprise test automation framework. It establishes a standardised, enterprise-wide automated testing capability to replace fragmented, ad-hoc, and manual testing approaches previously used across FNB lines of business. No existing vendor product is being upgraded or replaced.

---

### 4. Are servers being installed or replaced as part of this project?

No dedicated servers are being installed solely for FNB-ETAF. The framework is deployed on existing FNB infrastructure:

- **Developer and QA engineer workstations** (Windows and macOS): For local test development, execution, and debugging.
- **Existing CI/CD pipeline servers** (already provisioned and approved): For automated test execution triggered by code deployments and scheduled pipeline runs.
- **Existing test execution servers** within approved QA and Staging environments: For scheduled, on-demand, and regression test runs.

FNB-ETAF is a client-side tooling solution. It does not require dedicated server infrastructure. All test execution connects outbound to existing FNB application servers and databases within already-approved network segments.

---

### 5. Has all licensing been reviewed by IT Service Quality (ITAM)?

All software dependencies used by FNB-ETAF are **open-source libraries** distributed under Apache License 2.0, MIT License, Eclipse Public License 2.0, or equivalent permissive open-source licenses. No commercial software licenses are required.

A complete dependency listing with license types is provided in Question 6. ITAM review is recommended to formally confirm open-source license compliance with FNB's software governance policy.

---

## Architecture and Integration

### 6. What are the software requirements and dependencies for the system? List all software (including versions) that is not part of our base system builds.

All dependencies are open-source Java libraries distributed via Maven Central (https://repo.maven.apache.org). No vendor-provided binaries, commercial software, or proprietary packages are required.

**Core Runtime and Build:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| Java JDK | 21 LTS | GPL v2 with Classpath Exception (OpenJDK) | Runtime and compilation |
| Apache Maven | 3.9.x | Apache License 2.0 | Build automation and dependency management |

**Test Framework:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| Cucumber Java | 7.20.0 | MIT License | BDD test framework — Gherkin scenario execution |
| Cucumber TestNG | 7.20.0 | MIT License | TestNG integration for Cucumber |
| Cucumber PicoContainer | 7.20.0 | MIT License | Dependency injection for step definitions |
| TestNG | 7.10.2 | Apache License 2.0 | Test execution engine and parallel execution |
| JUnit 5 (Jupiter) | 5.10.2 | Eclipse Public License 2.0 | Unit test support |

**UI and Browser Automation:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| Microsoft Playwright (Java) | 1.45.0 | Apache License 2.0 | Cross-browser UI automation (Chrome, Edge, Firefox, WebKit) |
| Playwright Driver Bundle | 1.45.0 | Apache License 2.0 | Bundled Playwright browser binaries |

**Mobile Automation:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| Appium Java Client | 9.4.0 | Apache License 2.0 | Native mobile app and mobile browser automation |
| Selenium API | 4.26.0 | Apache License 2.0 | WebDriver API (used by Appium) |
| Selenium Remote Driver | 4.26.0 | Apache License 2.0 | Remote WebDriver support |
| Selenium Support | 4.26.0 | Apache License 2.0 | WebDriver utility classes |
| Byte Buddy | 1.15.7 | Apache License 2.0 | Runtime code generation (required by Appium) |

**API Testing:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| JSON Path | 3.0.0 | Apache License 2.0 | JSON response path querying |
| JSON Smart | 2.6.0 | Apache License 2.0 | JSON parsing |

**Performance Testing:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| JMeter Java DSL | 1.29.1 | Apache License 2.0 | Programmatic JMeter test execution |
| Apache JMeter Core | 5.5 | Apache License 2.0 | Performance test execution engine |
| Apache JMeter HTTP | 5.5 | Apache License 2.0 | HTTP performance test support |

**Document and Data Validation:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| Apache PDFBox | 2.0.30 | Apache License 2.0 | PDF document content validation |
| Apache POI | 5.2.5 | Apache License 2.0 | Excel test data management and reading |
| Apache POI OOXML | 5.2.5 | Apache License 2.0 | Excel XLSX format support |
| Saxon-HE | 11.3 | Mozilla Public License 2.0 | XML/XPath processing for XML validation |
| Xalan | 2.7.2 | Apache License 2.0 | XSLT processing |

**Database Connectivity:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| mssql-jdbc | 13.4.0.jre11 | MIT License | Microsoft SQL Server JDBC driver |
| PostgreSQL JDBC | 42.7.3 | BSD 2-Clause License | PostgreSQL JDBC driver |

**Reporting:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| extentreports-cucumber7-adapter | 1.14.0 | Apache License 2.0 | Extent Reports Cucumber 7 adapter |
| ExtentReports | 5.1.0 | Apache License 2.0 | Core HTML and report generation engine |
| extent-pdf-report | 2.12.0 | Apache License 2.0 | Glass-style PDF report generation |
| cucumber-pdf-report | 2.14.0 | Apache License 2.0 | Cucumber-specific PDF reporting |
| XChart | 3.8.0 | Apache License 2.0 | Chart generation within PDF reports |
| Freemarker | 2.3.32 | Apache License 2.0 | Template engine used by Extent Reports |
| RxJava3 | 3.1.6 | Apache License 2.0 | Reactive programming (used by Extent Reports) |
| Jsoup | 1.15.3 | MIT License | HTML parsing for report generation |

**Utilities and Infrastructure:**

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| SnakeYAML | 2.2 | Apache License 2.0 | YAML configuration file parsing |
| SLF4J API | 2.0.16 | MIT License | Logging facade |
| Log4j2 API | 2.25.3 | Apache License 2.0 | Logging implementation |
| Log4j2 Core | 2.25.3 | Apache License 2.0 | Logging core engine |
| Commons IO | 2.15.1 | Apache License 2.0 | File I/O utility classes |
| Commons Codec | 1.16.0 | Apache License 2.0 | Encoding and decoding utilities |
| Commons Collections | 4.4 | Apache License 2.0 | Extended Java collections |
| Commons Compress | 1.25.0 | Apache License 2.0 | Archive and compression support |
| Lombok | 1.18.34 | MIT License | Java boilerplate code reduction |
| OpenTelemetry SDK | 1.43.0 | Apache License 2.0 | Telemetry and tracing (used by Selenium) |
| Kotlin stdlib | 1.6.21 | Apache License 2.0 | Kotlin runtime (required by Appium client) |
| Failsafe | 3.3.2 | Apache License 2.0 | Retry and resilience (used by Selenium) |

**External Process Dependencies (not Java libraries — installed separately):**

| Component | Version | License | Purpose |
|:---|:---|:---|:---|
| Appium Server | 2.x | Apache License 2.0 | Mobile automation server — runs as a local process on the test execution machine |
| Appium UiAutomator2 Driver | Latest stable | Apache License 2.0 | Android native app automation driver |
| Appium XCUITest Driver | Latest stable | Apache License 2.0 | iOS native app automation driver |
| Node.js | 18 LTS or 20 LTS | MIT License | Appium Server runtime environment |

All Java dependencies are resolved from **Maven Central** (https://repo.maven.apache.org). Appium Server and drivers are installed from the **npm registry** (https://registry.npmjs.org). Both repositories may be proxied through FNB's internal JFrog Artifactory instance to enforce dependency governance.

---

### 7. Is this solution deployed internally, hosted in the cloud, or implemented as a hybrid?

FNB-ETAF is deployed **entirely on-premises within FNB's internal network**. There are no cloud-hosted components associated with the framework itself.

- **Test execution** occurs on FNB-managed developer workstations, CI/CD pipeline servers, and dedicated QA test execution servers — all within FNB's internal network.
- **Appium Server** runs as a local process on the test execution machine and is not exposed on any external network interface.
- **Playwright browsers** (Chromium, Firefox, WebKit) are launched as local child processes on the test execution machine.
- **Target applications under test** are hosted on FNB's existing on-premises or approved cloud infrastructure. FNB-ETAF connects to those applications over standard HTTPS (port 443) from within the FNB internal network.
- **No cloud services** are invoked by FNB-ETAF itself. The framework does not send data to any external cloud platform.

There is no cloud application stack associated with FNB-ETAF.

---

### 8. Is there vendor provided hardware or virtual images associated with this system?

No. FNB-ETAF is a pure software framework distributed as open-source Java libraries. There is no vendor-provided hardware, virtual machine image, or appliance associated with this system.

---

### 9. Does this system have a web interface that external bank customers authenticate into?

No. FNB-ETAF has no web interface of any kind accessible by external bank customers or any external party. It is an internal tooling platform used exclusively by FNB QA engineers and developers. There is no customer-facing component, no login portal, and no externally accessible endpoint.

---

### 10. What regions will be implemented for this system?

FNB-ETAF is an enterprise-wide tooling platform used across all FNB environments. The framework itself does not reside in any specific environment — it is a client-side tool that connects to whichever target environment is configured in its YAML configuration files at runtime.

| Environment | Usage |
|:---|:---|
| **Development (Dev)** | Developers run FNB-ETAF locally to validate code changes before committing |
| **QA / Test** | Automated regression, smoke, and integration test suites run against QA environments |
| **Staging / UAT** | Full regression suites run against Staging environments before production deployments |
| **Production** | Smoke tests and post-deployment validation tests run to confirm production health |

---

### 11. Provide a detailed technical network diagram of the solution.

A Microsoft Visio network diagram will be provided separately by the FNB IT team. The diagram will include:

- FNB-ETAF test execution machines (developer workstations and CI/CD servers) within the FNB internal network trust zone.
- Outbound HTTPS connections (port 443, TLS 1.2/1.3) from test execution machines to FNB application servers across all environments.
- Outbound JDBC connections (port 1433, TLS encrypted) from test execution machines to FNB SQL Server database servers.
- Local Appium Server process on the test execution machine communicating with connected mobile devices via USB or local Wi-Fi network (no external network exposure).
- Outbound HTTPS connections (port 443) from CI/CD build servers to Maven Central or FNB's internal JFrog Artifactory proxy for dependency resolution during build.
- No inbound connections to FNB-ETAF from any external system.
- Network segmentation showing test execution machines within the FNB internal trust zone.
- FNB server names, IP addresses, operating system versions, and SQL Server versions for all target systems.

**Key architectural characteristics:**
- All connections from FNB-ETAF to target applications are **outbound only** from the test execution machine.
- No ports are opened on the test execution machine to accept inbound connections from any external system.
- All HTTPS traffic uses TLS 1.2 or higher with FNB-approved cipher suites.
- Appium Server binds to localhost only and is not accessible from the network.

---

### 12. Provide a detailed listing of Firewall rule changes required with this design.

All connections are **outbound from FNB-ETAF test execution machines** to target systems within FNB's internal network. No inbound firewall rules are required.

| Source System | Source Region | Destination System | Destination Region | Port | Protocol | Purpose |
|:---|:---|:---|:---|:---|:---|:---|
| Test execution machine / CI/CD server | All (Dev/QA/Staging/Prod) | FNB application servers (web tier) | Corresponding environment | 443 | HTTPS/TLS | UI and API test execution against FNB web applications |
| Test execution machine / CI/CD server | All | FNB SQL Server database servers | Corresponding environment | 1433 | TCP/JDBC over TLS | Database validation test execution |
| CI/CD build server | Internal | Maven Central (repo.maven.apache.org) or FNB JFrog Artifactory | N/A (internet / internal) | 443 | HTTPS | Java dependency download during build |
| CI/CD build server | Internal | npm registry (registry.npmjs.org) or FNB Artifactory npm proxy | N/A (internet / internal) | 443 | HTTPS | Appium Server and driver installation |
| Test execution machine | Internal | FNB JFrog Artifactory (if applicable) | Internal | 443 | HTTPS | Internal artifact repository access |

Specific source and destination IP addresses, DNS names, and server names will be provided in the accompanying firewall rule spreadsheet once environment-specific server details are confirmed with the FNB infrastructure team.

---

### 13. List all network ports, protocols, and security ciphers the application uses.

| Port | Protocol | Direction | Purpose | Security |
|:---|:---|:---|:---|:---|
| 443 | HTTPS (TLS 1.2 / TLS 1.3) | Outbound | All web application and API test connections to FNB application servers | TLS 1.2 minimum; TLS 1.3 preferred; FNB-standard cipher suites |
| 1433 | TCP with TLS (JDBC) | Outbound | SQL Server database connections | `encrypt=true`; `trustServerCertificate=true` for internal certificates; TLS 1.2 minimum |
| 4723 | HTTP | Localhost loopback only | Appium Server communication between test process and local Appium daemon | Localhost only — bound to 127.0.0.1; not exposed on any network interface |
| 443 | HTTPS (TLS 1.2 / TLS 1.3) | Outbound | Maven Central / npm registry / JFrog Artifactory dependency downloads during build | TLS 1.2 minimum |

No non-standard or custom ports are used for any external communication. All external traffic uses HTTPS on port 443.

---

### 14. Are data file transmissions required for this system?

No MoveIt or scheduled automated file transmissions are required. FNB-ETAF reads test input data from local YAML configuration files and Excel spreadsheets stored on the test execution machine or in the source code repository. Test output (HTML and PDF reports, screenshots) is written to local directories on the test execution machine or to an internal network share accessible within FNB's network. No automated file transfers to external systems are performed.

---

### 15. Will data migration occur as part of this effort?

No. FNB-ETAF does not store, own, or migrate application data. It reads test configuration from files and writes test results to report files. No data migration is required or performed.

---

### 16. Are server file shares required?

Test reports generated by FNB-ETAF may optionally be published to an existing FNB internal network share for team-wide visibility and audit evidence retention. If a network share is used, it will be an existing FNB-approved internal file share governed by standard FNB access controls. No new file shares need to be created specifically for FNB-ETAF. The CI/CD service account requires read/write access to the designated report output directory only.

---

### 17. Does this system require additional architecture for failover?

No. FNB-ETAF is a stateless test execution tool. Each test run is independent. If a test run fails due to infrastructure issues, the run can be re-triggered. No failover architecture, load balancing, or high-availability configuration is required for the framework itself.

---

### 18. Does this system require SMTP for email relay?

FNB-ETAF does not directly use SMTP for email relay. Test result notifications are delivered through the existing CI/CD pipeline notification system (e.g., Jenkins email plugin or equivalent), which uses FNB's existing approved SMTP relay infrastructure. No new SMTP configuration is required for FNB-ETAF. Test reports are delivered as file artifacts (HTML, PDF) to shared directories and CI/CD build artefact stores.

---

## Security and Access

### 19. What data types reside within this system?

FNB-ETAF handles the following data types during test execution:

| Data Type | Classification | Protection Method |
|:---|:---|:---|
| Test configuration (URLs, environment settings, locator definitions) | Internal | Stored in YAML files in source control; no sensitive credentials stored in plain text |
| Test credentials (usernames and passwords for dedicated test accounts) | Confidential | Stored as environment variables or in FNB's approved secrets management solution; never hardcoded in source code or committed to version control |
| Test input data (form field values, test parameters) | Internal | Stored in Excel files; synthetic data only — no real customer PII is used |
| Test report artefacts (HTML reports, PDF reports, screenshots, video recordings) | Internal / Confidential | Stored on internal network shares; may contain screenshots of application UI; treated as confidential if any application data is visible |
| API response data (captured during test execution) | Confidential | Held in memory during test execution only; not persisted to disk unless explicitly captured in a test report |

**PII Policy:** FNB-ETAF is designed and governed to use **synthetic test data exclusively**. Real customer personally identifiable information (PII), payment card data (PCI), or any regulated data must not be used in automated test scenarios. Dedicated test accounts with synthetic data are provisioned specifically for automation purposes by the FNB test data management process. Any test report that inadvertently captures real PII must be treated as confidential and managed in accordance with FNB's data classification and retention policies.

---

### 20. Is development occurring on this system?

Yes. FNB-ETAF is developed entirely in-house by the FNB QA Engineering team.

- **Development team:** FNB internal QA engineers and automation developers.
- **Code ownership:** FNB owns 100% of the FNB-ETAF source code. No third-party vendor owns or has access to the codebase.
- **Source control:** All code is stored in FNB's internal Git repository. All changes are submitted via pull request and require peer review before merging to the main branch.
- **Secure coding practices:** Static code analysis is performed as part of the Maven build process. OWASP Dependency Check is used to identify known CVEs in open-source dependencies. No credentials, secrets, or sensitive configuration are stored in source code.
- **SDLC:** FNB-ETAF follows FNB's standard Software Development Lifecycle (SDLC) including requirements review, development, code review, testing, and change management approval.
- **Code proxy:** Maven dependencies are resolved through FNB's internal JFrog Artifactory proxy where configured, enabling governance of permitted open-source libraries.

---

### 21. Explain how the system is kept up to date and maintained.

FNB-ETAF is maintained by the FNB QA Engineering team under the following maintenance model:

- **Framework updates:** Source code changes are managed through FNB's standard change management process. All updates are peer-reviewed, tested, and approved before deployment.
- **Dependency security patching:** Open-source library versions are reviewed on a quarterly basis. Critical CVEs identified by OWASP Dependency Check or FNB's vulnerability management programme are remediated within the timeframes defined by FNB's patch management policy.
- **Browser binary management:** Playwright browser binaries are version-pinned in the Maven build configuration and updated as part of the quarterly dependency review cycle.
- **Mobile driver updates:** Appium drivers (UiAutomator2 for Android, XCUITest for iOS) are updated when new mobile operating system versions require compatibility changes.
- **No external vendor patch cycle:** As an in-house framework, FNB-ETAF has no external vendor responsible for patching. The FNB QA Engineering team is solely responsible for all maintenance and security updates.

---

### 22. Are changes required to bypass Anti-virus real-time protection?

FNB-ETAF launches browser processes (Chromium, Firefox, WebKit) and Appium Server as child processes during test execution. These are well-known, widely-used open-source applications. If AV real-time protection interferes with browser binary management or process launching, directory exclusions may be required for:

- Playwright browser binary cache directory (typically `%USERPROFILE%\.cache\ms-playwright` on Windows or `~/.cache/ms-playwright` on Linux/macOS).
- Appium Server Node.js process and driver binaries.

Any AV exclusions must be reviewed and formally approved by the FNB Information Security team before implementation, following FNB's AV exclusion request process.

---

### 23. What logging capabilities does the system have? Is it able to SysLog?

FNB-ETAF uses **Apache Log4j2** for all application logging. Log output includes:

- Test execution progress (scenario start/end, step pass/fail, duration).
- Framework configuration loading and validation events.
- Browser and mobile driver initialisation and teardown events.
- Error messages and stack traces for failed test steps.
- Soft assertion failure summaries.
- Report generation completion events.

Log output destinations:
- **Console (stdout/stderr):** Captured by CI/CD pipeline log aggregation systems.
- **Log files:** Written to the local test execution machine; configurable path via `log4j2.xml`.

**SysLog capability:** FNB-ETAF can be configured to forward logs to a SysLog endpoint by adding a SysLog appender to the Log4j2 configuration. This capability is available but not enabled by default. It can be activated upon request from the FNB Security Operations or IT Operations team without any code changes.

---

### 24. Is there fraud potential with this system?

FNB-ETAF is an internal QA tooling platform with no customer-facing interface. Direct fraud potential is low. The primary risk is unauthorised use of test account credentials to access FNB application environments. This risk is mitigated by:

- Test accounts are provisioned with the minimum permissions required for test execution only (no access to production data or production systems).
- Test credentials are stored in environment variables or FNB's approved secrets management solution, not in source code or configuration files.
- Access to FNB-ETAF source code and test credentials is restricted to authorised FNB QA team members through FNB's standard access control processes.
- Test accounts are segregated from production accounts and are subject to FNB's standard account lifecycle management.

---

### 25. Is this a SOX application?

FNB-ETAF itself is not a SOX-in-scope application — it is an internal testing tool. However, FNB-ETAF is used to test SOX-relevant FNB applications and systems. Test execution evidence (screenshots, HTML reports, PDF reports) generated by FNB-ETAF may be used as audit evidence for SOX compliance testing and quality gate approvals. Such evidence artefacts must be retained in accordance with FNB's SOX evidence retention policy and stored in FNB-approved audit evidence repositories.

---

### 26. Does this system integrate with Active Directory?

FNB-ETAF does not directly integrate with Active Directory (AD) for its own authentication or authorisation. Access to FNB-ETAF source code, CI/CD pipeline configuration, and test execution infrastructure is governed by FNB's standard AD-based access controls applied to the Git repository and CI/CD server platforms. No LDAP queries or AD authentication calls are made by the FNB-ETAF framework itself during test execution.

---

### 27. Does the system support Multi-Factor Authentication?

FNB-ETAF does not have its own authentication system and therefore does not implement MFA directly. Access to the framework is controlled through:

- **Git repository access:** Governed by FNB's standard AD/MFA-enforced source control authentication.
- **CI/CD server access:** Governed by FNB's standard AD/MFA-enforced CI/CD platform authentication.
- **Test execution machine access:** Governed by FNB's standard workstation login policies including MFA where applicable per FNB policy.

When FNB-ETAF test scenarios execute against MFA-protected FNB applications, MFA handling is managed through test-environment-specific mechanisms such as dedicated test accounts with MFA bypass configurations approved for QA environments, or TOTP-based test accounts.

---

### 28. What service accounts are required?

| Service Account | Purpose | Minimum Permissions Required |
|:---|:---|:---|
| CI/CD Pipeline Service Account | Executes FNB-ETAF test runs within the CI/CD pipeline | Read access to source code repository; write access to test report output directory; outbound network access to FNB application servers and SQL Server within test environments |
| Test Automation Database Account | Executes SQL validation queries during database test scenarios | Read-only access to designated test database schemas; no write, DDL, or production data access |
| Test Application Accounts (per application) | Simulates end-user actions within FNB applications during test execution | Standard end-user permissions within test environments only; no production environment access; no elevated or administrative privileges |

All service accounts must comply with FNB's principle of least privilege. Passwords and credentials must be stored in FNB's approved secrets management solution and rotated in accordance with FNB's password management policy. Service account access must be reviewed as part of FNB's periodic access review cycle.

---

### 29. Do default, built-in accounts, or back-door access exist in the system?

No. FNB-ETAF is a custom in-house framework with no built-in user management system, no default accounts, and no back-door access mechanisms. There is no login interface, no user database, and no authentication system within the framework itself. All access to the framework is controlled through FNB's standard infrastructure access controls.

---

### 30. What account is used to perform installations and upgrades?

FNB-ETAF is installed by executing `mvn clean install` on the target machine. This uses the standard developer workstation account or the CI/CD service account. No elevated, administrative, or root privileges are required for installation or upgrade. Maven resolves and downloads dependencies from Maven Central or FNB's internal Artifactory proxy and compiles the Java source code using standard user permissions.

---

### 31. Does an outside 3rd party access the system for support purposes?

No. FNB-ETAF is developed and maintained entirely by FNB's internal QA Engineering team. No third-party vendor, contractor, or external party has access to the FNB-ETAF codebase, test execution infrastructure, or test credentials. All support and maintenance is performed by FNB staff.

---

### 32. Does the system store passwords in cleartext?

No. FNB-ETAF does not store passwords or credentials in cleartext. Credential management follows these controls:

- All test account credentials and service account passwords are stored as **environment variables** on the test execution machine or CI/CD server, resolved at runtime.
- Where FNB's approved secrets management solution is available (e.g., CyberArk, HashiCorp Vault, or equivalent), credentials are retrieved programmatically at runtime using the secrets management API.
- No credentials, passwords, API keys, or tokens are hardcoded in source code, configuration files, or any artefact committed to version control.
- Configuration files reference credentials using variable substitution (e.g., `${DB_PASSWORD}`) that is resolved from the runtime environment.

---

## Database and Data Management

### 33. Does the system require a database?

FNB-ETAF does not require a dedicated database for its own operation. It connects to **existing FNB database servers** within test environments for database validation test scenarios. Connection details:

- **SQL Server Driver:** Microsoft JDBC Driver for SQL Server (mssql-jdbc 13.4.0.jre11).
- **Connection security:** JDBC over TLS with `encrypt=true` configured. Internal certificates are trusted via `trustServerCertificate=true` for FNB internal CA-signed certificates.
- **Authentication:** SQL Server authentication using a dedicated read-only test service account. Windows Integrated Authentication (Kerberos) is also supported where applicable.
- **Access level:** Read-only queries for data validation assertions. No schema changes, data inserts, updates, or deletes are performed by FNB-ETAF against any database.
- **Data classification:** FNB-ETAF database tests operate exclusively on test schemas containing synthetic data. No production data is accessed.

---

### 34. What are the uptime requirements?

FNB-ETAF is a test execution tool with no independent uptime requirement. Test runs are scheduled or triggered on-demand through the CI/CD pipeline. The availability of FNB-ETAF test execution is dependent on the availability of the CI/CD infrastructure, which is governed by FNB's existing CI/CD platform SLA. No additional uptime SLA is required for FNB-ETAF itself.

---

## User Management

### 35. Who are the approvers of access for the system?

Access to FNB-ETAF source code, CI/CD pipeline configuration, and test credentials is approved by the **FNB QA Engineering Manager** in accordance with FNB's standard IT access request and approval process. Access requests must be submitted through FNB's standard access management system.

---

### 36. What LOBs access the system, and how many users are there total?

FNB-ETAF is an enterprise-wide platform available to all FNB lines of business that require automated testing capability.

| User Group | Role | Approximate Count |
|:---|:---|:---|
| FNB QA Engineering | Test automation developers and engineers — full framework access | 10–30 |
| FNB DevOps / CI/CD Operations | Pipeline administrators — CI/CD configuration access | 3–8 |
| FNB Application Development | Developers reviewing test results — read-only report access | 20–50 |
| FNB QA Analysts (non-developer) | Test scenario execution and report review | 10–30 |

User counts will grow as FNB-ETAF is adopted across additional lines of business. Access is managed through FNB's standard role-based access control process.

---

### 37. Who are the Application admins, DB Admins, and other Admin functions?

| Role | Responsibility |
|:---|:---|
| FNB-ETAF Framework Owner / Application Admin | FNB QA Engineering Lead — owns the framework source code, approves dependency updates, manages test credential governance, and approves access requests |
| CI/CD Platform Admin | FNB DevOps team — manages CI/CD pipeline configuration and service account permissions for automated test execution |
| Database Admin | FNB DBA team — provisions and manages the read-only test service account for SQL Server connections used by FNB-ETAF |

---

### 38. Detail the user permission setup.

| Role | Access Level | Description |
|:---|:---|:---|
| QA Automation Developer | Read / Write | Full access to FNB-ETAF source code; can create and modify test scenarios, configurations, page objects, and framework components |
| QA Engineer (non-developer) | Read / Execute | Can execute test runs and view reports; cannot modify framework source code or configuration |
| CI/CD Service Account | Execute only | Can trigger test runs in the pipeline; cannot modify source code or access test credentials directly |
| Application Developer | Read (reports only) | Can view test execution reports; no access to test credentials, framework configuration, or source code |
| FNB-ETAF Framework Owner | Administrator | Full access including dependency governance, credential rotation, access approval, and framework architecture decisions |

**Separation of duties** is enforced through:
- Git branch protection rules requiring pull request review before any code is merged to the main branch.
- CI/CD pipeline access controls restricting who can modify pipeline configuration.
- Secrets management controls restricting access to test credentials to authorised roles only.
- Read-only access for report consumers, preventing any modification of test artefacts.

---

## AI Usage and Architecture

### 39. Is AI utilised in this solution?

FNB-ETAF does not incorporate AI or machine learning components in its current implementation. All test logic is deterministic, rule-based, and fully auditable. Test scenarios are authored by FNB QA engineers and executed without AI-driven decision-making.

A future roadmap item includes AI-assisted test generation capabilities (using large language model APIs to generate Cucumber test scenarios from application specifications), but this functionality is not part of the current implementation being submitted for SAR review. A separate SAR addendum will be submitted prior to implementing any AI capabilities.

---

### 40–43. AI backend, GenAI, model type, training approach.

Not applicable. No AI components are present in the current FNB-ETAF implementation.

---

## Risk and Governance

### 44–48. AI risk assessments, OpsRisk review, safeguards, monitoring, human-in-the-loop controls.

Not applicable. No AI components are present in the current FNB-ETAF implementation. If AI-assisted test generation is introduced in a future release, a full OpsRisk review and AI governance assessment will be completed and documented in a separate SAR addendum prior to deployment.

---

## Data and Privacy

### 49–50. Training data, user data handling in AI interactions.

Not applicable. No AI components are present in the current FNB-ETAF implementation.

---

*Document prepared by: FNB QA Engineering Team*
*Framework name: FNB-ETAF — First National Bank Enterprise Test Automation Framework*
*Review requested from: FNB Security Architecture Team*
*Date: July 2026*
