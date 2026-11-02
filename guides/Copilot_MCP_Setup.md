# FNB-ETAF: GitHub Copilot and local Playwright MCP setup

Updated: 2 November 2026. This guide replaces the earlier after-pull and first-time MCP instructions. Use the scripts supplied with this update; older versions do not have `mcp-setup` or `-RegisterCopilot`.

Normal GitHub Copilot helps write and understand tests. Optional Playwright MCP gives Copilot tools to inspect an approved, non-sensitive live page from the tester's own laptop. Neither installs automatic AI self-healing or gives permission to edit active locators automatically.

The folder is **`.fnb-etaf`**, not `.fb-etaf`. You generate it locally with a **command**, not a Git commit. Never push this folder.

## 1. What each tester needs

Use a company-managed laptop, an individual approved GitHub Copilot account/license, IntelliJ IDEA with the GitHub Copilot plugin, Java 21, Maven or an approved Maven Wrapper, and the approved framework/consumer project. Optional MCP additionally needs company-approved Node.js with npm/npx, approved Google Chrome for the default browser, package-registry access through the company-approved configuration, and access to the target network/VPN. Use a supported Node.js LTS release compatible with the pinned MCP package. [1] [2] [3] [4]

Your GitHub organization/enterprise administrator must enable **MCP servers in Copilot**. A target application's test account does not authorize sending page content to an AI service. Obtain approval for the exact environment, origin, visible data, and read-only discovery purpose. Do not use sensitive banking/customer pages, production, login/MFA pages, or shared browser sessions for this workflow. [1]

Do not install IntelliJ's own MCP server to make this work. We use **GitHub Copilot as the MCP client** and the local pinned **Playwright MCP server** as its browser tool. The built-in GitHub MCP server is for GitHub tools, not Playwright webpage inspection.

## 2. Framework owner versus Artifactory tester

**Framework owners** apply this Dev-based update in a working branch, validate it, have it reviewed, and publish the new approved framework version through the existing Artifactory release process. They maintain the central rules in `src/main/resources/copilot/fnb-etaf-copilot-profile.yml`. The existing Maven coordinates and normal runtime are not changed by this update.

**Artifactory testers** update their existing framework dependency to the approved published version. Do not copy Java framework classes into the consumer project's `src/main/java`, edit dependency sources, or publish a private replacement of the framework. The Copilot CLI classes and resource templates are inside the new framework JAR. An older JAR will not contain `CopilotMcpEnvironmentSetup`.

Every consumer project also needs the helper scripts from this package under its root `scripts/` directory, including both `copilot-common` helpers and `register-copilot-mcp.ps1`. Copy the scripts and this guide into the team's consumer-project template once. Consumer projects retain their own `src/test/resources/config/config.yml`, locator YAML, features, data, and approved dependency configuration.

A Maven JAR does **not** automatically install `.github` instructions into a consumer repository. Owners export/review those instructions in each consumer template using the explicit sync command described below. They contain rules, not local origins or credentials. Instructions guide Copilot; branch protection, reviews, controlled publishing, and client permissions provide enforcement.

## 3. Keep local files out of Git first

Ensure the project-root `.gitignore` has this exact line:

```gitignore
/.fnb-etaf/
```

Commit this ignore rule with the shared update. The local initializer refuses to proceed without it. Do not use `git add -f` on the ignored folder. If `.fnb-etaf` was already tracked, an ignore rule alone will not untrack it: stop and ask the repository/security owner to review untracking and any disclosure.

Shared files that may be reviewed and committed are framework code/tests, safe embedded templates, scripts, the central profile, generated `.github/*.md` instructions, and this guide. Do not commit `.fnb-etaf`, the personal Copilot MCP file, private backups, browser profiles, cookies, tokens, customer data, or evidence.

## 4. Set Java 21 and install Copilot

In IntelliJ, set **Project SDK**, **Project language level**, and **Maven Runner JRE** to Java 21. Reload the Maven project. IntelliJ's bundled Maven is not automatically available to a terminal command named `mvn`.

