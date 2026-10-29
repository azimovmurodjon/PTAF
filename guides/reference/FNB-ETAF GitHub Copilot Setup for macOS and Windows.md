# FNB-ETAF GitHub Copilot Setup for macOS and Windows

**Audience:** FNB-ETAF developers who have pulled the branch containing the Copilot integration.

**Framework requirement:** Java 21, Maven, IntelliJ IDEA, and an organization-approved GitHub Copilot account.

This is the canonical FNB-ETAF Copilot setup guide. Follow only the command blocks for your operating system:

- **macOS:** use IntelliJ Terminal or macOS Terminal with `zsh`.
- **Windows:** use IntelliJ Terminal or Windows PowerShell. Do not use the macOS `.sh` commands in Windows PowerShell.

> **Important:** `.fnb-etaf/` is intentionally absent after a Git pull. The folder is created **locally**, only when optional Playwright MCP locator discovery is approved. Never commit or push `.fnb-etaf/`.

## 1. What belongs in Git and what stays local

### Commit and push the shared framework integration

These files are shared with the team and belong in the approved FNB-ETAF branch:

```text
.github/copilot-instructions.md
.github/instructions/

src/main/java/com/ptaf/copilot/
src/test/java/com/ptaf/copilot/
src/main/resources/copilot/

scripts/sync-copilot-instructions.sh
scripts/sync-copilot-instructions.ps1
scripts/copilot-discovery.sh
scripts/copilot-discovery.ps1

.gitignore
```

The root `.gitignore` must contain this rule:

```gitignore
/.fnb-etaf/
```

The only central, human-maintained source of Copilot rules is:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

The files under `.github/` are generated from this central profile. Do not manually edit generated `.github/copilot-instructions.md` files or `.github/instructions/` files.

### Generate this workspace locally only

When a developer chooses to use the optional local Playwright MCP discovery workflow, the framework creates this local folder:

```text
.fnb-etaf/
├── copilot/
│   └── mcp.json
├── discovery-inputs/
└── discovery-drafts/
```

`mcp.json` can contain an exact approved target origin. This is why the folder stays local and ignored.

### Never commit or push this local workspace

```text
.fnb-etaf/
.fnb-etaf/copilot/mcp.json
.fnb-etaf/discovery-inputs/
.fnb-etaf/discovery-drafts/
```

Never store passwords, tokens, API keys, cookies, customer data, real internal URLs, screenshots, videos, page source, reports, browser storage, or test evidence in source-controlled Copilot prompts, examples, instructions, or configuration.

## 2. Team access model for different laptops and testers

FNB-ETAF uses one shared framework integration and one local setup per tester. The framework’s Copilot rules, scripts, tests, and safety settings are committed once. Every tester then uses their own authorized identity and their own local configuration on their own company-managed laptop.

```text
Shared FNB-ETAF Git repository
    └── Committed Copilot rules, Java tools, tests, scripts, and .gitignore rule

Each tester's managed laptop
    ├── Their own IntelliJ GitHub Copilot sign-in
    ├── Their own approved non-production application access, when needed for normal tests
    ├── Their own local .fnb-etaf/ folder, only if MCP is approved
    └── Their own IntelliJ MCP configuration

Never shared through Git or a common folder
    ├── Passwords, MFA codes, tokens, cookies, or authorization headers
    ├── Chrome, Edge, or other persistent browser profiles
    ├── Authenticated browser sessions or saved session-state files
    ├── Local .fnb-etaf/ folders and mcp.json files
    └── Customer data, screenshots, reports, or other sensitive evidence
```

### Grant each tester individual access

Before onboarding a tester, the responsible GitHub, application, and security owners should grant the tester only the access required for their role. This normally means an organization-approved GitHub Copilot license, access to the approved FNB-ETAF repository, and a personally assigned non-production application role or test account if the tester will run authenticated normal FNB-ETAF automation.

Do not give the whole team one shared GitHub identity, application account, password, browser profile, cookie, token, or authenticated browser session. Individual access provides traceability, allows access to be removed for one person, and prevents one tester's session from affecting another tester.

### Keep normal FNB-ETAF execution separate from MCP

Normal FNB-ETAF test execution continues to use the framework's existing approved configuration and secret-management process. It may execute authenticated functional tests only under the current approved test-data, account, and environment controls.

