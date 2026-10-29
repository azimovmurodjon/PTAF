# FNB-ETAF GitHub Copilot and Optional Playwright MCP Installation Guide

**Use this guide for a new FNB-ETAF tester on macOS or Windows.**

FNB-ETAF supports two separate workflows:

1. **Normal GitHub Copilot Chat** helps developers understand and safely extend the framework. Every tester may use this after their GitHub Copilot access is approved.
2. **Optional Playwright MCP locator discovery** is a local, review-only tool for a small number of visible controls on an approved non-sensitive page. It is not required for normal Copilot use or normal FNB-ETAF automation.

> Start with normal Copilot Chat. Configure MCP only after the target, purpose, and local browser access have been approved.

## 1. Keep shared framework files and local files separate

The FNB-ETAF framework repository contains the shared Copilot rules. These files are committed once by framework owners and pulled by every tester:

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
```

The central human-maintained rule file is:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

Do not manually edit `.github/copilot-instructions.md` or the files in `.github/instructions/`. They are generated from the central profile.

The following local folder is deliberately **not** included after Git clone or pull:

```text
.fnb-etaf/
```

It is created only if a tester enables optional MCP locally. The root `.gitignore` must contain this line:

```gitignore
/.fnb-etaf/
```

Never commit or push these local paths:

```text
.fnb-etaf/
.fnb-etaf/copilot/mcp.json
.fnb-etaf/discovery-inputs/
.fnb-etaf/discovery-drafts/
```

**Do not commit `.fnb-etaf/`.**

Do not share or commit browser profiles, browser sessions, cookies, passwords, MFA codes, tokens, API keys, authorization headers, customer data, real internal URLs, screenshots, reports, or test evidence.

## 2. Give every tester individual access

Each tester must use their own company-managed laptop, GitHub identity, and approved software. Do not share a GitHub account or a browser session between testers.

Before setup, the responsible team must provide each tester with:

- Access to the approved FNB-ETAF repository or the approved consumer project that uses the framework from Artifactory.
- An organization-approved GitHub Copilot license.
- IntelliJ IDEA, Git, Java 21, and Maven.
- An individually assigned non-production application test account only if the tester needs to run authenticated **normal FNB-ETAF tests**.

Normal authenticated FNB-ETAF testing and optional MCP are different. An approved normal test account does **not** authorize MCP to log in or reuse an authenticated browser session. MCP remains non-authenticated and review-only unless application and security owners separately approve a different design.

## 3. Install normal GitHub Copilot first

### Step 1: Pull the approved framework branch

Open the FNB-ETAF project root in IntelliJ. Pull the approved branch using your normal Git process.

```text
git status
git pull --ff-only
```

It is correct that `.fnb-etaf/` does not exist after the pull.

### Step 2: Set Java 21 in IntelliJ

Set all three IntelliJ settings to JDK 21:

1. **Project Structure → Project SDK**
2. **Project Structure → Project language level**
3. **Settings/Preferences → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE**

Reload Maven after changing the JDK.

### Step 3: Verify Java 21 and Maven in the terminal

#### macOS — IntelliJ Terminal or zsh

Open a new terminal at the project root and run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
hash -r

java -version
javac -version
mvn -version
```

Every version command must show Java 21. If Maven is not found, install the organization-approved Maven distribution, reopen the terminal, and run the checks again.

To set Java 21 for future macOS zsh terminals, add these lines to `~/.zshrc` after confirming the JDK is approved:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

#### Windows — IntelliJ Terminal or PowerShell

Find the actual approved Java 21 installation directory. Its location depends on the JDK vendor. Set it for the current PowerShell session, replacing the sample directory with the actual directory on the laptop:

```powershell
$jdk21 = 'C:\Program Files\<Approved-JDK-Vendor>\jdk-21'
Test-Path $jdk21

$env:JAVA_HOME = $jdk21
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
javac -version
mvn -version
```

`Test-Path` must return `True`, and every version command must show Java 21. If Maven is not found, install the organization-approved Maven distribution and reopen PowerShell.

To make Java 21 permanent in Windows, use **Start → Edit the system environment variables → Environment Variables**. Set user variable `JAVA_HOME` to the exact Java 21 directory. Add `%JAVA_HOME%\bin` to the user `Path`. Restart IntelliJ and PowerShell.

> Do not downgrade the framework to Java 17. A Java 17 terminal can produce `UnsupportedClassVersionError` for Java 21 class files.

### Step 4: Compile and validate the committed Copilot integration

Run the commands for your operating system from the FNB-ETAF project root.

#### macOS

```bash
mvn clean test-compile -DskipTests

mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true

chmod +x scripts/sync-copilot-instructions.sh
./scripts/sync-copilot-instructions.sh verify .
```

