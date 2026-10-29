# FNB-ETAF GitHub Copilot and Optional Playwright MCP: After-Pull Setup and Usage

**Audience:** FNB-ETAF developers and testers using IntelliJ IDEA after pulling the governed Copilot integration.

**Framework standard:** Java 21 and Maven.

This guide explains what to do after you pull the Copilot integration into an FNB-ETAF workspace. It covers two separate capabilities:

1. **GitHub Copilot repository guidance.** This is the normal capability. Copilot reads the committed `.github` instruction files and helps draft or review FNB-ETAF work while following the framework rules.
2. **Local Playwright MCP discovery.** This is optional. It is a short, manually enabled, review-only session for finding locator candidates on an explicitly approved, non-sensitive test page.

> The Playwright MCP option is not required to use GitHub Copilot. Do not enable it until the normal Copilot workflow is working and you have approval for the exact non-sensitive target origin.

## What the committed integration does and does not do

The integration adds governed Copilot guidance, local helper scripts, a central profile, templates, and browserless contract tests. It does **not** change normal FNB-ETAF runtime behavior. It does not start a browser during `mvn test`, change `Hooks`, change `BrowserFactory`, change runners, change reports, change your active features, or connect to an application by itself.

The central editable source for FNB-ETAF AI guidance is:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

The committed `.github` files are generated from that profile. Do not edit `.github/copilot-instructions.md` or files under `.github/instructions/` manually.

The optional local MCP template and local discovery drafts are stored under:

```text
.fnb-etaf/
```

This folder is ignored by Git. Confirm that the project `.gitignore` contains this exact rule before using local MCP discovery:

```gitignore
/.fnb-etaf/
```

Do not commit the folder. It may contain a locally approved target origin in `mcp.json`.

## 1. Pull the branch and confirm the workspace

Open a Terminal in the FNB-ETAF project root. Pull your normal approved branch using your team’s standard Git process. If your branch is already checked out and you use fast-forward pulls, the following is a safe example:

```bash
git status
git pull --ff-only
```

Check that the Copilot files are present after the pull:

```bash
find .github -maxdepth 2 -type f | sort
find src/main/java/com/ptaf/copilot -maxdepth 1 -type f | sort
find src/main/resources/copilot -maxdepth 1 -type f | sort
```

You should see the root instruction file, scoped instruction files, the `com.ptaf.copilot` package, and the four central resource files.

## 2. Configure Java 21 before running Maven or helper scripts

FNB-ETAF uses **Java 21**. Java 17 cannot run the Copilot classes because Java 17 understands class-file versions only through 61, while Java 21 produces class-file version 65.

On macOS, run the following in the same IntelliJ Terminal window that you will use for Maven and the helper scripts:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
hash -r

java -version
javac -version
mvn -version
```

Each version command must show Java 21. If the first command reports that Java 21 is not installed, install an organization-approved JDK 21 and try again. Do not use Java 17 for this project. Java 23 is not required for this integration.

To use Java 21 automatically in new macOS Terminal windows, add these two lines to `~/.zshrc` only after you confirm that `/usr/libexec/java_home -v 21` returns the intended approved JDK:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

In IntelliJ IDEA, set **Project SDK**, **Project language level**, and the **Maven runner JDK** to Java 21. Then reload the Maven project and open a fresh IntelliJ Terminal.

## 3. Compile and run the Copilot safety contracts

Run this from the framework root:

```bash
mvn clean test-compile -DskipTests
```

Then run the focused browserless tests:

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true
```

A successful run must report zero failures and zero errors. These tests do not open a browser, do not start Playwright MCP, and do not call an application. They validate the central profile, generated instruction behavior, local template rules, strict candidate parsing, and review-only draft safeguards.

## 4. Verify the committed Copilot instructions

The current integration already commits the generated `.github` instruction files. After a normal pull, you only need to verify them; you do not need to regenerate them.

On macOS or Linux:

```bash
chmod +x scripts/sync-copilot-instructions.sh
./scripts/sync-copilot-instructions.sh verify .
```

On Windows PowerShell:

```powershell
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
```

The verification command compiles the relevant source and confirms that the committed instruction files match the central profile. It does not modify normal tests or framework execution.

If verification reports drift after an approved profile change, regenerate and review the instruction files:

```bash
./scripts/sync-copilot-instructions.sh sync .
./scripts/sync-copilot-instructions.sh verify .
```

Commit only the reviewed `.github` changes. Never manually edit generated instruction files.

## 5. Install and sign in to GitHub Copilot in IntelliJ

Install the **GitHub Copilot** plugin from the JetBrains Marketplace if it is not already installed. Restart IntelliJ after installation. Then use **Tools → GitHub Copilot → Login to GitHub** and complete the GitHub authorization flow. GitHub documents these IntelliJ steps and the compatible JetBrains IDEs. [1]

