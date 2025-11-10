package com.ptaf.ui.action_performer;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.ptaf.ui.handlers.LocatorHandler;
import com.ptaf.ui.helpers.ElementLocatorHelper;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.ui.page_helper.PageHelper;
import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementActionImpl (Modal + Nested Iframe Resolver, Depth-3)
 *
 * What’s improved:
 * - Correctly resolves inner modal iframes (e.g. iframeWindowModal1524 within iframeWindowModal7543).
 * - Prefers exact modal names when present; otherwise picks topmost visible modal that actually contains the final chain.
 * - Recursively probes nested iframes (depth up to 3) for the element chain.
 * - Uses ATTACHED first (fast) then VISIBLE only when needed.
 * - Public API remains unchanged.
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    public ElementActionImpl(Page page) { super(page); }

    // ============================================================
    // Core locator building (frames + element chain)
    // ============================================================
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {
        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        String[] locatorParts = normalizeAndSplitChain(fullLocatorString);

        Locator currentLocator = null;

        try {
            Object context = page;

            if (frameLocator != null) {
                context = frameLocator;
            } else if (iFrame != null && !iFrame.isEmpty()) {
                FrameLocator fl = resolveFrameContext(page, iFrame, element, key);
                if (iFrame_2 != null && !iFrame_2.isEmpty()) {
                    fl = resolveFrameContext(fl, iFrame_2, element, key);
                }
                if (iFrame_3 != null && !iFrame_3.isEmpty()) {
                    fl = resolveFrameContext(fl, iFrame_3, element, key);
                }
                context = fl;
            }

            for (int i = 0; i < locatorParts.length; i++) {
                String part = locatorParts[i].trim();
                String locatorType = parseType(part);
                String locator = parseValue(part);

                if (i == 0) {
                    if (context instanceof Page) {
                        currentLocator = locatorHandler.getLocatorForType(locatorType, (Page) context, locator);
                    } else {
                        currentLocator = locatorHandler.getLocatorForType(locatorType, (FrameLocator) context, locator);
                    }
                } else {
                    currentLocator = locatorHandler.getLocatorForType(locatorType, currentLocator, locator);
                }
            }

            return currentLocator;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    // ============================================================
    // Frame resolution (Exact-name > chain-probe > legacy)
    // ============================================================

    private FrameLocator resolveFrameContext(Page page, String iframeSelector, String element, String key) {
        // First: try exact-name modal if user provided one (e.g., iframe[name="iframeWindowModal7543"])
        String exactName = extractExactModalName(iframeSelector);
        if (exactName != null) {
            FrameLocator fl = page.frameLocator("iframe[name=\"" + exactName + "\"]");
            FrameLocator deep = findDeepestFrameContainingChain(fl, element, key, 3);
            if (deep != null) return deep;
            // If exact didn’t directly contain the element, still fall back to nested probe within it
            FrameLocator nested = pickBestFrameByDeepProbe(fl, "iframe", element, key, 3);
            if (nested != null) return nested;
        }

        // Otherwise, use robust selection:
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;

        String relaxed = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = relaxed != null &&
                (relaxed.contains("iframeWindowModal") || relaxed.contains("frameborder"));

        String modalCandidateCSS = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

        // Short, non-fatal wait for likely candidates
        try {
            if (isXPathSelector(relaxed)) {
                page.waitForSelector("xpath=//iframe", new Page.WaitForSelectorOptions().setTimeout(1500));
            } else {
                page.waitForSelector(looksModal ? modalCandidateCSS : relaxed,
                        new Page.WaitForSelectorOptions().setTimeout(1500));
            }
        } catch (Throwable ignored) {}

        // 1) If an explicit index was provided, attempt that exact index first.
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(page, idxSel.base).nth(idxSel.indexZeroBased);
            // Deep probe inside this candidate too (handles inner modals)
            FrameLocator deep = findDeepestFrameContainingChain(candidate, element, key, 3);
            if (deep != null) return deep;

            logger.info("Indexed iframe {}[{}] did not expose target; auto-probing others...",
                    idxSel.base, idxSel.indexZeroBased);
        }

        // 2) Deep-probe topmost visible candidates first (by z-index) to find frame actually containing the chain
        FrameLocator best = pickBestFrameByDeepProbe(page, looksModal ? modalCandidateCSS : relaxed, element, key, 3);
        if (best != null) return best;

        // 3) Legacy fallback: return the first matching frame locator (may be wrong but better than failing early)
        return frameLocatorForSelector(page, looksModal ? modalCandidateCSS : relaxed);
    }

    private FrameLocator resolveFrameContext(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        String exactName = extractExactModalName(iframeSelector);
        if (exactName != null) {
            FrameLocator fl = parentFrame.frameLocator("iframe[name=\"" + exactName + "\"]");
            FrameLocator deep = findDeepestFrameContainingChain(fl, element, key, 3);
            if (deep != null) return deep;
            FrameLocator nested = pickBestFrameByDeepProbe(fl, "iframe", element, key, 3);
            if (nested != null) return nested;
        }

        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;

        String relaxed = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = relaxed != null &&
                (relaxed.contains("iframeWindowModal") || relaxed.contains("frameborder"));

        String modalCandidateCSS = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

        // Short, non-fatal wait for nested candidates
        try {
            if (isXPathSelector(relaxed)) {
                parentFrame.locator("xpath=//iframe").first()
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(1500));
            } else {
                parentFrame.locator(looksModal ? modalCandidateCSS : relaxed).first()
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(1500));
            }
        } catch (Throwable ignored) {}

        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(parentFrame, idxSel.base).nth(idxSel.indexZeroBased);
            FrameLocator deep = findDeepestFrameContainingChain(candidate, element, key, 3);
            if (deep != null) return deep;

            logger.info("Nested indexed iframe {}[{}] did not expose target; auto-probing others...",
                    idxSel.base, idxSel.indexZeroBased);
        }

        FrameLocator best = pickBestFrameByDeepProbe(parentFrame, looksModal ? modalCandidateCSS : relaxed, element, key, 3);
        if (best != null) return best;

        return frameLocatorForSelector(parentFrame, looksModal ? modalCandidateCSS : relaxed);
    }

    // ============================================================
    // Deep probing (handles inner modals / nested iframes)
    // ============================================================

    /** Try to find the deepest descendant frame (up to maxDepth) that contains the final chain. */
    private FrameLocator findDeepestFrameContainingChain(FrameLocator start, String element, String key, int maxDepth) {
        if (start == null) return null;

        // If the chain is already attached/visible in this frame, return this frame
        if (isFinalChainAttachedInContext(start, element, key) || isFinalChainVisibleInContext(start, element, key)) {
            // Also try to see if it’s actually inside a child iframe to be more precise
            FrameLocator child = pickBestFrameByDeepProbe(start, "iframe", element, key, maxDepth - 1);
            return (child != null) ? child : start;
        }

        // Otherwise probe children
        return pickBestFrameByDeepProbe(start, "iframe", element, key, maxDepth - 1);
    }

    /** Deep-probe helpers that sort candidates by z-index (topmost first) and prefer the one that *contains* the chain. */
    private FrameLocator pickBestFrameByDeepProbe(Page page, String candidateSelector, String element, String key, int depthLeft) {
        Locator iframes = locatorForSelector(page, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        for (Integer idx : indicesByZIndexDesc(iframes)) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = frameLocatorForSelector(page, candidateSelector).nth(idx);
            FrameLocator deep = (depthLeft > 0) ? findDeepestFrameContainingChain(fl, element, key, depthLeft) : null;
            if (deep != null) return deep;
            if (isFinalChainAttachedInContext(fl, element, key) || isFinalChainVisibleInContext(fl, element, key)) return fl;
        }
        return null;
    }

    private FrameLocator pickBestFrameByDeepProbe(FrameLocator parent, String candidateSelector, String element, String key, int depthLeft) {
        Locator iframes = locatorForSelector(parent, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        for (Integer idx : indicesByZIndexDesc(iframes)) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = frameLocatorForSelector(parent, candidateSelector).nth(idx);
            FrameLocator deep = (depthLeft > 0) ? findDeepestFrameContainingChain(fl, element, key, depthLeft) : null;
            if (deep != null) return deep;
            if (isFinalChainAttachedInContext(fl, element, key) || isFinalChainVisibleInContext(fl, element, key)) return fl;
        }
        return null;
    }

    // ============================================================
    // Visibility & scoring helpers
    // ============================================================

    /** Fast-path: ATTACHED state (element exists in DOM of that frame). */
    private boolean isFinalChainAttachedInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(600));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Slower path: VISIBLE state (useful for interactable targets). */
    private boolean isFinalChainVisibleInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(1200));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private Locator buildLocatorInContext(FrameLocator frame, String element, String key) {
        String full = elementLocatorHelper.getElement(element, key);
        String[] chain = normalizeAndSplitChain(full);

        Locator current = null;
        for (int i = 0; i < chain.length; i++) {
            String part = chain[i].trim();
            String type = parseType(part);
            String value = parseValue(part);
            if (i == 0) {
                current = locatorHandler.getLocatorForType(type, frame, value);
            } else {
                current = locatorHandler.getLocatorForType(type, current, value);
            }
        }
        return current;
    }

    private boolean safeIsVisible(Locator loc) {
        try { return loc.isVisible(); } catch (Throwable t) { return false; }
    }

    private int safeZIndex(Locator iframeEl) {
        try {
            Object val = iframeEl.evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
            return (val instanceof Number) ? ((Number) val).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private List<Integer> indicesByZIndexDesc(Locator iframes) {
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);
        order.sort((a, b) -> Integer.compare(safeZIndex(iframes.nth(b)), safeZIndex(iframes.nth(a))));
        return order;
    }

    // ============================================================
    // Selector parsing & normalization (CSS + XPath)
    // ============================================================

    /** Accept both " > " and "&gt;"; normalize to '>' and split. */
    private String[] normalizeAndSplitChain(String raw) {
        if (raw == null) return new String[0];
        String normalized = raw.replace("&gt;", ">");
        return normalized.split("\\s*>\\s*");
    }

    /** Extract TYPE from "TYPE_value" or return token itself if no underscore. */
    private String parseType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(0, idx) : token).trim();
    }

    /** Extract value from "TYPE_value"; returns "" if no underscore (unnamed role). */
    private String parseValue(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(idx + 1) : "").trim();
    }

    /**
     * Parse indexed iframe selector.
     * Supports:
     *   (iframe[...])[2]                    // CSS-like
     *   (//iframe[@attr='v'])[2]            // XPath
     */
    private IndexedSelector parseIndexedIframeSelector(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // XPath form: (//iframe[@...])[2]
        Pattern xPathIndexed = Pattern.compile("^\\(\\s*(//iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mx = xPathIndexed.matcher(s);
        if (mx.find()) {
            String base = mx.group(1);
            int oneBased = Integer.parseInt(mx.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        // CSS-like form: (iframe[...])[2]
        Pattern cssIndexed = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mc = cssIndexed.matcher(s);
        if (mc.find()) {
            String base = mc.group(1);
            int oneBased = Integer.parseInt(mc.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        return null;
    }

    /** If strict modal like iframe[name="iframeWindowModal1857"] → return exact name; else null. */
    private String extractExactModalName(String sel) {
        if (sel == null) return null;
        Matcher m = Pattern.compile("iframe\\[name\\s*=\\s*\"(iframeWindowModal\\d+)\"\\]").matcher(sel.trim());
        if (m.find()) return m.group(1);
        return null;
    }

    /** Relax strict modal name to starts-with if needed. */
    private String relaxModalSelectorIfNeeded(String sel) {
        if (sel == null) return null;
        String s = sel.trim();

        // CSS strict → relax
        if (s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }

        return s;
    }

    private static final class IndexedSelector {
        final String base;
        final int indexZeroBased;
        IndexedSelector(String base, int indexZeroBased) {
            this.base = base;
            this.indexZeroBased = indexZeroBased;
        }
    }

    // ============================================================
    // Engine-aware helpers (CSS vs XPath)
    // ============================================================

    private boolean isXPathSelector(String sel) {
        if (sel == null) return false;
        String s = sel.trim();
        // If it starts with '//' it's XPath; parentheses alone can be CSS indexed, so check more strictly
        return s.startsWith("//") || s.startsWith("(//");
    }

    private Locator locatorForSelector(Page page, String sel) {
        return isXPathSelector(sel) ? page.locator("xpath=" + sel) : page.locator(sel);
    }

    private Locator locatorForSelector(FrameLocator frame, String sel) {
        return isXPathSelector(sel) ? frame.locator("xpath=" + sel) : frame.locator(sel);
    }

    private FrameLocator frameLocatorForSelector(Page page, String sel) {
        return isXPathSelector(sel) ? page.frameLocator("xpath=" + sel) : page.frameLocator(sel);
    }

    private FrameLocator frameLocatorForSelector(FrameLocator frame, String sel) {
        return isXPathSelector(sel) ? frame.frameLocator("xpath=" + sel) : frame.frameLocator(sel);
    }

    // ============================================================
    // Action execution wrappers (public API unchanged)
    // ============================================================

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // small non-fatal wait in case a modal is animating in
            if (page != null) {
                try {
                    page.waitForSelector("iframe[name^='iframeWindowModal'], iframe[frameborder='0px']",
                            new Page.WaitForSelectorOptions().setTimeout(500));
                } catch (Throwable ignored) {}
            }

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
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return performAction(page, null, null, null, action, element, key, value, null);
    }

    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performAction(null, null, null, null, action, element, key, value, frameLocator);
    }

    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                          String action, String element, String key, String value, FrameLocator frameLocator) {
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
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                                   String action, String element, String key, String value, FrameLocator frameLocator) {
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

    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        FileChooser fileChooser = page.waitForFileChooser(() -> page.click(getElement(element, key)));
        fileChooser.setFiles(Paths.get(getElement(element, file_name)));
    }

    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = getElement(element, key);
        String fileName = extractFileName(documentLinkName);
        System.out.println(fileName);
        try {
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(fileName)).click();
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

    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        return parseValue(locatorValue);
    }
}