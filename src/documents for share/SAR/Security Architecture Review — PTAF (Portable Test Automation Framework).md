# Security Architecture Review — PTAF (Portable Test Automation Framework)

---

## System Overview

### 1. What is the system name?

**PTAF — Portable Test Automation Framework**

PTAF is an in-house enterprise test automation framework developed by the FNB Quality Assurance team to support automated testing of all FNB digital applications, including the eStore Common App (eCA), Mortgage Borrower/Loan Officer views, In-Branch Console-less application, and supporting APIs.

---

### 2. Provide a description of the system and what it does.

PTAF is a Java-based, Cucumber BDD (Behaviour-Driven Development) test automation framework built on top of the following open-source testing libraries: Playwright (UI browser automation), Appium (native mobile app and mobile browser automation), REST Assured (API testing), JMeter DSL (performance testing), Apache PDFBox (PDF validation), and Apache POI (Excel data management).

The framework enables FNB QA engineers and testers to write, execute, and report on automated test scenarios across six testing domains:

- **UI Automation:** End-to-end browser-based testing of web applications using Playwright. Supports Chrome, Edge, Firefox, WebKit, and mobile browser simulation.
- **Mobile Native App Automation:** Native iOS and Android app testing using Appium with real devices and cloud emulators.
- **Mobile Native Browser Automation:** Browser automation on real Android (Chrome) and iOS (Safari) devices using Appium.
- **API Testing:** REST API validation using REST Assured, supporting JSON/XML response assertions, authentication header injection, and schema validation.
- **Performance Testing:** Load and stress testing using the JMeter Java DSL library, executed programmatically within the framework.
- **Database Testing:** SQL Server query execution and result validation using JDBC.

The framework generates rich HTML and PDF test reports using the Extent Reports (Grasshopper) library, with screenshots, video capture, and per-feature individual reports.

PTAF runs exclusively within FNB's internal network on developer and QA engineer workstations, CI/CD pipeline servers, and dedicated test execution servers. It does not host any externally accessible services, APIs, or web interfaces. It is a tooling framework used by FNB staff to validate other FNB systems — it is not a customer-facing application.

---

### 3. Is this a new software implementation or a software upgrade?

This is a **new software implementation**. PTAF is a newly developed in-house test automation framework. It replaces manual and ad-hoc testing approaches previously used by the FNB QA team. No existing vendor product is being upgraded or replaced.

---

### 4. Are servers being installed or replaced as part of this project?

No dedicated servers are being installed solely for PTAF. The framework runs on:

- **Developer and QA engineer workstations** (Windows/macOS): For local test development and execution.
- **Existing CI/CD pipeline servers** (Jenkins or equivalent, already provisioned): For automated test execution triggered by code deployments.
- **Existing test execution servers** (already provisioned as part of the QA/Staging environment): For scheduled and on-demand test runs.

No new server provisioning is required for PTAF itself. The framework is a client-side tooling solution that connects to existing test environments.

---

### 5. Has all licensing been reviewed by IT Service Quality (ITAM)?

All software dependencies used by PTAF are **open-source libraries** licensed under Apache License 2.0, MIT License, or equivalent permissive open-source licenses. No commercial software licenses are required. A full dependency list with license types is provided in Question 6.

ITAM review is recommended to formally confirm open-source license compliance with FNB policy.

---

## Architecture and Integration

### 6. What are the software requirements and dependencies for the system? List all software (including versions) that is not part of our base system builds.

The following table lists all software dependencies required by PTAF. All are open-source libraries distributed via Maven Central (Java package repository). No vendor-provided binaries or commercial software are required.

