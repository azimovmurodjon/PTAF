# Validation — FNB-ETAF selected-environment MCP update

Baseline: remote `dev`, commit `c66665783dc5a4a16dd508cd86a5ebace8334694`. Validation date: 2 November 2026.

## Completed checks

- Untouched baseline: Java 21 `mvn clean test-compile -DskipTests` passed.
- Updated source: Java 21 `mvn clean test-compile -DskipTests` passed.
- Existing Copilot profile/export/discovery contracts plus new selected-environment contracts: **80 tests, 0 failures, 0 errors, 0 skipped**.
- `mvn package -DskipTests` passed. The normal framework JAR contains `CopilotMcpEnvironmentSetup` and the embedded local selection template. The existing attached shaded artifact also builds; build warnings do not change the MCP package's runtime behavior. The package deliberately does not distribute that large bundle.
- Instruction sync then verification passed.
- Bash helper syntax checks passed.
- PowerShell script parser/registration/Maven-failure contracts: **16 checks passed**, using PowerShell 7.6.9 on the Linux sandbox. Tests use synthetic files and do not modify a real user's Copilot profile. Scripts are written to Windows PowerShell 5.1-compatible syntax; native Windows 5.1 and macOS IntelliJ UI operation were not directly exercised.
- Separate Maven dependency-consumer smoke test passed using only the rebuilt thin JAR under isolated test-only Maven coordinates and the scripts. No framework Java source classes were copied into the consumer. Tested PowerShell template generation, environment override, HTTPS origin extraction, target switch, nondefault port, explicit personal-config registration/merge, other-server preservation, backups, Bash idempotence, unchanged standard configuration, instruction export/verification, and Git ignore.
- Normal runtime source directories and `pom.xml` have no changes in this overlay.
- The exact delivery patch applied successfully to a fresh clean baseline worktree. Java 21 compilation passed again; all **80 focused tests** and **16 script checks** passed again, instruction verification passed, and `git diff --check` passed. This validates the delivered patch rather than only the development workspace.

## Not claimed

No internal endpoint was contacted, no user application suite was executed, and no production or sensitive browser page was inspected. There is no claim that the user's workstation network/TLS/proxy/Chrome setup or IntelliJ plugin session is working; check those locally after installing the reviewed update.

No automatic login, click-through flow navigation, active YAML editing, runtime AI self-healing, remote/cloud browser inspection, CI browser discovery, or automatic MCP startup was added.

Selecting an alias and writing local approval fields do not confer organizational approval. Playwright origin lists and AI instruction files are convenience guardrails, not hard security boundaries; individual client tool permissions, network controls, and human review remain necessary.
