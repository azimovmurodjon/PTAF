# FNB-ETAF GitHub Copilot Complete Team Setup

**Use this guide after pulling the FNB-ETAF branch that contains the Copilot integration.**

This is the complete setup process for normal GitHub Copilot usage and the optional local Playwright MCP locator-discovery workflow. It clearly separates what belongs in Git from what is generated locally on each developer workstation.

> **The key rule:** Commit the framework Copilot integration. Do not commit `.fnb-etaf/`. Each developer creates `.fnb-etaf/` locally only when they need the optional Playwright MCP discovery workflow.

## 1. What to commit, what to generate locally, and what never to push

### Commit and push these framework files

These files are shared framework assets. They can be pushed to the approved FNB-ETAF branch:

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

The root `.gitignore` must include this rule:

```gitignore
/.fnb-etaf/
```

The framework’s central human-maintained Copilot guidance is:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

The `.github/copilot-instructions.md` file and files below `.github/instructions/` are generated from that central profile. Do not manually edit generated instruction files.

### Generate these files locally on each workstation

The following folder is created locally only after a developer runs the optional MCP template command:

```text
.fnb-etaf/
├── copilot/
│   └── mcp.json
├── discovery-inputs/
└── discovery-drafts/
```

The local `mcp.json` can contain an exact approved target origin. That is why `.fnb-etaf/` stays local and ignored by Git.

### Never commit or push these files

Do not commit or push any of these paths:

```text
.fnb-etaf/
.fnb-etaf/copilot/mcp.json
.fnb-etaf/discovery-inputs/
.fnb-etaf/discovery-drafts/
```

Do not put credentials, passwords, tokens, cookies, API keys, customer data, screenshots, videos, internal URLs, report output, browser storage, or runtime evidence into source-controlled Copilot files.

## 2. What the integration does

The committed integration provides two separate capabilities.

### Normal GitHub Copilot guidance

This is the normal workflow. Copilot reads the committed repository instructions and can help explain code, find existing reusable steps, review a defect, draft Gherkin, suggest locator candidates, and propose a minimal change. It does not need `.fnb-etaf`, a browser, Playwright MCP, or an application connection.

The instructions tell Copilot to preserve working framework behavior. In particular, it must not change Hooks, BrowserFactory, runners, reporting lifecycle, video lifecycle, `@LastScenario`, Maven configuration, normal UI behavior, API behavior, mobile behavior, or performance architecture without an explicit approved requirement.

### Optional local Playwright MCP discovery

This is an optional, short, local workflow for reviewing a small number of visible locator candidates on an explicitly approved, non-sensitive test page. It is not part of FNB-ETAF runtime. It does not run through Maven tests, CI/CD, Hooks, BrowserFactory, a Cucumber runner, or generated test execution.

The MCP template uses headless, isolated, sandboxed browser settings. It generates review-only drafts. A human must review and manually copy an approved locator or scenario into the active FNB-ETAF resources.

## 3. One-time setup after pulling the framework

### Step 1: Pull the approved branch

Open the FNB-ETAF project root in IntelliJ IDEA. Use your normal approved Git process. A safe terminal example is:

```bash
git status
git pull --ff-only
```

After pulling, it is correct that you do **not** see `.fnb-etaf/` in the project. Do not copy or create it manually.

### Step 2: Configure Java 21

FNB-ETAF uses Java 21. Both IntelliJ and the terminal must use Java 21.

In IntelliJ, configure these to JDK 21:

1. **File → Project Structure → Project SDK**
2. **File → Project Structure → Project language level**
3. **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE**

On macOS, open a new IntelliJ Terminal in the project root and run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
hash -r

java -version
javac -version
mvn -version
```

Each command must report Java 21. A Java 17 terminal can produce `UnsupportedClassVersionError` because it cannot load Java 21 class files.

To make Java 21 the default in future macOS terminal windows, add these lines to `~/.zshrc` after confirming that the selected JDK is organization-approved:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

### Step 3: Compile the framework

Run:

```bash
mvn clean test-compile -DskipTests
```

This confirms the framework and Copilot classes compile. It does not run application tests.

### Step 4: Run focused Copilot safety tests

Run:

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true
```

The result must show **0 failures** and **0 errors**. These tests are browserless. They do not start Playwright MCP or contact an application.

