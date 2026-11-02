package com.ptaf.copilot;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Explicit, optional, browserless preparation of a local MCP configuration.
 *
 * <p>Approval is entered manually in the ignored selection file after owner approval, not inferred
 * from alias names or normal framework configuration. Only the standard config.yml is read;
 * framework configuration, browser state, and runtime behavior are never modified.</p>
 */
public final class CopilotMcpEnvironmentSetup {
    public static final String SELECTION_RESOURCE = "copilot/fnb-etaf-mcp-selection-template.yml";
    public static final Path SELECTION_PATH = Path.of(".fnb-etaf/copilot/selection.yml");
    public static final Path MCP_PATH = Path.of(".fnb-etaf/copilot/mcp.json");
    private static final String ORIGIN_MARKER = "https://<APPROVED_TARGET_ORIGIN>";
    private static final String IDENTIFIER = "[A-Za-z][A-Za-z0-9_.-]{0,99}";
    private static final Set<String> SELECTION_KEYS = Set.of("environment", "selected_target_alias",
            "approved_target_aliases", "non_sensitive_target_approved");
    private final CopilotPlaywrightMcpTemplateRenderer renderer;

    public CopilotMcpEnvironmentSetup() {
        this(new CopilotPlaywrightMcpTemplateRenderer());
    }