#### Windows PowerShell

```powershell
mvn clean test-compile -DskipTests

mvn test `
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest `
  -DfailIfNoTests=true

.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
```

The Maven test command must finish with **0 failures** and **0 errors**. These commands do not create `.fnb-etaf`, open a browser, start MCP, or contact an application. They only compile and validate the committed Copilot integration.

### Step 5: Install and sign in to GitHub Copilot in IntelliJ

The IntelliJ steps are the same on macOS and Windows:

1. Open **Settings/Preferences → Plugins → Marketplace**.
2. Search for **GitHub Copilot**.
3. Install the plugin and restart IntelliJ.
4. Select **Tools → GitHub Copilot → Login to GitHub**.
5. Complete the organization-approved GitHub authorization process. [1]

Open **Copilot Chat** after login. Normal Copilot Chat is ready to use. It does not need Node.js, `.fnb-etaf`, an application URL, or a browser session.

### Step 6: Use Copilot in review-first mode

For a new test requirement, first ask Copilot to inspect and propose a small design. Use this prompt:

```text
Review this FNB-ETAF requirement before proposing changes:

[Paste the manual steps and expected results here.]

First inspect existing related feature files, Cucumber steps, configuration aliases,
and YAML locator files. Reuse existing framework steps and locators whenever possible.

Return only:
1. Existing reusable steps and locator keys.
2. Missing locator, configuration, or step information.
3. A clean draft Gherkin scenario.
4. A YAML draft only if a locator is genuinely missing.
5. The exact project files that require a human-reviewed change.

Do not edit files, commit, push, run tests, or change Hooks, BrowserFactory, runners,
reports, Maven files, configuration, or lifecycle code until I approve.
```

Review the proposed change. After approval, request the smallest necessary update. For input, textarea, or select values, use the existing **has value** assertion. For visible labels, headings, messages, buttons, and table content, use text or contains assertions.

After an approved normal Copilot change, run:

```text
mvn clean test-compile -DskipTests
Run the focused feature, runner, or test that covers the approved change.
git diff --check
git status
```

## 4. Install optional local Playwright MCP only after approval

### Step 7: Confirm MCP is allowed for the task

Use MCP only when all of the following are true:

- The organization allows GitHub Copilot MCP servers.
- A human has approved the exact non-production HTTPS origin.
- The page is non-sensitive and does not require a login or MFA.
- The purpose is to review a small number of visible locator candidates.
- The task does not need typing, form submission, account data, test data, popup/frame handling, upload, download, browser storage, network inspection, screenshots, or browser-code execution.

If one condition is not met, do not use MCP. Continue with normal Copilot Chat and the established FNB-ETAF locator-authoring process.

### Step 8: Install or verify Node.js 20 or later

Node.js is needed only for MCP. Normal Copilot Chat does not use it.

#### macOS

```bash
node --version
npx --version
```

#### Windows PowerShell

```powershell
node --version
npx --version
```

If Node.js is missing or below version 20, install the organization-approved Node.js 20 or later package. Reopen the terminal and repeat the checks. [3]

### Step 9: Generate `.fnb-etaf` locally

This is the only installation command that creates `.fnb-etaf`.

#### macOS

```bash
chmod +x scripts/copilot-discovery.sh
./scripts/copilot-discovery.sh mcp-template .
git check-ignore -v .fnb-etaf/copilot/mcp.json
```