### Step 5: Verify the committed Copilot instructions

On macOS or Linux:

```bash
chmod +x scripts/sync-copilot-instructions.sh
./scripts/sync-copilot-instructions.sh verify .
```

On Windows PowerShell:

```powershell
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
```

This verifies that the committed `.github` instruction files match the central profile. It does not change normal framework execution.

## 4. Normal GitHub Copilot usage: use this first

### Step 6: Install and sign in to GitHub Copilot

Install the **GitHub Copilot** plugin from the JetBrains Marketplace if it is not already installed. Restart IntelliJ. Then select **Tools → GitHub Copilot → Login to GitHub** and complete the approved organization login process. [1]

Open **Copilot Chat** after login. Normal Copilot Chat works without `.fnb-etaf`.

### Step 7: Ask Copilot to review before it changes anything

Use this prompt for a new manual test case or requirement:

```text
Review this FNB-ETAF requirement before proposing changes:

[Paste the manual steps and expected results here.]

First inspect the existing related feature files, Cucumber steps, configuration aliases,
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

Review the response. Only after you approve the design should you ask Copilot to make the smallest safe code or resource change.

For an input, textarea, or select value assertion, use the existing **has value** assertion. For visible content such as a heading, message, label, button, or table value, use a text or contains assertion.

### Step 8: Validate an approved change

After a reviewed change, use the normal FNB-ETAF validation process:

```bash
mvn clean test-compile -DskipTests
# Run the focused feature, runner, or test that covers the approved change.
git diff --check
git status
```

Do not use Copilot to make broad lifecycle changes to fix one test or locator problem.

## 5. Generate `.fnb-etaf` only when optional MCP discovery is approved

### Step 9: Confirm that MCP use is permitted

Use local Playwright MCP only when all of the following are true:

- GitHub Copilot MCP servers are permitted by your organization.
- A human has approved the exact HTTPS target origin.
- The target is non-production and non-sensitive.
- The task is limited to a small number of visible locator candidates.
- The page does not require login, MFA, customer data, account information, credentials, test data entry, submission, downloads, uploads, popups, or frame interaction.

If any condition is not met, do not use MCP. Use normal Copilot Chat and existing FNB-ETAF framework authoring patterns.

### Step 10: Confirm Node.js for MCP only

The normal Copilot workflow does not need Node.js. MCP requires Node.js 20 or later and `npx`. [3]

Run:

```bash
node --version
npx --version
```

If Node is unavailable or below version 20, install only an organization-approved Node.js version.

### Step 11: Generate the local `.fnb-etaf` folder

Run this only from the FNB-ETAF project root:

```bash
chmod +x scripts/copilot-discovery.sh
./scripts/copilot-discovery.sh mcp-template .
```

On Windows PowerShell:

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-template -ConsumerRoot .
```

This is the command that creates the local folder:

```text
.fnb-etaf/copilot/mcp.json
```

It creates a template only. It does not start Node.js, download a package, start Playwright MCP, launch a browser, or connect to an application.

Confirm Git ignores the generated file:

```bash
git check-ignore -v .fnb-etaf/copilot/mcp.json
```

The command must print the `.gitignore` rule for `/.fnb-etaf/`. If it does not, add this exact line to `.gitignore`, commit the `.gitignore` update, and run the check again:

```gitignore
/.fnb-etaf/
```

### Step 12: Configure the approved target origin locally

Open the newly generated local file:

```text
.fnb-etaf/copilot/mcp.json
```

It includes this placeholder:

```text
https://<APPROVED_TARGET_ORIGIN>
```

Replace only that placeholder with the exact approved HTTPS origin, for example:

```text
https://approved-nonproduction.example
```

An origin contains only a scheme and host. Do not add a path, credentials, wildcard, multiple origins, or a production target without explicit approval. Do not change the generated security options.

### Step 13: Add the local MCP configuration to IntelliJ

Open **Copilot Chat** in IntelliJ. Switch to **Agent mode**, open the tools configuration control, and select **Add MCP Tools**. Open IntelliJ’s `mcp.json` from that screen. Copy the content from your local `.fnb-etaf/copilot/mcp.json` into IntelliJ’s MCP configuration and save it. [2]

