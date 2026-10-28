package com.ptaf.copilot;

import java.nio.file.Path;

/**
 * Explicit command-line entry point for optional local Playwright MCP configuration and discovery drafts.
 *
 * <p>Usage:</p>
 * <pre>
 * CopilotDiscoveryTool mcp-template --root /consumer/root
 * CopilotDiscoveryTool validate --input .fnb-etaf/discovery-inputs/request.yml
 * CopilotDiscoveryTool draft --input .fnb-etaf/discovery-inputs/request.yml --root /consumer/root
 * </pre>
 *
 * <p>This tool does not start Playwright MCP, navigate to a URL, open a browser, inspect page data,
 * authenticate, execute tests, or write active FNB-ETAF resources. During {@code draft}, it only
 * verifies in memory that a named standard configuration alias exists and is HTTP(S); it never
 * prints, returns, or writes the resolved address.</p>
 */
public final class CopilotDiscoveryTool {
    private CopilotDiscoveryTool() {
        // Command-line utility class.
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Executes a local operation without calling {@link System#exit(int)} to enable contract tests. */
    public static int run(String... args) {
        try {
            Command command = Command.parse(args);
            CopilotDiscoveryPolicy policy = CopilotProfileLoader.loadEmbeddedProfile().discoveryPolicy();
            if (policy == null) {
                throw new CopilotProfileException("The embedded FNB-ETAF Copilot profile does not define a discovery policy.");
            }
            CopilotDiscoveryDraftExporter exporter = new CopilotDiscoveryDraftExporter();
            switch (command.operation()) {
                case "mcp-template" -> {
                    Path output = exporter.writeMcpTemplate(policy, command.root());
                    System.out.println("FNB-ETAF local Playwright MCP template written to " + output);
                    System.out.println("It is not enabled. Review it and replace only the approved-origin placeholder in IntelliJ.");
                }
                case "validate" -> {
                    CopilotDiscoveryRequest request = CopilotDiscoveryRequestLoader.load(command.input());
                    CopilotDiscoveryValidator.validate(request, policy);
                    System.out.println("FNB-ETAF discovery candidate file is valid. No files were written.");
                }
                case "draft" -> {
                    CopilotDiscoveryRequest request = CopilotDiscoveryRequestLoader.load(command.input());
                    CopilotDiscoveryDraftExporter.CopilotDiscoveryDraftOutput output =
                            exporter.writeDraft(request, policy, command.root());
                    System.out.println("FNB-ETAF review-only discovery drafts written to " + output.directory());
                    System.out.println("Active test resources were not modified. Review and promote entries manually.");
                }
                default -> throw new IllegalArgumentException("Unsupported operation: " + command.operation());
            }
            return 0;
        } catch (IllegalArgumentException exception) {
            System.err.println("FNB-ETAF Copilot discovery usage error: " + exception.getMessage());
            printUsage();
            return 2;
        } catch (Exception exception) {
            System.err.println("FNB-ETAF Copilot discovery operation failed: " + exception.getMessage());
            return 1;
        }
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  CopilotDiscoveryTool mcp-template --root <consumer-repository-root>");
        System.err.println("  CopilotDiscoveryTool validate --input <candidate.yml>");
        System.err.println("  CopilotDiscoveryTool draft --input <candidate.yml> --root <consumer-repository-root>");
    }

    private record Command(String operation, Path input, Path root) {
        private static Command parse(String... args) {
            if (args == null || args.length < 3) {
                throw new IllegalArgumentException("An operation and required arguments are needed.");
            }
            if ("mcp-template".equals(args[0]) && args.length == 3 && "--root".equals(args[1])) {
                return new Command("mcp-template", null, path(args[2], "Consumer repository root"));
            }
            if ("validate".equals(args[0]) && args.length == 3 && "--input".equals(args[1])) {
                return new Command("validate", path(args[2], "Candidate input"), null);
            }
            if ("draft".equals(args[0]) && args.length == 5 && "--input".equals(args[1]) && "--root".equals(args[3])) {
                return new Command("draft", path(args[2], "Candidate input"), path(args[4], "Consumer repository root"));
            }
            throw new IllegalArgumentException("Arguments do not match a supported operation.");
        }

        private static Path path(String value, String label) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(label + " must not be blank.");
            }
            return Path.of(value);
        }
    }
}
