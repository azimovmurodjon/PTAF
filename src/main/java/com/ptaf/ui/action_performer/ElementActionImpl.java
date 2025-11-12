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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementActionImpl (Topmost-Modal + Nested-Probing + XPath index support)
 *
 * Adds/keeps:
 * - Topmost visible modal preference: iframe[name^="iframeWindowModal"] is auto-picked by highest z-index when no index given.
 * - Works with both CSS and XPath iframe selectors, including indexed forms:
 *     • CSS:  (iframe[frameborder='0px'])[2]
 *     • XPath: (//iframe[@frameborder='0px'])[2]
 * - Nested probing: for parent modal (e.g., 7543) -> child modal (e.g., 1524), we probe *within the parent frame*
 *   and select the child that actually exposes the full final chain, so duplicates in the parent do not confuse selection.
 * - Fully backward compatible with your YAML chains and LocatorHandler (buttons, placeholders, text, role, css/xpath, etc.).
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private static final String MODAL_NAME_PREFIX_CSS = "iframe[name^='iframeWindowModal']";
    private static final String MODAL_OR_FRAMEBORDER_CSS = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

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
                FrameLocator fl = findFrameWithElement(page, iFrame, element, key);
                if (iFrame_2 != null && !iFrame_2.isEmpty()) {
                    fl = findFrameWithElement(fl, iFrame_2, element, key);
                }
                if (iFrame_3 != null && !iFrame_3.isEmpty()) {
                    fl = findFrameWithElement(fl, iFrame_3, element, key);
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
    // Frame resolution (Index-aware + Modal-aware + Topmost preference)
    // ============================================================

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;

        String selNormalized = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = isModalish(selNormalized);

        // Short, non-fatal wait
        try {
            if (isXPathSelector(selNormalized)) {
                page.waitForSelector("xpath=//iframe", new Page.WaitForSelectorOptions().setTimeout(1500));
            } else {
                page.waitForSelector(looksModal ? MODAL_OR_FRAMEBORDER_CSS : selNormalized,
                        new Page.WaitForSelectorOptions().setTimeout(1500));
            }
        } catch (Throwable ignored) {}

        // 0) Fast path: if caller gave a generic modal selector or a strict modal name without index -> pick TOPMOST first
        if (idxSel == null && looksModal) {
            FrameLocator top = pickTopmostVisibleFrame(page, MODAL_NAME_PREFIX_CSS);
            if (top != null && isFinalChainVisibleInContext(top, element, key)) {
                logger.info("Topmost visible modal selected (page): {}", MODAL_NAME_PREFIX_CSS);
                return top;
            }
        }

        // 1) Try explicit index first
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(page, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                logger.info("Resolved indexed iframe {}[{}] for element '{}':'{}'",
                        idxSel.base, idxSel.indexZeroBased, element, key);
                return candidate;
            } else {
                logger.info("Indexed iframe {}[{}] did not expose target; auto-probing others...", idxSel.base, idxSel.indexZeroBased);
            }
        }

        // 2) Auto-probe by full chain (modal set vs provided selector)
        if (looksModal) {
            FrameLocator best = pickBestFrameByChainProbe(page, MODAL_OR_FRAMEBORDER_CSS, element, key);
            if (best != null) return best;
        } else {
            FrameLocator best = pickBestFrameByChainProbe(page, selNormalized, element, key);
            if (best != null) return best;
        }

        // 3) Legacy fallback linear scan
        Locator iframeLocator = locatorForSelector(page, selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; i++) {
            FrameLocator fl = frameLocatorForSelector(page, selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }

        // 4) Final fallback
        return frameLocatorForSelector(page, selNormalized);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;

        String selNormalized = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = isModalish(selNormalized);

        // Short, non-fatal wait (within the parent frame)
        try {
            if (isXPathSelector(selNormalized)) {
                parentFrame.locator("xpath=//iframe").first()
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(1500));
            } else {
                parentFrame.locator(looksModal ? MODAL_OR_FRAMEBORDER_CSS : selNormalized).first()
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(1500));
            }
        } catch (Throwable ignored) {}

        // 0) Fast path inside parent: pick TOPMOST child modal first if no index given
        if (idxSel == null && looksModal) {
            FrameLocator top = pickTopmostVisibleFrame(parentFrame, MODAL_NAME_PREFIX_CSS);
            if (top != null && isFinalChainVisibleInContext(top, element, key)) {
                logger.info("Topmost visible modal selected (nested): {}", MODAL_NAME_PREFIX_CSS);
                return top;
            }
        }

        // 1) Try explicit index first
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(parentFrame, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                logger.info("Resolved nested indexed iframe {}[{}] for element '{}':'{}'",
                        idxSel.base, idxSel.indexZeroBased, element, key);
                return candidate;
            } else {
                logger.info("Nested indexed iframe {}[{}] did not expose target; auto-probing others...", idxSel.base, idxSel.indexZeroBased);
            }
        }

        // 2) Auto-probe within parent
        if (looksModal) {
            FrameLocator best = pickBestFrameByChainProbe(parentFrame, MODAL_OR_FRAMEBORDER_CSS, element, key);
            if (best != null) return best;
        } else {
            FrameLocator best = pickBestFrameByChainProbe(parentFrame, selNormalized, element, key);
            if (best != null) return best;
        }

        // 3) Legacy fallback linear scan
        Locator iframeLocator = locatorForSelector(parentFrame, selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; i++) {
            FrameLocator fl = frameLocatorForSelector(parentFrame, selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }

        // 4) Final fallback
        return frameLocatorForSelector(parentFrame, selNormalized);
    }

    private boolean isModalish(String selNormalized) {
        return selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));
    }

    // ============================================================
    // Topmost selection & chain-probing (z-index aware)
    // ============================================================

    private FrameLocator pickTopmostVisibleFrame(Page page, String selectorCss) {
        Locator iframes = page.locator(selectorCss);
        int count = iframes.count();
        if (count == 0) return null;
        List<Integer> order = indicesByZIndexDesc(iframes);
        for (Integer idx : order) {
            if (safeIsVisible(iframes.nth(idx))) {
                return page.frameLocator(selectorCss).nth(idx);
            }
        }
        return null;
    }

    private FrameLocator pickTopmostVisibleFrame(FrameLocator parentFrame, String selectorCss) {
        Locator iframes = parentFrame.locator(selectorCss);
        int count = iframes.count();
        if (count == 0) return null;
        List<Integer> order = indicesByZIndexDesc(iframes);
        for (Integer idx : order) {
            if (safeIsVisible(iframes.nth(idx))) {
                return parentFrame.frameLocator(selectorCss).nth(idx);
            }
        }
        return null;
    }

    private FrameLocator pickBestFrameByChainProbe(Page page, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(page, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        List<Integer> order = indicesByZIndexDesc(iframes);
        for (Integer idx : order) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = frameLocatorForSelector(page, candidateSelector).nth(idx);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
        }

        int bestIdx = -1;
        int bestCount = -1;
        int bestZ = Integer.MIN_VALUE;

        for (Integer idx : order) {
            FrameLocator fl = frameLocatorForSelector(page, candidateSelector).nth(idx);
            int countMatches = countFinalChainMatches(fl, element, key);
            int z = safeZIndex(iframes.nth(idx));

            if (countMatches > bestCount || (countMatches == bestCount && z > bestZ)) {
                bestCount = countMatches;
                bestZ = z;
                bestIdx = idx;
            }
        }
        if (bestIdx >= 0) return frameLocatorForSelector(page, candidateSelector).nth(bestIdx);
        return null;
    }

    private FrameLocator pickBestFrameByChainProbe(FrameLocator parentFrame, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(parentFrame, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        List<Integer> order = indicesByZIndexDesc(iframes);
        for (Integer idx : order) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
        }

        int bestIdx = -1;
        int bestCount = -1;
        int bestZ = Integer.MIN_VALUE;

        for (Integer idx : order) {
            FrameLocator fl = frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
            int countMatches = countFinalChainMatches(fl, element, key);
            int z = safeZIndex(iframes.nth(idx));

            if (countMatches > bestCount || (countMatches == bestCount && z > bestZ)) {
                bestCount = countMatches;
                bestZ = z;
                bestIdx = idx;
            }
        }
        if (bestIdx >= 0) return frameLocatorForSelector(parentFrame, candidateSelector).nth(bestIdx);
        return null;
    }

    // ============================================================
    // Visibility & scoring helpers
    // ============================================================

    private boolean isFinalChainVisibleInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(1500));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private int countFinalChainMatches(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            return finalLocator.count();
        } catch (Throwable t) {
            return 0;
        }
    }

    private boolean isFirstTokenVisibleInContext(FrameLocator frame, String element, String key) {
        String[] parts = normalizeAndSplitChain(elementLocatorHelper.getElement(element, key));
        String first = parts[0].trim();
        String t = parseType(first);
        String v = parseValue(first);
        Locator test = locatorHandler.getLocatorForType(t, frame, v);
        try {
            test.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(1000));
            return true;
        } catch (Throwable ignored) {
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

    /** TYPE from "TYPE_value" or token itself if no underscore. */
    private String parseType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(0, idx) : token).trim();
    }

    /** VALUE from "TYPE_value"; "" if no underscore (unnamed role). */
    private String parseValue(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(idx + 1) : "").trim();
    }

    /**
     * Parse indexed iframe selector.
     * Supports:
     *   (iframe[...])[2]                // CSS-like
     *   (//iframe[@attr='v'])[2]        // XPath
     */
    private IndexedSelector parseIndexedIframeSelector(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // XPath: (//iframe[@...])[2]
        Pattern xPathIndexed = Pattern.compile("^\\(\\s*(//iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mx = xPathIndexed.matcher(s);
        if (mx.find()) {
            String base = mx.group(1);
            int oneBased = Integer.parseInt(mx.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        // CSS-like: (iframe[...])[2]
        Pattern cssIndexed = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mc = cssIndexed.matcher(s);
        if (mc.find()) {
            String base = mc.group(1);
            int oneBased = Integer.parseInt(mc.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        return null;
    }

    /**
     * If strict modal name like iframe[name="iframeWindowModal1857"] → relax to starts-with,
     * so "topmost" logic can kick in safely without breaking explicit index syntax.
     */
    private String relaxModalSelectorIfNeeded(String sel) {
        if (sel == null) return null;
        String s = sel.trim();

        // CSS strict → relax to name^
        if (s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }
        // Already generic modal
        if (s.equals("iframe[name^=\"iframeWindowModal\"]") || s.equals("iframe[name^='iframeWindowModal']")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }
        // XPath strict → keep; probing will still use generic CSS set for modals
        return s;
    }

    private static final class IndexedSelector {
        final String base;           // e.g. iframe[frameborder='0px'] OR //iframe[@frameborder='0px']
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
        return s.startsWith("//") || s.startsWith("(");
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
                    page.waitForSelector(MODAL_OR_FRAMEBORDER_CSS, new Page.WaitForSelectorOptions().setTimeout(500));
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