Install the **GitHub Copilot** plugin from **Settings/Preferences → Plugins**, restart IntelliJ, and sign in through your organization's approved GitHub process. Do not uninstall the existing correct plugin just because MCP is not configured yet. [2]

### Windows: open IntelliJ PowerShell

Open **View → Tool Windows → Terminal**. Select **Windows PowerShell** or **PowerShell** in the terminal-tab dropdown. If necessary, set **Settings → Tools → Terminal → Shell path** to the organization-approved `powershell.exe` or `pwsh.exe`, then open a new terminal tab.

The terminal should accept `$PSVersionTable`. A prompt like `PS C:\...>` indicates PowerShell. A plain `C:\...>` prompt may be Command Prompt, which does not directly run these `.ps1` commands. Do not paste macOS `.sh` commands into Command Prompt.

From the project root containing `pom.xml`, run:

```powershell
$PSVersionTable.PSVersion
Test-Path .\pom.xml
java -version
javac -version
mvn -version
node --version
npm.cmd --version
npx.cmd --version
```

`Test-Path` must return `True`. Java, javac, and Maven's reported Java must be 21. Node/npm/npx are needed only for optional MCP. If the project has `mvnw.cmd`, the helper scripts use it automatically; otherwise install approved Maven and put its `bin` directory on `PATH`.

If Maven is missing, obtain its approved install directory, then add that actual directory for the current terminal session:

```powershell
$mavenBin = 'C:\<actual-approved-maven-folder>\bin'
if (-not (Test-Path -LiteralPath (Join-Path $mavenBin 'mvn.cmd'))) { throw 'Correct the Maven path first.' }
$env:Path = "$mavenBin;$env:Path"
mvn -version
```

For a permanent workstation change, use your organization's supported environment-variable setup and restart IntelliJ. Do not downgrade to Java 17, change `pom.xml` to solve missing `mvn`, or bypass corporate PowerShell execution policies.

### macOS: open IntelliJ Terminal

From the project root in zsh/bash, run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
mvn -version
node --version
npm --version
npx --version
chmod +x scripts/copilot-discovery.sh scripts/sync-copilot-instructions.sh
```

Use the actual approved Java 21 installation. Obtain approved Maven/Node software from the workstation team if missing. Do not change the framework's Java version.

## 5. Verify normal Copilot instructions

Framework owners run this from the framework root before delivery:

```powershell
mvn clean test-compile '-DskipTests'
mvn test '-Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest,CopilotMcpEnvironmentSetupContractTest' '-DfailIfNoTests=true'
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
git diff --check
```

macOS equivalent:

```bash
mvn clean test-compile -DskipTests
mvn test -Dtest=CopilotProfileContractTest,CopilotInstructionExporterTest,CopilotDiscoveryDraftContractTest,CopilotMcpEnvironmentSetupContractTest -DfailIfNoTests=true
./scripts/sync-copilot-instructions.sh verify .
git diff --check
```

The focused test classes exist in the **framework repository**, not automatically in an Artifactory consumer. Consumer owners run their own compilation, focused tests, and instruction export/verification instead:

```powershell
.\scripts\sync-copilot-instructions.ps1 -Operation sync -ConsumerRoot .
.\scripts\sync-copilot-instructions.ps1 -Operation verify -ConsumerRoot .
```

Or on macOS:

```bash
./scripts/sync-copilot-instructions.sh sync .
./scripts/sync-copilot-instructions.sh verify .
```

Back up or commit your current approved instructions before sync: it updates the expected generated files from the embedded profile and can replace their current content. Review the instruction diff before committing it, and reconcile any separate consumer-specific policies through the owner rather than losing them. Verification reports mismatches and unexpected generated files without editing them. **Verify and contract-test commands do not create `.fnb-etaf`.** Normal Copilot Chat works without MCP.

## 6. Generate `.fnb-etaf` locally — the exact installation command

**Windows: in your IntelliJ PowerShell terminal, at the root containing `pom.xml`, run:**

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-template -ConsumerRoot .
Test-Path .\.fnb-etaf\copilot\mcp.json
Test-Path .\.fnb-etaf\copilot\selection.yml
git check-ignore -v .fnb-etaf/copilot/mcp.json .fnb-etaf/copilot/selection.yml
```

