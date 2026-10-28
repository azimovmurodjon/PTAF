package com.ptaf.copilot;

/**
 * Raised when the centrally managed Copilot profile is malformed, unsafe, or incomplete.
 */
public class CopilotProfileException extends RuntimeException {

    public CopilotProfileException(String message) {
        super(message);
    }

    public CopilotProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}