After signing in, open **Copilot Chat**. The `.github/copilot-instructions.md` file provides repository-wide rules. The files under `.github/instructions/` add rules for Java, features, UI locators, API/DB/data, mobile, performance, reporting, and Maven paths.

## 6. Use normal Copilot Chat for test drafting

Normal Copilot Chat is the recommended first workflow. It does not need Playwright MCP. Ask Copilot to inspect only your local repository context and return a review before making changes.

Use this prompt as a starting point:

```text
Create an FNB-ETAF review-only test draft for the manual scenario below.

[Paste the business steps and expected results here.]

First inspect existing feature files, step definitions, configuration aliases,
and relevant YAML locator files. Reuse existing framework steps and locators
where possible.

Return:
1. Existing reusable steps and locator keys.
2. Any missing locator or configuration entry.
3. A clean Gherkin draft.
4. A YAML draft only when a locator is missing.
5. The exact active files that would need a human-reviewed change.

Do not edit, commit, push, run tests, change Hooks, BrowserFactory, runners,
reports, Maven files, or configuration until I approve.
```

For UI inputs, tell Copilot to use the existing **has value** assertion when you are verifying an input, textarea, or select value. Use text or contains assertions only for visible content such as headings, messages, labels, buttons, and table cells.

Before accepting a proposed change, review it yourself. Copilot guidance does not replace code review, security review, or focused FNB-ETAF validation.

## 7. Optional local Playwright MCP locator discovery

Use this section only when all of the following are true:

- Your organization permits MCP servers in GitHub Copilot.
- You have an explicitly approved **exact HTTPS origin**.
- The target is a non-production, non-sensitive test page.
- The page contains no customer data, account data, secrets, or regulated information.
- You need a small set of locator candidates for visible top-level controls.
- Node.js and `npx` are installed on the local workstation. Playwright MCP documents Node.js 20 or later as a prerequisite. [3]

Stop the discovery session if you see a redirect, login page, MFA prompt, popup, download, sensitive data, or another unexpected page. Use the existing manual FNB-ETAF workflow for those cases.

### 7.1 Generate the local MCP template

On macOS or Linux:

```bash
chmod +x scripts/copilot-discovery.sh
./scripts/copilot-discovery.sh mcp-template .
```

On Windows PowerShell:

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-template -ConsumerRoot .
```

This creates:

```text
.fnb-etaf/copilot/mcp.json
```

The command creates a template only. It does not start Node.js, download a package, start Playwright MCP, open a browser, or call an application.

### 7.2 Configure the local MCP server in IntelliJ

Open the local `.fnb-etaf/copilot/mcp.json` file. It contains this placeholder:

```text
https://<APPROVED_TARGET_ORIGIN>
```

Replace only that placeholder with the exact approved HTTPS origin. Use only an origin, for example `https://approved-nonproduction.example`; do not add a path, credentials, wildcard, multiple origins, or a production domain without explicit approval.

In IntelliJ, open **Copilot Chat**, select **Agent mode**, select the tools configuration control, then choose **Add MCP Tools**. Open the IntelliJ `mcp.json` file from that interface and copy the content from your local `.fnb-etaf/copilot/mcp.json` into it. GitHub documents the Agent mode and **Add MCP Tools** setup for JetBrains. [2]

Keep the provided safe settings unchanged. In particular, retain the pinned package version, `--isolated`, `--headless`, `--sandbox`, `--codegen=java`, `--no-webmcp`, `--block-service-workers`, and the blocked-origins option.

Do not add `@latest`, a persistent user profile, storage state, a secrets file, a proxy, browser extension, CDP connection, TLS-error bypass, unrestricted file access, arbitrary browser-code execution, screenshots, PDF, vision, DevTools, network, or console capability. The server should be enabled only for the approved, short discovery session and disabled afterward.

> The origin option is an accidental-navigation guard, not a complete network boundary. Your organization must still rely on its normal workstation, proxy, firewall, and browser egress controls. [3]

### 7.3 Ask Copilot for candidate text only

Use a narrow prompt. Replace the placeholders, but do not put secrets, test data, account details, or an internal URL into the prompt.

```text
Use the local fnb-etaf-playwright-local MCP server only for the exact approved
non-sensitive origin configured locally.

Do not log in, type, click, submit, change application state, follow redirects,
open popups, upload or download files, inspect storage, network, or console,
take screenshots, save files, run browser code, or expose page data.

Inspect only the visible page structure needed for these controls:
[describe the two or three controls in business language].

Return a candidate YAML in the FNB-ETAF discovery_draft schema. Use the existing
configuration alias [approved_alias], not a URL. Use page_context: page. Propose
only click, verify_visible, verify_enabled, or verify_exists actions. Do not
create or change files.
```

