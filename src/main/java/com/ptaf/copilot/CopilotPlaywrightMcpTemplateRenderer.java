package com.ptaf.copilot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Renders the local-only JetBrains {@code mcp.json} template from the central policy.
 *
 * <p>The rendered file deliberately keeps an approved-origin placeholder. A tester must replace
 * that placeholder manually in IntelliJ's local MCP configuration and obtain the appropriate
 * approval before enabling the server. FNB-ETAF never launches the server or supplies a target.</p>
 */
public final class CopilotPlaywrightMcpTemplateRenderer {
    /** Classpath resource containing the static JSON template. */
    public static final String TEMPLATE_RESOURCE = "copilot/fnb-etaf-playwright-mcp-template.json";

    /** Renders a template with the central policy's pinned package and local-safe defaults. */
    public String render(CopilotDiscoveryPolicy policy) {
        if (policy == null) {
            throw new CopilotProfileException("Discovery policy must not be null.");
        }
        if (!policy.defaultHeadless()) {
            throw new CopilotProfileException("The governed local Playwright MCP template requires default_headless: true. "
                    + "Use a separately approved headed inspection process when visual browser review is required.");
        }
        String template = readTemplate();
        return template
                .replace("${MCP_SERVER_NAME}", policy.mcpServerName())
                .replace("${PLAYWRIGHT_MCP_PACKAGE}", policy.playwrightMcpPackage())
                .replace("${HEADLESS_ARGUMENT}", "--headless")
                .replace("${BROWSER}", policy.defaultBrowser())
                .replace("${CONSOLE_LEVEL}", policy.consoleLevel())
                .replace("${IDLE_TIMEOUT_MILLIS}", String.valueOf(policy.idleTimeoutMillis()));
    }

    private String readTemplate() {
        try (InputStream input = CopilotPlaywrightMcpTemplateRenderer.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (input == null) {
                throw new CopilotProfileException("Playwright MCP template was not found on the classpath: "
                        + TEMPLATE_RESOURCE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n").replace('\r', '\n');
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not read Playwright MCP template resource.", exception);
        }
    }
}