Both `Test-Path` commands should return `True`. Git should show the `/.fnb-etaf/` rule for both files. Do not push the folder. This command does not run tests, start MCP, or open a website. Repeating it preserves existing local files.

macOS:

```bash
./scripts/copilot-discovery.sh mcp-template .
test -f .fnb-etaf/copilot/mcp.json
test -f .fnb-etaf/copilot/selection.yml
git check-ignore -v .fnb-etaf/copilot/mcp.json .fnb-etaf/copilot/selection.yml
```

The generated MCP template still contains an origin placeholder. Do not register or start that empty template yet.

## 7. Select an already-approved target in one local place

Open **`.fnb-etaf/copilot/selection.yml`** in IntelliJ. The initial file is deliberately disabled:

```yaml
environment: 'QA'
selected_target_alias: ''
approved_target_aliases: []
non_sensitive_target_approved: false
```

After the application/data/security owners approve the exact non-sensitive target and AI discovery purpose, enter the existing alias from your standard `config.yml`. Example using a logical alias only:

```yaml
environment: 'QA'
selected_target_alias: 'HARNESS_PREPROD_STAGE'
approved_target_aliases:
  - 'HARNESS_PREPROD_STAGE'
non_sensitive_target_approved: true
```

Do not paste a URL into this selection file. Retain the actual URL only in your existing organization-approved standard configuration. This package does not add, commit, or alter an internal URL.

The selected alias must exist in `src/test/resources/config/config.yml`. Resolution uses this order:

```text
environments.<environment>.<selected_target_alias>
then, if absent:
<selected_target_alias>
```

This follows normal environment override/key lookup without calling or changing `getBaseUrl`. For top-level aliases, the alias's own configured value is used. `environment` is the environment identifier, not a URL. The local setup command does not use IntelliJ runner VM options or `-Denv`; select the desired environment explicitly in this local file.

A synthetic configuration example illustrates the lookup without real endpoints:

```yaml
HARNESS_PREPROD_STAGE: 'https://fallback.example.invalid/form?formCode=sample'
environments:
  QA:
    HARNESS_PREPROD_STAGE: 'https://qa.example.invalid/form?formCode=sample'
```

For that example, `environment: 'QA'` selects the QA override and the setup writes only `https://qa.example.invalid` into MCP's allowed-origin argument. Paths, query strings, and fragments are not copied. These sample domains are illustrative and are not working targets.

Only HTTPS targets are accepted. Credentials in URLs, wildcard origins, localhost/loopback/metadata targets, malformed ports, and the environment labels `PROD`, `PRODUCTION`, or `LIVE` are rejected. These checks cannot prove that a target is non-production or non-sensitive. Human approval must cover the actual resolved target, including after endpoint changes.

## 8. Automatically prepare and register the selected origin on Windows

Stop `fnb-etaf-playwright-local` in Copilot Tools before switching targets. Then run this **one command** in IntelliJ PowerShell:

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-setup -ConsumerRoot . -RegisterCopilot
```

It reads your local selection, resolves the existing URL using the selected environment, derives its origin, and updates only `.fnb-etaf/copilot/mcp.json`. It then merges **only** `fnb-etaf-playwright-local` into the personal GitHub Copilot configuration. Other server entries and top-level settings are preserved. Existing personal JSON gets a private timestamped backup; the prior generated project-local MCP file is kept as `.fnb-etaf/copilot/mcp.json.bak` when changed. Neither backup goes in Git.

The default Windows personal path matches the file opened in the Copilot screenshots:

```text
%LOCALAPPDATA%\github-copilot\intellij\mcp.json
```

If your installed plugin opens a different personal path, first use **Copilot Chat → Agent → tools/sliders icon → Add More Tools/Add MCP Tools** to find the actual file. Then explicitly supply that personal path outside the project:

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-setup -ConsumerRoot . -RegisterCopilot -CopilotConfigPath 'C:\<actual-personal-folder>\mcp.json'
```