    CopilotMcpEnvironmentSetup(CopilotPlaywrightMcpTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    /** Initializes safe empty defaults once; existing local selections are never replaced. */
    public Path initializeSelection(Path consumerRoot) throws IOException {
        Path root = validatedRoot(consumerRoot);
        Path selection = safePath(root, SELECTION_PATH);
        if (!Files.exists(selection, LinkOption.NOFOLLOW_LINKS)) {
            writeAtomic(root, selection, readSelectionTemplate(), false);
        }
        return selection;
    }

    /** Preserves an existing MCP file and selection; a new template retains the origin placeholder. */
    public Path writeMcpTemplate(CopilotDiscoveryPolicy policy, Path consumerRoot) throws IOException {
        Path root = validatedRoot(consumerRoot);
        Path output = governedOutput(root, policy);
        Path selection = safePath(root, SELECTION_PATH);
        String template = Files.exists(output, LinkOption.NOFOLLOW_LINKS) ? null : renderer.render(policy);
        // Preflight both local artifacts before initializing either one.
        if (!Files.exists(selection, LinkOption.NOFOLLOW_LINKS)) {
            writeAtomic(root, selection, readSelectionTemplate(), false);
        }
        if (template != null) {
            writeAtomic(root, output, template, false);
        }
        return output;
    }

    /**
     * Resolves an explicitly approved alias and writes only a governed origin-only MCP file.
     * If selection is missing, initializes empty defaults and asks the caller to edit and rerun.
     * All selection/configuration/URI validation finishes before an MCP file or backup is written.
     */
    public SetupOutput setup(CopilotDiscoveryPolicy policy, Path consumerRoot) throws IOException {
        Path root = validatedRoot(consumerRoot);
        Path output = governedOutput(root, policy);
        Path selectionPath = safePath(root, SELECTION_PATH);
        if (!Files.exists(selectionPath, LinkOption.NOFOLLOW_LINKS)) {
            initializeSelection(root);
            throw new CopilotProfileException("Local selection.yml initialized with no approvals. "
                    + "After owner approval, edit the approved fields and rerun mcp-setup. No MCP file was changed.");
        }
        Selection selection = readSelection(selectionPath);
        Path config = safePath(root, CopilotDiscoveryTargetConfigurationVerifier.STANDARD_CONFIGURATION_PATH);
        Map<?, ?> values = readYamlMapping(config, "standard FNB-ETAF configuration");
        Object rawTarget = resolve(values, "environments." + selection.environment() + "." + selection.alias());
        if (rawTarget == null) {
            rawTarget = resolve(values, selection.alias());
        }
        if (!(rawTarget instanceof String target) || target.isBlank()) {
            throw new CopilotProfileException("Selected alias is not a non-blank URL in standard configuration.");
        }
        String origin = approvedOrigin(target);
        String template = renderer.render(policy);
        int marker = template.indexOf(ORIGIN_MARKER);
        if (marker < 0 || marker != template.lastIndexOf(ORIGIN_MARKER)) {
            throw new CopilotProfileException("The governed MCP template must contain exactly one approved-origin marker.");
        }
        // Replace the exact governed marker only, never arbitrary consumer data.
        String rendered = template.replace(ORIGIN_MARKER, origin);
        String current = Files.exists(output, LinkOption.NOFOLLOW_LINKS) ? readLocal(output) : null;
        if (rendered.equals(current)) {
            return new SetupOutput(output, selection.alias(), false);
        }
        if (current != null) {
            // A fixed local backup keeps the immediately previous file, without accumulating artifacts.
            Path backup = safePath(root, Path.of(".fnb-etaf/copilot/mcp.json.bak"));
            writeAtomic(root, backup, current, true);
        }
        writeAtomic(root, output, rendered, true);
        return new SetupOutput(output, selection.alias(), true);
    }

    private static Selection readSelection(Path path) {
        Map<?, ?> values = readYamlMapping(path, "local MCP selection");
        if (!values.keySet().equals(SELECTION_KEYS)) {
            throw new CopilotProfileException("Local MCP selection must contain only the four required selection fields.");
        }
        if (!Boolean.TRUE.equals(values.get("non_sensitive_target_approved"))) {
            throw new CopilotProfileException("Owner approval for a non-sensitive target is required. "
                    + "Edit local selection.yml after owner approval and rerun mcp-setup.");
        }
        if (!(values.get("environment") instanceof String environment) || !environment.matches(IDENTIFIER)) {
            throw new CopilotProfileException("Selection environment must be a safe identifier of at most 100 characters.");
        }
        if (Set.of("PROD", "PRODUCTION", "LIVE").contains(environment.toUpperCase(Locale.ROOT))) {
            throw new CopilotProfileException("Production/live environments are not permitted for local MCP setup.");
        }
        if (!(values.get("selected_target_alias") instanceof String alias) || !alias.matches(IDENTIFIER)) {
            throw new CopilotProfileException("Selected target alias must be a safe identifier of at most 100 characters.");
        }
        if (!(values.get("approved_target_aliases") instanceof List<?> approved)
                || approved.stream().anyMatch(value -> !(value instanceof String text) || !text.matches(IDENTIFIER))
                || !approved.contains(alias)) {
            throw new CopilotProfileException("Selected alias must be explicitly listed in approved_target_aliases. "
                    + "Aliases do not grant automatic approval.");
        }
        return new Selection(environment, alias);
    }

    private static Map<?, ?> readYamlMapping(Path path, String label) {
        try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(10);
            options.setCodePointLimit(250_000);
            Object parsed = new Yaml(new SafeConstructor(options)).load(input);
            if (!(parsed instanceof Map<?, ?> values)) {
                throw new CopilotProfileException("The " + label + " must be a YAML mapping.");
            }
            return values;
        } catch (CopilotProfileException exception) {
            throw exception;
        } catch (RuntimeException | IOException exception) {
            // SnakeYAML diagnostics and causes can contain raw URL/secret values. Never propagate them.
            throw new CopilotProfileException("Could not safely read or parse the " + label + ".");
        }
    }