Keep the generated settings unchanged. They include isolated, headless, sandboxed browser use and a restricted target-origin list. Do not add persistent profiles, stored session state, secrets files, proxies, CDP endpoints, extensions, TLS-error bypasses, unrestricted file access, screenshot/PDF capability, DevTools access, network inspection, console inspection, or arbitrary browser-code execution.

Enable MCP only for the approved locator-discovery task. Disable it afterward.

## 6. Use MCP for review-only locator discovery

### Step 14: Ask for candidate text, not direct changes

Use a prompt like this:

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

### Step 15: Validate a local candidate YAML file

Copy the reviewed candidate text into a local ignored file, for example:

```text
.fnb-etaf/discovery-inputs/sample-page.yml
```

Use this committed template as the starting format:

```text
src/main/resources/copilot/fnb-etaf-discovery-candidate-template.yml
```

The candidate must use an existing configuration alias. It must not include a resolved URL, password, token, cookie, authorization value, account data, typed input value, page source, HTML, screenshot, header, or report data.

Validate the candidate:

```bash
./scripts/copilot-discovery.sh validate .fnb-etaf/discovery-inputs/sample-page.yml
```

If validation fails, correct the local candidate. Do not weaken the central safety profile just to accept it.

### Step 16: Generate review-only drafts locally

After validation succeeds, run:

```bash
./scripts/copilot-discovery.sh draft .fnb-etaf/discovery-inputs/sample-page.yml .
```

The tool writes only local review artifacts below:

```text
.fnb-etaf/discovery-drafts/
```

The output includes a locator YAML draft, a Gherkin feature draft, and a README. The tool does not overwrite active FNB-ETAF locators, feature files, configuration, Hooks, BrowserFactory, runners, reports, or tests.

### Step 17: Review and promote manually

Review the locator candidate against the approved target. Prefer stable test IDs, then stable IDs, then unique accessible role/name selectors. Use CSS or XPath only when a stable higher-level locator is unavailable.

Manually copy only approved locator entries into the correct file under:

```text
src/test/resources/elements/
```

Manually copy only approved Gherkin into the correct file under:

```text
src/test/resources/features/
```

Run the focused FNB-ETAF test manually. Commit only the reviewed active framework changes. Do not commit `.fnb-etaf/`.

## 7. Change Copilot instructions correctly

When a framework-wide Copilot rule changes, edit only:

```text
src/main/resources/copilot/fnb-etaf-copilot-profile.yml
```

Then regenerate the committed instruction files and validate them:

```bash
mvn test \
  -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest \
  -DfailIfNoTests=true

./scripts/sync-copilot-instructions.sh sync .
./scripts/sync-copilot-instructions.sh verify .
git diff --check
```

Review and commit the central profile, generated `.github` instructions, focused tests, and documentation together. Do not commit the local `.fnb-etaf` workspace.

## 8. Quick troubleshooting

### Java error mentions class-file version 61

The terminal is using Java 17. Set Java 21, open a fresh IntelliJ Terminal, reload Maven, and run:

```bash
mvn clean test-compile -DskipTests
```

### `.fnb-etaf` does not exist after pull

This is correct. It is local only. Generate it only when MCP is approved:

```bash
./scripts/copilot-discovery.sh mcp-template .
```

### Copilot is available but MCP controls are not shown

Confirm that the Copilot plugin is installed, IntelliJ has been restarted, and you are signed in with organization-approved Copilot access. MCP can be disabled by an organization policy. Ask the Copilot administrator; do not bypass the policy. [1] [2]

### The local `mcp.json` still shows `<APPROVED_TARGET_ORIGIN>`

This is correct until a human supplies the exact approved target origin locally. Never commit the completed local configuration.

### The page needs a login, sensitive data, an iframe, popup, submission, upload, or download

Stop the MCP session and use normal FNB-ETAF development and review instead.

## References

[1]: https://docs.github.com/copilot/managing-copilot/configure-personal-settings/installing-the-github-copilot-extension-in-your-environment "Installing the GitHub Copilot extension in your environment"
[2]: https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/customize-copilot/extend-copilot-with-tools-and-context/extend-copilot-chat-with-mcp "Extend Copilot Chat with MCP in JetBrains IDEs"
[3]: https://playwright.dev/docs/getting-started-mcp "Playwright MCP getting started"