Do not use `.github/mcp.json` or a project path. Registration into the repository is refused. Do not replace an existing personal configuration wholesale if it has other servers.

If you prefer to review before registration, separate the operations:

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-setup -ConsumerRoot .
# Review the local allowed origin privately in IntelliJ. Do not paste it into Chat.
.\scripts\register-copilot-mcp.ps1 -ConsumerRoot .
```

Neither script launches a browser or explicitly starts an MCP server. Copilot may detect the saved configuration; follow its permission controls before use. Updating the file alone is not evidence that a webpage is reachable.

### macOS preparation and manual registration

Run:

```bash
./scripts/copilot-discovery.sh mcp-setup .
```

Open **Copilot Chat → Agent → tools/sliders icon → Add More Tools/Add MCP Tools**. Use the personal/user MCP file opened by your installed plugin; do not assume the Windows path applies to macOS. Merge the `fnb-etaf-playwright-local` server entry from the generated local JSON into its `servers` object. Preserve any other servers and back up the personal file locally first. If it is truly empty, the complete generated JSON can replace the empty file. Do not save a copy under `.github`. [1]

## 9. Start the registered server and check its tools

The **Auto** dropdown chooses an AI model; it does not configure MCP. In normal GitHub Copilot Chat, select **Agent**, then click the tools/sliders icon. In the tool dialog, the registered row should read:

```text
fnb-etaf-playwright-local    Stopped    Start
```

Click **Start**. When connected, expand the server row and enable only the tools needed for the approved inspection. Click **Apply** in the tools dialog and **OK** if the main Settings window is also open. If the plugin presents those controls in a different order, first save the selection and reopen the tools list to start the server. Starting the MCP process is not a webpage test. The tools must load successfully. [1]

For read-only discovery, keep only the required navigation-to-the-explicit-approved-target and visible-page snapshot/inspection tools. Do not enable browser click/type/fill/submit, uploads/downloads, screenshots, network/storage/console inspection, arbitrary evaluation/code, or authentication tools. For this session, deselect unneeded file-write, patch, terminal-execution, and subagent tools. The default MCP toolset is not inherently read-only; the generated flags and instructions do not remove all dangerous tools.

Keep **Default Approvals** and inspect each requested tool call. Do not select Bypass Approvals or Autopilot. An initial approved direct navigation is necessary to open a page; do not approve click-through workflow navigation or unexpected redirects. Stop if sensitive data or a login page appears.

The server runs a separate isolated **headless** browser by default. You normally will not see a browser window. It does not attach to the browser you already have open or inherit its cookies/login. [3]

Do not change Gateway auto-start, built-in GitHub MCP, registry, or sampling settings to fix browser setup. The sampling dialog saying “No MCP servers configured” is not the server editor. Do not install a second unrelated IntelliJ MCP server. Agent Customizations may also manage MCP for other providers; use the tools associated with the actual Copilot session in which you will make the request.

## 10. Inspect an approved visible page and keep locator storage review-only

Use the existing selected alias rather than placing an internal URL in a Chat prompt. After confirming reachability and approval, use:

```text
Use fnb-etaf-playwright-local only, on this local tester workstation.
Read the selected alias in .fnb-etaf/copilot/selection.yml and resolve only that
alias from the selected environment in the standard config.yml. Do not display
or reproduce its URL or unrelated configuration values in your response.

Request approval for one direct navigation to the selected non-sensitive view.
After approval, inspect only the currently visible top-level page and return up
to 10 locator candidates supported by the tool results. Prefer stable test IDs,
IDs, labels, placeholders, or accessible role/name when actually available.
Do not guess attributes you cannot inspect; mark uncertain candidates for
manual verification. Use logical YAML keys and the existing target alias.

