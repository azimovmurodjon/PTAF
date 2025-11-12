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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementActionImpl (Topmost-Modal + Adaptive Waits + Overlay Bypass)
 *
 * - Picks the topmost visible modal iframe: iframe[name^="iframeWindowModal"], regardless of numeric suffix.
 * - Prefers inner modal if same controls exist in outer modal.
 * - Adaptive waiting: short probes, longer only when truly needed.
 * - Overlay bypass: waits briefly for #TranslucentOverlay__root to disappear, then force-clicks as a last resort.
 * - Keeps API stable; safe for existing code (download, reports, etc.).
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    // ---------- Tunables (fast defaults; you can tweak if needed) ----------
    private static final int PROBE_WAIT_MS = 500;             // quick checks for frames/elements
    private static final int VISIBLE_WAIT_MS = 1800;          // default wait for final element visibility
    private static final int OVERLAY_WAIT_MS = 800;           // wait for overlay to go away before clicking
    private static final int NESTED_SCAN_HARD_CAP = 6;        // max iframes scanned in a context
    private static final String MODAL_CSS = "iframe[name^='iframeWindowModal']";
    private static final String MODAL_OR_LEGACY_CSS = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";
    private static final String OVERLAY_ID = "#TranslucentOverlay__root";

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    // Keep our own Page ref for DI flexibility; PageHelper may also hold one.
    private Page pageRef;

    // Standard ctor
    public ElementActionImpl(Page page) {
        super(page);
        this.pageRef = page;
    }

    // No-arg ctor for DI frameworks (e.g., Cucumber/Pico) that insist on it.
    public ElementActionImpl() {
        super(null);
    }

    /** For DI: call once you have a Page available (e.g., from Hooks). */
    public void init(Page page) {
        this.pageRef = page;
        // If PageHelper exposes a setter, call it here (pseudo):
        // super.setPage(page);
    }

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
            Object context = (frameLocator != null) ? frameLocator : ((page != null) ? page : this.pageRef);
            if (context == null) {
                throw new IllegalStateException("No Page or FrameLocator context available. Did you forget to call init(Page)?");
            }

            if (frameLocator == null && iFrame != null && !iFrame.isEmpty()) {
                // Index/modal-aware, inner-first resolution
                FrameLocator fl = findFrameWithElement(asPage(context), iFrame, element, key);
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

    private Page asPage(Object context) {
        if (context instanceof Page) return (Page) context;
        if (this.pageRef != null) return this.pageRef;
        throw new IllegalStateException("Page context is required.");
    }

    // ============================================================
    // Frame resolution (Index-aware + Modal-aware + Inner-first)
    // ============================================================

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        String sel = normalizeModalOrLeave(iframeSelector);
        IndexedSelector idxSel = parseIndexedIframeSelector(sel);

        tryQuickFramePresence(page, sel);

        // 0) If caller used a strict modal name → always consider topmost modal first.
        if (isModalish(sel)) {
            FrameLocator topModal = topmostVisibleModal(page);
            if (topModal != null && isFinalChainVisibleInContext(topModal, element, key)) {
                logger.info("Topmost visible modal selected (page): {}", MODAL_CSS);
                return topModal;
            }
        }

        // 1) Try explicit index first (CSS/XPath)
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(page, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) return candidate;
            logger.info("Indexed iframe {}[{}] did not expose target; probing…", idxSel.base, idxSel.indexZeroBased);
        }

        // 2) Probe candidates (modal set OR the provided selector)
        String candidates = isModalish(sel) ? MODAL_OR_LEGACY_CSS : sel;
        FrameLocator best = pickBestFrameByChainProbe(page, candidates, element, key);
        if (best != null) return best;

        // 3) Legacy linear scan (hard-capped)
        Locator all = locatorForSelector(page, sel);
        int count = Math.min(all.count(), NESTED_SCAN_HARD_CAP);
        for (int i = 0; i < count; i++) {
            FrameLocator fl = frameLocatorForSelector(page, sel).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }

        // 4) Fallback to raw selector
        return frameLocatorForSelector(page, sel);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        String sel = normalizeModalOrLeave(iframeSelector);
        IndexedSelector idxSel = parseIndexedIframeSelector(sel);

        try {
            parentFrame.locator("iframe").first()
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(PROBE_WAIT_MS));
        } catch (Throwable ignored) {}

        if (isModalish(sel)) {
            FrameLocator topModal = topmostVisibleModal(parentFrame);
            if (topModal != null && isFinalChainVisibleInContext(topModal, element, key)) {
                logger.info("Topmost visible modal selected (nested): {}", MODAL_CSS);
                return topModal;
            }
        }

        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(parentFrame, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) return candidate;
            logger.info("Nested indexed iframe {}[{}] did not expose target; probing…", idxSel.base, idxSel.indexZeroBased);
        }

        String candidates = isModalish(sel) ? MODAL_OR_LEGACY_CSS : sel;
        FrameLocator best = pickBestFrameByChainProbe(parentFrame, candidates, element, key);
        if (best != null) return best;

        Locator all = locatorForSelector(parentFrame, sel);
        int count = Math.min(all.count(), NESTED_SCAN_HARD_CAP);
        for (int i = 0; i < count; i++) {
            FrameLocator fl = frameLocatorForSelector(parentFrame, sel).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }
        return frameLocatorForSelector(parentFrame, sel);
    }

    private boolean isModalish(String sel) {
        if (sel == null) return false;
        String s = sel.toLowerCase();
        return s.contains("iframewindowmodal") || s.contains("frameborder");
    }

    private void tryQuickFramePresence(Page page, String sel) {
        try {
            if (isXPathSelector(sel)) {
                page.waitForSelector("xpath=//iframe", new Page.WaitForSelectorOptions().setTimeout(PROBE_WAIT_MS));
            } else {
                page.waitForSelector(isModalish(sel) ? MODAL_OR_LEGACY_CSS : sel,
                        new Page.WaitForSelectorOptions().setTimeout(PROBE_WAIT_MS));
            }
        } catch (Throwable ignored) {}
    }

    // ============================================================
    // Topmost visible modal helpers
    // ============================================================

    private FrameLocator topmostVisibleModal(Page page) {
        Locator iframes = page.locator(MODAL_CSS);
        int n = iframes.count();
        if (n == 0) return null;

        int bestIdx = -1;
        int bestZ = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            Locator el = iframes.nth(i);
            if (!safeIsVisible(el)) continue;
            int z = safeZIndex(el);
            if (z > bestZ) { bestZ = z; bestIdx = i; }
        }
        return bestIdx >= 0 ? page.frameLocator(MODAL_CSS).nth(bestIdx) : null;
    }

    private FrameLocator topmostVisibleModal(FrameLocator parent) {
        Locator iframes = parent.locator(MODAL_CSS);
        int n = iframes.count();
        if (n == 0) return null;

        int bestIdx = -1;
        int bestZ = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            Locator el = iframes.nth(i);
            if (!safeIsVisible(el)) continue;
            int z = safeZIndex(el);
            if (z > bestZ) { bestZ = z; bestIdx = i; }
        }
        return bestIdx >= 0 ? parent.frameLocator(MODAL_CSS).nth(bestIdx) : null;
    }

    // ============================================================
    // Auto-probing by full chain
    // ============================================================

    private FrameLocator pickBestFrameByChainProbe(Page page, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(page, candidateSelector);
        int count = Math.min(iframes.count(), NESTED_SCAN_HARD_CAP);
        if (count == 0) return null;

        int bestIdx = -1, bestCount = -1, bestZ = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            Locator el = iframes.nth(i);
            if (!safeIsVisible(el)) continue;
            FrameLocator fl = frameLocatorForSelector(page, candidateSelector).nth(i);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl; // perfect
            int c = countFinalChainMatches(fl, element, key);
            int z = safeZIndex(el);
            if (c > bestCount || (c == bestCount && z > bestZ)) {
                bestCount = c; bestZ = z; bestIdx = i;
            }
        }
        return bestIdx >= 0 ? frameLocatorForSelector(page, candidateSelector).nth(bestIdx) : null;
    }

    private FrameLocator pickBestFrameByChainProbe(FrameLocator parentFrame, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(parentFrame, candidateSelector);
        int count = Math.min(iframes.count(), NESTED_SCAN_HARD_CAP);
        if (count == 0) return null;

        int bestIdx = -1, bestCount = -1, bestZ = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            Locator el = iframes.nth(i);
            if (!safeIsVisible(el)) continue;
            FrameLocator fl = frameLocatorForSelector(parentFrame, candidateSelector).nth(i);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
            int c = countFinalChainMatches(fl, element, key);
            int z = safeZIndex(el);
            if (c > bestCount || (c == bestCount && z > bestZ)) {
                bestCount = c; bestZ = z; bestIdx = i;
            }
        }
        return bestIdx >= 0 ? frameLocatorForSelector(parentFrame, candidateSelector).nth(bestIdx) : null;
    }

    // ============================================================
    // Visibility & scoring helpers
    // ============================================================

    private boolean isFinalChainVisibleInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(VISIBLE_WAIT_MS));
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
                    .setTimeout(PROBE_WAIT_MS));
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

        Pattern xPathIndexed = Pattern.compile("^\\(\\s*(//iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mx = xPathIndexed.matcher(s);
        if (mx.find()) {
            String base = mx.group(1);
            int oneBased = Integer.parseInt(mx.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        Pattern cssIndexed = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mc = cssIndexed.matcher(s);
        if (mc.find()) {
            String base = mc.group(1);
            int oneBased = Integer.parseInt(mc.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }
        return null;
    }

    /** If strict modal name like iframe[name="iframeWindowModal1857"] → relax to starts-with; else return as-is. */
    private String normalizeModalOrLeave(String sel) {
        if (sel == null) return null;
        String s = sel.trim();
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
        Page pg = page != null ? page : this.pageRef;

        try {
            // Quick probe for any modal iframe to avoid long waits during transitions
            if (pg != null) {
                try {
                    pg.waitForSelector(MODAL_OR_LEGACY_CSS,
                            new Page.WaitForSelectorOptions().setTimeout(PROBE_WAIT_MS));
                } catch (Throwable ignored) {}
            }

            if (frameLocator != null) {
                targetLocator = getLocator(null, null, null, element, key, null, frameLocator);
            } else if (pg != null) {
                targetLocator = getLocator(iFrame, iFrame_2, iFrame_3, element, key, pg, null);
            } else {
                throw new IllegalArgumentException("A Page or FrameLocator context is required.");
            }

            if (targetLocator == null) {
                throw new IllegalStateException("Failed to resolve a target Locator for element: " + element + " / key: " + key);
            }

            actionPerformer.waitForLocator(targetLocator); // your existing explicit wait

            // Overlay bypass on clicks
            if ("click".equalsIgnoreCase(action)) {
                if (!tryOverlaySafeClick(targetLocator)) {
                    // If overlay is gone or force still fails, fall back to standard performer
                    actionPerformer.performAction(pg, action, targetLocator, value);
                }
            } else {
                actionPerformer.performAction(pg, action, targetLocator, value);
            }
            return true;

        } catch (Exception e) {
            logger.error("Error while performing action '{}' on element '{}' key '{}'", action, element, key, e);
            return false;
        }
    }

    /** Wait briefly for overlay to disappear; force-click as a last resort. */
    private boolean tryOverlaySafeClick(Locator locator) {
        try {
            Locator overlay = locator.page().locator(OVERLAY_ID);
            // Wait briefly for overlay to detach or become hidden
            overlay.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.DETACHED)
                    .setTimeout(OVERLAY_WAIT_MS));
        } catch (Throwable ignored) {
            // overlay still present; try hidden state next
            try {
                locator.page().locator(OVERLAY_ID).waitFor(
                        new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(OVERLAY_WAIT_MS));
            } catch (Throwable ignored2) {
                // Still intercepting — force click
                try {
                    locator.first().click(new Locator.ClickOptions().setForce(true));
                    return true;
                } catch (Throwable ignored3) {
                    // fall through to standard click path
                }
            }
        }
        return false; // proceed with normal click in caller
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
                logger.error("Locator not found for element: {} key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page != null ? page : this.pageRef, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageWithReturn for element '{}' action '{}':", element, action, e);
            return null;
        }
    }

    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                                   String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for nested frame element: {} key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page != null ? page : this.pageRef, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageFrameWithReturn for element '{}' action '{}':", element, action, e);
            return null;
        }
    }

    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        // Fix “Variable used in lambda should be final or effectively final”
        final String clickTarget = getElement(element, key);
        final String filePath = getElement(element, file_name);
        FileChooser chooser = page.waitForFileChooser(() -> page.click(clickTarget));
        chooser.setFiles(Paths.get(filePath));
    }

    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = getElement(element, key);
        String fileName = extractFileName(documentLinkName);
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
        Page ctx = page != null ? page : this.pageRef;
        return getLocator(null, null, null, element, key, ctx, null);
    }

    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String key) {
        Page ctx = page != null ? page : this.pageRef;
        return getLocator(iFrame, iFrame_2, iFrame_3, element, key, ctx, null);
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