The optional Playwright MCP workflow is more restricted. It is for review-only locator discovery on an approved non-sensitive page. It must not log in, store credentials, reuse browser cookies, reuse an authenticated browser profile, enter test data, submit a form, download/upload a file, handle a popup or frame, inspect browser storage, inspect network traffic, or run as part of normal FNB-ETAF execution.

> Therefore, a tester's approved normal test account does **not** automatically authorize MCP use after login. Authenticated MCP discovery requires a separate architecture and security approval. Keep the current MCP workflow non-authenticated unless that separate approval is granted.

### Onboard a new tester

Use the following order for each new tester:

1. Confirm the tester has repository access, an approved Copilot license, Java 21, Maven, and IntelliJ IDEA.
2. Confirm the tester has an individual approved non-production test account only if their normal FNB-ETAF tests require authentication.
3. Have the tester pull the approved framework branch and complete the Java, Maven, and normal Copilot steps in this guide.
4. Stop here unless the tester has an approved MCP locator-discovery task.
5. For approved MCP use, have the tester generate their own local `.fnb-etaf/copilot/mcp.json`, verify Git ignores it, and configure the exact approved origin locally.
6. Review the tester's first MCP draft before they manually promote any locator or feature change into active framework resources.

### Remove a tester's access

When a tester changes teams or no longer requires access, the responsible owners should remove their repository and Copilot entitlement, disable or remove their application test account as appropriate, and remove their local IntelliJ MCP configuration. Because browser sessions, credentials, and `.fnb-etaf/` files are never shared or committed, removing one tester's access does not require changing the framework repository.

## 3. Two ways to use Copilot in FNB-ETAF

### Normal Copilot Chat — use this first

Normal GitHub Copilot Chat reads the committed repository instructions. It can explain code, find existing reusable Gherkin steps, review a problem, draft a scenario, propose a locator, and suggest a minimal safe change. It does **not** require `.fnb-etaf`, Node.js, Playwright MCP, or a browser connection.

The central profile instructs Copilot to preserve the existing FNB-ETAF contracts. It must not change Hooks, BrowserFactory, runners, report lifecycle, video lifecycle, `@LastScenario`, Maven configuration, ordinary UI/API/mobile behavior, or the performance architecture without an approved requirement.

### Optional local Playwright MCP discovery — use only with approval

Playwright MCP is an optional local workflow for reviewing a small number of visible locator candidates on an explicitly approved, non-sensitive test page. It is not part of normal framework runtime. It is never started by Maven, Cucumber, TestNG, Hooks, BrowserFactory, CI/CD, or a normal test run.

The MCP workflow returns review-only drafts. A person must review and manually promote approved locators and Gherkin into the active FNB-ETAF files.

## 4. First-time workstation setup

### Step 1: Pull the approved FNB-ETAF branch

Open the FNB-ETAF project root in IntelliJ. Use your normal approved Git process.

**macOS or Windows PowerShell**

```text
git status
git pull --ff-only
```

After the pull, it is correct that `.fnb-etaf/` does not exist. Do not create it manually. The optional MCP generation command creates it later.

### Step 2: Set Java 21 in IntelliJ

FNB-ETAF uses Java 21. Configure IntelliJ before running Maven.

On **macOS**, open **IntelliJ IDEA → Settings**. On **Windows**, open **File → Settings**. Then set these to a JDK 21 installation:

1. **Project Structure → Project SDK**: Java 21.
2. **Project Structure → Project language level**: 21.
3. **Build, Execution, Deployment → Build Tools → Maven → Runner → JRE**: Java 21.

Reload Maven after making these changes.

### Step 3: Confirm Java 21 in your terminal

#### macOS — zsh

Open a new IntelliJ Terminal window at the project root and run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
hash -r

java -version
javac -version
mvn -version
```

Each command must report Java 21.

To make Java 21 the default for future macOS terminal windows, add these lines to `~/.zshrc` after confirming that the JDK is organization-approved:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

Open a new terminal after saving `.zshrc`.

#### Windows — PowerShell

First identify the actual JDK 21 directory that is installed on your computer. Common locations are under `C:\Program Files\Java\` or `C:\Program Files\Eclipse Adoptium\`, but the exact directory name depends on the approved JDK vendor and update version.

Set the path only for the current PowerShell session, replacing the sample path with your actual Java 21 directory:

```powershell
$jdk21 = 'C:\Program Files\<Approved-JDK-Vendor>\jdk-21'
Test-Path $jdk21