| Dependency | Version | License | Purpose |
|:---|:---|:---|:---|
| **Java JDK** | 21 (LTS) | GPL v2 with Classpath Exception (OpenJDK) | Runtime and compilation |
| **Apache Maven** | 3.9.x | Apache License 2.0 | Build and dependency management |
| **Cucumber Java** | 7.20.0 | MIT License | BDD test framework |
| **Cucumber TestNG** | 7.20.0 | MIT License | TestNG integration for Cucumber |
| **TestNG** | 7.10.2 | Apache License 2.0 | Test execution engine |
| **Microsoft Playwright (Java)** | 1.45.0 | Apache License 2.0 | UI browser automation |
| **Appium Java Client** | 9.4.0 | Apache License 2.0 | Mobile native app and browser automation |
| **Selenium API** | 4.26.0 | Apache License 2.0 | WebDriver API (used by Appium) |
| **REST Assured** | (via transitive) | Apache License 2.0 | API testing |
| **JMeter Java DSL** | 1.29.1 | Apache License 2.0 | Performance testing |
| **Apache JMeter Core** | 5.5 | Apache License 2.0 | Performance test execution engine |
| **Apache PDFBox** | 2.0.30 | Apache License 2.0 | PDF document validation |
| **Apache POI** | 5.2.5 | Apache License 2.0 | Excel test data management |
| **extentreports-cucumber7-adapter** | 1.14.0 | Apache License 2.0 | Extent HTML/PDF report generation |
| **extent-pdf-report** | 2.12.0 | Apache License 2.0 | Glass-style PDF report generation |
| **cucumber-pdf-report** | 2.14.0 | Apache License 2.0 | Cucumber-specific PDF reporting |
| **ExtentReports** | 5.1.0 | Apache License 2.0 | Core reporting engine |
| **SnakeYAML** | 2.2 | Apache License 2.0 | YAML configuration file parsing |
| **SLF4J API** | 2.0.16 | MIT License | Logging facade |
| **Log4j2** | 2.25.3 | Apache License 2.0 | Logging implementation |
| **Jackson (JSON)** | 3.0.0 (via json-path) | Apache License 2.0 | JSON parsing |
| **Commons IO** | 2.15.1 | Apache License 2.0 | File I/O utilities |
| **Commons Codec** | 1.16.0 | Apache License 2.0 | Encoding utilities |
| **mssql-jdbc** | 13.4.0.jre11 | MIT License | SQL Server JDBC driver |
| **PostgreSQL JDBC** | 42.7.3 | BSD 2-Clause | PostgreSQL JDBC driver (optional) |
| **Lombok** | 1.18.34 | MIT License | Java boilerplate reduction |
| **Byte Buddy** | 1.15.7 | Apache License 2.0 | Runtime code generation (used by Appium) |
| **OpenTelemetry SDK** | 1.43.0 | Apache License 2.0 | Telemetry (used by Selenium) |
| **Kotlin stdlib** | 1.6.21 | Apache License 2.0 | Kotlin runtime (used by Appium) |
| **JUnit 5** | 5.10.2 | Eclipse Public License 2.0 | Unit test support |
| **Freemarker** | 2.3.32 | Apache License 2.0 | Template engine (used by Extent Reports) |
| **RxJava3** | 3.1.6 | Apache License 2.0 | Reactive programming (used by Extent Reports) |
| **XChart** | 3.8.0 | Apache License 2.0 | Chart generation in PDF reports |
| **Saxon-HE** | 11.3 | Mozilla Public License 2.0 | XML/XPath processing |
| **Xalan** | 2.7.2 | Apache License 2.0 | XSLT processing |

**Appium Server** (external process, not a Java dependency):

| Component | Version | License | Purpose |
|:---|:---|:---|:---|
| **Appium Server** | 2.x | Apache License 2.0 | Mobile automation server (runs locally on test execution machine) |
| **Appium UiAutomator2 Driver** | Latest | Apache License 2.0 | Android native automation driver |
| **Appium XCUITest Driver** | Latest | Apache License 2.0 | iOS native automation driver |
| **Node.js** | 18+ LTS | MIT License | Appium server runtime |

