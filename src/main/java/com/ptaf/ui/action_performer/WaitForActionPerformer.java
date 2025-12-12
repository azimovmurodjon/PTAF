package com.ptaf.ui.action_performer;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitForActionPerformer {

    private static final Logger logger = LoggerFactory.getLogger(WaitForActionPerformer.class);

    public void performAction(Page page, String action, Locator targetLocator, String value) {
        try {
            switch (action.toLowerCase()) {
                case "file_chooser":
                    // Wait for a file chooser to be displayed
                    page.waitForFileChooser(() -> click(targetLocator)); // Assuming value is not needed here.
                    break;
                case "wait_for_popup":
                        page.waitForPopup(() -> click(targetLocator));
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) {
            // Logs the error and throws a runtime exception
            logger.error("Error while performing action: {}", e.getMessage());
            throw new RuntimeException("Action failed: " + e.getMessage(), e);
        }
    }

    private void click(Locator targetLocator) {
        try {
            targetLocator.click();
        } catch (Exception e) {
            logger.error("Error while clicking on target locator: {}", e.getMessage());
            throw new RuntimeException("Click action failed: " + e.getMessage(), e);
        }
    }
}