    /** Uses the same dot-separated nested-key traversal as normal getBaseUrl configuration. */
    private static Object resolve(Map<?, ?> values, String key) {
        Object current = values;
        for (String segment : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private static String approvedOrigin(String target) {
        try {
            URI uri = new URI(target.trim()).parseServerAuthority();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.isOpaque() || host == null
                    || uri.getRawUserInfo() != null || uri.getPort() > 65535 || uri.getPort() == 0
                    || uri.getRawAuthority().endsWith(":")) {
                throw unsafeTarget();
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            if (unsafeHost(host)) {
                throw unsafeTarget();
            }
            return "https://" + host + (uri.getPort() == -1 || uri.getPort() == 443 ? "" : ":" + uri.getPort());
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw unsafeTarget();
        }
    }

    private static boolean unsafeHost(String host) {
        if (host.isEmpty() || host.length() > 253 || host.contains("*") || host.contains("%")
                || host.equals("localhost") || host.endsWith(".localhost")
                || host.equals("metadata") || host.startsWith("metadata.")
                || host.endsWith(".metadata.google.internal")) {
            return true;
        }
        if (host.startsWith("[")) {
            try {
                // Literal IPv6 only: this never performs a hostname lookup or contacts a target.
                InetAddress address = InetAddress.getByName(host.substring(1, host.length() - 1));
                return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isMulticastAddress() || unsafeIpv4(address.getHostAddress())
                        || address.getHostAddress().equalsIgnoreCase("fd00:ec2:0:0:0:0:0:254");
            } catch (IOException exception) {
                return true;
            }
        }
        if (host.matches("[0-9.]+")) {
            if (!host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                return true; // Reject alternate numeric loopback encodings.
            }
            for (String octet : host.split("\\.")) {
                if (octet.length() > 3 || (octet.length() > 1 && octet.startsWith("0"))
                        || Integer.parseInt(octet) > 255) {
                    return true;
                }
            }
            return unsafeIpv4(host);
        }
        return host.startsWith("0x") || host.equals("localhost.localdomain") || host.endsWith(".localdomain");
    }

    private static boolean unsafeIpv4(String host) {
        return host.startsWith("127.") || host.startsWith("169.254.") || host.startsWith("0.")
                || host.equals("100.100.100.200") || host.equals("192.0.0.192") || host.equals("255.255.255.255");
    }

    private static CopilotProfileException unsafeTarget() {
        return new CopilotProfileException("Selected target must resolve to a valid HTTPS origin with a safe host and port; "
                + "userinfo, loopback, localhost, metadata and wildcard origins are forbidden.");
    }

    private static Path governedOutput(Path root, CopilotDiscoveryPolicy policy) {
        if (policy == null || !MCP_PATH.toString().replace('\\', '/').equals(policy.mcpTemplateOutputPath())) {
            throw new CopilotProfileException("MCP setup may write only the governed .fnb-etaf/copilot/mcp.json path.");
        }
        return safePath(root, MCP_PATH);
    }

    private static Path validatedRoot(Path consumerRoot) {
        if (consumerRoot == null) {
            throw new CopilotProfileException("Consumer repository root is required.");
        }
        Path root = consumerRoot.toAbsolutePath().normalize();
        rejectSymlinks(root);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new CopilotProfileException("Consumer repository root must be an existing directory.");
        }
        Path ignore = safePath(root, Path.of(".gitignore"));
        try {
            if (!Files.isRegularFile(ignore, LinkOption.NOFOLLOW_LINKS)
                    || !readLocal(ignore).lines().anyMatch("/.fnb-etaf/"::equals)) {
                throw new CopilotProfileException("Add the exact line /.fnb-etaf/ to the consumer root .gitignore before MCP setup/template initialization.");
            }
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not read the consumer root .gitignore safely.");
        }
        return root;
    }

    private static Path safePath(Path root, Path relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new CopilotProfileException("MCP setup paths must remain inside the consumer root.");
        }
        rejectSymlinks(path);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new CopilotProfileException("MCP setup requires regular local files.");
        }
        return path;
    }

    private static void rejectSymlinks(Path path) {
        Path current = path.getRoot();
        for (Path segment : path) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw new CopilotProfileException("Symbolic links are not permitted in MCP setup paths or ancestors.");
            }
        }
    }

    private static String readSelectionTemplate() {
        try (InputStream input = CopilotMcpEnvironmentSetup.class.getClassLoader().getResourceAsStream(SELECTION_RESOURCE)) {
            if (input == null) {
                throw new CopilotProfileException("Embedded MCP selection defaults are unavailable.");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not read embedded MCP selection defaults.");
        }
    }

    private static String readLocal(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeAtomic(Path root, Path output, String content, boolean replace) throws IOException {
        safePath(root, root.relativize(output));
        Files.createDirectories(output.getParent());
        rejectSymlinks(output);
        Path temporary = Files.createTempFile(output.getParent(), ".fnb-etaf-mcp-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            safePath(root, root.relativize(output));
            if (!replace && Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            try {
                if (replace) {
                    Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (AtomicMoveNotSupportedException exception) {
                if (replace) {
                    Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(temporary, output);
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Result deliberately contains an alias and local path, never the resolved URL. */
    public record SetupOutput(Path mcpPath, String selectedTargetAlias, boolean changed) {
    }

    private record Selection(String environment, String alias) {
    }
}