All dependencies are downloaded from **Maven Central** (https://repo.maven.apache.org) and **npm registry** (https://registry.npmjs.org) during the build process. No third-party package repositories are used.

---

### 7. Is this solution deployed internally, hosted in the cloud, or implemented as a hybrid?

PTAF is deployed **entirely on-premises within FNB's internal network**. There are no cloud-hosted components.

- **Test execution** occurs on developer workstations, CI/CD servers, and QA test execution servers — all within FNB's internal network.
- **Appium Server** runs as a local process on the test execution machine (not exposed externally).
- **Playwright browsers** (Chromium, Firefox, WebKit) are launched as local processes on the test execution machine.
- **Target applications under test** (eCA, Mortgage, In-Branch) are hosted on FNB's existing on-premises infrastructure.
- **No cloud services** are invoked by PTAF itself. The framework connects only to FNB-hosted application endpoints.

There is no cloud application stack associated with PTAF.

---

### 8. Is there vendor provided hardware or virtual images associated with this system?

No. PTAF is a pure software framework with no vendor-provided hardware or virtual machine images. All software is distributed as open-source Java libraries via Maven Central.

---

### 9. Does this system have a web interface that external bank customers authenticate into?

No. PTAF does not have any web interface accessible by external bank customers. It is an internal tooling framework used exclusively by FNB QA engineers and developers. There is no customer-facing component, no login portal, and no externally accessible endpoint.

---

### 10. What regions will be implemented for this system?

PTAF will be used across all FNB test environments to validate applications in those environments:

| Region | Purpose |
|:---|:---|
| **Development (Dev)** | Developers run PTAF locally to validate code changes before committing |
| **QA / Test** | Automated regression and smoke test suites run against the QA environment |
| **Staging (UAT)** | Full regression suites run against the Staging environment before production deployments |
| **Production** | Smoke tests and synthetic monitoring tests run post-deployment to validate production health |

PTAF itself does not reside in any specific environment — it is a client-side tool that connects to whichever environment is configured in its YAML configuration files.

---

### 11. Provide a detailed technical network diagram of the solution.

A Microsoft Visio network diagram will be provided separately by the FNB IT team. The diagram will include:

- Test execution machines (developer workstations and CI/CD servers) within the FNB internal network.
- Outbound connections from test execution machines to FNB application servers (eCA, Mortgage, In-Branch) on port 443 (HTTPS).
- Outbound connections from test execution machines to FNB SQL Server on port 1433 (JDBC).
- Local Appium Server process on the test execution machine communicating with connected mobile devices via USB or local network.
- Maven Central dependency download path (outbound port 443 from build servers to repo.maven.apache.org).
- No inbound connections to PTAF from any external system.

**Key network characteristics:**
- All connections from PTAF to target applications are **outbound only** from the test execution machine.
- No ports are opened on the test execution machine to accept inbound connections.
- All HTTPS traffic uses TLS 1.2 or higher.

---

### 12. Provide a detailed listing of Firewall rule changes required with this design.

PTAF requires the following firewall rules to function. All connections are **outbound from test execution machines** to target systems.

| Source | Destination | Port | Protocol | Purpose |
|:---|:---|:---|:---|:---|
| Test execution machine (internal) | FNB eCA application servers (QA/Staging/Prod) | 443 | HTTPS/TLS | UI and API test execution against eCA |
| Test execution machine (internal) | FNB Mortgage application servers (QA/Staging/Prod) | 443 | HTTPS/TLS | UI and API test execution against Mortgage app |
| Test execution machine (internal) | FNB In-Branch application servers (QA/Staging/Prod) | 443 | HTTPS/TLS | UI and API test execution against In-Branch app |
| Test execution machine (internal) | FNB SQL Server (QA/Staging/Prod) | 1433 | TCP/JDBC | Database validation tests |
| Test execution machine (internal) | repo.maven.apache.org | 443 | HTTPS | Maven dependency download during build |
| Test execution machine (internal) | registry.npmjs.org | 443 | HTTPS | Appium driver download during setup |
| Test execution machine (internal) | FNB JFrog/Artifactory (if applicable) | 443 | HTTPS | Internal artifact repository |
| CI/CD server (internal) | All above destinations | 443 / 1433 | HTTPS/TCP | Same as above for automated pipeline execution |

No inbound firewall rules are required for PTAF.

---

### 13. List all network ports, protocols, and security ciphers the application uses.

| Port | Protocol | Direction | Purpose | Cipher/Security |
|:---|:---|:---|:---|:---|
| 443 | HTTPS (TLS 1.2/1.3) | Outbound | All web application and API test connections | TLS 1.2 minimum, TLS 1.3 preferred |
| 1433 | TCP (JDBC over TLS) | Outbound | SQL Server database connections | TLS encryption with `encrypt=true` and `trustServerCertificate=true` for internal certificates |
| 4723 | HTTP (localhost only) | Local loopback | Appium Server communication (test machine to local Appium process only) | Localhost only — not exposed on network interface |
| 443 | HTTPS | Outbound | Maven Central / npm registry dependency downloads | TLS 1.2/1.3 |

No custom or non-standard ports are used. All external communication uses standard HTTPS on port 443.

---

### 14. Are data file transmissions required for this system?

No MoveIt or scheduled file transmissions are required. PTAF reads test data from local YAML and Excel files on the test execution machine. Test reports (HTML and PDF) are written to local directories on the test execution machine or to a shared network drive accessible within the FNB internal network. No automated file transfers to external systems are performed.

---

### 15. Will data migration occur as part of this effort?

No. PTAF does not store or migrate application data. It reads test data from configuration files and writes test results to report files. No data migration is required.

---

### 16. Are server file shares required?

Test reports generated by PTAF may optionally be written to a shared network drive for team visibility. If a network share is used, it will be an existing FNB internal file share with standard FNB access controls. No new file shares need to be created specifically for PTAF. Read/write access for the CI/CD service account to the report output directory is the only requirement.

---

### 17. Does this system require additional architecture for failover?

No. PTAF is a stateless test execution tool. If a test run fails due to infrastructure issues, the test can be re-executed. No failover architecture is required.

---

### 18. Does this system require SMTP for email relay?

PTAF does not currently use SMTP for email relay. Test results are delivered via HTML and PDF reports written to shared directories, and optionally via CI/CD pipeline notifications (Jenkins email plugin or equivalent) which use the existing FNB SMTP relay already approved for CI/CD systems. No new SMTP configuration is required for PTAF itself.

---

## Security and Access

### 19. What data types reside within this system?

PTAF handles the following data types:

| Data Type | Classification | Protection Method |
|:---|:---|:---|
| **Test configuration** (URLs, environment settings) | Internal | Stored in YAML files in source control; no sensitive credentials stored in plain text |
| **Test credentials** (usernames/passwords for test accounts) | Confidential | Stored as environment variables or in a secrets manager; never hardcoded in source code |
| **Test data** (synthetic/mock data for form inputs) | Internal | Stored in Excel files; uses synthetic data only — no real customer PII |
| **Test reports** (screenshots, HTML/PDF reports) | Internal | Stored on internal network shares; may contain screenshots of application UI |
| **Application responses** (API response bodies captured during tests) | Confidential | Stored temporarily in memory during test execution; not persisted to disk unless explicitly captured in reports |

**PII Handling:** PTAF is designed to use **synthetic test data only**. Real customer PII (names, SSNs, account numbers) must not be used in automated test scenarios. Test accounts with synthetic data are provisioned specifically for automation purposes. If any test scenario inadvertently captures real PII in a screenshot or report, that report must be treated as confidential and stored in accordance with FNB data retention policies.

**PCI/GLBA:** PTAF does not store payment card data. Test scenarios that interact with payment flows use test card numbers provided by payment processors for testing purposes only.

---

### 20. Is development occurring on this system?

Yes. PTAF is developed in-house by the FNB QA Engineering team.

- **Development team:** FNB internal QA engineers and automation developers.
- **Secure coding practices:** Code is stored in FNB's internal Git repository (GitHub Enterprise or equivalent). All code changes go through pull request review before merging. Static code analysis tools are used during the build process.
- **SDLC:** PTAF follows FNB's standard SDLC process including code review, testing, and change management.
- **Code ownership:** FNB owns all PTAF source code. No third-party vendor owns or has access to the codebase.
- **Code proxy:** Maven Central is accessed through FNB's internal JFrog Artifactory proxy (if configured) to control which open-source dependencies are permitted.

---

### 21. Explain how the system is kept up to date and maintained.

PTAF is maintained by the FNB QA Engineering team. Updates are applied as follows:

- **Framework updates:** The FNB team updates PTAF source code as needed to support new testing requirements or fix defects. All updates go through the standard FNB change management process.
- **Dependency updates:** Open-source library versions are reviewed quarterly. Security vulnerabilities in dependencies are identified using Maven dependency check tools (OWASP Dependency Check or equivalent) and updated promptly upon discovery of critical CVEs.
- **Playwright browser updates:** Playwright downloads browser binaries automatically. The version is pinned in `pom.xml` and updated as part of the quarterly dependency review.
- **Appium driver updates:** Appium drivers (UiAutomator2, XCUITest) are updated when new mobile OS versions require compatibility updates.
- **No vendor patch cycle:** As an in-house tool, PTAF has no external vendor patch cycle. The FNB team is responsible for all maintenance.

---

### 22. Are changes required to bypass Anti-virus real-time protection?

PTAF launches browser processes (Chromium, Firefox, WebKit) and Appium Server as child processes during test execution. These are standard, well-known open-source applications. If AV real-time protection interferes with browser process launching or Playwright's browser binary management, exclusions may be required for:

- Playwright browser binary directory (typically `%USERPROFILE%\.cache\ms-playwright` on Windows or `~/.cache/ms-playwright` on Linux/macOS).
- Appium Server Node.js process.

These exclusions should be evaluated and approved by the FNB Security team on a case-by-case basis per test execution machine.

---

### 23. What logging capabilities does the system have? Is it able to SysLog?

PTAF uses **Log4j2** for application logging. Log output includes:

- Test execution progress (scenario start/end, step pass/fail).
- Configuration loading events.
- Driver initialization and teardown events.
- Error messages and stack traces for failed steps.

Logs are written to:
- **Console output** (stdout/stderr) — captured by CI/CD pipeline log systems.
- **Log files** on the local test execution machine (configurable via `log4j2.xml`).

PTAF can be configured to forward logs to a SysLog endpoint by updating the Log4j2 configuration to include a SysLog appender. This is not enabled by default but can be enabled upon request from the FNB Security or Operations team.

---

### 24. Is there fraud potential with this system?

PTAF is an internal QA tooling framework with no customer-facing interface. Fraud potential is low. The primary risk is unauthorized use of test credentials to access FNB application environments. This is mitigated by:

- Test accounts are provisioned with minimal permissions (read/write to test data only; no access to production data).
- Test credentials are stored in environment variables or secrets management systems, not in source code.
- Access to PTAF source code and test credentials is restricted to FNB QA team members.

---

### 25. Is this a SOX application?

PTAF itself is not a SOX application — it is a testing tool. However, PTAF is used to test SOX-relevant applications (eCA, Mortgage, In-Branch). Test results and evidence generated by PTAF (screenshots, reports) may be used as audit evidence for SOX compliance testing. Report artifacts must be retained in accordance with FNB's SOX evidence retention policy.

---

### 26. Does this system integrate with Active Directory?

PTAF does not directly integrate with Active Directory. Access to PTAF source code and CI/CD pipeline configuration is controlled through FNB's standard AD-based access controls on the Git repository and CI/CD server. No LDAP or AD authentication is performed by the PTAF framework itself.

---

### 27. Does the system support Multi-Factor Authentication?

PTAF does not have its own authentication system. Access to the framework is controlled through:

- **Git repository access:** Controlled by FNB's standard AD/MFA-enforced Git authentication.
- **CI/CD server access:** Controlled by FNB's standard AD/MFA-enforced CI/CD authentication.
- **Test execution machine access:** Controlled by FNB's standard workstation login policies (AD + MFA where applicable).

PTAF test scenarios that test MFA-protected applications handle MFA through test-specific mechanisms (e.g., TOTP test accounts, bypass configurations in test environments).

---

### 28. What service accounts are required?

| Service Account | Purpose | Minimum Permissions Required |
|:---|:---|:---|
| **CI/CD pipeline service account** | Executes PTAF test runs in the CI/CD pipeline | Read access to source repository; write access to test report output directory; network access to test environment application servers and SQL Server |
| **Test automation SQL account** | Executes database validation queries during test runs | Read-only access to test database schemas; no write permissions to production data |
| **Test application accounts** | Simulates user actions in eCA, Mortgage, In-Branch applications | Standard end-user permissions in test environments only; no production access |

All service accounts must follow FNB's principle of least privilege. Passwords must be stored in FNB's approved secrets management solution and rotated according to FNB's password policy.

---

### 29. Do default, built-in accounts, or back-door access exist in the system?

No. PTAF is a custom in-house framework with no default accounts, built-in user management, or back-door access mechanisms. There is no login interface, no user database, and no authentication system within PTAF itself.

---

### 30. What account is used to perform installations and upgrades?

PTAF is installed by running `mvn clean install` on the developer or CI/CD server. This uses the standard developer or CI/CD service account. No elevated or administrative privileges are required for installation. Maven downloads dependencies from Maven Central (or FNB's internal Artifactory proxy) and compiles the Java source code.

---

### 31. Does an outside 3rd party access the system for support purposes?

No. PTAF is developed and maintained entirely by FNB's internal QA Engineering team. No third-party vendor has access to the PTAF codebase or test execution infrastructure.

---

### 32. Does the system store passwords in cleartext?

No. PTAF does not store passwords in cleartext. Test credentials are managed as follows:

- Stored as **environment variables** on the test execution machine or CI/CD server.
- Stored in FNB's **approved secrets management solution** (e.g., HashiCorp Vault, CyberArk, or equivalent).
- Never hardcoded in source code or configuration files committed to version control.
- Configuration files that reference credentials use variable substitution (e.g., `${TEST_PASSWORD}`) resolved at runtime from the environment.

---

## Database and Data Management

### 33. Does the system require a database?

PTAF itself does not require a dedicated database. It connects to **existing FNB SQL Server instances** in test environments for database validation test scenarios. The connection details are:

- **Driver:** Microsoft JDBC Driver for SQL Server (mssql-jdbc 13.4.0.jre11).
- **Connection:** JDBC over TLS with `encrypt=true`.
- **Authentication:** SQL Server authentication using a dedicated read-only test service account (Windows Integrated Authentication is also supported).
- **Access:** Read-only queries for data validation; no schema changes or data writes are performed by PTAF.
- **Sensitive data:** PTAF database tests operate on test schemas containing synthetic data only. No production data is accessed.

---

### 34. What are the uptime requirements?

PTAF is a test execution tool with no uptime requirement of its own. Test runs are scheduled or triggered on-demand. The uptime requirement for PTAF is the same as the CI/CD pipeline infrastructure it runs on, which is governed by FNB's existing CI/CD SLA.

---

## User Management

### 35. Who are the approvers of access for the system?

Access to PTAF source code and CI/CD pipeline configuration is approved by the **FNB QA Engineering Manager** and governed by FNB's standard IT access request process.

---

### 36. What LOBs access the system, and how many users are there total?

| Line of Business | Role | Approximate User Count |
|:---|:---|:---|
| FNB QA Engineering | Test automation developers and engineers | 5–15 |
| FNB DevOps / CI/CD | Pipeline administrators | 2–5 |
| FNB Application Development | Developers reviewing test results | 10–20 (read-only report access) |

---

### 37. Who are the Application admins, DB Admins, and other Admin functions?

| Role | Responsibility |
|:---|:---|
| **PTAF Framework Owner / Application Admin** | FNB QA Engineering Lead — manages framework source code, approves dependency updates, manages test credentials |
| **CI/CD Admin** | FNB DevOps team — manages pipeline configuration and service account permissions |
| **DB Admin** | FNB DBA team — manages the read-only test service account for SQL Server connections |

---

### 38. Detail the user permission setup.

| Role | Access Level | Description |
|:---|:---|:---|
| **QA Automation Developer** | Read/Write | Full access to PTAF source code; can create and modify test scenarios, configurations, and reports |
| **QA Engineer (non-developer)** | Read/Execute | Can execute test runs and view reports; cannot modify framework source code |
| **CI/CD Service Account** | Execute only | Can trigger test runs in the pipeline; cannot modify source code |
| **Developer (read-only)** | Read | Can view test reports; no access to test credentials or framework configuration |
| **PTAF Framework Owner** | Admin | Full access including dependency management and credential rotation |

Separation of duties is enforced by Git branch protection rules (no direct commits to main branch; pull request review required) and by restricting test credential access to the Framework Owner and designated QA leads.

---

## AI Usage and Architecture

### 39. Is AI utilized in this solution?

PTAF does not currently incorporate AI or machine learning components in its core execution engine. All test logic is deterministic and rule-based.

A future roadmap item includes AI-assisted test generation (using LLM APIs to generate Cucumber scenarios from application URLs), but this is not part of the current implementation being reviewed.

---

### 40–43. AI backend, GenAI, model type.

Not applicable. No AI components are present in the current PTAF implementation.

---

## Risk and Governance

### 44–48. AI risk assessments, OpsRisk review, safeguards, monitoring.

Not applicable. No AI components are present in the current PTAF implementation. If AI-assisted test generation is added in a future release, a separate SAR addendum will be submitted for OpsRisk review at that time.

---

## Data and Privacy

### 49–50. Training data, user data handling in AI interactions.

Not applicable. No AI components are present in the current PTAF implementation.

---

*Document prepared by: FNB QA Engineering Team*
*Review requested from: FNB Security Architecture Team*
*Date: July 2026*
