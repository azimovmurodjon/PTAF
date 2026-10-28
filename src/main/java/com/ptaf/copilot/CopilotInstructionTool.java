package com.ptaf.copilot;

import java.nio.file.Path;

/**
 * Explicit command-line entry point for generating or verifying FNB-ETAF Copilot instructions.
 *
 * <p>Usage:</p>
 * <pre>
 * java ... com.ptaf.copilot.CopilotInstructionTool sync --root /path/to/consumer-repository
 * java ... com.ptaf.copilot.CopilotInstructionTool verify --root /path/to/consumer-repository
 * </pre>
 *
 * <p>The tool does not contact GitHub, authenticate to Copilot, execute tests, or change FNB-ETAF runtime behavior.</p>
 */
public final class CopilotInstructionTool {
    private CopilotInstructionTool() {
        // Command-line utility class.
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Executes the requested operation without calling {@link System#exit(int)}. This enables direct contract testing.
     *
     * @param args command-line arguments
     * @return zero on success, two for invalid usage, or one for a validation/IO failure
     */
    public static int run(String... args) {
        try {
            Command command = Command.parse(args);
            CopilotProfile profile = CopilotProfileLoader.loadEmbeddedProfile();
            CopilotInstructionExporter exporter = new CopilotInstructionExporter();
            if (command.operation().equals("sync")) {
                exporter.sync(profile, command.root());
                System.out.println("FNB-ETAF Copilot instructions synchronized under " + command.root());
            } else {
                exporter.verify(profile, command.root());
                System.out.println("FNB-ETAF Copilot instructions are synchronized under " + command.root());
            }
            return 0;
        } catch (IllegalArgumentException exception) {
            System.err.println("FNB-ETAF Copilot instruction usage error: " + exception.getMessage());
            printUsage();
            return 2;
        } catch (Exception exception) {
            System.err.println("FNB-ETAF Copilot instruction operation failed: " + exception.getMessage());
            return 1;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: CopilotInstructionTool <sync|verify> --root <consumer-repository-root>");
    }

    private record Command(String operation, Path root) {
        private static Command parse(String... args) {
            if (args == null || args.length != 3 || !"--root".equals(args[1])) {
                throw new IllegalArgumentException("Expected an operation and --root <directory>.");
            }
            if (!"sync".equals(args[0]) && !"verify".equals(args[0])) {
                throw new IllegalArgumentException("Operation must be either 'sync' or 'verify'.");
            }
            if (args[2] == null || args[2].isBlank()) {
                throw new IllegalArgumentException("Consumer repository root must not be blank.");
            }
            return new Command(args[0], Path.of(args[2]));
        }
    }
}