#### Windows PowerShell

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-template -ConsumerRoot .
git check-ignore -v .fnb-etaf/copilot/mcp.json
```

The generated local file is:

```text
.fnb-etaf/copilot/mcp.json
```

The `git check-ignore` command must show the root `.gitignore` rule `/.fnb-etaf/`. If it prints nothing, stop. Add that exact rule to the root `.gitignore`, commit the `.gitignore` change, then repeat the local generation and verification.

The template command does not start MCP, download an MCP package, launch a browser, or contact an application.

### Step 10: Configure the approved origin locally

Open the local file:

```text
.fnb-etaf/copilot/mcp.json
```

Replace only this placeholder:

```text
https://<APPROVED_TARGET_ORIGIN>
```

with the exact approved non-production HTTPS origin. An origin has only the scheme and host, for example:

```text
https://approved-nonproduction.example
```

Do not add a path, credentials, wildcard, multiple origins, or a production target. Do not change any other generated security settings. Do not commit the file.

### Step 11: Add the local MCP configuration to IntelliJ

Open **Copilot Chat**. Switch to **Agent mode**, open the MCP/tools configuration, and use **Add MCP Tools** to open IntelliJ’s MCP configuration file. Copy the contents of the local `.fnb-etaf/copilot/mcp.json` into IntelliJ’s MCP configuration and save it. The exact menu label can vary by Copilot plugin version. [2]

Keep the template’s restrictive settings. Do not add persistent browser profiles, saved sessions, cookies, credentials, CDP endpoints, proxies, extensions, TLS bypasses, unrestricted file access, screenshots, PDF capture, network tools, DevTools, or arbitrary browser-code execution.

Enable MCP only for the approved locator-discovery task. Disable it after the task is complete.

## 5. Use MCP for review-only locator drafts

### Step 12: Ask Copilot for candidate text only

Do not place real internal URLs, credentials, tokens, account details, or sensitive page information in the prompt. Use the approved origin configured locally and an existing configuration alias in the response.

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

Copilot must return candidate text in Chat. It must not edit active framework files.

### Step 13: Validate and render local drafts

Copy the reviewed candidate YAML into this local ignored file:

```text
.fnb-etaf/discovery-inputs/sample-page.yml
```

Start from this committed template:

```text
src/main/resources/copilot/fnb-etaf-discovery-candidate-template.yml
```

#### macOS

```bash
./scripts/copilot-discovery.sh validate .fnb-etaf/discovery-inputs/sample-page.yml
./scripts/copilot-discovery.sh draft .fnb-etaf/discovery-inputs/sample-page.yml .
```

#### Windows PowerShell

```powershell
.\scripts\copilot-discovery.ps1 -Operation validate -InputFile .fnb-etaf\discovery-inputs\sample-page.yml
.\scripts\copilot-discovery.ps1 -Operation draft -InputFile .fnb-etaf\discovery-inputs\sample-page.yml -ConsumerRoot .
```

The tool writes review-only material below:

```text
.fnb-etaf/discovery-drafts/
```

Manually review the draft. Copy only approved locators into `src/test/resources/elements/` and only approved Gherkin into `src/test/resources/features/`. Then run the focused FNB-ETAF test. Commit the approved active framework changes only, never `.fnb-etaf/`.

## 6. Update framework-wide Copilot rules only through owners

Only designated framework owners should change the central profile:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

After an approved profile change, regenerate and validate the committed instructions.

#### macOS

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true

./scripts/sync-copilot-instructions.sh sync .
./scripts/sync-copilot-instructions.sh verify .
git diff --check
```

#### Windows PowerShell

```powershell
mvn test `
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest `
  -DfailIfNoTests=true

.\scripts\sync-copilot-instructions.ps1 -Operation sync -ConsumerRoot .
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
git diff --check
```

Protect the profile, generated `.github` files, scripts, and Copilot tests with protected branches and required review from FNB-ETAF owners. Git permissions, required pull-request reviews, CI validation, and controlled Artifactory publishing are the enforcement controls. An instruction file alone cannot prevent a tester from editing files locally.

## 7. Add or remove a tester

For a new tester, grant repository access and a Copilot license, then have them complete Steps 1–6. Grant an individual non-production test account only if normal FNB-ETAF execution needs it. Complete Steps 7–13 only when MCP is approved for that tester and task.

When a tester no longer needs access, remove their repository access and Copilot entitlement, revoke or disable their individual application test account as appropriate, and remove their local IntelliJ MCP configuration. No framework source-code change is required because sessions, credentials, and `.fnb-etaf/` files were never shared or committed.

## 8. Troubleshooting

**Java 17 is shown or an `UnsupportedClassVersionError` occurs.** Set Java 21 in IntelliJ and in the current macOS/Windows terminal as shown in Step 3. Restart IntelliJ Terminal or PowerShell before trying Maven again.

**PowerShell blocks a `.ps1` script.** Do not bypass a company execution policy. Ask the workstation or security administrator to provide the approved PowerShell execution policy or code-signing process.

**`.fnb-etaf` is missing after Git pull.** This is correct. It exists only after the local Step 9 template command.

**`git check-ignore` returns no output.** Ensure the root `.gitignore` contains exactly `/.fnb-etaf/`. Commit that ignore rule before using MCP.

**MCP options are not available in IntelliJ.** Confirm the Copilot plugin is installed, IntelliJ was restarted, and your organization allows Copilot MCP. If it is disabled by policy, ask the Copilot administrator. Do not bypass the policy. [1] [2]

**The page requires login, MFA, a popup, iframe, test data, submission, upload, download, or customer data.** Do not use MCP. Use normal FNB-ETAF development and review instead.

## References

[1]: https://docs.github.com/copilot/managing-copilot/configure-personal-settings/installing-the-github-copilot-extension-in-your-environment "Installing the GitHub Copilot extension in your environment"
[2]: https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/customize-copilot/extend-copilot-with-tools-and-context/extend-copilot-chat-with-mcp "Extend Copilot Chat with MCP in JetBrains IDEs"
[3]: https://playwright.dev/docs/getting-started-mcp "Playwright MCP getting started"