Copilot must return candidate text in the chat. It must not directly edit your repository.

### 7.4 Validate a local candidate file

Copy the reviewed candidate text into a local ignored file, for example:

```text
.fnb-etaf/discovery-inputs/checkout.yml
```

Start from this committed template:

```text
src/main/resources/copilot/fnb-etaf-discovery-candidate-template.yml
```

The candidate must use a configuration alias that already exists in `src/test/resources/config/config.yml`. It must not contain a real URL, password, token, cookie, authorization value, user information, typed field values, page source, HTML, screenshots, headers, or report data.

Validate the candidate before writing any draft:

```bash
./scripts/copilot-discovery.sh validate .fnb-etaf/discovery-inputs/checkout.yml
```

On Windows PowerShell:

```powershell
.\scripts\copilot-discovery.ps1 -Operation validate -InputFile .fnb-etaf/discovery-inputs/checkout.yml
```

Validation creates no files and does not touch active resources. If it rejects the candidate, correct the candidate. Do not weaken the central safety profile simply to accept a rejected value.

### 7.5 Render review-only local drafts

After validation passes, run:

```bash
./scripts/copilot-discovery.sh draft .fnb-etaf/discovery-inputs/checkout.yml .
```

On Windows PowerShell:

```powershell
.\scripts\copilot-discovery.ps1 -Operation draft -InputFile .fnb-etaf/discovery-inputs/checkout.yml -ConsumerRoot .
```

The tool writes only ignored, review-only files below:

```text
.fnb-etaf/discovery-drafts/<locator_file_name>/
```

That folder contains a locator YAML draft, a Gherkin feature draft, and a README. It will not overwrite an existing draft directory.

### 7.6 Review and promote manually

Review each candidate locator on the approved non-sensitive page. Prefer a stable test ID first, then a unique accessible role/name, then a stable ID. Use CSS and XPath only when a stable higher-level option is unavailable.

Confirm that any proposed click does not create an unapproved state change. Then manually copy approved locator entries into the appropriate file under `src/test/resources/elements/` and manually copy approved Gherkin into the appropriate feature file under `src/test/resources/features/`.

Run the normal focused FNB-ETAF test after manual promotion. Commit only the approved active locator and feature changes. Keep `.fnb-etaf/` local and ignored.

## 8. Maintain the Copilot rules in one place

When a framework-wide Copilot rule must change, edit only:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

Do not add secrets, real internal URLs, application-specific sensitive selectors, screenshots, videos, or output evidence to this profile.

After an approved central-profile change, run:

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true

./scripts/sync-copilot-instructions.sh sync .
./scripts/sync-copilot-instructions.sh verify .
git diff --check
```

Review and commit the central profile, the regenerated `.github` files, any focused tests, and related documentation together. Do not commit `.fnb-etaf/`.

## 9. Troubleshooting

### Java reports `UnsupportedClassVersionError`

If you see a message that the runtime recognizes class-file versions only up to 61, your Terminal is using Java 17. Re-run the Java 21 commands from section 2, open a fresh IntelliJ Terminal, and run `mvn clean test-compile -DskipTests` again.

### IntelliJ does not show Copilot or MCP controls

Confirm the GitHub Copilot plugin is installed, IntelliJ was restarted, and you are signed in with organization-approved Copilot access. For MCP specifically, an organization or enterprise Copilot policy can disable MCP servers. Ask the Copilot administrator rather than bypassing the policy. [1] [2]

### The MCP template still contains `<APPROVED_TARGET_ORIGIN>`

This is expected. The template stays disabled until a human enters the exact approved HTTPS origin locally. Do not commit that completed local file.

### Candidate validation rejects a URL, token, or credential-like value

Remove the value. Use an existing configuration alias and only safe locator/action metadata. Do not place a resolved target URL in the candidate YAML.

### Candidate discovery needs login, an iframe, a popup, submission, file handling, or sensitive data

Stop using MCP discovery. Build the test through the existing FNB-ETAF workflow and normal review process.

### The draft command says that the directory already exists

This is intentional. The tool will not overwrite a review artifact. Review the existing local draft, archive it outside source control, or explicitly delete the old ignored draft before generating a new one.

## References

[1]: https://docs.github.com/copilot/managing-copilot/configure-personal-settings/installing-the-github-copilot-extension-in-your-environment "Installing the GitHub Copilot extension in your environment"
[2]: https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/customize-copilot/extend-copilot-with-tools-and-context/extend-copilot-chat-with-mcp "Extend Copilot Chat with MCP in JetBrains IDEs"
[3]: https://playwright.dev/docs/getting-started-mcp "Playwright MCP getting started"
