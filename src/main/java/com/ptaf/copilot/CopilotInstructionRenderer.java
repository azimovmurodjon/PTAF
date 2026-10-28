package com.ptaf.copilot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts the one central FNB-ETAF profile into deterministic GitHub Copilot Markdown files.
 */
public final class CopilotInstructionRenderer {

    /**
     * Renders all allow-listed instruction outputs without writing to disk.
     *
     * @param profile validated central profile
     * @return stable ordered list of generated targets
     */
    public List<CopilotInstructionTarget> render(CopilotProfile profile) {
        CopilotProfileValidator.validate(profile);

        String marker = generatedMarker(profile.version());
        List<CopilotInstructionTarget> targets = new ArrayList<>();
        targets.add(new CopilotInstructionTarget(
                CopilotProfileValidator.REPOSITORY_INSTRUCTION_PATH,
                marker + "\n\n" + normalize(profile.repositoryInstruction())));

        for (Map.Entry<String, String> entry : profile.pathInstructions().entrySet()) {
            String content = normalize(entry.getValue());
            int frontMatterEnd = content.indexOf("\n---\n");
            String withMarker = content.substring(0, frontMatterEnd + 5)
                    + "\n" + marker + "\n\n"
                    + content.substring(frontMatterEnd + 5).stripLeading();
            targets.add(new CopilotInstructionTarget(entry.getKey(), withMarker));
        }
        return List.copyOf(targets);
    }

    /**
     * Returns the marker used by verification to identify FNB-ETAF-generated instruction files.
     *
     * @param version central profile version
     * @return generated-file comment
     */
    public static String generatedMarker(String version) {
        return CopilotProfileValidator.GENERATED_MARKER + " v" + version
                + ". Update src/main/resources/copilot/fnb-etaf-copilot-profile.yml; do not edit this generated file. -->";
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n').strip() + "\n";
    }
}