$env:JAVA_HOME = $jdk21
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
javac -version
mvn -version
```

`Test-Path` must return `True`. Each version command must report Java 21.

To set Java 21 permanently on Windows, use **Start → Edit the system environment variables → Environment Variables**. Create or update the user variable `JAVA_HOME` with the actual JDK 21 directory. Add `%JAVA_HOME%\bin` to the user `Path`. Close and reopen IntelliJ and PowerShell after the update.

> A Java 17 terminal can cause `UnsupportedClassVersionError` because it cannot load Java 21 class files. Do not change the framework to Java 17.

### Step 4: Compile the framework

Run the following from the FNB-ETAF project root.

**macOS — zsh**

```bash
mvn clean test-compile -DskipTests
```

**Windows — PowerShell**

```powershell
mvn clean test-compile -DskipTests
```

This compiles FNB-ETAF and the Copilot integration. It does not run application tests.

### Step 5: Run the focused Copilot contract tests

**macOS — zsh**

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true
```

**Windows — PowerShell**

```powershell
mvn test `
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest `
  -DfailIfNoTests=true
```

The result must show **0 failures** and **0 errors**. These focused tests do not open a browser, start Playwright MCP, or call an application.

### Step 6: Verify the committed Copilot instruction files

**macOS — zsh**

```bash
chmod +x scripts/sync-copilot-instructions.sh
./scripts/sync-copilot-instructions.sh verify .
```

**Windows — PowerShell**

```powershell
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
```

This confirms that the committed `.github` files match the central profile under `src/main/resources`. It does not affect standard FNB-ETAF test execution.

## 5. Install and use normal GitHub Copilot

### Step 7: Install the IntelliJ plugin and sign in

The steps are the same on macOS and Windows.

1. Open **Settings/Preferences → Plugins → Marketplace**.
2. Search for **GitHub Copilot**.
3. Install the plugin and restart IntelliJ.
4. Select **Tools → GitHub Copilot → Login to GitHub**.
5. Complete your organization-approved GitHub authorization flow. [1]

Open **Copilot Chat** after login. Normal Copilot Chat works without `.fnb-etaf`.

### Step 8: Ask Copilot to review before it makes changes

For a new manual test case or automation requirement, use this prompt in Copilot Chat:

```text
Review this FNB-ETAF requirement before proposing changes:

[Paste the manual steps and expected results here.]

First inspect the related existing feature files, Cucumber steps, configuration aliases,
and YAML locator files. Reuse existing framework steps and locators whenever possible.

Return only:
1. Existing reusable steps and locator keys.
2. Missing locator, configuration, or step information.
3. A clean draft Gherkin scenario.
4. A YAML draft only if a locator is genuinely missing.
5. The exact project files that would require a human-reviewed change.

Do not edit files, commit, push, run tests, change Hooks, BrowserFactory, runners,
reports, Maven files, configuration, or lifecycle code until I approve.
```

Review Copilot’s answer before accepting a change. Copilot is an assistant; it is not the framework owner.

For input, textarea, or select values, use the existing **has value** assertion. For visible content such as headings, labels, buttons, messages, and table cells, use a text or contains assertion.

### Step 9: Validate an approved normal Copilot change

**macOS — zsh**

```bash
mvn clean test-compile -DskipTests
# Run the focused feature, runner, or test covering your approved change.
git diff --check
git status
```

**Windows — PowerShell**

```powershell
mvn clean test-compile -DskipTests
# Run the focused feature, runner, or test covering your approved change.
git diff --check
git status
```

Do not make broad changes to Hooks, BrowserFactory, runners, reports, or Maven just to correct one locator or feature problem.

## 6. Generate `.fnb-etaf` locally only when MCP is approved

### Step 10: Confirm the MCP task is allowed

Use local Playwright MCP only when all these conditions are met:

- GitHub Copilot MCP servers are allowed by your organization.
- A human has approved the exact HTTPS target origin.
- The page is non-production and non-sensitive.
- The task needs only a small number of visible locator candidates.
- The page does not require login, MFA, customer information, credentials, test-data entry, submission, iframe handling, popup handling, upload, or download.

