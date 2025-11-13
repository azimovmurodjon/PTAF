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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElementActionImpl extends PageHelper implements ElementAction {

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    // Overlay & modal constants (tuned for speed)
    private static final String OVERLAY_CANDIDATES =
            "#TranslucentOverlay__root, .modal-backdrop, .k-overlay, .cdk-overlay-backdrop";
    private static final String MODAL_IFRAME_CSS =
            "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

    // You can tune this down/up if needed
    private static final long OVERLAY_WAIT_MS = 1200L;

    public ElementActionImpl(Page page) {
        super(page);
    }

    // ============================================================
    // Public API
    // ============================================================

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
                return null;
            }
            // Short overlay wait (only if overlay is actually present)
            waitForNoOverlay(page, OVERLAY_WAIT_MS);
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                                   String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                return null;
            }
            waitForNoOverlay(page, OVERLAY_WAIT_MS);
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
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
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator) {
        List<ElementHandle> elementHandles = new ArrayList<>();
        try {
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            if (targetLocator != null) {
                elementHandles = targetLocator.elementHandles();
            }
        } catch (Exception ignored) {
        }
        return elementHandles;
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        return parseValue(locatorValue);
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

            for (int i = 0; i < locatorParts.length; ++i) {
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
    // Action execution (fast modal handling)
    // ============================================================

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator;

        try {
            if (frameLocator != null) {
                targetLocator = getLocator(null, null, null, element, key, null, frameLocator);
            } else {
                if (page == null) {
                    throw new IllegalArgumentException("A Page or FrameLocator context is required.");
                }
                targetLocator = getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
            }

            if (targetLocator == null) {
                throw new IllegalStateException(
                        "Failed to resolve a target Locator for element: " + element + " with key: " + key);
            }

            if (page != null) {
                // Short overlay wait only when overlay really present
                waitForNoOverlay(page, OVERLAY_WAIT_MS);
            }

            actionPerformer.waitForLocator(targetLocator);
            actionPerformer.performAction(page, action, targetLocator, value);
            return true;

        } catch (Exception primaryEx) {
            // Fallback: if we’re on a modal-heavy page, try all visible modal frames (topmost first)
            if (page != null) {
                List<FrameLocator> visibleModals = getVisibleModalFramesTopFirst(page);
                if (!visibleModals.isEmpty()) {
                    for (FrameLocator fl : visibleModals) {
                        try {
                            Locator alt = getLocator(null, null, null, element, key, null, fl);
                            if (alt != null) {
                                waitForNoOverlay(page, OVERLAY_WAIT_MS);
                                actionPerformer.waitForLocator(alt);
                                actionPerformer.performAction(page, action, alt, value);
                                return true;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return false;
        }
    }

    // ============================================================
    // Frame resolution (no long waits, still modal-aware)
    // ============================================================

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;
        String selNormalized = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));

        // 0) If caller already uses generic modal selector, just pick topmost visible.
        if (looksModal &&
                ("iframe[name^=\"iframeWindowModal\"]".equals(selNormalized)
                        || MODAL_IFRAME_CSS.equals(selNormalized))) {
            FrameLocator top = topmostVisibleModal(page);
            if (top != null) {
                return top;
            }
        }

        // 1) Indexed iframe first (no waits)
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(page, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                return candidate;
            }
        }

        // 2) Auto-probe by chain
        FrameLocator best;
        if (looksModal) {
            best = pickBestFrameByChainProbe(page, MODAL_IFRAME_CSS, element, key);
        } else {
            best = pickBestFrameByChainProbe(page, selNormalized, element, key);
        }
        if (best != null) {
            return best;
        }

        // 3) Legacy fallback scanning
        Locator iframeLocator = locatorForSelector(page, selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; ++i) {
            FrameLocator fl = frameLocatorForSelector(page, selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) {
                return fl;
            }
        }

        // 4) Final fallback
        return frameLocatorForSelector(page, selNormalized);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;
        String selNormalized = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));

        if (looksModal &&
                ("iframe[name^=\"iframeWindowModal\"]".equals(selNormalized)
                        || MODAL_IFRAME_CSS.equals(selNormalized))) {
            FrameLocator top = topmostVisibleModal(parentFrame);
            if (top != null) {
                return top;
            }
        }

        // No explicit waits here – we rely on locator chain + actionPerformer to wait.

        // 1) Indexed
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(parentFrame, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                return candidate;
            }
        }

        // 2) Auto-probe
        FrameLocator best;
        if (looksModal) {
            best = pickBestFrameByChainProbe(parentFrame, MODAL_IFRAME_CSS, element, key);
        } else {
            best = pickBestFrameByChainProbe(parentFrame, selNormalized, element, key);
        }
        if (best != null) {
            return best;
        }

        // 3) Legacy scan
        Locator iframeLocator = locatorForSelector(parentFrame, selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; ++i) {
            FrameLocator fl = frameLocatorForSelector(parentFrame, selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) {
                return fl;
            }
        }

        return frameLocatorForSelector(parentFrame, selNormalized);
    }

    // ============================================================
    // Modal helpers (topmost visible first)
    // ============================================================

    private FrameLocator topmostVisibleModal(Page page) {
        Locator ifr = page.locator(MODAL_IFRAME_CSS);
        int count = ifr.count();
        if (count == 0) return null;

        int bestIdx = -1;
        int bestZ = Integer.MIN_VALUE;

        for (int i = 0; i < count; ++i) {
            Locator n = ifr.nth(i);
            if (safeIsVisible(n)) {
                int z = safeZIndex(n);
                if (z > bestZ) {
                    bestZ = z;
                    bestIdx = i;
                }
            }
        }

        return bestIdx >= 0 ? page.frameLocator(MODAL_IFRAME_CSS).nth(bestIdx) : null;
    }

    private FrameLocator topmostVisibleModal(FrameLocator parent) {
        Locator ifr = parent.locator(MODAL_IFRAME_CSS);
        int count = ifr.count();
        if (count == 0) return null;

        int bestIdx = -1;
        int bestZ = Integer.MIN_VALUE;

        for (int i = 0; i < count; ++i) {
            Locator n = ifr.nth(i);
            if (safeIsVisible(n)) {
                int z = safeZIndex(n);
                if (z > bestZ) {
                    bestZ = z;
                    bestIdx = i;
                }
            }
        }

        return bestIdx >= 0 ? parent.frameLocator(MODAL_IFRAME_CSS).nth(bestIdx) : null;
    }

    private List<FrameLocator> getVisibleModalFramesTopFirst(Page page) {
        Locator ifr = page.locator(MODAL_IFRAME_CSS);
        int count = ifr.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            order.add(i);
        }

        order.sort((a, b) -> Integer.compare(safeZIndex(ifr.nth(b)), safeZIndex(ifr.nth(a))));
        List<FrameLocator> result = new ArrayList<>();
        for (Integer idx : order) {
            if (safeIsVisible(ifr.nth(idx))) {
                result.add(page.frameLocator(MODAL_IFRAME_CSS).nth(idx));
            }
        }
        return result;
    }

    // ============================================================
    // Probing / visibility helpers
    // ============================================================

    private FrameLocator pickBestFrameByChainProbe(Page page, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(page, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        List<Integer> order = indicesByZIndexDesc(iframes);
        for (Integer idx : order) {
            if (safeIsVisible(iframes.nth(idx))) {
                FrameLocator fl = frameLocatorForSelector(page, candidateSelector).nth(idx);
                if (isFinalChainVisibleInContext(fl, element, key)) {
                    return fl;
                }
            }
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
        return bestIdx >= 0 ? frameLocatorForSelector(page, candidateSelector).nth(bestIdx) : null;
    }

    private FrameLocator pickBestFrameByChainProbe(FrameLocator parentFrame, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(parentFrame, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        List<Integer> order = indicesByZIndexDesc(iframes);
        for (Integer idx : order) {
            if (safeIsVisible(iframes.nth(idx))) {
                FrameLocator fl = frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
                if (isFinalChainVisibleInContext(fl, element, key)) {
                    return fl;
                }
            }
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
        return bestIdx >= 0 ? frameLocatorForSelector(parentFrame, candidateSelector).nth(bestIdx) : null;
    }

    private boolean isFinalChainVisibleInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(800.0)); // shorter timeout
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private int countFinalChainMatches(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            return finalLocator.count();
        } catch (Throwable e) {
            return 0;
        }
    }

    private boolean isFirstTokenVisibleInContext(FrameLocator frame, String element, String key) {
        String[] parts = normalizeAndSplitChain(elementLocatorHelper.getElement(element, key));
        if (parts.length == 0) return false;
        String first = parts[0].trim();
        String t = parseType(first);
        String v = parseValue(first);
        Locator test = locatorHandler.getLocatorForType(t, frame, v);
        try {
            test.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(600.0));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private Locator buildLocatorInContext(FrameLocator frame, String element, String key) {
        String full = elementLocatorHelper.getElement(element, key);
        String[] chain = normalizeAndSplitChain(full);
        Locator current = null;
        for (int i = 0; i < chain.length; ++i) {
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
        try {
            return loc.isVisible();
        } catch (Throwable e) {
            return false;
        }
    }

    private int safeZIndex(Locator iframeEl) {
        try {
            Object val = iframeEl.evaluate(
                    "e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
            return (val instanceof Number) ? ((Number) val).intValue() : 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    private List<Integer> indicesByZIndexDesc(Locator iframes) {
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            order.add(i);
        }
        order.sort((a, b) -> Integer.compare(safeZIndex(iframes.nth(b)), safeZIndex(iframes.nth(a))));
        return order;
    }

    // ============================================================
    // Selector parsing & normalization
    // ============================================================

    private String[] normalizeAndSplitChain(String raw) {
        if (raw == null) return new String[0];
        String normalized = raw.replace("&gt;", ">");
        return normalized.split("\\s*>\\s*");
    }

    private String parseType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(0, idx) : token).trim();
    }

    private String parseValue(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(idx + 1) : "").trim();
    }

    private IndexedSelector parseIndexedIframeSelector(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        Pattern xPathIndexed = Pattern.compile("^\\(\\s*(//iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$",
                Pattern.CASE_INSENSITIVE);
        Matcher mx = xPathIndexed.matcher(s);
        if (mx.find()) {
            String base = mx.group(1);
            int oneBased = Integer.parseInt(mx.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        Pattern cssIndexed = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$",
                Pattern.CASE_INSENSITIVE);
        Matcher mc = cssIndexed.matcher(s);
        if (mc.find()) {
            String base = mc.group(1);
            int oneBased = Integer.parseInt(mc.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }
        return null;
    }

    private String relaxModalSelectorIfNeeded(String sel) {
        if (sel == null) return null;
        String s = sel.trim();
        if (s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }
        return s;
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
    // Overlay handling (short + cheap)
    // ============================================================

    private void waitForNoOverlay(Page page, long timeoutMs) {
        try {
            Locator overlay = page.locator(OVERLAY_CANDIDATES);
            if (overlay.count() == 0) {
                return; // nothing to wait for
            }

            long end = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < end) {
                boolean anyVisible = false;
                int c = overlay.count();
                for (int i = 0; i < c; ++i) {
                    try {
                        if (overlay.nth(i).isVisible()) {
                            anyVisible = true;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                if (!anyVisible) {
                    return;
                }
                Thread.sleep(60L); // short sleep for responsiveness
            }

            try {
                overlay.first().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(200.0));
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    // ============================================================
    // Utility
    // ============================================================

    public static String extractFileName(String filePath) {
        String[] parts = filePath.split("/");
        return parts[parts.length - 1];
    }

    public String getElement(String element, String key) {
        try {
            return (String) YamlReader.get("elements." + element + "." + key);
        } catch (Exception e) {
            throw e;
        }
    }

    private boolean assertElementText(Page page, String element, String key,
                                      String expectedText, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            String actualText = targetLocator.first().textContent();
            return expectedText.equals(actualText);
        } catch (Exception e) {
            return false;
        }
    }

    private Locator getLocatorBasedOnPage(Page page, String element, String key) {
        return getLocator(null, null, null, element, key, page, null);
    }

    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                               String element, String key) {
        return getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
    }

    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }

    private static final class IndexedSelector {
        final String base;
        final int indexZeroBased;

        IndexedSelector(String base, int indexZeroBased) {
            this.base = base;
            this.indexZeroBased = indexZeroBased;
        }
    }
}