Do not log in, click through the flow, type, submit, accept dialogs, open popups,
inspect frames, run arbitrary browser code, inspect network/storage/console,
capture screenshots/source, edit files, run terminal commands, or start other
agents. Stop on redirects, sensitive content, login requirements, or TLS errors.
Return candidate text only; do not change active framework files.
```

MCP page results may be passed to the configured AI model. “Local browser” does not mean all page data stays on the laptop. Approval must cover that disclosure. MCP cannot automatically inspect unseen next pages or authenticated screens under this policy.

Use supported framework locator types, not arbitrary new syntax. Start a local candidate YAML from `src/main/resources/copilot/fnb-etaf-discovery-candidate-template.yml`; in an Artifactory consumer, obtain this safe template from the approved JAR/package. Copy reviewed candidate text into an ignored input file, then validate/render it explicitly:

```powershell
.\scripts\copilot-discovery.ps1 -Operation validate -InputFile .fnb-etaf\discovery-inputs\sample-page.yml -ConsumerRoot .
.\scripts\copilot-discovery.ps1 -Operation draft -InputFile .fnb-etaf\discovery-inputs\sample-page.yml -ConsumerRoot .
```

macOS:

```bash
./scripts/copilot-discovery.sh validate .fnb-etaf/discovery-inputs/sample-page.yml .
./scripts/copilot-discovery.sh draft .fnb-etaf/discovery-inputs/sample-page.yml .
```

Create the input directory/file locally if missing. The generator writes only ignored review drafts. A tester reviews and manually promotes approved entries into their consumer project's `src/test/resources/elements/` and `features/`, then runs the focused normal test. No framework source-class changes are required for a normal locator/test update. Stop/disable the MCP server after the approved task.

## 11. Switch targets later — change one selection, rerun one command

After initial approval setup, keep a list of individually approved aliases in local `approved_target_aliases`. To switch between those already-approved targets, change **`selected_target_alias`** in `selection.yml`; change `environment` only if the approved environment also changes. Stop the MCP server, then run:

```powershell
.\scripts\copilot-discovery.ps1 -Operation mcp-setup -ConsumerRoot . -RegisterCopilot
```

Start/restart **only** `fnb-etaf-playwright-local` in Copilot Tools, then make the next explicit approved inspection request. You do not need to hand-edit the origin twice or call normal `getBaseUrl` to register it.

One origin covers paths/query strings on the same scheme, host, and effective port. A different subdomain/port is another origin. This preset deliberately selects **one origin at a time** rather than silently accumulating hosts. If external assets/redirects need other origins, stop and obtain a separately reviewed configuration. Do not solve a load error using `*` or broad domain wildcards. Playwright's origin lists are convenience guardrails, **not a security boundary**, and do not fully govern redirects; use corporate network controls and client-level permissions for real isolation. [3]

## 12. Troubleshooting based on the actual installation errors

**`mvn` is not recognized:** Maven is unavailable in that terminal's PATH. IntelliJ's Maven GUI can work while PowerShell cannot. Fix the approved Maven path or use the approved Wrapper; reopen IntelliJ. The updated script stops rather than proceeding with stale/missing dependencies.

**Script is not recognized:** Run from the root containing `pom.xml`, and check `Test-Path .\scripts\copilot-discovery.ps1`. Consumer projects need the scripts in addition to the framework dependency. Do not use a `.sh` command in Command Prompt.

**PowerShell blocks scripts:** Follow your organization's execution-policy/signing process. Do not use `-ExecutionPolicy Bypass`, unblock arbitrary files, or override centrally enforced restrictions.

**`NoClassDefFoundError` for SnakeYAML:** A manual `java -cp target/classes` command is missing dependencies. Use the supplied helper, which generates the runtime classpath. In a consumer, check that the correct framework artifact and transitive dependencies resolved. Do not remove dependencies or guess Maven plugin upgrades.

**Unknown `mcp-setup` / missing tool class:** An older script or older framework JAR is still in use. Apply this package in the producer, publish the reviewed version, and update the consumer dependency/scripts. No hard-coded release version is assumed here.

**`.fnb-etaf` is still missing:** Verification/tests do not create it. Run the exact `mcp-template` command in section 6. If the build fails, resolve it first. The folder is intentionally absent after Git pull.

**Selection/approval error:** Use the exact four-field schema. Select an alias present in the standard config and the local approved list. Only record `true` after actual owner approval. If a URL/environment changes, confirm the approval still applies.

**npm `ENOENT` / missing `%APPDATA%\npm`:** If this exact missing-folder error occurs, run the following in IntelliJ PowerShell, then retry starting the registered server:

```powershell
$npmBin = Join-Path $env:APPDATA 'npm'
New-Item -ItemType Directory -Path $npmBin -Force | Out-Null
Test-Path $npmBin
node --version
npm.cmd --version
npx.cmd --version
npx.cmd -y '@playwright/mcp@0.0.83' --help
```

The final command checks the pinned package's help, not a webpage. It may download the package through the configured approved registry. Do not override a blocked corporate registry or proxy. If `npx` cannot be launched by the IDE, restart IntelliJ after Node installation and have IT verify the executable resolution; do not add unrestricted server commands.

**Browser executable missing:** The default MCP channel is installed Google Chrome, not automatically the Java framework's downloaded Chromium. Obtain the approved Chrome installation. If the team approves a different browser channel, the framework owner should update the central policy and regenerate; avoid unreviewed workstation variants.

**Server is Running but webpage fails:** Running means the MCP process initialized, not that the endpoint works. Open the exact target manually in approved Chrome/Edge on the same workstation/network. If it fails, ask the application/network team to repair the endpoint. If normal browser access succeeds, compare the approved channel, certificate trust, and managed network settings with IT. Do not bypass security controls.

**`ERR_SSL_VERSION_OR_CIPHER_MISMATCH`:** This is a TLS-handshake failure for the attempted endpoint, not evidence that the framework's locators are broken. Ask the application/network team to confirm the current HTTPS host/port and TLS 1.2/1.3 configuration. Do not fix it by adding origins, downgrading to HTTP, or enabling `--ignore-https-errors`.

**“Copilot cannot browse”:** Check for the registered server and actual browser tools in the current session. Attaching `mcp.json` to Chat does not register a server. Ask for a specific approved tool operation rather than relying on the model's capability statement. Record a sanitized actual MCP log if startup fails.

**No MCP options / organization policy disabled:** Have the Copilot administrator check entitlement/plugin compatibility/MCP policy. Do not install a different agent to bypass policy. [1]

## 13. Owner validation, maintenance and rollback

After central-profile changes, explicitly run instruction `sync`, then `verify`, focused Copilot tests, and `git diff --check`. Normal Maven tests do not regenerate instructions or local MCP settings.

Owners can additionally run the local PowerShell synthetic-file script contracts:

```powershell
.\scripts\tests\Test-CopilotLocalScripts.ps1
```

To undo a personal target change, stop the server and restore the desired private backup in the personal configuration, preserving any newer unrelated server changes. To stop using MCP, remove/disable only `fnb-etaf-playwright-local` from the personal configuration and remove the ignored project-local folder if no drafts are needed. Normal framework automation continues unaffected.

Use branch protection, owner review, and controlled Artifactory publishing for framework changes. Local YAML approval fields record a tester's explicit choice; they are not a signed central authorization mechanism and cannot stop a user with local write access from editing their own files. Do not claim they enforce enterprise security by themselves.

## References

[1]: https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/customize-copilot/extend-copilot-with-tools-and-context/extend-copilot-chat-with-mcp "GitHub Copilot MCP configuration and JetBrains prerequisites"
[2]: https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/set-up-copilot/install-copilot-extension "Installing the GitHub Copilot extension"
[3]: https://playwright.dev/mcp/configuration/options "Playwright MCP configuration and access-control limitations"
[4]: https://nodejs.org/en/download "Official Node.js downloads"