If one condition is not met, do not use MCP. Use normal Copilot Chat and the established FNB-ETAF authoring process instead.

### Step 11: Confirm Node.js only if you will use MCP

Normal Copilot Chat does not need Node.js. The optional MCP server needs an organization-approved Node.js 20 or later installation and `npx`. [3]

**macOS — zsh**

```bash
node --version
npx --version
```

**Windows — PowerShell**

```powershell
node --version
npx --version
```

Install an organization-approved Node.js version only if Node.js is missing or reports lower than version 20.

### Step 12: Generate the local `.fnb-etaf` MCP template

This command creates `.fnb-etaf/copilot/mcp.json` locally. It does not start Node.js, download a package, start Playwright MCP, launch a browser, or connect to an application.

**macOS — zsh**

```bash
chmod +x scripts/copilot-discovery.sh
./scripts/copilot-discovery.sh mcp-template .
```

**Windows — PowerShell**

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-template -ConsumerRoot .
```

Confirm the local configuration is ignored by Git.

**macOS — zsh or Windows — PowerShell**

```text
git check-ignore -v .fnb-etaf/copilot/mcp.json
```

The command must print the ignore rule for `/.fnb-etaf/`. If it does not, add the following exact line to the root `.gitignore`, commit the `.gitignore` change, and rerun the check:

```gitignore
/.fnb-etaf/
```

### Step 13: Configure the approved target origin locally

Open the new local file:

```text
.fnb-etaf/copilot/mcp.json
```

It contains this placeholder:

```text
https://<APPROVED_TARGET_ORIGIN>
```

Replace **only** that placeholder with the exact approved HTTPS origin. An example safe format is:

```text
https://approved-nonproduction.example
```

An origin contains only the scheme and host. Do not add a path, credentials, wildcard, multiple origins, or a production origin without explicit approval. Do not change the generated MCP security settings.

### Step 14: Add the local MCP configuration to IntelliJ

The IntelliJ steps are the same on macOS and Windows.

Open **Copilot Chat**, switch to **Agent mode**, open the tools configuration control, and select **Add MCP Tools**. Open IntelliJ’s `mcp.json` from that screen. Copy the content from your local `.fnb-etaf/copilot/mcp.json` into IntelliJ’s MCP configuration and save it. [2]

Keep the generated options unchanged. The template deliberately uses isolated, headless, sandboxed browser discovery and a restricted origin. Do not add persistent browser profiles, saved sessions, secrets files, proxies, CDP endpoints, browser extensions, TLS-error bypasses, unrestricted file access, screenshots, PDFs, DevTools, network inspection, console inspection, or arbitrary browser-code execution.

Enable MCP only for the approved locator-discovery task. Disable it after the task is complete.

## 7. Use MCP for review-only locator discovery

### Step 15: Request candidate text only

Use this prompt in Copilot Chat. Do not include a real internal URL, credentials, account details, token, or sensitive page content.

```text
Use the local fnb-etaf-playwright-local MCP server only for the exact approved
non-sensitive origin configured locally.

Do not log in, type, click, submit, change application state, follow redirects,
open popups, upload or download files, inspect storage, network, or console,
take screenshots, save files, run browser code, or expose page data.

Inspect only the visible page structure needed for these controls:
[describe two or three controls in business language].

