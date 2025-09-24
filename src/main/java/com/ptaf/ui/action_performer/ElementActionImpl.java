package com.ptaf.ui.action_performer;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.ptaf.ui.handlers.LocatorHandler;
import com.ptaf.ui.helpers.ElementLocatorHelper;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.ui.page_helper.PageHelper;
import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ElementActionImpl is an implementation of the ElementAction interface that provides methods for
 * performing actions and assertions on web elements within an instance of a Playwright Page or FrameLocator.
 * It utilizes the ActionPerformer, LocatorHandler, and ElementLocatorHelper to manage interactions
 * with various elements on a web page.
 *
 * This version adds robust handling for dynamic nested frames (e.g., popup-in-popup with identical
 * iframe selectors like iframe[frameborder='0px'] where '0px' can change to 1px/2px and multiple
 * siblings may exist simultaneously). It keeps existing behavior when a single frame exists and only
 * applies fallback logic when needed.
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer(); // Handles action execution on Locators
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper(); // Assists in locating elements
    private final LocatorHandler locatorHandler = new LocatorHandler(); // Manages Locator creation based on type

    // ---------------------- Dynamic frame helpers (BACKWARD-COMPATIBLE) ----------------------

    // Configurable max index for probing multiple identical sibling frames.
    // Default 3. Override at runtime with: -Dptaf.maxFrameIndex=5
    private static final int MAX_DYNAMIC_FRAME_INDEX = Integer.getInteger("ptaf.maxFrameIndex", 3);

    /**
     * If the frame selector is brittle like iframe[frameborder='0px'], make it robust by
     * matching any frameborder value that ends with 'px' (0px, 1px, 2px, ...).
     */
    private String relaxFrameSelector(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        // Match patterns like iframe[frameborder='0px'] or iframe[frameBorder="2px"]
        Pattern p = Pattern.compile("(?i)(\\biframe\\b[^\\[]*)\\[(frameborder)\\s*=\\s*['\"]?\\d+px['\"]?\\]");
        Matcher m = p.matcher(raw);
        if (m.find()) {
            String before = m.group(1);     // e.g., "iframe"
            String attr   = m.group(2);     // frameborder
            // First fallback: ends-with px for that attribute
            return before + "[" + attr + "$='px']";
        }
        return raw; // unchanged if no exact Npx pattern
    }

    /**
     * If relaxing to $='px' still doesn't resolve, fall back to attribute presence [frameborder].
     */
    private String presenceFallbackSelector(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String low = raw.toLowerCase();
        if (low.contains("frameborder$='px'")) {
            // Replace ends-with with plain presence
            return raw.replaceAll("(?i)frameborder\\$='px'", "frameborder");
        }
        if (low.contains("frameborder=")) {
            // Replace exact match with plain presence
            return raw.replaceAll("(?i)\\[(frameborder)\\s*=\\s*[^\\]]+\\]", "[frameborder]");
        }
        return raw;
    }

    /**
     * Build a locator chain inside the given FrameLocator context and return a non-empty locator if found.
     */
    private Locator buildAndCheck(FrameLocator ctx, String[] locatorParts) {
        Locator current = null;
        for (int i = 0; i < locatorParts.length; i++) {
            String part = locatorParts[i].trim();
            String locatorType = elementLocatorHelper.getLocatorType(part);
            String locator = elementLocatorHelper.getLocator(part);
            if (i == 0) {
                current = locatorHandler.getLocatorForType(locatorType, ctx, locator);
            } else {
                current = locatorHandler.getLocatorForType(locatorType, current, locator);
            }
        }
        return (current != null && current.count() > 0) ? current : null;
    }

    /**
     * Build a locator chain inside the given Page context and return a non-empty locator if found.
     * (Used when there is no frame context.)
     */
    private Locator buildAndCheck(Page ctx, String[] locatorParts) {
        Locator current = null;
        for (int i = 0; i < locatorParts.length; i++) {
            String part = locatorParts[i].trim();
            String locatorType = elementLocatorHelper.getLocatorType(part);
            String locator = elementLocatorHelper.getLocator(part);
            if (i == 0) {
                current = locatorHandler.getLocatorForType(locatorType, ctx, locator);
            } else {
                current = locatorHandler.getLocatorForType(locatorType, current, locator);
            }
        }
        return (current != null && current.count() > 0) ? current : null;
    }

    // ----------------------------------------------------------------------------------------

    /**
     * Constructor for ElementActionImpl. Inheritance from PageHelper allows
     * initializing with a Page instance for element interaction.
     *
     * @param page The Playwright Page instance to interact with web elements.
     *             This page will be used to perform various actions and assertions on web elements.
     */
    public ElementActionImpl(Page page) {
        super(page); // Initialize the PageHelper with the provided Page
    }

    /**
     * Retrieves a Locator for the specified element by determining its type and context.
     * This method is the central orchestrator for locator chaining.
     * It parses locator strings with " > " to build complex, chained locators.
     *
     * Enhancements:
     * - Keeps existing behavior and signatures.
     * - If straight resolution fails, applies relaxed selector for frameborder and nth() probing
     *   across sibling frames (0..MAX_DYNAMIC_FRAME_INDEX).
     *
     * @param iFrame      top-level frame selector (can be null/empty)
     * @param iFrame_2    second-level nested frame selector (can be null/empty)
     * @param iFrame_3    third-level nested frame selector (can be null/empty)
     * @param element     YAML element group
     * @param key         YAML key within the element group
     * @param page        Page context (used when frameLocator is null)
     * @param frameLocator Optional explicit FrameLocator context (takes precedence if provided)
     * @return The final Locator object after resolving the entire chain.
     * @throws RuntimeException If the locator cannot be resolved.
     */
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {
        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        // Split the string by " > " to get the parts of the chain.
        String[] locatorParts = fullLocatorString.split("\\s*>\\s*");

        try {
            // 1) Build initial context (respect explicit frameLocator first)
            if (frameLocator != null) {
                // Build chain in the explicit frameLocator context
                Locator candidate = buildAndCheck(frameLocator, locatorParts);
                if (candidate != null) return candidate;
                throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "' using provided FrameLocator.");
            }

            // If no explicit FrameLocator is given, try page or page->frames chain
            if (iFrame == null || iFrame.isEmpty()) {
                // No frames — build directly on page
                Locator candidate = buildAndCheck(page, locatorParts);
                if (candidate != null) return candidate;
                throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "' (page context).");
            }

            // 2) Try original frame chain -> element chain
            FrameLocator chain = page.frameLocator(iFrame);
            if (iFrame_2 != null && !iFrame_2.isEmpty()) chain = chain.frameLocator(iFrame_2);
            if (iFrame_3 != null && !iFrame_3.isEmpty()) chain = chain.frameLocator(iFrame_3);

            Locator firstAttempt = buildAndCheck(chain, locatorParts);
            if (firstAttempt != null) return firstAttempt;

            // 3) Relax frame selectors (frameborder='0px' -> frameborder$='px')
            String relaxed1 = relaxFrameSelector(iFrame);
            String relaxed2 = (iFrame_2 != null && !iFrame_2.isEmpty()) ? relaxFrameSelector(iFrame_2) : null;
            String relaxed3 = (iFrame_3 != null && !iFrame_3.isEmpty()) ? relaxFrameSelector(iFrame_3) : null;

            FrameLocator relaxedChain = page.frameLocator(relaxed1 != null ? relaxed1 : iFrame);
            if (relaxed2 != null) relaxedChain = relaxedChain.frameLocator(relaxed2);
            else if (iFrame_2 != null && !iFrame_2.isEmpty()) relaxedChain = relaxedChain.frameLocator(iFrame_2);
            if (relaxed3 != null) relaxedChain = relaxedChain.frameLocator(relaxed3);
            else if (iFrame_3 != null && !iFrame_3.isEmpty()) relaxedChain = relaxedChain.frameLocator(iFrame_3);

            Locator relaxedAttempt = buildAndCheck(relaxedChain, locatorParts);
            if (relaxedAttempt != null) return relaxedAttempt;

            // 4) Presence fallback for frameborder + nth() probing (siblings)
            String presence1 = presenceFallbackSelector(relaxed1 != null ? relaxed1 : iFrame);
            String presence2 = (relaxed2 != null)
                    ? presenceFallbackSelector(relaxed2)
                    : (iFrame_2 != null ? presenceFallbackSelector(iFrame_2) : null);
            String presence3 = (relaxed3 != null)
                    ? presenceFallbackSelector(relaxed3)
                    : (iFrame_3 != null ? presenceFallbackSelector(iFrame_3) : null);

            // Probe across siblings for each provided frame level
            if (presence1 != null) {
                for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                    FrameLocator fl1 = page.frameLocator(presence1).nth(i);

                    if (presence2 == null && presence3 == null) {
                        Locator candidate = buildAndCheck(fl1, locatorParts);
                        if (candidate != null) {
                            logger.info("Resolved via dynamic frame probing at level1 nth({}).", i);
                            return candidate;
                        }
                    } else if (presence2 != null && presence3 == null) {
                        for (int j = 0; j <= MAX_DYNAMIC_FRAME_INDEX; j++) {
                            FrameLocator fl2 = fl1.frameLocator(presence2).nth(j);
                            Locator candidate = buildAndCheck(fl2, locatorParts);
                            if (candidate != null) {
                                logger.info("Resolved via dynamic frame probing at level2 nth({},{})", i, j);
                                return candidate;
                            }
                        }
                    } else if (presence2 != null /* && presence3 != null */) {
                        for (int j = 0; j <= MAX_DYNAMIC_FRAME_INDEX; j++) {
                            FrameLocator fl2 = fl1.frameLocator(presence2).nth(j);
                            for (int k = 0; k <= MAX_DYNAMIC_FRAME_INDEX; k++) {
                                FrameLocator fl3 = fl2.frameLocator(presence3).nth(k);
                                Locator candidate = buildAndCheck(fl3, locatorParts);
                                if (candidate != null) {
                                    logger.info("Resolved via dynamic frame probing at level3 nth({},{},{})", i, j, k);
                                    return candidate;
                                }
                            }
                        }
                    }
                }
            }

            // If we reach here, nothing matched
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "' after dynamic probing.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    // ------------------------- Existing public API below (UNCHANGED) -------------------------

    @Override
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return performAction(page, null, null, null, action, element, key, value, null);
    }

    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performAction(null, null, null, null, action, element, key, value, frameLocator);
    }

    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        return performAction(page, iFrame, iFrame_2, iFrame_3, action, element, key, value, null);
    }

    @Override
    public boolean getElementHandlePage(Page page, String element, String key) {
        List<ElementHandle> elementHandles = getElementHandleList(page, element, key, null);
        return !elementHandles.isEmpty();
    }

    @Override
    public boolean getElementHandleFrame(FrameLocator frameLocator, String element, String key) {
        List<ElementHandle> elementHandles = getElementHandleList(null, element, key, frameLocator);
        return !elementHandles.isEmpty();
    }

    @Override
    public boolean assertElementTextPage(Page page, String element, String key, String expectedText) {
        return assertElementText(page, element, key, expectedText, null);
    }

    @Override
    public boolean assertElementTextFrame(FrameLocator frameLocator, String element, String key, String expectedText) {
        return assertElementText(null, element, key, expectedText, frameLocator);
    }

    @Override
    public String performActionPageWithReturn(Page page, String action, String element, String key, String value) {
        try {
            Locator targetLocator = getLocatorBasedOnPage(page, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for element: {} with key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageWithReturn for element '{}' and action '{}':", element, action, e);
            return null;
        }
    }

    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for nested frame element: {} with key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageFrameWithReturn for element '{}' and action '{}':", element, action, e);
            return null;
        }
    }

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // Simplified context checking
            if (frameLocator != null) {
                targetLocator = getLocator(null, null, null, element, key, null, frameLocator);
            } else if (page != null) {
                targetLocator = getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
            } else {
                throw new IllegalArgumentException("A Page or FrameLocator context is required.");
            }

            if (targetLocator == null) {
                throw new IllegalStateException("Failed to resolve a target Locator for element: " + element + " with key: " + key);
            }

            actionPerformer.waitForLocator(targetLocator);
            actionPerformer.performAction(page, action, targetLocator, value);
            return true;
        } catch (Exception e) {
            logger.error("Error while performing action '{}' on element '{}' with key '{}'", action, element, key, e);
        }

        return false;
    }

    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        FileChooser fileChooser = page.waitForFileChooser(() ->
                page.click(getElement(element, key)));
        fileChooser.setFiles(Paths.get(getElement(element, file_name)));
    }

    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = getElement(element, key);
        String fileName = extractFileName(documentLinkName);
        System.out.println(fileName);
        try {
            page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(fileName)).click();
        } catch (Exception e) {
            logger.error("Failed to click on element by Role '{}'", element + key, e);
        }
    }

    public static String extractFileName(String filePath) {
        String[] parts = filePath.split("/");
        return parts[parts.length - 1];
    }

    public String getElement(String element, String key) {
        try {
            return (String) YamlReader.get("elements." + element + "." + key);
        } catch (Exception e) {
            logger.error("Failed to retrieve selector for element '{}'", element + key, e);
            throw e;
        }
    }

    private boolean assertElementText(Page page, String element, String key, String expectedText, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            String actualText = targetLocator.first().textContent();
            boolean isTextMatching = expectedText.equals(actualText);
            logger.info("Asserting text on element '{}': expected '{}', actual '{}'", element, expectedText, actualText);
            if (!isTextMatching) {
                logger.error("Text mismatch: expected '{}' but found '{}'", expectedText, actualText);
            }
            return isTextMatching;
        } catch (Exception e) {
            logger.error("Error while asserting text on element '{}'", element, e);
            return false;
        }
    }

    @Override
    public List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator) {
        List<ElementHandle> elementHandles = new ArrayList<>();
        try {
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            if (targetLocator != null) {
                elementHandles = targetLocator.elementHandles();
            } else {
                logger.error("Target locator for element '{}' could not be determined.", element);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve element handles for '{}'", element, e);
        }
        return elementHandles;
    }

    private Locator getLocatorBasedOnPage(Page page, String element, String key) {
        return getLocator(null, null, null, element, key, page, null);
    }

    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String key) {
        return getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
    }

    @SuppressWarnings("unused")
    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        return elementLocatorHelper.getLocator(locatorValue);
    }
}