Return a candidate YAML in the FNB-ETAF discovery_draft schema. Use the existing
configuration alias [approved_alias], not a URL. Use page_context: page. Propose
only click, verify_visible, verify_enabled, or verify_exists actions. Do not
create or change files.
```

Copilot must return candidate text in the chat. It must not write to active framework files.

### Step 16: Validate the local candidate YAML

Copy the reviewed candidate text into a local ignored file:

```text
.fnb-etaf/discovery-inputs/sample-page.yml
```

Start from this committed template:

```text
src/main/resources/copilot/fnb-etaf-discovery-candidate-template.yml
```

The candidate must contain an existing configuration alias. It must not contain a resolved URL, password, token, cookie, authorization value, account information, typed input value, page source, HTML, screenshot, header, or report data.

**macOS — zsh**

```bash
./scripts/copilot-discovery.sh validate .fnb-etaf/discovery-inputs/sample-page.yml
```

**Windows — PowerShell**

```powershell
.\scripts\copilot-discovery.ps1 -Operation validate -InputFile .fnb-etaf\discovery-inputs\sample-page.yml
```

If validation fails, correct the local candidate. Do not weaken the central safety profile to accept an unsafe candidate.

### Step 17: Create review-only local drafts

**macOS — zsh**

```bash
./scripts/copilot-discovery.sh draft .fnb-etaf/discovery-inputs/sample-page.yml .
```

**Windows — PowerShell**

```powershell
.\scripts\copilot-discovery.ps1 -Operation draft -InputFile .fnb-etaf\discovery-inputs\sample-page.yml -ConsumerRoot .
```

The tool writes its review-only outputs below:

```text
.fnb-etaf/discovery-drafts/
```

It does not overwrite active locators, features, configuration, Hooks, BrowserFactory, runners, reports, or tests.

### Step 18: Review and promote manually

Review locator candidates against the approved page. Prefer stable test IDs, then stable IDs, then unique accessible role/name locators. Use CSS or XPath only when a stable higher-level locator does not exist.

Copy approved locators manually into the correct active file under:

```text
src/test/resources/elements/
```

Copy approved Gherkin manually into the correct active file under:

```text
src/test/resources/features/
```

Run the focused FNB-ETAF test. Commit only the reviewed active framework changes. Do not commit `.fnb-etaf/`.

## 8. Update Copilot instructions for the whole team

When you need to change a framework-wide Copilot rule, update only:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

Then regenerate, verify, and review the committed instructions.

**macOS — zsh**

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true

./scripts/sync-copilot-instructions.sh sync .
./scripts/sync-copilot-instructions.sh verify .
git diff --check
```

**Windows — PowerShell**

```powershell
mvn test `
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest `
  -DfailIfNoTests=true

.\scripts\sync-copilot-instructions.ps1 -Operation sync -ConsumerRoot .
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
git diff --check
```

Review and commit the central profile, generated `.github` files, focused tests, and documentation together. Never commit `.fnb-etaf/`.

## 9. Troubleshooting

### macOS: `java -version` reports Java 17

Run the Java 21 commands in Step 3 again, open a new terminal, and confirm IntelliJ Maven Runner also uses Java 21. Do not change the framework to Java 17.

### Windows: `java -version` reports Java 17 or `Test-Path` reports `False`

The Windows JDK path is incorrect or Java 21 is not installed. Find the approved JDK 21 folder, set `JAVA_HOME` to that exact directory, restart IntelliJ and PowerShell, and verify the version commands again.

### Windows: PowerShell blocks the `.ps1` script

Do not bypass a company execution policy. Ask your workstation or security administrator to provide the approved PowerShell execution policy or approved script-signing process for this project.

### The `.fnb-etaf` folder is missing after pull

This is correct. It is local only. Generate it only after MCP is approved, using the command for your operating system in Step 12.

### `git check-ignore` prints nothing for `mcp.json`

Confirm the root `.gitignore` contains the exact line `/.fnb-etaf/`. Commit that `.gitignore` rule. Then delete and regenerate the local file if needed, and rerun the check.

### Copilot works but no MCP option is shown

Confirm the GitHub Copilot plugin is installed, IntelliJ was restarted, and you have organization-approved Copilot access. MCP can also be disabled by enterprise policy. Ask the Copilot administrator to confirm the policy. Do not bypass it. [1] [2]

### MCP candidate validation rejects a URL or secret-like value

This is expected. Replace the resolved URL with an existing configuration alias and remove the sensitive value. Keep the candidate limited to safe locator metadata.

### The page needs login, MFA, customer data, a popup, iframe, submission, upload, or download

Do not use MCP. Disable the local MCP server and use the normal FNB-ETAF review and authoring workflow.

## References

[1]: https://docs.github.com/copilot/managing-copilot/configure-personal-settings/installing-the-github-copilot-extension-in-your-environment "Installing the GitHub Copilot extension in your environment"
[2]: https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/customize-copilot/extend-copilot-with-tools-and-context/extend-copilot-chat-with-mcp "Extend Copilot Chat with MCP in JetBrains IDEs"
[3]: https://playwright.dev/docs/getting-started-mcp "Playwright MCP getting started"
