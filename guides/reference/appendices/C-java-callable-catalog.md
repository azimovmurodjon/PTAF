# Appendix C — Java Callable Catalog

**Generated from the active repository:** `scripts/generate_java_callable_catalog.py`.

This appendix indexes public and protected Java method and constructor declarations from the active FNB-ETAF source tree. It is designed for source navigation: use the path, source line, signature, and nearest Javadoc summary to locate an implementation quickly. Private methods are intentionally not cataloged, and the source itself remains authoritative for behavior.

> **Read the relevant chapter first.** The chapter library explains intent, configuration, usage, and operational boundaries. This appendix supplies the detailed method-level map needed when tracing an implementation or deciding where a new capability belongs.

## Coverage

| Item | Count |
|---|---:|
| Java source files cataloged | 144 |
| Public or protected declarations cataloged | 1363 |

## Quick navigation

- [Regular web UI](#regular-web-ui-and-browser-lifecycle)
- [API and API performance](#api-and-http-api-performance)
- [Database](#database)
- [Mobile](#mobile-and-mobile-browser)
- [Data, files, and PDF](#data-files-and-pdf)
- [UI performance](#isolated-ui_performance)
- [Reporting and utilities](#reporting-hooks-and-shared-utilities)
- [All remaining packages](#all-source-files)

## Regular web UI and browser lifecycle

### `Hooks` — `com.ptaf.hooks`

Source: [`src/main/java/com/ptaf/hooks/Hooks.java`](../../../src/main/java/com/ptaf/hooks/Hooks.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 126 | `public Hooks()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 130 | `public void setUp(Scenario scenario)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 245 | `public void tearDown(Scenario scenario)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 433 | `public static void waitForCurrentPageToLoad()` | Waits until current page is loaded. |
| 459 | `public static void setPage(Page page)` | Sets the active page for the current thread. |
| 478 | `public static void closeBrowserResources()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 661 | `public static void markBrowserClosedIntentionally()` | Marks the current @LastScenario feature's close operation as deliberate. |
| 1030 | `public static boolean maximizeBrowserWindow(Page page)` | Retains compatibility with the framework's explicit maximize action without changing browser lifecycle behavior. |
| 1062 | `public static Page getPage()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1070 | `public static Browser getBrowser()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1078 | `public static Scenario getCurrentScenario()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1082 | `public static void setCurrentScenario(Scenario scenario)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1086 | `public static BrowserContext getContext()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `ActionPerformer` — `com.ptaf.ui.action_performer`

Source: [`src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java`](../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 243 | `public String performActionWithReturn(Page page, String action, Locator targetLocator, String value)` | Convenience wrapper that forwards to performAction and returns the String result. |
| 264 | `public String performAction(Page page, String action, Locator targetLocator, String value)` | Main entry point: performs a variety of actions against the provided locator or page. |
| 742 | `public void waitForLocator(Locator locator)` | Public helper: waits for the first element matched by the locator to become visible. |

### `ElementActionImpl` — `com.ptaf.ui.action_performer`

Source: [`src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java`](../../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 48 | `public ElementActionImpl(Page page)` | Constructor. |
| 73 | `public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3, String element, String key, Page page, FrameLocator frameLocator)` | Resolves a Locator based on a defined element key in the element YAML. |
| 138 | `public boolean performActionPage(Page page, String action, String element, String key, String value)` | Convenience wrapper to perform an action on a Page context. |
| 153 | `public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value)` | Convenience wrapper to perform an action on a FrameLocator context. |
| 172 | `public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator)` | Convenience wrapper for performing an action on a Page within one or more nested iframes. |
| 185 | `public boolean getElementHandlePage(Page page, String element, String key)` | Attempts to retrieve ElementHandles for an element defined in YAML on a Page. |
| 199 | `public boolean getElementHandleFrame(FrameLocator frameLocator, String element, String key)` | Attempts to retrieve ElementHandles for an element defined in YAML on a FrameLocator. |
| 214 | `public boolean assertElementTextPage(Page page, String element, String key, String expectedText)` | Assert that the text content of an element on the Page matches the expectedText. |
| 228 | `public boolean assertElementTextFrame(FrameLocator frameLocator, String element, String key, String expectedText)` | Assert that the text content of an element inside a FrameLocator matches the expectedText. |
| 244 | `public String performActionPageWithReturn(Page page, String action, String element, String key, String value)` | Perform an action that returns a String result (for actions that produce values). |
| 277 | `public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator)` | Perform an action that returns a String result within nested iframe context on a Page. |
| 354 | `public void uploadFile(Page page, String file_name, String element, String key)` | Uploads a file by invoking a file chooser on the page and setting the file. |
| 371 | `public void clickOnDocumentLinkName(Page page, String element, String key)` | Clicks a link identified by its accessible name (role=link, name=). |
| 392 | `public static String extractFileName(String filePath)` | Extract the file name from a path string by splitting on "/" and returning the last element. |
| 405 | `public String getElement(String element, String key)` | Helper to fetch raw element selector strings from YAML using YamlReader. |
| 456 | `public List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator)` | Retrieve a list of ElementHandle objects for an element defined in YAML. |
| 520 | `public String getExactLocator(String element, String key)` | Returns the raw locator string for a given element and key (the actual selector portion), not including any locator type prefix. |

### `UIAssert` — `com.ptaf.ui.assertions`

Source: [`src/main/java/com/ptaf/ui/assertions/UIAssert.java`](../../../src/main/java/com/ptaf/ui/assertions/UIAssert.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 49 | `public UIAssert(Page page)` | Create a new UIAssert bound to a Playwright Page. |
| 72 | `public void textEquals(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expectedText)` | Assert exact text equals (strict). |
| 100 | `public void textContains(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String mustContain)` | Assert text contains (delegates to ActionPerformer 'hastext'). |
| 111 | `public void valueEquals(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expected)` | Assert input value equals (uses 'hasequalvalue'). |
| 122 | `public void attributeEquals(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String attribute, String expected)` | Assert attribute equals. |
| 148 | `public void attributeContains(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String attribute, String mustContain)` | Assert attribute contains. |
| 175 | `public void isVisible(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert element is visible. |
| 184 | `public void isHidden(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert element is hidden. |
| 193 | `public void isEnabled(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert element is enabled. |
| 202 | `public void isDisabled(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert element is disabled. |
| 211 | `public void isChecked(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert checkbox/radio is checked. |
| 220 | `public void hasClass(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String classSubstring)` | Assert element has a class that contains the given substring. |
| 229 | `public void exists(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert element exists (one or more matches). |
| 242 | `public void notExists(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Assert element does not exist. |
| 271 | `public void waitForText(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expectedSubstring)` | Wait for text to contain the expected substring. |
| 280 | `public void waitForValue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expectedValue)` | Wait for an input value to equal expectedValue. |
| 290 | `public void textEquals(Page page, String element, String locator, String expectedText)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 293 | `public void textContains(Page page, String element, String locator, String mustContain)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 296 | `public void valueEquals(Page page, String element, String locator, String expected)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 299 | `public void attributeEquals(Page page, String element, String locator, String attribute, String expected)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 302 | `public void attributeContains(Page page, String element, String locator, String attribute, String mustContain)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 305 | `public void isVisible(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 308 | `public void isHidden(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 311 | `public void isEnabled(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 314 | `public void isDisabled(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 317 | `public void isChecked(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 320 | `public void hasClass(Page page, String element, String locator, String classSubstring)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 323 | `public void exists(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 326 | `public void notExists(Page page, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 329 | `public void waitForText(Page page, String element, String locator, String expectedSubstring)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 332 | `public void waitForValue(Page page, String element, String locator, String expectedValue)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 346 | `public void textEqualsTrimmed(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expectedText)` | Assert text equals after trimming and normalizing internal whitespace. |
| 373 | `public void textEqualsTrimmed(Page page, String element, String locator, String expectedText)` | Convenience overload for trimmed equality without iframe params. |
| 382 | `public void countEquals(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, int expectedCount)` | Assert that the number of matched nodes equals expected. |
| 405 | `public void countEquals(Page page, String element, String locator, int expectedCount)` | Convenience overload for countEquals without iframe params. |

### `LocatorHandler` — `com.ptaf.ui.handlers`

Source: [`src/main/java/com/ptaf/ui/handlers/LocatorHandler.java`](../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 97 | `public Locator getLocatorForType(String locatorType, Page page, String locator)` | Map a locatorType + locator to a Playwright Locator in the context of a Page. |
| 313 | `public Locator getLocatorForType(String locatorType, FrameLocator frame, String locator)` | Map a locatorType + locator to a Playwright Locator in the context of a FrameLocator. |
| 440 | `public Locator getLocatorForType(String locatorType, Locator baseLocator, String locator)` | Map a locatorType + locator to a Playwright Locator in the context of an existing base Locator. |

### `ElementLocatorHelper` — `com.ptaf.ui.helpers`

Source: [`src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java`](../../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 44 | `public String getElement(String element, String key)` | Retrieve an element property value from the YAML configuration. |
| 143 | `public String getLocatorType(String part)` | Extract the locator type token from a combined locator string. |
| 174 | `public String getLocator(String part)` | Extract the locator value from a combined locator string. |
| 201 | `public boolean hasExplicitValue(String part)` | Determine whether the given locator string contains an explicit value portion. |
| 217 | `public String[] splitTypeAndValue(String part)` | Split the provided locator string into a two-element array: [type, value]. |

### `ElementAction` — `com.ptaf.ui.interfaces`

Source: [`src/main/java/com/ptaf/ui/interfaces/ElementAction.java`](../../../src/main/java/com/ptaf/ui/interfaces/ElementAction.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `ElementLocator` — `com.ptaf.ui.interfaces`

Source: [`src/main/java/com/ptaf/ui/interfaces/ElementLocator.java`](../../../src/main/java/com/ptaf/ui/interfaces/ElementLocator.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `PageHelper` — `com.ptaf.ui.page_helper`

Source: [`src/main/java/com/ptaf/ui/page_helper/PageHelper.java`](../../../src/main/java/com/ptaf/ui/page_helper/PageHelper.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 86 | `public PageHelper(Page page)` | Create a new PageHelper tied to the provided Playwright {@link Page}. |

### `FrameCommonMethods` — `com.ptaf.ui.pages`

Source: [`src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java`](../../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 51 | `public FrameCommonMethods(Page page)` | Initializes an instance of FrameCommonMethods with a specified Playwright page. |
| 67 | `public void click(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Clicks on an element within specified iframes. |
| 82 | `public void fill(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Fills an input field within specified iframes with the provided value. |
| 97 | `public void select(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Selects an option from a dropdown within specified iframes. |
| 111 | `public void check(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks a checkbox element within specified iframes. |
| 125 | `public void uncheck(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Unchecks a checkbox element within specified iframes. |
| 139 | `public void hover(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Hovers over a specified element within nested iframes. |
| 154 | `public void type(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Types a specified value into a designated input field within nested iframes. |
| 169 | `public void press(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Presses a specified key on the targeted element within nested iframes. |
| 183 | `public void dblclick(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Double-clicks on the specified element within nested iframes. |
| 198 | `public void screenshot(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Takes a screenshot of the specified element within nested iframes. |
| 217 | `public void download(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Initiates a file download from a web page and saves it to a specified location. |
| 233 | `public void download_optional(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Initiates a file download_optional from a web page and saves it to a specified location. |
| 249 | `public void scroll(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Scrolls the page to the specified element within nested iframes. |
| 263 | `public void focus(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Focuses on the specified element within nested iframes. |
| 278 | `public void blur(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Removes focus from the specified element within nested iframes. |
| 292 | `public void clear(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Clears the value of an input or element within nested iframes. |
| 306 | `public void drag(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Drags the specified element within nested iframes. |
| 320 | `public String gettext(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Gets the text content of the specified element within nested iframes. |
| 339 | `public void get_and_contain_text(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Gets the text content of the specified element within nested iframes and contains with found text content with locator value. |
| 353 | `public void isvisible(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks whether the specified element is visible within nested iframes. |
| 368 | `public void isenabled(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element is enabled within nested iframes. |
| 384 | `public void isdisabled(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element is disabled within nested iframes. |
| 400 | `public void ishidden(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element is hidden within nested iframes. |
| 415 | `public void ischecked(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified checkbox is currently checked within nested iframes. |
| 429 | `public void exists(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element exists within nested iframes. |
| 443 | `public void not_exists(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element not_exists within nested iframes. |
| 461 | `public void rightclick(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Right-clicks on a specified element within nested iframes. |
| 475 | `public void tap(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Taps on a specified element, primarily for mobile scenarios, within nested iframes. |
| 490 | `public void uploadFile(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Uploads a file to a designated input element (type=file) within nested iframes. |
| 504 | `public void selectMultiple(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Selects multiple options from a dropdown element within nested iframes. |
| 519 | `public void getAttribute(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Retrieves the value of a specified attribute from an element within nested iframes. |
| 534 | `public void setAttribute(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Sets a specified attribute on an element within nested iframes. |
| 549 | `public void removeAttribute(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Removes a specified attribute from an element within nested iframes. |
| 564 | `public void evaluate(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Evaluates JavaScript on the specified element within nested iframes. |
| 578 | `public void waitForElement(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Waits for a specified element to be present within nested iframes. |
| 592 | `public void waitForState(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Waits for the specified element to reach a certain state within nested iframes. |
| 607 | `public void waitForText(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Waits for specified text to appear in a particular element within nested iframes. |
| 622 | `public void waitForValue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Waits for a specified value in an element within nested iframes. |
| 636 | `public void dragStart(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Initiates a drag action on the specified element within nested iframes. |
| 650 | `public void dragEnd(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Ends a drag action on the specified element within nested iframes. |
| 665 | `public void input(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Inputs a specified value into a designated element within nested iframes. |
| 680 | `public void selectFile(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Selects a file for an input element of type=file within nested iframes. |
| 695 | `public void hasText(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Checks if the specified element contains the given text within nested iframes. |
| 709 | `public void hasclass(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element has the given CSS class within nested iframes. |
| 724 | `public void hasEqualValue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Compares the value of the specified element against an expected value within nested iframes. |
| 738 | `public void isempty(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Checks if the specified element is empty within nested iframes. |
| 753 | `public void hasvalue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value)` | Checks if the specified element has the expected value within nested iframes. |
| 767 | `public String getStringValue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Retrieves the current value of the specified element within nested iframes. |
| 786 | `public void reportListOfDropdown(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Reports all available options from a dropdown element located within nested iframes. |
| 803 | `public void reportElementString(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String label)` | Captures and reports the string value of a specific element along with a screenshot, handling nested iframe contexts. |
| 816 | `public void reportString(String title, String value)` | Captures and reports the string value of a specific element along with a screenshot, handling nested iframe contexts. |
| 833 | `public void getvalue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Retrieves the value of a specified element located within nested iframes and performs the "getvalue" action using the shared action execution logic. |
| 848 | `public Locator getElement_locator(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Retrieves the locator for the specified element within nested iframes. |
| 852 | `public String get_frame_element_string_value(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 866 | `public void contain(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expectedText)` | Asserts that the specified element contains the expected text. |
| 973 | `public void getListOfElements(Page page, String iFrame, String element, String locator)` | Retrieves and prints a list of elements on the specified page within nested iframes. |
| 999 | `public void uncheckRadioButton(Page page, String iFrame, String element, String locator)` | Unchecks a radio button from a list of elements within nested iframes. |
| 1025 | `public void clickRadioButton(Page page, String iFrame, String element, String locator)` | Clicks a radio button from a list of elements within nested iframes. |
| 1162 | `public static void setCurrentScenario(Scenario scenario)` | Stores the current Cucumber scenario in a thread-local variable. |
| 1190 | `public void finalizeScenario(Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator)` | Finalizes the scenario by performing necessary cleanup actions and capturing screenshots if no failures occurred during the scenario execution. |

### `PageCommonMethods` — `com.ptaf.ui.pages`

Source: [`src/main/java/com/ptaf/ui/pages/PageCommonMethods.java`](../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 70 | `public PageCommonMethods(Page page)` | Initializes an instance of PageCommonMethods with a given Playwright page. |
| 82 | `public void click(Page page, String element, String locator)` | Clicks on a web element specified by the locator. |
| 93 | `public void radio(Page page, String element, String locator)` | Check on radio on a web element specified by the locator. |
| 105 | `public void fill(Page page, String element, String locator, String value)` | Fills an input field with the specified value. |
| 117 | `public void select(Page page, String element, String locator, String value)` | Selects an option from a dropdown based on the provided value. |
| 128 | `public void check(Page page, String element, String locator)` | Checks a checkbox element. |
| 139 | `public void uncheck(Page page, String element, String locator)` | Unchecks a checkbox element. |
| 150 | `public void hover(Page page, String element, String locator)` | Hovers over a specified element. |
| 162 | `public void type(Page page, String element, String locator, String value)` | Types a specified value into a designated input field. |
| 174 | `public void press(Page page, String element, String locator, String value)` | Presses a specified key on the target element. |
| 185 | `public void dblclick(Page page, String element, String locator)` | Double-clicks on the specified element. |
| 202 | `public void screenshot(Page page, String element, String locator, String value)` | Takes a screenshot of the specified element on the current Playwright Page. |
| 231 | `public void fullscreenshot(Page page, String element, String locator, String value)` | Captures a full-page screenshot of the entire scrollable page content (not just the visible viewport) and saves it to the specified file path. |
| 257 | `public void maximize(Page page, String element, String locator)` | Maximizes the current browser window (or popup window) to fill the screen. |
| 274 | `public void download(Page page, String element, String locator, String value)` | Initiates a file download from the given page by interacting with a specified element and saves the downloaded file to the provided directory with a custom suffix. |
| 299 | `public void scroll(Page page, String element, String locator)` | Scrolls the page to the specified element. |
| 310 | `public void focus(Page page, String element, String locator)` | Focuses on the specified element. |
| 322 | `public void blur(Page page, String element, String locator, String value)` | Removes focus from the specified element. |
| 333 | `public void clear(Page page, String element, String locator)` | Clears the value of an input or element. |
| 344 | `public void drag(Page page, String element, String locator)` | Drags the specified element. |
| 357 | `public String gettext(Page page, String element, String locator)` | Retrieves the visible text content of the specified element on the given page. |
| 374 | `public void get_and_contain_text(Page page, String element, String locator)` | Gets the text content of the specified element and contains content. |
| 385 | `public void isvisible(Page page, String element, String locator)` | Checks whether the given element is visible on the page. |
| 397 | `public void isenabled(Page page, String element, String locator)` | Checks if the given element is enabled on the current page. |
| 409 | `public void isdisabled(Page page, String element, String locator)` | Checks if the given element is disabled on the current page. |
| 421 | `public void ishidden(Page page, String element, String locator)` | Checks if the given element is hidden on the current page. |
| 432 | `public void ischecked(Page page, String element, String locator)` | Checks if the specified checkbox is currently checked. |
| 447 | `public Locator getElement_locator(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator)` | Retrieves the locator for the specified element within nested iframes. |
| 459 | `public void exists(Page page, String element, String locator)` | Checks if the specified element exists on the page. |
| 470 | `public void not_exists(Page page, String element, String locator)` | Checks if the specified element not_exists on the page. |
| 485 | `public void rightclick(Page page, String element, String locator)` | Right-clicks on the specified element. |
| 496 | `public void tap(Page page, String element, String locator)` | Taps on the specified element, primarily for mobile scenarios. |
| 508 | `public void uploadFile(Page page, String element, String locator, String value)` | Uploads a file to a designated input element (type=file). |
| 519 | `public void selectMultiple(Page page, String element, String locator)` | Selects multiple options from a dropdown element. |
| 531 | `public void getAttribute(Page page, String element, String locator, String value)` | Retrieves the value of a specified attribute from an element. |
| 543 | `public void setAttribute(Page page, String element, String locator, String value)` | Sets a specified attribute on an element. |
| 555 | `public void removeAttribute(Page page, String element, String locator, String value)` | Removes a specified attribute from an element. |
| 567 | `public void evaluate(Page page, String element, String locator, String value)` | Evaluates JavaScript on the specified element. |
| 578 | `public void waitForElement(Page page, String element, String locator)` | Waits for a specified element to be present on the page. |
| 589 | `public void waitForState(Page page, String element, String locator)` | Waits until a specified element reaches a certain state. |
| 601 | `public void waitForText(Page page, String element, String locator, String value)` | Waits for specified text to appear in a particular element. |
| 613 | `public void waitForValue(Page page, String element, String locator, String value)` | Waits for a specified value in an element. |
| 624 | `public void dragStart(Page page, String element, String locator)` | Initiates a drag action on the specified element. |
| 635 | `public void dragEnd(Page page, String element, String locator)` | Ends a drag action on the specified element. |
| 647 | `public void input(Page page, String element, String locator, String value)` | Inputs a specified value into a designated element. |
| 659 | `public void selectFile(Page page, String element, String locator, String value)` | Selects a file for an input element of type=file. |
| 671 | `public void hasText(Page page, String element, String locator, String value)` | Checks if the specified element contains the given text. |
| 682 | `public void hasclass(Page page, String element, String locator)` | Checks if the specified element has the given CSS class. |
| 694 | `public void hasEqualValue(Page page, String element, String locator, String value)` | Compares the value of the specified element against an expected value. |
| 705 | `public void isempty(Page page, String element, String locator)` | Checks if the specified element is empty. |
| 717 | `public void contain(Page page, String element, String locator, String expectedText)` | Asserts that the specified element contains the expected text. |
| 730 | `public void file_chooser_for_upload(Page page, String fileName, String element, String locator)` | Initiates a file chooser for the upload feature. |
| 742 | `public void click_document_link(Page page, String element, String locator)` | Clicks on a document link. |
| 843 | `public void hasvalue(Page page, String element, String locator, String value)` | Checks if the specified element has the expected value. |
| 865 | `public void equalsListText(Page page, String element, String locator, String value)` | Checks whether the list of text values from the specified element matches the expected string. |
| 888 | `public void validateSort(Page page, String element, String locator, String order)` | Validates whether the values of the specified element are sorted according to the given order. |
| 899 | `public void reportListOfDropdown(Page page, String element, String locator)` | Reports all available options from a dropdown element located within nested iframes. |
| 913 | `public void reportElementString(Page page, String element, String locator, String label)` | Captures and reports the string value of a specific element along with a screenshot, handling nested iframe contexts. |
| 925 | `public void reportString(String title, String value)` | Captures and reports the string value |
| 949 | `public String getvalue(Page page, String element, String locator)` | Retrieves the current value of the specified element. |
| 965 | `public void getListOfElements(Page page, String element, String locator)` | Retrieves and prints a list of elements on the specified page. |
| 993 | `public void clickRadioButton(Page page, String element, String locator)` | Attempts to click a radio button within a list of elements on a specified page. |
| 1163 | `public static void setCurrentScenario(Scenario scenario)` | Stores the current Cucumber scenario in a thread-local variable. |
| 1188 | `public void finalizeScenario()` | Finalizes the scenario. |
| 1206 | `public void finalizeScenarioScreenshot(Page page, String targetLocator)` | Finalizes the current scenario and captures a screenshot if no failures occurred. |

### `BrowserFactory` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/BrowserFactory.java`](../../../src/main/java/com/ptaf/utils/BrowserFactory.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 90 | `public static Browser createBrowser(BrowserTypeEnum browserTypeEnum)` | Create a Playwright Browser instance for a given BrowserTypeEnum. |
| 120 | `public static Browser createBrowser(String profileName)` | Create a Playwright Browser instance that corresponds to a mobile browser profile. |
| 151 | `public static boolean isMobileBrowserProfile(String browserName)` | Check whether a given browserName corresponds to a known mobile browser profile. |
| 160 | `public static boolean hasActiveMobileBrowserProfile()` | Returns true if there is an active mobile browser profile set for the current thread. |
| 292 | `public static BrowserContext createContextWithVideo(Browser browser)` | Create a new BrowserContext with video recording and mobile profile support as configured. |

### `FeatureArtifactNameResolver` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 45 | `public static String resolveFeatureName(Scenario scenario)` | Resolves the exact declared {@code Feature:} title for the supplied scenario. |
| 60 | `public static String resolveFeatureName(URI uri)` | Resolves the declared {@code Feature:} title from a Cucumber feature URI. |
| 90 | `public static Path buildArtifactPath(Path outputDirectory, Scenario scenario, String originalFileName)` | Creates a unique artifact path within {@code outputDirectory}. |
| 102 | `public static Path buildArtifactPath(Path outputDirectory, URI featureUri, String originalFileName)` | Creates a unique artifact path from a feature URI while preserving the source extension. |
| 116 | `public static Path createFeatureDirectory(Path outputRoot, Scenario scenario) throws IOException` | Creates and returns a filesystem-safe subdirectory named after the declared {@code Feature:} title. |
| 128 | `public static Path createFeatureDirectory(Path outputRoot, URI featureUri) throws IOException` | Creates and returns a filesystem-safe Feature-name subdirectory from a feature URI. |
| 140 | `public static String buildArtifactFileName(Scenario scenario, String originalFileName)` | Creates a feature-based artifact file name with a microsecond timestamp. |
| 151 | `public static String buildArtifactFileName(URI featureUri, String originalFileName)` | Creates a feature-based artifact file name from a feature URI with a microsecond timestamp. |

### `ScreenshotHandler` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ScreenshotHandler.java`](../../../src/main/java/com/ptaf/utils/ScreenshotHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 55 | `public static void handleScenarioTeardown(Scenario scenario, Page page, String status)` | Handles the teardown process for the given scenario by attempting to capture a full-page screenshot from the provided Playwright Page and attach it to the Cucumber scenario report. |
| 110 | `public static void handleScenarioTeardownLocator(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String status)` | Handles the teardown process for a scenario where the target for the screenshot may be inside one or more nested iframes. |

## API and HTTP/API performance

### `ApiRequestHandler` — `com.ptaf.api.handlers`

Source: [`src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java`](../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 122 | `public static APIRequestContext getContext(String serviceName)` | Creates and returns an APIRequestContext for the current execution thread. |
| 243 | `public static void disposeContext()` | Disposes the APIRequestContext and closes the Playwright instance for the current thread. |

### `ApiActionImpl` — `com.ptaf.api.implementation`

Source: [`src/main/java/com/ptaf/api/implementation/ApiActionImpl.java`](../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 93 | `public ApiActionImpl()` | Default constructor which instantiates the underlying performer responsible for executing HTTP requests. |
| 111 | `public void setHeader(String key, String value)` | Adds or replaces a header for the next request constructed in the current thread. |
| 125 | `public void setPathParameter(String key, String value)` | Sets a path parameter to be applied when the endpoint contains placeholders. |
| 137 | `public void setQueryParameter(String key, Object value)` | Adds or replaces a query parameter for the next request on the current thread. |
| 148 | `public void setRequestBody(Object body)` | Sets the request body to be used for the next request in the current thread. |
| 174 | `public ApiResponseWrapper sendRequest(String serviceName, String requestKey)` | Sends an HTTP request based on a YAML definition key and the state previously configured via setHeader, setPathParameter, setQueryParameter, and setRequestBody. |
| 213 | `public ApiResponseWrapper getLastResponse()` | Returns the last ApiResponseWrapper stored for the current thread. |
| 228 | `public int getResponseStatusCode()` | Convenience method to obtain the HTTP status code of the last response. |
| 239 | `public String getResponseBody()` | Convenience method to obtain the raw response body (as a String) of the last response. |
| 258 | `public Object getValueFromResponse(String jsonPath)` | Extracts a value from the last response body using a JSONPath expression. |

### `ApiAction` — `com.ptaf.api.interfaces`

Source: [`src/main/java/com/ptaf/api/interfaces/ApiAction.java`](../../../src/main/java/com/ptaf/api/interfaces/ApiAction.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `ApiCommonMethods` — `com.ptaf.api.methods`

Source: [`src/main/java/com/ptaf/api/methods/ApiCommonMethods.java`](../../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 41 | `public ApiCommonMethods()` | Default constructor. |
| 54 | `public void setHeader(String key, String value)` | Adds or updates a header in the outgoing request. |
| 69 | `public void setPathParameter(String key, String value)` | Sets a path parameter to be substituted in the endpoint URL before sending the request. |
| 83 | `public void setQueryParameter(String key, Object value)` | Sets a query parameter for the outgoing request. |
| 95 | `public void setRequestBody(Object body)` | Assigns the request body that will be sent with the request. |
| 117 | `public void sendRequest(String serviceName, String requestKey)` | Sends a previously constructed API request. |
| 134 | `public void verifyResponseStatusCode(int expectedStatusCode)` | Verifies that the status code of the last API response matches the expected value. |
| 152 | `public void verifyResponseBodyContains(String expectedText)` | Verifies that the response body from the last API call contains a specific piece of text. |
| 170 | `public void verifyResponseHeader(String headerName, String expectedValue)` | Verifies the value of a specific header from the last API response. |
| 198 | `public void verifyJsonPathValue(String jsonPath, String expectedValue)` | Verifies that a value extracted from the JSON response body via JSONPath matches an expected value. |
| 221 | `public Object getValueByJsonPath(String jsonPath)` | Retrieves a value from the last response using a JSONPath expression. |

### `ApiActionPerformer` — `com.ptaf.api.performer`

Source: [`src/main/java/com/ptaf/api/performer/ApiActionPerformer.java`](../../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 54 | `public ApiActionPerformer()` | Default constructor. |
| 88 | `public ApiResponseWrapper sendRequest(APIRequestContext context, String method, String endpoint, Map<String, String> headers, Map<String, Object> queryParams, Map<String, String> pathParams, Object body)` | Builds and sends an HTTP request using the provided APIRequestContext and returns a wrapped response. |

### `ApiResponseWrapper` — `com.ptaf.api.wrapper`

Source: [`src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java`](../../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 60 | `public ApiResponseWrapper(int statusCode, String body, Map<String, String> headers)` | Construct a new ApiResponseWrapper. |
| 76 | `public int getStatusCode()` | Get the HTTP status code for this response. |
| 87 | `public String getBody()` | Get the response body as a String. |
| 98 | `public Map<String, String> getHeaders()` | Get the response headers map. |
| 112 | `public String toString()` | Provide a concise, human-readable representation of the wrapper primarily useful for logging and debugging. |

### `PerformanceAssertionEngine` — `com.ptaf.performance.assertions`

Source: [`src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java`](../../../src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 53 | `public void validate(PerformanceExecutionResult result, PerformanceAssertionProfile profile)` | Validate the given performance execution result against the provided assertion profile. |
| 85 | `public void assertErrorPercent(PerformanceExecutionResult result, double maxAllowedPercent)` | Assert that the error percent of the execution result does not exceed the provided maximum allowed percent. |
| 112 | `public void assertAverageResponseTime(PerformanceExecutionResult result, long maxAllowedMs)` | Assert that the average response time of the execution result does not exceed the provided maximum allowed milliseconds. |
| 139 | `public void assertP95ResponseTime(PerformanceExecutionResult result, long maxAllowedMs)` | Assert that the 95th percentile (P95) response time of the execution result does not exceed the provided maximum allowed milliseconds. |

### `PerformanceAuthTokenManager` — `com.ptaf.performance.auth`

Source: [`src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java`](../../../src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 57 | `public void saveToken(String alias, String token)` | Saves a token without expiration. |
| 84 | `public void saveToken(String alias, String token, Instant expiresAt)` | Saves a token with expiration timestamp. |
| 102 | `public boolean hasToken(String alias)` | Returns true if a token exists for the given alias. |
| 118 | `public String getToken(String alias)` | Returns the token value (string) for the given alias. |
| 142 | `public AuthToken getTokenDetails(String alias)` | Returns the AuthToken (token + optional expiry) for the given alias. |
| 168 | `public boolean isExpired(String alias)` | Returns true if the token associated with the given alias is expired. |
| 197 | `public PerformanceHeaderManager applyBearerToken(String alias, PerformanceHeaderManager headerManager)` | Applies bearer token to the given header manager using the specified alias. |
| 217 | `public void removeToken(String alias)` | Removes the token associated with the given alias. |
| 229 | `public void clearAll()` | Clears all stored tokens from the manager. |
| 244 | `public record AuthToken(String token, Instant expiresAt)` | Immutable token metadata holder. |

### `PerformanceProfileBuilder` — `com.ptaf.performance.builders`

Source: [`src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 72 | `public PerformanceProfileBuilder()` | Initializes the builder using framework default configuration values. |
| 90 | `public PerformanceProfileBuilder withUsers(int users)` | Sets the number of virtual users to simulate. |
| 105 | `public PerformanceProfileBuilder withRampUpSeconds(int rampUpSeconds)` | Sets the ramp-up time in seconds. |
| 120 | `public PerformanceProfileBuilder withHoldSeconds(int holdSeconds)` | Sets the hold (steady-state) time in seconds for duration mode. |
| 137 | `public PerformanceProfileBuilder withIterations(int iterations)` | Sets the number of iterations per user for iteration mode. |
| 152 | `public PerformanceProfile build()` | Creates a validated PerformanceProfile instance from the configured values. |
| 216 | `public static PerformanceProfileBuilder fromDefaults()` | Convenience factory returning a new builder initialized from framework defaults. |
| 231 | `public static PerformanceProfileBuilder fromProfile(PerformanceProfile profile)` | Creates a builder pre-populated from an existing PerformanceProfile instance. |

### `PerformanceRequestBuilder` — `com.ptaf.performance.builders`

Source: [`src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 150 | `public PerformanceRequestBuilder()` | Create a new builder instance initializing defaults. |
| 164 | `public PerformanceRequestBuilder withRequestName(String requestName)` | Set a user-friendly request name for reporting. |
| 175 | `public PerformanceRequestBuilder withMethod(String method)` | Set the HTTP method for the request. |
| 186 | `public PerformanceRequestBuilder withProtocol(String protocol)` | Override the protocol (http/https). |
| 197 | `public PerformanceRequestBuilder withHost(String host)` | Override the host for the request. |
| 208 | `public PerformanceRequestBuilder withPort(int port)` | Override the destination port. |
| 219 | `public PerformanceRequestBuilder withPath(String path)` | Set the request path (resource path). |
| 230 | `public PerformanceRequestBuilder withJsonBody(String requestBody)` | Provide an inline JSON request body. |
| 241 | `public PerformanceRequestBuilder withRequestBody(String requestBody)` | Provide an inline request body (generic). |
| 252 | `public PerformanceRequestBuilder withContentType(String contentType)` | Set Content-Type header value for the request. |
| 263 | `public PerformanceRequestBuilder withAcceptType(String acceptType)` | Set Accept header value for the request. |
| 275 | `public PerformanceRequestBuilder withHeader(String name, String value)` | Add a single header. |
| 289 | `public PerformanceRequestBuilder withHeaders(Map<String, String> headers)` | Add multiple headers at once by merging the provided map into the builder's header map. |
| 303 | `public PerformanceRequestBuilder withBearerTokenAlias(String bearerTokenAlias)` | Configure bearer token alias. |
| 315 | `public PerformanceRequestBuilder withBasicAuth(String username, String password)` | Configure basic authentication credentials. |
| 328 | `public PerformanceRequestBuilder withYamlBodyKey(String yamlBodyKey)` | Specify a YAML key whose value will be loaded and used as the request body if no inline body is provided. |
| 342 | `public PerformanceRequestBuilder withCsvBody(String csvFilePath, String csvRowIdentifier, String csvColumnName)` | Specify CSV-based payload source details. |
| 360 | `public PerformanceRequestBuilder withExcelBody(String excelFilePath, String excelRowIdentifier, String excelColumnName)` | Specify Excel-based payload source details. |
| 382 | `public PerformanceRequest build()` | Finalize the configuration, resolve payloads if needed, validate required fields, and construct an immutable {@link PerformanceRequest} instance. |

### `PerformanceTestPlanBuilder` — `com.ptaf.performance.builders`

Source: [`src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 55 | `public DslTestPlan buildHttpTestPlan(PerformanceRequest request, PerformanceProfile profile, Map<String, String> tokenStore, String jtlFilePath, String dashboardPath, String summaryPath)` | Overload used by PerformanceEngine. |
| 94 | `public DslTestPlan buildHttpTestPlan(PerformanceRequest request, PerformanceProfile profile, Map<String, String> resolvedHeaders, Path dashboardPath, Path jtlFilePath)` | Builds a complete HTTP test plan using already-resolved headers. |

### `PerformanceConfigurationProperties` — `com.ptaf.performance.config`

Source: [`src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java`](../../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 44 | `public static PerformanceProfile getDefaultProfile()` | Build and return the default {@link PerformanceProfile} using values read from YAML. |
| 70 | `public static PerformanceAssertionProfile getDefaultAssertionProfile()` | Build and return the default {@link PerformanceAssertionProfile} using values read from YAML. |
| 88 | `public static String getProtocol()` | Retrieve the protocol to use for performance tests (for example "http" or "https"). |
| 100 | `public static String getHost()` | Retrieve the host to target for performance tests. |
| 112 | `public static int getPort()` | Retrieve the port to use for performance tests. |
| 124 | `public static String getResultsFolder()` | Retrieve the folder path where raw results should be written. |
| 135 | `public static String getDashboardFolder()` | Retrieve the folder path where dashboard assets should be written. |
| 147 | `public static String getReportsBaseDirectory()` | Return the base directory where performance reports are stored. |

### `PerformanceYamlReader` — `com.ptaf.performance.config`

Source: [`src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java`](../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 119 | `public static Object get(String key)` | Retrieve a value from the loaded YAML configuration using a dot-separated key path. |
| 149 | `public static String getString(String key)` | Convenience accessor that returns the configuration value as a {@link String}. |
| 167 | `public static int getInt(String key, int defaultValue)` | Convenience accessor that returns the configuration value as an {@code int}. |
| 184 | `public static long getLong(String key, long defaultValue)` | Convenience accessor that returns the configuration value as a {@code long}. |
| 201 | `public static double getDouble(String key, double defaultValue)` | Convenience accessor that returns the configuration value as a {@code double}. |

### `BasePerformanceEngine` — `com.ptaf.performance.core`

Source: [`src/main/java/com/ptaf/performance/core/BasePerformanceEngine.java`](../../../src/main/java/com/ptaf/performance/core/BasePerformanceEngine.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 111 | `protected BasePerformanceEngine()` | Protected constructor to initialize framework-owned dependencies. |

### `PerformanceEngine` — `com.ptaf.performance.core`

Source: [`src/main/java/com/ptaf/performance/core/PerformanceEngine.java`](../../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 128 | `public PerformanceExecutionResult runHttpTest(PerformanceRequest request)` | Run a HTTP performance test using default profile and default assertion profile. |
| 145 | `public PerformanceExecutionResult runHttpTest(PerformanceRequest request, PerformanceProfile profile, PerformanceAssertionProfile assertionProfile)` | Run a HTTP performance test with a provided profile and assertion profile. |
| 161 | `public PerformanceExecutionResult runHttpTestExpectingFailure(PerformanceRequest request)` | Run a HTTP performance test that is expected to fail (negative/expected failure test), using default profiles. |
| 179 | `public PerformanceExecutionResult runHttpTestExpectingFailure(PerformanceRequest request, PerformanceProfile profile, PerformanceAssertionProfile assertionProfile)` | Run a HTTP performance test that is expected to fail (negative/expected failure test) with explicit profile parameters. |
| 207 | `public PerformanceExecutionResult runHttpTest(PerformanceRequest request, PerformanceProfile profile, PerformanceAssertionProfile assertionProfile, boolean expectedFailureMode)` | Core method that executes a HTTP performance scenario and produces a detailed result. |
| 437 | `public void storeBearerToken(String alias, String tokenValue)` | Store a bearer token value under a short alias to be used by future requests. |
| 464 | `public String getBearerToken(String alias)` | Retrieve a previously stored bearer token by alias. |
| 478 | `public Map<String, String> getTokenStore()` | Expose the internal token store. |
| 488 | `public PerformanceRunReport getCurrentRunReport()` | Return the current run-level aggregated PerformanceRunReport instance. |

### `PerformanceExecutionManager` — `com.ptaf.performance.core`

Source: [`src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java`](../../../src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 53 | `public PerformanceExecutionResult execute(PerformanceRequest request, PerformanceProfile profile, PerformanceAssertionProfile assertionProfile, DslTestPlan testPlan, Path dashboardPath, Path jtlFilePath, Path summaryFilePath, Path readableSummaryFilePath, Path runReportRootPath)` | Executes the given performance test plan and returns a framework-owned result. |

### `PerformanceHeaderManager` — `com.ptaf.performance.headers`

Source: [`src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java`](../../../src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 66 | `public PerformanceHeaderManager addDefaultHeader(String key, String value)` | Adds or replaces a default framework-level header. |
| 83 | `public PerformanceHeaderManager addDefaultHeaders(Map<String, String> headers)` | Adds multiple default framework-level headers. |
| 109 | `public PerformanceHeaderManager addRequestHeader(String key, String value)` | Adds or replaces a request-level header. |
| 125 | `public PerformanceHeaderManager addRequestHeaders(Map<String, String> headers)` | Adds multiple request-level headers. |
| 151 | `public PerformanceHeaderManager addBearerToken(String token)` | Adds Authorization header using Bearer token strategy. |
| 184 | `public PerformanceHeaderManager addBasicAuth(String username, String password)` | Adds Authorization header using Basic authentication strategy. |
| 215 | `public PerformanceHeaderManager addContentType(String contentType)` | Adds Content-Type header. |
| 235 | `public PerformanceHeaderManager addAccept(String accept)` | Adds Accept header. |
| 258 | `public Map<String, String> build()` | Returns final merged headers. |
| 277 | `public PerformanceHeaderManager clearRequestHeaders()` | Clears request-specific headers only. |
| 290 | `public PerformanceHeaderManager clearAll()` | Clears all headers (both default and request-level). |

### `PerformanceAssertionProfile` — `com.ptaf.performance.models`

Source: [`src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 69 | `public PerformanceAssertionProfile(double maxErrorPercent, long maxAverageResponseTimeMs, long maxP95ResponseTimeMs)` | Construct a new PerformanceAssertionProfile. |
| 83 | `public double getMaxErrorPercent()` | Get the configured maximum error percentage threshold. |
| 92 | `public long getMaxAverageResponseTimeMs()` | Get the configured maximum average response time threshold in milliseconds. |
| 101 | `public long getMaxP95ResponseTimeMs()` | Get the configured maximum 95th-percentile response time threshold in milliseconds. |
| 112 | `public boolean hasConfiguredThresholds()` | Determine if any threshold has been configured (i.e., is greater than 0). |
| 132 | `public boolean isErrorPercentBreached(double actualErrorPercent)` | Check whether the actual error percentage breaches the configured maximum. |
| 150 | `public boolean isAverageResponseBreached(long actualAverageResponseTimeMs)` | Check whether the actual average response time breaches the configured maximum. |
| 168 | `public boolean isP95ResponseBreached(long actualP95ResponseTimeMs)` | Check whether the actual 95th-percentile response time breaches the configured maximum. |
| 184 | `public boolean hasAnyBreach(double actualErrorPercent, long actualAverageResponseTimeMs, long actualP95ResponseTimeMs)` | Check if any of the configured thresholds are breached. |
| 260 | `public String toString()` | Produce a compact string representation useful for logging and debugging. |

### `PerformanceExecutionResult` — `com.ptaf.performance.models`

Source: [`src/main/java/com/ptaf/performance/models/PerformanceExecutionResult.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionResult.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 360 | `public PerformanceExecutionResult( String testName, String testPurpose, String performanceTestType, String testGoal, String httpMethod, String targetPath, String fullTargetUrl, String contentType, String acceptType,` | Primary constructor. |
| 479 | `public String getTestName()` | @return normalized test name (never null; empty string if not provided) |
| 486 | `public String getTestPurpose()` | @return normalized test purpose/description |
| 493 | `public String getPerformanceTestType()` | @return performance test type |
| 500 | `public String getTestGoal()` | @return business test goal |
| 511 | `public String getHttpMethod()` | @return HTTP method used in the scenario |
| 518 | `public String getTargetPath()` | @return normalized target path (path component) |
| 525 | `public String getFullTargetUrl()` | @return normalized full target URL |
| 532 | `public String getContentType()` | @return content type used in requests |
| 539 | `public String getAcceptType()` | @return accept type used in requests |
| 546 | `public String getAuthType()` | @return authentication type used |
| 553 | `public String getPayloadSourceType()` | @return payload source type |
| 560 | `public String getPayloadSourceDetails()` | @return payload source details |
| 571 | `public int getUsers()` | @return configured number of virtual users |
| 578 | `public int getRampUpSeconds()` | @return configured ramp-up seconds |
| 585 | `public int getHoldSeconds()` | @return configured hold seconds |
| 592 | `public int getIterations()` | @return configured iterations |
| 599 | `public String getExecutionMode()` | @return execution mode name |
| 610 | `public double getMaxAllowedErrorPercent()` | @return maximum allowed error percent configured for the scenario |
| 617 | `public long getMaxAllowedAverageResponseTimeMs()` | @return maximum allowed average response time (ms) |
| 624 | `public long getMaxAllowedP95ResponseTimeMs()` | @return maximum allowed 95th percentile response time (ms) |
| 635 | `public long getTotalScenarioDurationMs()` | @return total scenario duration in milliseconds |
| 642 | `public long getTotalSamples()` | @return total observed samples |
| 649 | `public long getTotalErrors()` | @return total observed errors |
| 656 | `public double getErrorPercent()` | @return observed error percent |
| 663 | `public long getMinResponseTimeMs()` | @return minimum observed response time (ms) |
| 670 | `public long getAverageResponseTimeMs()` | @return average observed response time (ms) |
| 677 | `public long getP95ResponseTimeMs()` | @return 95th percentile observed response time (ms) |
| 684 | `public long getMaxResponseTimeMs()` | @return maximum observed response time (ms) |
| 695 | `public int getRiskScore()` | @return numeric risk score |
| 702 | `public String getRiskLevel()` | @return normalized risk level string |
| 709 | `public String getThresholdBreachSummary()` | @return threshold breach summary text (never blank; default when none) |
| 716 | `public String getRecommendedAction()` | @return recommended action for stakeholders |
| 727 | `public String getResponseTimeAssessment()` | @return response time assessment text |
| 734 | `public String getErrorAssessment()` | @return error assessment text |
| 741 | `public String getStabilityAssessment()` | @return stability assessment text |
| 748 | `public String getFirstFailureIndicator()` | @return first failure indicator text |
| 755 | `public String getFinalConclusion()` | @return final conclusion text |
| 766 | `public String getDashboardPath()` | @return dashboard file path |
| 773 | `public String getJtlFilePath()` | @return jtl/sample file path |
| 780 | `public String getSummaryFilePath()` | @return machine-readable summary file path |
| 787 | `public String getReadableSummaryFilePath()` | @return human-readable summary file path |
| 794 | `public String getRunReportRootPath()` | @return run-level root path for the report artifacts |
| 805 | `public PerformanceExecutionStatus getExecutionStatus()` | @return execution status enum (may be null if not set) |
| 812 | `public boolean isExecutionPassed()` | @return true if the run passed assertions |
| 819 | `public boolean isExpectedFailureMode()` | @return true if this run was deliberately run expecting failure |
| 826 | `public boolean isActualFailureDetected()` | @return true if an actual failure was detected during execution |
| 833 | `public String getFailureMessage()` | @return framework-captured failure message (normalized) |
| 842 | `public double getErrorPercentage()` | Compatibility helper for older code that may still use this naming. |
| 853 | `public boolean hasThresholdBreach()` | @return true when thresholdBreachSummary contains a real breach message |
| 862 | `public boolean hasErrors()` | @return true when at least one error was observed or error percentage > 0 |
| 870 | `public boolean hasHighOrCriticalRisk()` | @return true when scenario risk is considered High or Critical. |
| 877 | `public boolean isLowRisk()` | @return true if risk level text equals "Low" (case-insensitive) |
| 884 | `public boolean isMediumRisk()` | @return true if risk level text equals "Medium" (case-insensitive) |
| 891 | `public boolean isHighRisk()` | @return true if risk level text equals "High" (case-insensitive) |
| 898 | `public boolean isCriticalRisk()` | @return true if risk level text equals "Critical" (case-insensitive) |
| 908 | `public boolean isAttentionNeeded()` | Aggregates multiple signals to determine if the scenario requires attention. |
| 922 | `public String getBusinessOutcomeLabel()` | Produces a concise business-friendly label describing the outcome. |
| 943 | `public String getAttentionCategory()` | Returns a high-level attention category to help stakeholders triage the result. |
| 978 | `public String getPrimaryBusinessConcern()` | Provides a primary business-facing concern that summarizes the most critical issue. |
| 1005 | `public String getSafeFailureMessage()` | @return failure message sanitized for safe display (empty string if none) |
| 1012 | `public String getSafeTestName()` | @return test name safe for Excel/CSV display (N/A when not provided) |
| 1019 | `public String getSafeTargetPath()` | @return target path safe for Excel/CSV (N/A when not provided) |
| 1026 | `public String getSafeRecommendedAction()` | @return recommended action safe for Excel/CSV (N/A when not provided) |
| 1033 | `public String getSafeFinalConclusion()` | @return final conclusion safe for Excel/CSV (N/A when not provided) |
| 1040 | `public String getSafeResponseTimeAssessment()` | @return response time assessment safe for Excel/CSV (N/A when not provided) |
| 1047 | `public String getSafeErrorAssessment()` | @return error assessment safe for Excel/CSV (N/A when not provided) |
| 1054 | `public String getSafeStabilityAssessment()` | @return stability assessment safe for Excel/CSV (N/A when not provided) |
| 1061 | `public String getSafeFirstFailureIndicator()` | @return first failure indicator safe for Excel/CSV (N/A when not provided) |
| 1192 | `public String toString()` | Returns a compact, development-friendly string representation of the result object. |

### `PerformanceExecutionStatus` — `com.ptaf.performance.models`

Source: [`src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 109 | `public String getBusinessLabel()` | Returns the stable business label for this status. |
| 121 | `public String getSummaryMeaning()` | Returns a short explanation of what this status means in summaries. |
| 135 | `public boolean isPassLike()` | Returns true when the status should be treated as a passing outcome for high-level reports and stakeholder summaries. |
| 150 | `public boolean isFailureLike()` | Returns true when the status should be treated as a failing outcome for high-level reports. |
| 161 | `public boolean isSkipped()` | Returns true when the scenario was skipped (i.e., not executed). |
| 175 | `public boolean needsAttention()` | Convenience method to indicate whether this status requires attention from engineers, testers, or stakeholders. |
| 188 | `public boolean isExpectedFailureFlow()` | Returns true if this status is part of the expected-failure workflow. |
| 202 | `public static String toBusinessLabel(PerformanceExecutionStatus status)` | Gets the business label for a potentially-null status. |
| 216 | `public static String toSummaryMeaning(PerformanceExecutionStatus status)` | Gets the summary meaning for a potentially-null status. |

### `PerformanceProfile` — `com.ptaf.performance.models`

Source: [`src/main/java/com/ptaf/performance/models/PerformanceProfile.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceProfile.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 81 | `public PerformanceProfile(int users, int rampUpSeconds, int holdSeconds, int iterations)` | Create a new PerformanceProfile instance. |
| 94 | `public int getUsers()` | Get the configured number of users for this profile. |
| 103 | `public int getRampUpSeconds()` | Get the configured ramp-up duration in seconds. |
| 112 | `public int getHoldSeconds()` | Get the configured hold duration in seconds (time to keep the target load after ramp-up). |
| 121 | `public int getIterations()` | Get the configured number of iterations per user. |
| 134 | `public boolean isIterationBasedExecution()` | Determine if this profile represents an iteration-based execution. |
| 147 | `public boolean isDurationBasedExecution()` | Determine if this profile represents a duration-based execution. |
| 160 | `public int getTotalPlannedDurationSeconds()` | Get the total planned duration in seconds for the non-iteration execution flow. |
| 172 | `public boolean isSmokeLikeProfile()` | Heuristic classifier that marks very small profiles as "smoke-like". |
| 185 | `public boolean isLoadLikeProfile()` | Heuristic classifier for moderate load profiles. |
| 197 | `public boolean isHighLoadLikeProfile()` | Heuristic classifier for high-load profiles. |
| 221 | `public String toString()` | Render a concise string representation of the profile used for logging and diagnostics. |

### `PerformanceRequest` — `com.ptaf.performance.models`

Source: [`src/main/java/com/ptaf/performance/models/PerformanceRequest.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceRequest.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 155 | `public PerformanceRequest(String requestName, String method, String protocol, String host, int port, String path, String requestBody, String contentType, String acceptType, Map<String, String> headers,` | Primary constructor. |
| 206 | `public String getRequestName()` | @return normalized request name (may be empty but never null) |
| 213 | `public String getMethod()` | @return normalized HTTP method (uppercase; may be empty but never null) |
| 220 | `public String getProtocol()` | @return normalized protocol (lowercase; may be empty but never null) |
| 227 | `public String getHost()` | @return normalized host (may be empty but never null) |
| 234 | `public int getPort()` | @return sanitized port number (0 indicates unspecified) |
| 241 | `public String getPath()` | @return normalized request path (always non-null and starts with '/') |
| 248 | `public String getRequestBody()` | @return request body/payload (trimmed; may contain internal newlines; never null) |
| 255 | `public String getContentType()` | @return content-type value (may be empty but never null) |
| 262 | `public String getAcceptType()` | @return accept-type value (may be empty but never null) |
| 269 | `public Map<String, String> getHeaders()` | @return an unmodifiable map of headers; never null. |
| 276 | `public String getBearerTokenAlias()` | @return normalized bearer token alias (may be empty but never null) |
| 283 | `public String getBasicAuthUsername()` | @return normalized basic auth username (may be empty but never null) |
| 290 | `public String getBasicAuthPassword()` | @return basic auth password (raw, may be empty but never null). |
| 297 | `public String getPayloadSourceType()` | @return human-friendly payload source type (may be empty but never null) |
| 304 | `public String getPayloadSourceDetails()` | @return human-friendly payload source details (may be empty but never null) |
| 317 | `public boolean hasBearerTokenAuth()` | Convenience check whether bearer-token style authentication is configured. |
| 326 | `public boolean hasBasicAuth()` | Convenience check whether basic authentication is configured. |
| 333 | `public boolean hasHeaders()` | @return true when the request contains one or more headers |
| 340 | `public boolean hasRequestBody()` | @return true when the request body is non-blank (useful for determining payload presence) |
| 349 | `public String getResolvedAuthType()` | Resolve a human-readable authentication type for reporting. |
| 364 | `public String getSafeRequestName()` | Return a display-safe request name for reports. |
| 373 | `public String getSafeMethod()` | Return a display-safe HTTP method for reports. |
| 382 | `public String getSafeProtocol()` | Return a display-safe protocol for reports. |
| 391 | `public String getSafeHost()` | Return a display-safe host for reports. |
| 400 | `public String getSafePath()` | Return a display-safe path for reports. |
| 409 | `public String getSafeContentType()` | Return a display-safe content type for reports. |
| 418 | `public String getSafeAcceptType()` | Return a display-safe accept type for reports. |
| 427 | `public String getSafePayloadSourceType()` | Return a display-safe payload source type. |
| 436 | `public String getSafePayloadSourceDetails()` | Return a display-safe payload source details for reports. |
| 449 | `public String buildDisplayUrl()` | Build a simple display URL composed of protocol, host, port and path for reporting purposes. |
| 565 | `public String toString()` | Debug-friendly string representation. |

### `PerformanceRunReport` — `com.ptaf.performance.models`

Source: [`src/main/java/com/ptaf/performance/models/PerformanceRunReport.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceRunReport.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 58 | `public PerformanceRunReport(String runFolderName, String runRootPath, String executionTimestamp)` | Construct a run-level report container. |
| 69 | `public String getRunFolderName()` | @return configured run folder name for this report. |
| 76 | `public String getRunRootPath()` | @return configured root path for this run. |
| 83 | `public String getExecutionTimestamp()` | @return execution timestamp associated with this run. |
| 97 | `public void addScenarioResult(PerformanceExecutionResult result)` | Add a scenario-level execution result to this run report. |
| 110 | `public List<PerformanceExecutionResult> getScenarioResults()` | Get an unmodifiable view of scenario results collected for this run. |
| 117 | `public int getTotalScenarios()` | @return total number of scenarios recorded for this run. |
| 124 | `public boolean hasScenarioResults()` | @return true if at least one scenario result has been recorded. |
| 133 | `public long getPassedScenarios()` | Count scenarios with PASS status. |
| 144 | `public long getFailedScenarios()` | Count scenarios with FAIL status. |
| 155 | `public long getExpectedFailConfirmedScenarios()` | Count scenarios that were expected to fail and indeed failed (confirmed expected failures). |
| 166 | `public long getExpectedFailNotTriggeredScenarios()` | Count scenarios that were marked as expected-fail but did not trigger the expected failure. |
| 177 | `public long getSkippedScenarios()` | Count scenarios that were skipped during execution. |
| 191 | `public double getPassRatePercent()` | Compute pass rate as a percentage. |
| 206 | `public double getFailRatePercent()` | Compute fail rate as a percentage. |
| 221 | `public double getAverageErrorPercent()` | Compute average of scenario-level error percentages. |
| 241 | `public double getAverageRiskScore()` | Compute average risk score across scenarios. |
| 258 | `public long getTotalScenarioDurationMs()` | Total of all scenario durations in milliseconds. |
| 269 | `public long getAverageScenarioDurationMs()` | Average scenario duration in milliseconds, rounded to nearest long. |
| 282 | `public long getTotalSamples()` | Sum of total request samples across all scenarios. |
| 293 | `public long getTotalErrors()` | Sum of total errors across all scenarios. |
| 304 | `public long getSlowestP95ResponseTimeMs()` | The slowest (maximum) 95th-percentile response time across all scenarios. |
| 316 | `public long getSlowestAverageResponseTimeMs()` | The slowest (maximum) average response time across all scenarios. |
| 328 | `public int getHighestRiskScore()` | Highest risk score value observed across all scenarios. |
| 340 | `public long getHighestScenarioDurationMs()` | Longest scenario duration observed in the run. |
| 352 | `public long getShortestScenarioDurationMs()` | Shortest scenario duration observed in the run. |
| 364 | `public long getHighestTotalErrors()` | Highest total error count for a single scenario in the run. |
| 376 | `public double getHighestErrorPercent()` | Highest error percentage observed across all scenarios. |
| 388 | `public long getThresholdBreachScenarioCount()` | Count scenarios where configured thresholds were breached. |
| 399 | `public long getErrorScenarioCount()` | Count scenarios that have any errors (either totalErrors > 0 or errorPercent > 0.0). |
| 410 | `public long getHighOrCriticalRiskScenarioCount()` | Count scenarios whose risk is considered High or Critical (by label or by numeric score). |
| 421 | `public long getCriticalRiskScenarioCount()` | Count scenarios explicitly marked with risk level "Critical" (case-insensitive). |
| 432 | `public long getHighRiskScenarioCount()` | Count scenarios explicitly marked with risk level "High" (case-insensitive). |
| 443 | `public long getMediumRiskScenarioCount()` | Count scenarios explicitly marked with risk level "Medium" (case-insensitive). |
| 454 | `public long getLowRiskScenarioCount()` | Count scenarios explicitly marked with risk level "Low" (case-insensitive). |
| 466 | `public long getNoIssueScenarioCount()` | Count scenarios that do not require attention. |
| 477 | `public PerformanceExecutionResult getSlowestP95Scenario()` | Retrieve the scenario which has the slowest P95 response time. |
| 488 | `public PerformanceExecutionResult getSlowestAverageResponseScenario()` | Retrieve the scenario which has the slowest average response time. |
| 499 | `public PerformanceExecutionResult getHighestErrorScenario()` | Retrieve the scenario with the highest error percent. |
| 510 | `public PerformanceExecutionResult getHighestRiskScenario()` | Retrieve the scenario with the highest numeric risk score. |
| 521 | `public PerformanceExecutionResult getLongestDurationScenario()` | Retrieve the scenario that ran the longest. |
| 532 | `public PerformanceExecutionResult getShortestDurationScenario()` | Retrieve the scenario that completed the fastest (shortest total duration). |
| 543 | `public PerformanceExecutionResult getHighestTotalErrorsScenario()` | Retrieve the scenario with the highest total errors. |
| 565 | `public String getOverallConclusion()` | Produce a human-friendly overall conclusion for the run based on collected metrics. |
| 692 | `public String toString()` | Debug-friendly summary of the run report object and key aggregated metrics. |

### `CsvPayloadReader` — `com.ptaf.performance.payloads`

Source: [`src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java`](../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 62 | `public static String getData(String filePathOrClasspathResource, String rowIdentifier, String columnName)` | Retrieve a specific value from a CSV payload. |

### `PayloadSourceType` — `com.ptaf.performance.payloads`

Source: [`src/main/java/com/ptaf/performance/payloads/PayloadSourceType.java`](../../../src/main/java/com/ptaf/performance/payloads/PayloadSourceType.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `PerformancePayloadDefinition` — `com.ptaf.performance.payloads`

Source: [`src/main/java/com/ptaf/performance/payloads/PerformancePayloadDefinition.java`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadDefinition.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 87 | `public PerformancePayloadDefinition(PayloadSourceType sourceType, String inlineBody, String yamlKey, String filePath, String sheetName, String rowIdentifier, String columnName)` | Primary constructor that initializes all fields. |
| 111 | `public static PerformancePayloadDefinition inline(String inlineBody)` | Create a payload definition that uses an inline body string. |
| 131 | `public static PerformancePayloadDefinition yaml(String yamlKey)` | Create a payload definition that resolves payload from a YAML resource using a key. |
| 154 | `public static PerformancePayloadDefinition csv(String filePath, String rowIdentifier, String columnName)` | Create a payload definition that resolves payload from a CSV file. |
| 179 | `public static PerformancePayloadDefinition excel(String filePath, String rowIdentifier, String columnName)` | Create a payload definition that resolves payload from an Excel file. |
| 202 | `public static PerformancePayloadDefinition excel(String filePath, String sheetName, String rowIdentifier, String columnName)` | Create a payload definition that resolves payload from a specific sheet in an Excel file. |
| 222 | `public PayloadSourceType getSourceType()` | Get the configured payload source type. |
| 231 | `public String getInlineBody()` | Get the inline body content. |
| 240 | `public String getYamlKey()` | Get the YAML lookup key. |
| 249 | `public String getFilePath()` | Get the file path for CSV/Excel sources. |
| 258 | `public String getSheetName()` | Get the Excel sheet name. |
| 267 | `public String getRowIdentifier()` | Get the row identifier used for CSV/Excel lookup. |
| 276 | `public String getColumnName()` | Get the column name/header used for CSV/Excel lookup. |

### `PerformancePayloadResolver` — `com.ptaf.performance.payloads`

Source: [`src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 59 | `public static String resolve(PerformancePayloadDefinition definition)` | Resolve the payload body for the provided {@link PerformancePayloadDefinition}. |
| 90 | `public static String resolveYaml(String yamlKey)` | Public wrapper for YAML payload resolution by key. |
| 115 | `public static String resolveCsv(String filePath, String rowIdentifier, String columnName)` | Public wrapper for CSV payload resolution. |
| 150 | `public static String resolveExcel(String filePath, String rowIdentifier, String columnName)` | Public wrapper for Excel payload resolution. |

### `PerformanceExcelFormatHelper` — `com.ptaf.performance.reports`

Source: [`src/main/java/com/ptaf/performance/reports/PerformanceExcelFormatHelper.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelFormatHelper.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 67 | `public static String safeText(String value)` | Safely normalize free-form text for display in reports. |
| 93 | `public static String safeOrDefault(String value, String defaultValue)` | Returns the normalized text or a default if the input is absent/empty. |
| 109 | `public static String formatPercent(double value)` | Format a numeric value as a percentage string with two decimal places and a trailing '%'. |
| 124 | `public static String formatDecimal(double value)` | Format a numeric value as a decimal string with two decimal places. |
| 151 | `public static String formatScore(double value)` | Format a numeric score with a preference for whole numbers where appropriate. |
| 172 | `public static String formatInteger(long value)` | Format a long integer with grouping separators (thousands). |
| 182 | `public static String formatInteger(int value)` | Format an int integer with grouping separators (thousands). |
| 199 | `public static String formatMillisecondsAsSeconds(long milliseconds)` | Convert milliseconds to a compact seconds string with appropriate precision: seconds >= 100 -> no decimals (e.g., "123 sec") 10 one decimal (e.g., "12.3 sec") seconds &lt; 10 -> two decimals (e.g., "1.23 sec") Negative inputs are clamped to 0. |
| 237 | `public static String formatMillisecondsDetailed(long milliseconds)` | Provide a detailed, human-readable representation of a millisecond duration. |
| 291 | `public static String formatDurationSeconds(double seconds)` | Format a duration given in seconds into a compact, human-readable string. |
| 325 | `public static String formatNullableMetric(String label, String value)` | Format a metric label and its value, displaying "N/A" when the value is missing/empty. |
| 349 | `public static String normalizeRiskLevel(String riskLevel)` | Normalize common risk level strings into a predictable display form. |
| 385 | `public static String normalizeExecutionStatus(String status)` | Normalize execution status strings into an uppercase, underscore-separated token. |
| 403 | `public static double safeDouble(double value)` | Returns a safe double value for reporting: NaN or Infinite values are converted to 0.0. |
| 416 | `public static long safeLong(long value)` | Ensure a long value is non-negative by clamping negative inputs to 0. |
| 426 | `public static int safeInt(int value)` | Ensure an int value is non-negative by clamping negative inputs to 0. |

### `PerformanceExcelReportWriter` — `com.ptaf.performance.reports`

Source: [`src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 75 | `public Path writeRunReport(PerformanceRunReport runReport)` | Create the Excel workbook and write all report sheets to disk. |

### `PerformanceReportManager` — `com.ptaf.performance.reports`

Source: [`src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 57 | `public void ensureRunRootExists(Path runRootPath)` | Ensures the run root folder exists. |
| 76 | `public void ensureScenarioRootExists(Path scenarioRootPath)` | Ensures the scenario root folder exists. |
| 96 | `public Path prepareDashboardPath(Path scenarioRootPath)` | Returns dashboard folder path inside a scenario folder and ensures it exists. |
| 120 | `public Path prepareJtlFilePath(Path scenarioRootPath)` | Returns JTL file path inside a scenario folder. |
| 143 | `public Path prepareSummaryFilePath(Path scenarioRootPath)` | Returns technical summary file path inside a scenario folder. |
| 165 | `public Path prepareReadableSummaryFilePath(Path scenarioRootPath)` | Returns readable summary file path inside a scenario folder. |
| 188 | `public Path prepareRunSummaryFilePath(Path runRootPath)` | Returns run-level technical aggregate summary path. |
| 211 | `public Path prepareReadableRunSummaryFilePath(Path runRootPath)` | Returns run-level readable aggregate summary path. |
| 234 | `public Path prepareRunIndexFilePath(Path runRootPath)` | Returns run-level index file path. |

### `PerformanceSummaryWriter` — `com.ptaf.performance.reports`

Source: [`src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 54 | `public void writeTextSummary(PerformanceExecutionResult executionResult)` | Write a technical text summary to the file path specified on the provided executionResult. |
| 91 | `public void writeReadableSummary(PerformanceExecutionResult executionResult)` | Write a readable (narrative) summary to the file path specified on the provided executionResult. |
| 137 | `protected String buildTextSummary(PerformanceExecutionResult executionResult)` | Build the technical (compact) summary as a single string. |
| 278 | `protected String buildReadableSummary(PerformanceExecutionResult executionResult)` | Build the readable (narrative) summary as a single string. |

### `PerformancePathResolver` — `com.ptaf.performance.utils`

Source: [`src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java`](../../../src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 58 | `public static String buildExecutionTimestamp()` | Builds a timestamp string used for the shared run folder. |
| 72 | `public static Path getReportsBaseDirectory()` | Returns the default base reports directory as a Path. |
| 87 | `public static Path buildRunRootPath(String runFolderName)` | Builds the shared run root folder path by resolving the given run folder name against the base reports directory. |
| 106 | `public static Path buildScenarioRootPath(Path runRootPath, int scenarioSequence, String testName)` | Builds a scenario folder path inside the run root folder. |
| 133 | `public static Path buildDashboardPath(Path scenarioRootPath)` | Builds dashboard folder path inside a scenario folder. |
| 149 | `public static Path buildJtlFilePath(Path scenarioRootPath)` | Builds JTL file path inside a scenario folder. |
| 164 | `public static Path buildSummaryFilePath(Path scenarioRootPath)` | Builds technical summary file path inside a scenario folder. |
| 178 | `public static Path buildReadableSummaryFilePath(Path scenarioRootPath)` | Builds readable summary file path inside a scenario folder. |
| 192 | `public static Path buildRunSummaryFilePath(Path runRootPath)` | Builds run-level technical summary file path. |
| 206 | `public static Path buildReadableRunSummaryFilePath(Path runRootPath)` | Builds run-level readable summary file path. |
| 220 | `public static Path buildRunIndexFilePath(Path runRootPath)` | Builds run-level index file path. |
| 242 | `public static String buildSafeScenarioName(String testName)` | Creates a safe scenario folder name from the test name. |

### `PerformanceSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 71 | `public void weRunGetPerformanceTest(String path, String testName)` | Run a simple GET performance test for the supplied path and store the result. |
| 96 | `public void weRunGetPerformanceTestWithCustomProfile(String path, String testName, int users, int rampUpSeconds, int holdSeconds)` | Run a GET performance test with a custom load profile (users, ramp-up, hold). |
| 141 | `public void weRunPostPerformanceTest(String path, String testName, String jsonBody)` | Run a POST performance test using inline JSON body. |
| 168 | `public void weRunPostPerformanceTestWithCustomProfile(String path, String testName, String jsonBody, int users, int rampUpSeconds, int holdSeconds)` | Run a POST performance test using inline JSON body with a custom profile. |
| 211 | `public void weRunPutPerformanceTest(String path, String testName, String jsonBody)` | Run a PUT performance test using inline JSON body. |
| 234 | `public void weRunPutPerformanceTestWithCustomProfile(String path, String testName, String jsonBody, int users, int rampUpSeconds, int holdSeconds)` | Run a PUT performance test with inline JSON body and custom profile parameters. |
| 274 | `public void weRunDeletePerformanceTest(String path, String testName)` | Run a DELETE performance test for the given path. |
| 294 | `public void weRunDeletePerformanceTestWithCustomProfile(String path, String testName, int users, int rampUpSeconds, int holdSeconds)` | Run DELETE performance test with custom load profile. |
| 337 | `public void weRunYamlDrivenPostPerformanceTest(String path, String testName, String yamlKey)` | Run a POST where the request body is resolved from YAML using a provided key. |
| 361 | `public void weRunYamlDrivenPutPerformanceTest(String path, String testName, String yamlKey)` | Run a PUT where the request body is resolved from YAML using the given key. |
| 396 | `public void weRunCsvDrivenPostPerformanceTest(String path, String testName, String csvFile, String rowIdentifier, String columnName)` | Run a POST where the request body is sourced from a CSV file. |
| 424 | `public void weRunCsvDrivenPutPerformanceTest(String path, String testName, String csvFile, String rowIdentifier, String columnName)` | Run a PUT where the body is taken from a CSV file cell. |
| 456 | `public void weRunExcelDrivenPostPerformanceTest(String path, String testName, String excelFile, String rowIdentifier, String columnName)` | Run a POST where the body is sourced from an Excel file cell. |
| 484 | `public void weRunExcelDrivenPutPerformanceTest(String path, String testName, String excelFile, String rowIdentifier, String columnName)` | Run a PUT where the body is sourced from an Excel file cell. |
| 518 | `public void weStoreBearerTokenAlias(String alias, String tokenValue)` | Store a bearer token in the engine's token store under an alias. |
| 530 | `public void weRunAuthenticatedGetPerformanceTest(String path, String testName, String tokenAlias)` | Run an authenticated GET using a stored bearer token alias. |
| 553 | `public void weRunAuthenticatedYamlDrivenPostPerformanceTest(String path, String testName, String yamlKey, String tokenAlias)` | Run an authenticated YAML-driven POST using a stored bearer token alias. |
| 584 | `public void weRunBasicAuthGetPerformanceTest(String path, String testName, String username, String password)` | Run a GET performance test using HTTP Basic Authentication. |
| 615 | `public void weRunGetPerformanceTestExpectingFailure(String path, String testName)` | Run a GET performance test that is expected to fail. |
| 634 | `public void weRunBasicAuthGetPerformanceTestExpectingFailure(String path, String testName, String username, String password)` | Run a GET with basic auth that is expected to fail. |
| 657 | `public void weRunYamlDrivenPostPerformanceTestExpectingFailure(String path, String testName, String yamlKey)` | Run a YAML-driven POST that is expected to fail. |
| 683 | `public void performanceResultShouldBeAvailable()` | Assert that a performance result object is available (i.e. |
| 693 | `public void performanceDashboardPathShouldBeGenerated()` | Assert that the engine generated a dashboard path for the last run. |
| 702 | `public void performanceSummaryFilePathShouldBeGenerated()` | Assert that the engine generated a summary file path for the last run. |
| 711 | `public void performanceReadableSummaryFilePathShouldBeGenerated()` | Assert that the engine generated a readable summary file path for the last run. |
| 720 | `public void performanceJtlFilePathShouldBeGenerated()` | Assert that the engine generated a JTL file path for the last run. |
| 729 | `public void performanceRunReportRootPathShouldBeGenerated()` | Assert that the engine generated a root path for the run report. |
| 740 | `public void performanceExcelReportShouldBeGenerated()` | Assert that the Excel report for the run was generated on disk. |
| 759 | `public void performanceExecutionShouldPass()` | Assert that the last performance execution passed its configured assertions. |
| 776 | `public void performanceExecutionShouldFail()` | Assert that the last performance execution failed (an actual failure was detected). |
| 791 | `public void performanceExecutionShouldBeInExpectedFailureMode()` | Assert that the engine was running in expected-failure mode for the last execution. |
| 805 | `public void performanceFailureMessageShouldContain(String expectedText)` | Assert that the failure message contains the provided text fragment. |
| 826 | `public void performanceAverageResponseTimeShouldBeLessThan(long maxAverageResponseTime)` | Validate that the average response time observed is less than the provided threshold. |
| 846 | `public void performanceP95ResponseTimeShouldBeLessThan(long maxP95ResponseTime)` | Validate that the 95th percentile response time (P95) is below the provided value. |
| 866 | `public void performanceErrorPercentageShouldBeLessThan(double maxErrorPercentage)` | Validate that the error percentage is less than the provided threshold. |
| 887 | `public void performanceErrorPercentageShouldBeGreaterThan(double minimumErrorPercentage)` | Validate that the error percentage is greater than the provided minimum. |
| 906 | `public void performanceTotalErrorsShouldBeGreaterThan(long expectedMinimumErrors)` | Validate the total number of errors observed exceeds a threshold. |
| 925 | `public void performanceTotalSamplesShouldBeGreaterThan(long expectedMinimumSamples)` | Validate that the total number of samples (requests) exceeds the provided threshold. |
| 944 | `public void performanceTotalScenarioDurationShouldBeGreaterThan(long minimumDurationMs)` | Validate that the total scenario duration is greater than the given millisecond value. |
| 968 | `public void performanceRiskScoreShouldBeGreaterThan(int minimumRiskScore)` | Validate the risk score computed in the smart report is greater than the provided minimum. |
| 987 | `public void performanceRiskScoreShouldBeLessThan(int maximumRiskScore)` | Validate the risk score is less than the provided maximum. |
| 1006 | `public void performanceRiskLevelShouldBe(String expectedRiskLevel)` | Validate the textual risk level equals the expected value (case-insensitive). |
| 1023 | `public void performanceThresholdBreachSummaryShouldContain(String expectedText)` | Validate that the threshold breach summary contains the provided text. |
| 1040 | `public void performanceRecommendedActionShouldContain(String expectedText)` | Validate that the recommended action text contains the provided fragment. |

### `UiPerformanceSteps` — `com.ptaf.ui_performance.stepdefinitions`

Source: [`src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`](../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 27 | `public void createJourneyUsingConfiguredTarget(String journeyName)` | Starts a journey using the protocol, host, port, and base path from isolated YAML configuration. |
| 36 | `public void createJourney(String journeyName, String baseUrl)` | Retained for backward compatibility with the first UI performance template. |
| 42 | `public void addConfiguredNavigation(String routeName)` | Adds navigation using a named, relative route from the isolated UI performance YAML. |
| 50 | `public void addNavigation(String route)` | Retained for backward compatibility; new features should navigate through a configured route name. |
| 56 | `public void addDataFill(String locatorGroup, String locatorKey, String dataField)` | Adds a field-fill step whose value is read only from the separate CSV data column at runtime. |
| 65 | `public void addDataSelection(String locatorGroup, String locatorKey, String dataField)` | Adds a select-list step whose visible option label comes from the isolated CSV row. |
| 74 | `public void addLiteralFill(String locatorGroup, String locatorKey, String value)` | Adds a non-sensitive literal fill. |
| 89 | `public void addClick(String locatorGroup, String locatorKey)` | Adds a click step resolved only from the separate regular-style UI performance locator YAML. |
| 98 | `public void addPopupClick(String locatorGroup, String locatorKey)` | Adds an action that clicks a configured locator and continues the journey in its popup page. |
| 107 | `public void addVisibilityCheck(String locatorGroup, String locatorKey)` | Adds a visible-state validation step resolved only from the separate regular-style locator YAML. |
| 116 | `public void executeJourney()` | Executes all configured concurrent browser users and writes standalone performance reports. |
| 122 | `public void verifyPerformanceReport()` | Verifies that a completed run was collected; configured performance thresholds are enforced during execution. |
| 128 | `public void verifyReport()` | Retained for the initial feature wording. |

## Database

### `DatabaseHandler` — `com.ptaf.db.handlers`

Source: [`src/main/java/com/ptaf/db/handlers/DatabaseHandler.java`](../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 85 | `public static Connection getConnection() throws SQLException` | Returns the active database connection for the current execution thread. |
| 121 | `public static void closeConnection()` | Closes the database connection for the current thread and removes it from ThreadLocal. |

### `DatabaseActionImpl` — `com.ptaf.db.implementation`

Source: [`src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java`](../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 51 | `public DatabaseActionImpl()` | Creates DatabaseActionImpl with the default SQL performer. |
| 74 | `public List<Map<String, Object>> performQuery(String queryKey, Object... params)` | Executes a SELECT query identified by a logical YAML query key. |
| 107 | `public int performUpdate(String queryKey, Object... params)` | Executes an INSERT, UPDATE, or DELETE statement identified by a logical YAML query key. |
| 139 | `public boolean recordExists(String queryKey, Object... params)` | Verifies whether at least one record exists for the given SELECT query. |
| 161 | `public Map<String, Object> getSingleRecord(String queryKey, Object... params)` | Retrieves a single database record for the given query key. |
| 202 | `public Object getSingleValue(String queryKey, Object... params)` | Retrieves a single value from the first column of the first row returned by a query. |

### `DatabaseAction` — `com.ptaf.db.interfaces`

Source: [`src/main/java/com/ptaf/db/interfaces/DatabaseAction.java`](../../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `DatabaseCommonMethods` — `com.ptaf.db.pages`

Source: [`src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java`](../../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 71 | `public DatabaseCommonMethods()` | Default constructor that initializes the DatabaseCommonMethods class with the framework's default DatabaseAction implementation. |
| 87 | `public List<Map<String, Object>> getRecords(String queryKey, Object... params)` | Retrieves multiple records from the database using a logical SQL query key. |
| 119 | `public Map<String, Object> getSingleRecord(String queryKey, Object... params)` | Retrieves a single database record using a logical SQL query key. |
| 150 | `public Object getSingleValue(String queryKey, Object... params)` | Retrieves a single database value from the first column of the first row. |
| 172 | `public void verifyRecordExists(String queryKey, Object... params)` | Verifies that at least one record exists for the given query key and parameters. |
| 202 | `public void verifyRecordDoesNotExist(String queryKey, Object... params)` | Verifies that no records exist for the given query key and parameters. |
| 233 | `public void verifyRowsAffected(int expectedRowsAffected, String queryKey, Object... params)` | Executes an INSERT, UPDATE, or DELETE statement and validates the affected row count. |
| 274 | `public int executeUpdate(String queryKey, Object... params)` | Executes an INSERT, UPDATE, or DELETE statement and returns the affected row count. |

### `DatabaseActionPerformer` — `com.ptaf.db.performer`

Source: [`src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java`](../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 75 | `public DatabaseActionPerformer()` | Public constructor kept for compatibility with DatabaseActionImpl. |
| 102 | `public List<Map<String, Object>> executeQuery(Connection connection, String sql, List<Object> params) throws SQLException` | Executes a SELECT query and returns the results as a list of maps. |
| 189 | `public int executeUpdate(Connection connection, String sql, List<Object> params) throws SQLException` | Executes an INSERT, UPDATE, or DELETE SQL statement. |

### `DatabaseConnectionValidator` — `com.ptaf.db.validators`

Source: [`src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java`](../../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 86 | `public static boolean isDatabaseConnectionSuccessful()` | Validates that a SQL Server database connection can be opened successfully. |
| 169 | `public static void assertDatabaseConnectionSuccessful()` | Validates the database connection and throws an AssertionError when the connection is not successful. |

### `DatabaseHooks` — `com.ptaf.hooks`

Source: [`src/main/java/com/ptaf/hooks/DatabaseHooks.java`](../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 81 | `public void closeDatabaseConnectionAfterScenario(Scenario scenario)` | Closes the active database connection after each database scenario. |

### `DatabaseSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 61 | `public DatabaseSteps()` | Creates DatabaseSteps with the default DB common methods implementation. |
| 81 | `public void i_validate_the_database_connection_is_successful()` | Validates that the framework can successfully connect to the configured SQL Server database. |
| 98 | `public void the_database_does_not_contain_a_record_for_query(String queryKey, String params)` | Verifies that the database does not contain a matching record before test execution. |
| 115 | `public void i_verify_the_database_contains_a_record_for_query_with_parameters(String queryKey, String params)` | Verifies that the database contains at least one matching record. |
| 127 | `public void i_verify_the_database_does_not_contain_a_record_for_query_with_parameters(String queryKey, String params)` | Verifies that the database does not contain any matching record. |
| 139 | `public void i_insert_a_new_record_using_query_with_parameters(String queryKey, String params)` | Executes an INSERT statement and verifies that one row was inserted. |
| 151 | `public void i_update_a_record_using_query_with_parameters(String queryKey, String params)` | Executes an UPDATE statement and verifies that one row was updated. |
| 164 | `public void i_delete_records_using_query_with_parameters(int expectedRows, String queryKey, String params)` | Executes a DELETE statement and verifies the expected number of affected rows. |
| 188 | `public void i_verify_single_database_value_for_query_with_parameters_equals( String queryKey, String params, String expectedValue )` | Verifies that a single database value matches the expected value. |
| 222 | `public void i_execute_database_update_query_with_parameters_then_rows_should_be_affected( String queryKey, String params, int expectedRows )` | Executes an update statement and verifies the expected affected row count. |
| 246 | `public void i_verify_database_record_for_query_with_parameters_contains( String queryKey, String params, DataTable dataTable )` | Verifies returned column values from the first database record using a Cucumber DataTable. |

## Mobile and mobile browser

### `MobileAssert` — `com.ptaf.mobile.assertions`

Source: [`src/main/java/com/ptaf/mobile/assertions/MobileAssert.java`](../../../src/main/java/com/ptaf/mobile/assertions/MobileAssert.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 56 | `public static void assertVisible(String page, String locator)` | Asserts that the element identified by the given page and locator is visible. |
| 83 | `public static void assertNotVisible(String page, String locator)` | Asserts that the element identified by the given page and locator is not visible. |
| 110 | `public static void assertEnabled(String page, String locator)` | Asserts that the element identified by the given page and locator is enabled (interactable). |
| 141 | `public static void assertTextEquals(String page, String locator, String expected)` | Asserts that the text of the element identified by the given page and locator equals the expected value. |
| 173 | `public static void assertTextContains(String page, String locator, String expected)` | Asserts that the text of the element identified by the given page and locator contains the expected substring. |

### `MobileConfigurationProperties` — `com.ptaf.mobile.config`

Source: [`src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java`](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 48 | `public static boolean isEnabled()` | Whether mobile automation is enabled at all. |
| 55 | `public static String getAppiumServerUrl()` | The Appium server URL that tests should connect to. |
| 65 | `public static MobilePlatform getDefaultPlatform()` | The default mobile platform to use when none is explicitly specified. |
| 72 | `public static int getExplicitWaitSeconds()` | Default explicit wait timeout used by the framework when waiting for elements/conditions. |
| 81 | `public static int getImplicitWaitSeconds()` | Default implicit wait timeout configured on the driver. |
| 89 | `public static int getNewCommandTimeoutSeconds()` | New command timeout passed to the Appium driver to determine how long the session will remain alive without new commands. |
| 102 | `public static String getCapability(MobilePlatform platform, String capabilityName, String defaultValue)` | Fetches a string capability value for a given platform from the mobile configuration. |
| 117 | `public static boolean getCapabilityBoolean(MobilePlatform platform, String capabilityName, boolean defaultValue)` | Fetches a boolean capability value for a given platform from the mobile configuration. |
| 130 | `public static String getOrientation(MobilePlatform platform)` | Retrieves the desired screen orientation for the given platform. |
| 139 | `public static String getEvidenceOutputDirectory()` | Directory to write mobile evidence (screenshots, videos) into. |
| 146 | `public static boolean screenshotOnFailure()` | Whether to take a screenshot automatically when a test fails. |
| 153 | `public static boolean screenshotOnPass()` | Whether to take a screenshot automatically when a test passes. |
| 160 | `public static boolean screenshotAfterEachScenario()` | Whether to take a screenshot after each scenario (Cucumber). |
| 167 | `public static boolean attachScreenshotsToReport()` | Whether to attach captured screenshots to the test report. |
| 174 | `public static boolean videoRecordingEnabled()` | Whether video recording is enabled for mobile test sessions. |
| 181 | `public static boolean videoOnFailureOnly()` | Whether video recording should only be saved when a test fails. |
| 188 | `public static boolean attachVideoToReport()` | Whether captured video should be attached to the test report. |
| 199 | `public static int getPermissionPopupTimeoutSeconds()` | Timeout used only for optional permission/system-dialog checks. |
| 208 | `public static int getPermissionMaxPopups()` | Maximum number of permission dialogs that the framework should attempt to handle in a single loop. |
| 215 | `public static boolean capturePermissionEvidence()` | When true, explicit permission handling steps capture before/after evidence and attach it to the Cucumber report. |
| 233 | `public static boolean isBrowserModeEnabled()` | Determines whether Appium should run in real mobile browser mode for browser tests. |
| 258 | `public static String getBrowserCapability(MobilePlatform platform, String capabilityName, String defaultValue)` | Returns browser capability values with a split-config-first lookup strategy. |
| 275 | `public static boolean getBrowserCapabilityBoolean(MobilePlatform platform, String capabilityName, boolean defaultValue)` | Returns boolean browser capability values with the same split-config-first fallback behavior as {@link #getBrowserCapability(MobilePlatform, String, String)}. |

### `MobilePlatform` — `com.ptaf.mobile.config`

Source: [`src/main/java/com/ptaf/mobile/config/MobilePlatform.java`](../../../src/main/java/com/ptaf/mobile/config/MobilePlatform.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 64 | `public static MobilePlatform from(String value)` | Convert a string value to the corresponding {@link MobilePlatform} enum. |
| 92 | `public boolean isAndroid()` | Convenience check to determine if this platform instance represents Android. |
| 99 | `public boolean isIos()` | Convenience check to determine if this platform instance represents iOS. |

### `MobileYamlReader` — `com.ptaf.mobile.config`

Source: [`src/main/java/com/ptaf/mobile/config/MobileYamlReader.java`](../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 198 | `public static Object get(String key)` | Retrieve a value from the merged YAML configuration using dot-separated keys. |
| 228 | `public static String getString(String key, String defaultValue)` | Convenience accessor that returns a String representation of the value at the key. |
| 243 | `public static boolean getBoolean(String key, boolean defaultValue)` | Convenience accessor that returns a boolean value for the given key. |
| 259 | `public static int getInt(String key, int defaultValue)` | Convenience accessor that returns an int value for the given key. |

### `MobileDriverFactory` — `com.ptaf.mobile.drivers`

Source: [`src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java`](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 63 | `public static AppiumDriver createDriver(MobilePlatform platform)` | Create and return an AppiumDriver for a native mobile application (not browser). |
| 221 | `public static AppiumDriver createBrowserDriver(MobilePlatform platform)` | Create an AppiumDriver for browser automation on mobile (Chrome on Android or Safari on iOS). |

### `MobileDriverManager` — `com.ptaf.mobile.drivers`

Source: [`src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java`](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 70 | `public static AppiumDriver startDriver(MobilePlatform platform)` | Start a new Appium driver for a native mobile application on the specified platform. |
| 99 | `public static AppiumDriver startBrowserDriver(MobilePlatform platform)` | Start a new Appium driver configured to work with a mobile browser on the specified platform. |
| 122 | `public static boolean isBrowserSession()` | Returns whether the current thread's session was created as a browser session. |
| 130 | `public static AppiumDriver getDriver()` | Retrieve the Appium driver associated with the current thread. |
| 145 | `public static boolean hasDriver()` | Convenience check to determine if a driver exists for the current thread. |
| 152 | `public static MobilePlatform getPlatform()` | Retrieve the MobilePlatform associated with the current thread's driver. |
| 164 | `public static void closeDriver()` | Close and clean up the Appium driver associated with the current thread. |

### `MobileEvidenceManager` — `com.ptaf.mobile.evidence`

Source: [`src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java`](../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 75 | `public static void setCurrentScenario(Scenario scenario)` | Sets the current Cucumber Scenario for the calling thread. |
| 81 | `public static void clearCurrentScenario()` | Clears the current thread's associated Scenario. |
| 88 | `public static Scenario getCurrentScenario()` | Returns the Scenario associated with the current thread, or null if none. |
| 98 | `public static void startVideoIfEnabled(AppiumDriver driver)` | Starts native video recording on the provided Appium driver if video recording is enabled in configuration and if the driver supports the CanRecordScreen capability. |
| 130 | `public static void captureScenarioScreenshotIfConfigured(AppiumDriver driver, Scenario scenario)` | Captures a screenshot after a scenario finishes depending on configuration and scenario outcome. |
| 157 | `public static void captureAssertionFailureScreenshot(AppiumDriver driver, String failureName, String details)` | Captures a screenshot immediately when an assertion fails. |
| 232 | `public static void captureNamedScreenshot(AppiumDriver driver, String screenshotName)` | Explicitly captures a named screenshot and attaches it to the active Cucumber Scenario when possible. |
| 277 | `public static void stopVideoIfEnabled(AppiumDriver driver, Scenario scenario)` | Stops native screen recording (if enabled) on the provided driver and persists/attaches the video depending on configuration and scenario outcome. |

### `MobileLocatorHandler` — `com.ptaf.mobile.handlers`

Source: [`src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java`](../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 52 | `public By getLocatorForType(String locatorValue)` | Resolve a human-friendly PTAF locator string into a Selenium/Appium By locator. |

### `MobileActionImpl` — `com.ptaf.mobile.implementation`

Source: [`src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java`](../../../src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 46 | `public void tap(String page, String locator)` | Tap (single tap) on an element specified by page and locator. |
| 58 | `public void type(String page, String locator, String value)` | Type text into an input element identified by page and locator. |
| 69 | `public void clear(String page, String locator)` | Clear the contents of an input element. |
| 81 | `public String getText(String page, String locator)` | Retrieve the visible text of an element. |
| 92 | `public void waitForVisible(String page, String locator)` | Wait until the specified element is visible using default timeout from configuration. |
| 104 | `public void waitForVisible(String page, String locator, int timeoutSeconds)` | Wait until the specified element is visible with a custom timeout. |
| 116 | `public void waitForNotVisible(String page, String locator, int timeoutSeconds)` | Wait until the specified element is not visible (hidden or removed) within the timeout. |
| 126 | `public void pause(int seconds)` | Pause execution for a given number of seconds. |
| 138 | `public boolean isVisible(String page, String locator)` | Check whether an element is currently visible. |
| 150 | `public boolean isEnabled(String page, String locator)` | Check whether an element is enabled (interactable). |
| 162 | `public boolean isSelected(String page, String locator)` | Check whether an element is selected (for selectable elements like checkboxes). |
| 174 | `public void longPress(String page, String locator, long durationMillis)` | Perform a long-press gesture on an element for a specified duration. |
| 185 | `public void doubleTap(String page, String locator)` | Perform a double-tap gesture on an element. |
| 196 | `public void tapAt(int x, int y)` | Tap at absolute screen coordinates. |
| 209 | `public void drag(String fromPage, String fromLocator, String toPage, String toLocator)` | Drag an element from one locator to another (within possibly different pages). |
| 221 | `public void scrollUntilVisible(String page, String locator, int maxSwipes)` | Scroll repeatedly until a specific element becomes visible or maxSwipes is exhausted. |
| 231 | `public void scrollToText(String text)` | Scroll the view until the specified text is visible. |
| 239 | `public void hideKeyboard()` | Hide the on-screen keyboard if it is present. |
| 249 | `public void backgroundApp(int seconds)` | Send the application to background for a specified number of seconds. |
| 257 | `public void swipeUp()` | Perform an upward swipe gesture (commonly used for scrolling down content). |
| 265 | `public void swipeDown()` | Perform a downward swipe gesture (commonly used for scrolling up content). |
| 273 | `public void swipeLeft()` | Perform a leftward swipe gesture. |
| 281 | `public void swipeRight()` | Perform a rightward swipe gesture. |
| 289 | `public void pinchIn()` | Perform a pinch-in gesture (zoom out). |
| 297 | `public void zoomOut()` | Perform a zoom-out gesture (pinch-out). |
| 308 | `public void setOrientation(String orientation)` | Set the device orientation. |
| 319 | `public void setConfiguredOrientation()` | Set device orientation based on the test framework's configured default. |
| 329 | `public void activateApp(String appId)` | Activate (bring to foreground) the application identified by the given appId. |
| 339 | `public void terminateApp(String appId)` | Terminate the application identified by the given appId. |
| 350 | `public void openDeepLink(String url, String appPackageOrBundleId)` | Open a deep link URL for the specified application. |
| 361 | `public void pushFile(String remotePath, String localPath)` | Push a local file to the device/emulator at the specified remote path. |
| 372 | `public void pullFile(String remotePath, String localOutputPath)` | Pull a file from the device/emulator to the local output path. |
| 382 | `public void setClipboard(String text)` | Set device clipboard content to the provided text. |
| 392 | `public String getClipboard()` | Retrieve the current content of the device clipboard as text. |
| 402 | `public Set<String> getContexts()` | Get the set of available contexts (e.g., NATIVE_APP, WEBVIEW_*). |
| 412 | `public void switchContext(String contextName)` | Switch to a specific context by name (useful for webview/native transitions). |
| 420 | `public void switchToNativeContext()` | Convenience method to switch back to the native application context. |
| 431 | `public void grantPermission(String appId, String permission)` | Grant a runtime permission to the specified application (platform-dependent). |
| 442 | `public void revokePermission(String appId, String permission)` | Revoke a runtime permission from the specified application. |
| 452 | `public void openUrl(String url)` | Open a URL in the device's default browser or in a webview, depending on configuration. |
| 463 | `public void pressEnter(String page, String locator)` | Simulate pressing the Enter key on a specific element (useful to submit forms). |
| 473 | `public String getCurrentUrl()` | Get the current URL from a webview context. |
| 483 | `public String getTitle()` | Get the current page title from a webview context. |
| 493 | `public void savePageSource(String outputPath)` | Save the current page source to a local file for debugging or analysis. |

### `MobileAction` — `com.ptaf.mobile.interfaces`

Source: [`src/main/java/com/ptaf/mobile/interfaces/MobileAction.java`](../../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `MobileCommonMethods` — `com.ptaf.mobile.pages`

Source: [`src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java`](../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 58 | `public MobileCommonMethods(AppiumDriver driver)` | Create a wrapper around an AppiumDriver to expose reusable mobile actions. |
| 70 | `public void tap(String page, String locator)` | Tap a visible element identified by page/locator keys. |
| 80 | `public void type(String page, String locator, String value)` | Type into an input element after clearing it first. |
| 89 | `public void clear(String page, String locator)` | Clear the input field's current value. |
| 98 | `public String getText(String page, String locator)` | Return visible element text. |
| 108 | `public boolean isVisible(String page, String locator)` | Check if an element is visible on screen. |
| 119 | `public boolean isEnabled(String page, String locator)` | Check if an element is enabled (interactable). |
| 128 | `public boolean isSelected(String page, String locator)` | Check if an element is selected (useful for checkboxes/radio buttons). |
| 137 | `public void waitForVisible(String page, String locator)` | Explicitly wait until element is visible. |
| 151 | `public void waitForVisible(String page, String locator, int timeoutSeconds)` | Waits for a locator using an explicit timeout supplied from the feature file. |
| 176 | `public void waitForNotVisible(String page, String locator, int timeoutSeconds)` | Waits until a locator disappears or becomes invisible. |
| 199 | `public void pause(int seconds)` | Pauses execution for a given number of seconds. |
| 210 | `public void longPress(String page, String locator, long durationMillis)` | Long press (press-and-hold) on element center for a given duration. |
| 223 | `public void doubleTap(String page, String locator)` | Double-tap on an element by performing two quick tap actions at the element's center. |
| 239 | `public void tapAt(int x, int y)` | Single touch tap at an (x,y) coordinate in the viewport. |
| 257 | `public void drag(String fromPage, String fromLocator, String toPage, String toLocator)` | Drag one element to another element's center. |
| 276 | `public void scrollUntilVisible(String page, String locator, int maxSwipes)` | Scroll repeatedly (swipe up) until a target element becomes visible or until maxSwipes is reached. |
| 292 | `public void scrollToText(String text)` | Scroll to an element by visible text. |
| 308 | `public void hideKeyboard()` | Hide the on-screen keyboard if the driver supports it. |
| 315 | `public void backgroundApp(int seconds)` | Send app to background for a number of seconds. |
| 324 | `public void activateApp(String appId)` | Activate another app on the device by bundleId/package. |
| 334 | `public void terminateApp(String appId)` | Terminate another app on the device by bundleId/package. |
| 345 | `public void openDeepLink(String url, String appPackageOrBundleId)` | Open a deep link into a target application. |
| 360 | `public void pushFile(String remotePath, String localPath)` | Push a local file into the device/simulator at a remote path. |
| 376 | `public void pullFile(String remotePath, String localOutputPath)` | Pull a file from the device to a local output path. |
| 396 | `public void setClipboard(String text)` | Set plaintext clipboard on the device using Appium mobile command. |
| 405 | `public String getClipboard()` | Retrieve plaintext clipboard from the device. |
| 419 | `public Set<String> getContexts()` | Retrieve available automation contexts (e.g., NATIVE_APP, WEBVIEW_...) from the driver. |
| 432 | `public void switchContext(String contextName)` | Switch driver context (reflective invocation of driver.context(name)). |
| 441 | `public void switchToNativeContext()` | Convenience method to return to native context. |
| 444 | `public void grantPermission(String appId, String permission)` | Grant a runtime permission on Android using Appium changePermissions extension. |
| 449 | `public void revokePermission(String appId, String permission)` | Revoke a runtime permission on Android using Appium changePermissions extension. |
| 458 | `public void setOrientation(String orientation)` | Set device orientation. |
| 467 | `public void setConfiguredOrientation()` | Set orientation based on configuration properties for the current platform. |
| 484 | `public void openUrl(String url)` | Open a URL in a real mobile browser session. |
| 515 | `public void pressEnter(String page, String locator)` | Press Enter/Return key on a focused element identified by page/locator. |
| 520 | `public String getCurrentUrl()` | Get the current browser URL; ensures web context readiness first. |
| 523 | `public String getTitle()` | Get the current browser title; ensures web context readiness first. |
| 530 | `public void savePageSource(String outputPath)` | Save the current browser page source to a local file. |
| 790 | `public File takeScreenshot()` | Take a screenshot and return it as a File object. |
| 793 | `public void swipeUp()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 794 | `public void swipeDown()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 795 | `public void swipeLeft()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 796 | `public void swipeRight()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 797 | `public void pinchIn()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 798 | `public void zoomOut()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 824 | `public WebElement findVisibleElement(String page, String locator)` | Find a visible element using configured mobile locators and a smart two-phase explicit wait. |
| 908 | `public By resolveLocator(String page, String locator)` | Resolve a textual locator key into a Selenium By object. |

### `MobilePermissionHandler` — `com.ptaf.mobile.permissions`

Source: [`src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java`](../../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 55 | `public MobilePermissionHandler(AppiumDriver driver)` | Construct a permission handler bound to the provided Appium driver. |
| 70 | `public boolean allowIfDisplayed()` | Attempts to allow the currently displayed permission popup. |
| 81 | `public boolean denyIfDisplayed()` | Attempts to deny the currently displayed permission popup. |
| 94 | `public boolean allowWithTextIfDisplayed(String text)` | Attempts to click a permission/system-dialog button containing the requested visible text. |
| 107 | `public int allowAllIfDisplayed()` | Handles multiple permission popups in sequence. |
| 123 | `public int denyAllIfDisplayed()` | Handles multiple deny-style permission popups in sequence. |
| 149 | `public boolean handleIfDisplayed(String action, String text, int timeoutSeconds)` | Core handler that attempts to find and click a permission/system-dialog button. |

### `MobileBrowserEvidenceManager` — `com.ptaf.ui.mobilebrowser`

Source: [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 77 | `public static void captureScenarioScreenshotIfConfigured(Page page, Scenario scenario, String browserName)` | Capture a screenshot for the given Cucumber scenario if the current configuration and state demand it. |

### `MobileBrowserExecutionConfig` — `com.ptaf.ui.mobilebrowser`

Source: [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 61 | `public static boolean isEnabled()` | Whether the mobile-browser emulation features are enabled. |
| 74 | `public static String getOrientationMode()` | Returns the orientation mode used for the emulated device. |
| 84 | `public static String getEvidenceOutputDirectory()` | Directory where evidence artifacts (screenshots, videos) should be written. |
| 94 | `public static boolean screenshotOnFailure()` | Whether a screenshot should be taken whenever a test scenario fails. |
| 104 | `public static boolean screenshotOnPass()` | Whether a screenshot should be taken when a test scenario passes. |
| 114 | `public static boolean screenshotAfterEachScenario()` | Whether a screenshot should be taken after every scenario regardless of outcome. |
| 124 | `public static boolean attachScreenshotsToReport()` | Whether captured screenshots (evidence) should be attached to the test report. |
| 134 | `public static boolean videoRecordingEnabled()` | Whether video recording is enabled for scenarios. |
| 144 | `public static int getVideoSizeWidth()` | Video recording width in pixels. |
| 154 | `public static int getVideoSizeHeight()` | Video recording height in pixels. |
| 164 | `public static boolean visualEnabled()` | Whether visual regression checks are enabled. |
| 174 | `public static String getVisualBaselineDirectory()` | Directory that contains baseline images used for visual comparisons. |
| 184 | `public static String getVisualOutputDirectory()` | Directory where visual comparison output (diffs, artifacts) should be written. |
| 201 | `public static double getVisualMismatchThresholdPercent()` | Threshold for visual mismatch expressed as a percentage. |
| 211 | `public static boolean createBaselineIfMissing()` | Whether a missing visual baseline image should be created automatically. |
| 221 | `public static boolean attachVisualArtifactsToReport()` | Whether visual artifacts (diff images, comparison outputs) should be attached to the report. |

### `MobileBrowserProfile` — `com.ptaf.ui.mobilebrowser`

Source: [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 68 | `public MobileBrowserProfile(String name, String browserEngine, int viewportWidth, int viewportHeight, int screenWidth, int screenHeight, double deviceScaleFactor, boolean mobile, boolean touch, String userAgent, String platform, String deviceCategory, String orientation)` | Create a new immutable MobileBrowserProfile holding all required emulation parameters. |
| 95 | `public String getName()` | @return Human-readable profile name (may be null). |
| 101 | `public String getBrowserEngine()` | @return Browser engine string supplied at construction (may be null). |
| 106 | `public int getViewportWidth()` | @return Viewport width in CSS pixels. |
| 111 | `public int getViewportHeight()` | @return Viewport height in CSS pixels. |
| 116 | `public int getScreenWidth()` | @return Full device screen width in physical pixels. |
| 121 | `public int getScreenHeight()` | @return Full device screen height in physical pixels. |
| 126 | `public double getDeviceScaleFactor()` | @return Device pixel ratio (DPR) such as 1.0, 2.0. |
| 132 | `public boolean isMobile()` | @return True if the profile represents a mobile device. |
| 138 | `public boolean hasTouch()` | @return True if the device supports touch input. |
| 143 | `public String getUserAgent()` | @return User agent string to present to the web application under test (may be null). |
| 148 | `public String getPlatform()` | @return Platform string such as "iOS" or "Android" (may be null). |
| 153 | `public String getDeviceCategory()` | @return Device category such as "phone" or "tablet" (may be null). |
| 158 | `public String getOrientation()` | @return Orientation direction such as "portrait" or "landscape" (may be null). |
| 168 | `public boolean usesChromium()` | Convenience predicate: returns true if the configured browser engine is Chromium. |
| 178 | `public boolean usesWebKit()` | Convenience predicate: returns true if the configured browser engine is WebKit. |
| 188 | `public boolean usesFirefox()` | Convenience predicate: returns true if the configured browser engine is Firefox. |

### `MobileBrowserProfileRepository` — `com.ptaf.ui.mobilebrowser`

Source: [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 47 | `public static boolean isMobileBrowserProfile(String profileName)` | Checks whether the given profile name corresponds to a defined mobile browser profile. |
| 65 | `public static Optional<MobileBrowserProfile> findByName(String profileName)` | Finds a mobile browser profile by name and, if present, converts it into a {@link MobileBrowserProfile} object. |
| 86 | `public static Map<String, MobileBrowserProfile> getAllProfiles()` | Reads all mobile browser profiles from the YAML and returns them as a LinkedHashMap preserving the YAML iteration order. |

### `MobileBrowserVisualValidator` — `com.ptaf.ui.mobilebrowser`

Source: [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 82 | `public static void compareCurrentPage(Page page, Scenario scenario, String baselineName, String browserProfileName)` | Capture the current page screenshot, compare it to a baseline image, and handle artifacts and assertions. |

### `MobileBrowserYamlReader` — `com.ptaf.ui.mobilebrowser`

Source: [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `MobileBrowserVisualSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 51 | `public void captureScenario(Scenario scenario)` | Cucumber hook that runs before each scenario to capture the current Scenario object. |
| 73 | `public void iCompareMobileBrowserPageWithVisualBaseline(String baselineName)` | Step definition that triggers a visual comparison of the current mobile browser page against a named visual baseline. |

### `MobileSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/MobileSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 77 | `public void iStartMobileApplicationUsingPlatform(String platform)` | Start native mobile application driver for the specified platform. |
| 93 | `public void iStartMobileBrowserUsingPlatform(String platform)` | Start a mobile browser session (web context) for the specified platform. |
| 104 | `public void iOpenMobileBrowserUrl(String url)` | Open the given URL in the mobile browser context. |
| 116 | `public void iPressEnterOnMobilePageLocator(String page, String locator)` | Press the Enter key on an element found via page and locator. |
| 127 | `public void iSaveMobileBrowserPageSourceTo(String outputPath)` | Save the current mobile browser page source to the provided path. |
| 139 | `public void mobileBrowserCurrentUrlShouldContain(String expected)` | Assert that the mobile browser's current URL contains the expected substring. |
| 156 | `public void mobileBrowserTitleShouldContain(String expected)` | Assert that the mobile browser's title contains the expected substring, case-insensitive. |
| 171 | `public void iTapOnMobilePageLocator(String page, String locator)` | Tap (single press) on a mobile element identified by page and locator. |
| 183 | `public void iLongPressMobileElement(String page, String locator, int durationMillis)` | Long press (press and hold) on a mobile element for a specified duration. |
| 194 | `public void iDoubleTapMobileElement(String page, String locator)` | Perform a double tap on a mobile element. |
| 205 | `public void iTapMobileScreenAt(int x, int y)` | Tap a specific coordinate on the mobile screen. |
| 218 | `public void iDragMobileElement(String fromPage, String fromLocator, String toPage, String toLocator)` | Drag an element from one locator to another. |
| 230 | `public void iScrollMobileElementIntoView(String page, String locator, int maxSwipes)` | Scroll until the element becomes visible or the maximum number of swipes is reached. |
| 240 | `public void iScrollMobileScreenToText(String text)` | Scroll the mobile screen until the provided text is visible. |
| 252 | `public void iEnterMobileValueOnPageLocator(String value, String page, String locator)` | Enter text into a mobile input element found by page and locator. |
| 263 | `public void iClearMobilePageLocator(String page, String locator)` | Clear the content of a mobile element (e.g., input field). |
| 271 | `public void iHideMobileKeyboard()` | Attempt to hide the on-screen mobile keyboard if it is displayed. |
| 281 | `public void iBackgroundMobileAppForSeconds(int seconds)` | Send the app to background for the specified number of seconds. |
| 289 | `public void iSwipeMobileScreenUp()` | Swipe up on the mobile screen (useful for scrolling). |
| 297 | `public void iSwipeMobileScreenDown()` | Swipe down on the mobile screen (useful for scrolling). |
| 305 | `public void iSwipeMobileScreenLeft()` | Swipe left on the mobile screen (often used for carousel navigation). |
| 313 | `public void iSwipeMobileScreenRight()` | Swipe right on the mobile screen (often used for carousel navigation). |
| 321 | `public void iPinchInMobileScreen()` | Perform a pinch-in gesture on the mobile screen (zoom out). |
| 329 | `public void iZoomOutMobileScreen()` | Perform a zoom-out gesture on the mobile screen (zoom in). |
| 339 | `public void iRotateMobileScreenTo(String orientation)` | Set the device orientation using the provided orientation string. |
| 347 | `public void iRotateMobileScreenUsingConfiguredOrientation()` | Rotate the device using a pre-configured orientation from test configuration. |
| 357 | `public void iActivateMobileApp(String appId)` | Activate a different mobile application by its application identifier. |
| 367 | `public void iTerminateMobileApp(String appId)` | Terminate a mobile application identified by its application identifier. |
| 378 | `public void iOpenMobileDeepLink(String url, String appId)` | Open a deep link URL associated with a specific application. |
| 389 | `public void iPushLocalFileToMobilePath(String localPath, String remotePath)` | Push a local file from the test machine to a path on the mobile device. |
| 401 | `public void iPullMobileFileToLocalPath(String remotePath, String localPath)` | Pull a file from the mobile device to the local test machine. |
| 411 | `public void iSetMobileClipboardText(String text)` | Set the mobile device clipboard content to the provided text. |
| 421 | `public void iSwitchMobileContextTo(String contextName)` | Switch the mobile driver's context to the provided context name (for example WEBVIEW_x). |
| 429 | `public void iSwitchMobileContextToNativeApp()` | Switch the mobile driver's context back to the native application. |
| 440 | `public void iGrantMobilePermissionForApp(String permission, String appId)` | Grant a specific runtime permission for the app under test. |
| 451 | `public void iRevokeMobilePermissionForApp(String permission, String appId)` | Revoke a specific runtime permission for the app under test. |
| 463 | `public void iWaitUpToSecondsForMobilePageLocatorToBeVisible(int seconds, String page, String locator)` | Wait up to the specified number of seconds for an element to become visible. |
| 475 | `public void iWaitUpToSecondsForMobilePageLocatorToDisappear(int seconds, String page, String locator)` | Wait up to the specified number of seconds for an element to disappear (not visible). |
| 489 | `public void iPauseMobileExecutionForSeconds(int seconds)` | Pause execution for the specified number of seconds. |
| 501 | `public void iAllowMobilePermissionPopupIfDisplayed()` | If a runtime permission popup is displayed, click the "Allow" action. |
| 509 | `public void iDenyMobilePermissionPopupIfDisplayed()` | If a runtime permission popup is displayed, click the "Deny" action. |
| 519 | `public void iAllowMobilePermissionPopupWithTextIfDisplayed(String buttonText)` | If a runtime permission popup is displayed, click a button matching the provided text. |
| 527 | `public void iAllowAllMobilePermissionPopupsIfDisplayed()` | If any permission popups appear, accept them all (common for granting multiple permissions). |
| 535 | `public void iDenyAllMobilePermissionPopupsIfDisplayed()` | If any permission popups appear, deny them all. |
| 551 | `public void iHandleMobilePermissionPopupUsingActionIfDisplayed(String action)` | Handle a permission popup using a custom action string if displayed. |
| 564 | `public void iVerifyMobilePageLocatorIsVisible(String page, String locator)` | Verify that a mobile element is visible. |
| 576 | `public void iVerifyMobilePageLocatorTextContains(String page, String locator, String expected)` | Verify that a mobile element's text contains the expected substring. |
| 591 | `public void iCaptureMobileScreenshotNamed(String screenshotName)` | Capture a screenshot on the mobile device and save it with a friendly name. |
| 602 | `public void iVerifyMobileClipboardTextContains(String expected)` | Verify that the device clipboard contains the expected substring. |

## Data, files, and PDF

### `CsvCommonMethods` — `com.ptaf.csv`

Source: [`src/main/java/com/ptaf/csv/CsvCommonMethods.java`](../../../src/main/java/com/ptaf/csv/CsvCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 45 | `public void loadFromFile(String filePath)` | Load and parse a CSV file from the filesystem into the current scenario's CSV context. |
| 58 | `public void loadFromFile(String filePath, char delimiter)` | Load and parse a CSV file using a custom delimiter. |
| 71 | `public void loadFromString(String csvContent)` | Load and parse CSV content from a raw string into the current scenario's CSV context. |
| 89 | `public void assertValueEquals(int rowNumber, String columnName, String expected)` | Assert that the value of a specific cell (identified by row number and column name) equals the expected value exactly. |
| 112 | `public void assertValueEqualsByIndex(int rowNumber, int columnIndex, String expected)` | Assert that the value of a specific cell (identified by row number and column index) equals the expected value exactly. |
| 134 | `public void assertValueContains(int rowNumber, String columnName, String expected)` | Assert that the value of a specific cell contains the expected substring. |
| 156 | `public void assertValueNotEquals(int rowNumber, String columnName, String expected)` | Assert that the value of a specific cell does NOT equal the given value. |
| 176 | `public void assertRowCount(int expectedCount)` | Assert that the CSV contains exactly the expected number of data rows (excluding header). |
| 194 | `public void assertRowCountAtLeast(int minimumCount)` | Assert that the CSV contains at least the expected number of data rows. |
| 212 | `public void assertColumnExists(String columnName)` | Assert that a column with the given name exists in the CSV headers. |
| 229 | `public void assertColumnNotExists(String columnName)` | Assert that a column with the given name does NOT exist in the CSV headers. |
| 253 | `public void assertAllRowsValueEquals(String columnName, String expected)` | Assert that every data row has the same value in the specified column. |
| 286 | `public void extractAndStore(int rowNumber, String columnName, String variableName)` | Extract the value of a specific cell and store it under a named variable for later use. |
| 299 | `public String getStoredValue(String variableName)` | Retrieve a previously stored variable value by name. |
| 315 | `public void clear()` | Clear the CSV context and variable store for the current scenario. |

### `CsvContext` — `com.ptaf.csv`

Source: [`src/main/java/com/ptaf/csv/CsvContext.java`](../../../src/main/java/com/ptaf/csv/CsvContext.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 28 | `public static void set(CsvFileHandler handler)` | Store a {@link CsvFileHandler} instance for the current thread. |
| 38 | `public static CsvFileHandler get()` | Retrieve the {@link CsvFileHandler} for the current thread. |
| 46 | `public static void clear()` | Remove the {@link CsvFileHandler} for the current thread and release the parsed data. |
| 55 | `public static boolean isLoaded()` | Check whether CSV data is currently loaded for this thread. |

### `CsvFileHandler` — `com.ptaf.csv`

Source: [`src/main/java/com/ptaf/csv/CsvFileHandler.java`](../../../src/main/java/com/ptaf/csv/CsvFileHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 80 | `public void setDelimiter(char delimiter)` | Set the delimiter character to use when parsing the CSV. |
| 93 | `public void setHasHeaders(boolean hasHeaders)` | Set whether the first row of the CSV file is a header row. |
| 111 | `public void loadFromFile(String filePath)` | Load and parse a CSV file from the filesystem. |
| 147 | `public void loadFromString(String csvContent)` | Load and parse CSV content from a raw string. |
| 176 | `public String getValue(int rowNumber, String columnName)` | Get the value of a specific cell identified by 1-based row number and column name. |
| 203 | `public String getValueByIndex(int rowNumber, int columnIndex)` | Get the value of a specific cell identified by 1-based row number and 1-based column index. |
| 221 | `public int getRowCount()` | Get the total number of data rows in the CSV (excluding the header row). |
| 232 | `public boolean columnExists(String columnName)` | Check whether a column with the given name exists in the CSV headers. |
| 242 | `public List<String> getHeaders()` | Get all column header names in the order they appear in the CSV. |
| 252 | `public List<Map<String, String>> getAllRows()` | Get all data rows as a list of maps. |
| 264 | `public Map<String, String> getRow(int rowNumber)` | Get a single data row as a map of column name → value pairs. |

### `PdfMeta` — `com.ptaf.pdf`

Source: [`src/main/java/com/ptaf/pdf/PdfMeta.java`](../../../src/main/java/com/ptaf/pdf/PdfMeta.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 48 | `public static Map<String, String> documentInfo(String pdfPath)` | Read standard document information (metadata) from a non-password-protected PDF. |
| 69 | `public static Map<String, String> documentInfo(String pdfPath, String password)` | Read standard document information (metadata) from a password-protected PDF. |
| 89 | `public static Map<String, String> formFields(String pdfPath)` | Read AcroForm form field names and their current string values from a non-password PDF. |
| 105 | `public static Map<String, String> formFields(String pdfPath, String password)` | Read AcroForm form field names and their current string values from a password-protected PDF. |

### `PdfOcr` — `com.ptaf.pdf`

Source: [`src/main/java/com/ptaf/pdf/PdfOcr.java`](../../../src/main/java/com/ptaf/pdf/PdfOcr.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 47 | `public static String ocrPage(String pdfPath, int pageNumber, float dpi, String lang)` | OCR a single page of a PDF using the system-installed Tesseract OCR engine. |
| 79 | `public static String ocrPage(String pdfPath, String password, int pageNumber, float dpi, String lang)` | Password-protected PDF variant of ocrPage. |

### `PdfRenderDiff` — `com.ptaf.pdf`

Source: [`src/main/java/com/ptaf/pdf/PdfRenderDiff.java`](../../../src/main/java/com/ptaf/pdf/PdfRenderDiff.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 45 | `public final String diffImagePath; // where the red-highlight diff was written (if mismatch)` | If a visual diff image was written (when mismatch and an output path was provided), this contains the absolute path to that written PNG. |
| 54 | `public DiffResult(boolean match, double diffRatio, String diffImagePath)` | Construct a DiffResult. |
| 88 | `public static DiffResult compare(String actualImgPath, String expectedImgPath, int channelTolerance, double maxDiffRatio, String outDiffPath)` | Compare two images with RGBA channel tolerance; produce a red-overlay diff image if the difference ratio exceeds the allowed maxDiffRatio. |

### `PdfStore` — `com.ptaf.pdf`

Source: [`src/main/java/com/ptaf/pdf/PdfStore.java`](../../../src/main/java/com/ptaf/pdf/PdfStore.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 61 | `public static void setLastPdfPath(String path)` | Save the absolute path of the current PDF under test (thread-local). |
| 74 | `public static String getLastPdfPath()` | Retrieve the current PDF path for this thread (or null if not set). |
| 87 | `public static void clear()` | Clear the thread-local storage (usually not required unless reusing threads). |
| 119 | `public static String setLastFromDirectory(String dir, String endsWith)` | Pick the newest file in a directory and set it as the current PDF. |
| 157 | `public static void ensureExists()` | Guard helper: fail early if the current PDF path is missing or the file does not exist. |

### `PdfUtils` — `com.ptaf.pdf`

Source: [`src/main/java/com/ptaf/pdf/PdfUtils.java`](../../../src/main/java/com/ptaf/pdf/PdfUtils.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 56 | `public static String readAllText(String pdfPath)` | Read all text from the PDF at the given path and apply normalization. |
| 68 | `public static String readAllText(String pdfPath, String password)` | Read all text from a password-protected PDF and apply normalization. |
| 82 | `public static String readPageText(String pdfPath, int pageNumber)` | Read and normalize text from a single page in the PDF. |
| 94 | `public static String readPageText(String pdfPath, int pageNumber, String password)` | Read and normalize text from a single page in a password-protected PDF. |
| 109 | `public static List<String> readPages(String pdfPath, int from, int to)` | Read text across a closed page range and return a List of normalized strings, one string per page in the requested range. |
| 133 | `public static int pageCount(String pdfPath)` | Return the total number of pages in a non-password protected PDF. |
| 149 | `public static int pageCount(String pdfPath, String password)` | Return the total number of pages in a password-protected PDF. |
| 167 | `public static boolean isPdfFile(String pdfPath)` | Quick heuristic to determine whether a file appears to be a PDF. |
| 199 | `public static String renderPageToPng(String pdfPath, int pageNumber, float dpi, String outFilePath)` | Render a single page (1-based) of the given PDF to a PNG image file and return the absolute path of the written file. |
| 217 | `public static String renderPageToPng(String pdfPath, String password, int pageNumber, float dpi, String outFilePath)` | Version of renderPageToPng that supports password-protected PDFs. |
| 315 | `public static String normalize(String text)` | Normalize a string to make comparisons resilient across different PDF renderers and encodings. |

### `PdfValidator` — `com.ptaf.pdf`

Source: [`src/main/java/com/ptaf/pdf/PdfValidator.java`](../../../src/main/java/com/ptaf/pdf/PdfValidator.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 52 | `public static void assertContains(String pdfPath, String substring)` | Assert that the full text of the given PDF contains the provided substring. |
| 68 | `public static void assertContains(String pdfPath, String substring, String password)` | Same as assertContains(pdfPath, substring) but for password-protected PDFs. |
| 85 | `public static void assertContainsAll(String pdfPath, List<String> substrings)` | Assert that the full text of the PDF contains ALL of the provided substrings. |
| 101 | `public static void assertNotContains(String pdfPath, String substring)` | Assert that the full text does NOT contain the provided substring. |
| 114 | `public static void assertMatchesRegex(String pdfPath, String regex)` | Assert that the provided regular expression finds a match in the full document text. |
| 133 | `public static void assertPageContains(String pdfPath, int page, String substring)` | Assert that a specific page contains the expected substring. |
| 148 | `public static void assertPageMatchesRegex(String pdfPath, int page, String regex)` | Assert that a regular expression matches somewhere on the given page. |
| 173 | `public static void assertOcrPageContains(String pdfPath, int page, String substring, float dpi, String lang)` | Assert that OCR text extracted from a page contains the expected substring. |
| 194 | `public static void assertPageCountEquals(String pdfPath, int expected)` | Assert that the PDF has the expected number of pages. |
| 206 | `public static void assertIsPdf(String pdfPath)` | Assert that the given path references a PDF file. |
| 232 | `public static void assertPageVisualEquals(String pdfPath, int page, float dpi, String expectedPngPath, int channelTolerance, double maxDiffRatio, String diffOutPath)` | Render a page from the PDF and compare it visually against an expected PNG image. |
| 262 | `public static void assertMetadataContains(String pdfPath, String key, String expectedContains)` | Assert that the PDF document metadata (Info dictionary) contains an expected substring for a given key. |
| 278 | `public static void assertFormFieldEquals(String pdfPath, String fieldName, String expected)` | Assert that a form field in an interactive PDF equals the expected value. |
| 298 | `public static boolean isOcrEnabled()` | Returns true if OCR is enabled. |
| 316 | `public static boolean isTesseractAvailable()` | Detects whether system Tesseract is available on PATH. |

### `ExcelReader` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ExcelReader.java`](../../../src/main/java/com/ptaf/utils/ExcelReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 59 | `public static String getData(String filePath, String testCaseName, String columnName)` | Reads an Excel file and returns the string value of the cell located at the intersection of the row matching testCaseName (compares against the first column of each row) and the column identified by columnName in the header row. |

### `ExcelToYaml` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ExcelToYaml.java`](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 59 | `public static void convertExcelToYaml(String testcaseId, String excelFilePath, String yamlFilePath)` | Main entry point to convert Excel contents to YAML. |
| 200 | `public static Map<String, Object> getDataByTestcaseId(String testcaseId)` | Finds and returns the first row map whose "testcase_id" key equals the provided testcaseId. |

### `XmlCommonMethods` — `com.ptaf.xml`

Source: [`src/main/java/com/ptaf/xml/XmlCommonMethods.java`](../../../src/main/java/com/ptaf/xml/XmlCommonMethods.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 52 | `public void loadFromFile(String filePath)` | Load and parse an XML file from the filesystem into the current scenario's XML context. |
| 67 | `public void loadFromString(String xmlContent)` | Load and parse XML content from a raw string into the current scenario's XML context. |
| 86 | `public void assertValueEquals(String query, String expected)` | Assert that the value of an XML node or XPath expression equals the expected value exactly. |
| 106 | `public void assertValueContains(String query, String expected)` | Assert that the value of an XML node or XPath expression contains the expected substring. |
| 126 | `public void assertValueNotEquals(String query, String expected)` | Assert that the value of an XML node or XPath expression does NOT equal the given value. |
| 143 | `public void assertNodeExists(String query)` | Assert that at least one node matching the query exists in the document. |
| 159 | `public void assertNodeNotExists(String query)` | Assert that no node matching the query exists in the document. |
| 176 | `public void assertNodeCount(String xpathExpression, int expectedCount)` | Assert that the number of nodes matching an XPath expression equals the expected count. |
| 197 | `public void assertAttributeEquals(String query, String attributeName, String expected)` | Assert that the value of an attribute on the first matching node equals the expected value. |
| 224 | `public void extractAndStore(String query, String variableName)` | Extract the value of an XML node or XPath expression and store it in the variable store under the given name for use in later steps within the same scenario. |
| 237 | `public String getStoredValue(String variableName)` | Retrieve a previously stored variable value by name. |
| 253 | `public String getRawContent()` | Get the raw XML content of the currently loaded document as a string. |
| 265 | `public void clear()` | Clear the XML context and variable store for the current scenario. |

### `XmlContext` — `com.ptaf.xml`

Source: [`src/main/java/com/ptaf/xml/XmlContext.java`](../../../src/main/java/com/ptaf/xml/XmlContext.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 49 | `public static void set(XmlFileHandler handler)` | Store an {@link XmlFileHandler} instance for the current thread. |
| 63 | `public static XmlFileHandler get()` | Retrieve the {@link XmlFileHandler} for the current thread. |
| 74 | `public static void clear()` | Remove the {@link XmlFileHandler} for the current thread and release the parsed document. |
| 83 | `public static boolean isLoaded()` | Check whether an XML document is currently loaded for this thread. |

### `XmlFileHandler` — `com.ptaf.xml`

Source: [`src/main/java/com/ptaf/xml/XmlFileHandler.java`](../../../src/main/java/com/ptaf/xml/XmlFileHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 67 | `public XmlFileHandler()` | Creates a new XmlFileHandler with a fresh XPath engine. |
| 86 | `public void loadFromFile(String filePath)` | Load and parse an XML file from the filesystem. |
| 122 | `public void loadFromString(String xmlContent)` | Load and parse XML content from a raw string. |
| 156 | `public String getValue(String query)` | Retrieve the text value of an XML node or XPath expression. |
| 185 | `public int countNodes(String xpathExpression)` | Count the number of nodes matching an XPath expression. |
| 210 | `public boolean nodeExists(String query)` | Check whether a node or XPath expression matches at least one node in the document. |
| 237 | `public String getAttributeValue(String query, String attributeName)` | Retrieve the value of an attribute on the first node matching the given XPath or node name. |
| 268 | `public String getRawContent()` | Returns the raw XML string of the currently loaded document. |

### `ZipContext` — `com.ptaf.zip`

Source: [`src/main/java/com/ptaf/zip/ZipContext.java`](../../../src/main/java/com/ptaf/zip/ZipContext.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 61 | `public static void setExtractionResult(ZipFileHandler.ZipExtractionResult result)` | Stores the ZIP extraction result for the current scenario thread. |
| 72 | `public static ZipFileHandler.ZipExtractionResult getExtractionResult()` | Returns the current scenario's ZIP extraction result. |
| 81 | `public static String getExtractionDir()` | Returns the extraction directory path for the current scenario. |
| 92 | `public static Map<String, List<File>> getFilesByExtension()` | Returns the map of files discovered in the current scenario's extracted ZIP, grouped by lowercase file extension (e.g., "csv", "xml", "txt"). |
| 102 | `public static boolean hasExtractionResult()` | Returns whether a ZIP has been extracted in the current scenario. |
| 112 | `public static void clear()` | Clears the ZIP extraction state for the current scenario thread. |

### `ZipFileHandler` — `com.ptaf.zip`

Source: [`src/main/java/com/ptaf/zip/ZipFileHandler.java`](../../../src/main/java/com/ptaf/zip/ZipFileHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 75 | `public ZipExtractionResult unzip(String zipFilePath, String targetDir, boolean recursiveUnzip) throws IOException` | Extracts a ZIP file to the specified target directory. |
| 169 | `public Map<String, List<File>> discoverFiles(File rootDir)` | Discovers all files within the given root directory, grouped by their file extension (lowercase, without the leading dot). |
| 207 | `public File findFileByName(Map<String, List<File>> filesByExtension, String fileName)` | Finds a specific file by name within the extracted files map. |
| 229 | `public File findFirstFileByExtension(Map<String, List<File>> filesByExtension, String extension)` | Finds the first file with the given extension within the extracted files map. |
| 262 | `public File convertTxtToCsv(File txtFile, String delimiter) throws IOException` | Converts a delimited TXT file to a proper comma-separated CSV file. |
| 312 | `public void cleanup(String extractionDir)` | Deletes the entire extraction directory and all its contents recursively. |
| 335 | `public static String getDefaultExtractionDir()` | Returns the default extraction directory path. |
| 416 | `public ZipExtractionResult(String extractionDir, Map<String, List<File>> filesByExtension)` | Constructs a new ZipExtractionResult. |
| 427 | `public String toString()` | Returns a human-readable summary of the extracted files for logging. |

### `CsvSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/CsvSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 70 | `public void iLoadCsvFile(String filePath)` | Load and parse a CSV file from the filesystem into the current scenario's CSV context. |
| 91 | `public void iLoadCsvFileWithDelimiter(String filePath, String delimiter)` | Load and parse a CSV file using a custom delimiter character. |
| 119 | `public void iLoadCsvFromUiElement(String page, String locator)` | Extract CSV content from a visible UI element and load it into the CSV context. |
| 147 | `public void csvRowColumnEquals(int rowNumber, String columnName, String expected)` | Assert that the value of a cell (identified by row number and column name) equals the expected value. |
| 162 | `public void csvRowColumnContains(int rowNumber, String columnName, String expected)` | Assert that the value of a cell contains the expected substring. |
| 177 | `public void csvRowColumnNotEquals(int rowNumber, String columnName, String expected)` | Assert that the value of a cell does NOT equal the given value. |
| 197 | `public void csvRowColumnIndexEquals(int rowNumber, int columnIndex, String expected)` | Assert that the value of a cell (identified by row number and 1-based column index) equals the expected value. |
| 212 | `public void csvRowCountEquals(int expectedCount)` | Assert that the CSV contains exactly the expected number of data rows (excluding header). |
| 225 | `public void csvRowCountAtLeast(int minimumCount)` | Assert that the CSV contains at least the expected number of data rows. |
| 238 | `public void csvColumnExists(String columnName)` | Assert that a column with the given name exists in the CSV headers. |
| 251 | `public void csvColumnNotExists(String columnName)` | Assert that a column with the given name does NOT exist in the CSV headers. |
| 267 | `public void allCsvRowsHaveColumnEquals(String columnName, String expected)` | Assert that every data row has the same value in the specified column. |
| 287 | `public void csvRowColumnEqualsStoredValue(int rowNumber, String columnName, String variableName)` | Assert that the value of a cell equals a previously stored variable value. |
| 305 | `public void iExtractCsvRowColumnAndStoreAs(int rowNumber, String columnName, String variableName)` | Extract the value of a specific cell and store it under a named variable for use in later steps. |
| 316 | `public void clearCsvContext()` | Cucumber {@code @After} hook that clears the CSV context and variable store after each scenario. |

### `PdfSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/PdfSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 54 | `public void downloadPdf(String element, String key, String directory)` | Downloads a PDF from a page element and saves it to the given directory. |
| 79 | `public void setLastPdfFromDirectory(String dir)` | Sets the "last PDF" to the most recent PDF file found in the given directory. |
| 95 | `public void lastPdfShouldExist()` | Asserts that the "last PDF" path exists and points to an existing file. |
| 108 | `public void lastPdfShouldBeValidPdf()` | Asserts that the "last PDF" exists and is a valid PDF file (basic format validation). |
| 126 | `public void lastPdfShouldContain(String text)` | Asserts that the entire text content of the last PDF contains the provided substring. |
| 140 | `public void lastPdfShouldNotContain(String text)` | Asserts that the entire text content of the last PDF does NOT contain the provided substring. |
| 156 | `public void lastPdfShouldContainAll(DataTable table)` | Asserts that the last PDF contains all strings provided in a Cucumber DataTable. |
| 172 | `public void lastPdfShouldMatchRegex(String regex)` | Asserts that the last PDF matches the provided regular expression somewhere in its text. |
| 189 | `public void pageOfLastPdfShouldContain(int page, String text)` | Asserts that a specific page of the last PDF contains the provided text. |
| 204 | `public void pageOfLastPdfShouldMatchRegex(int page, String regex)` | Asserts that a specific page of the last PDF matches a regular expression. |
| 220 | `public void lastPdfShouldHavePages(int pages)` | Asserts that the last PDF has the expected number of pages. |
| 243 | `public void ocrShouldContain(int page, float dpi, String lang, String text)` | Runs OCR on a specific page of the last PDF and asserts the extracted text contains the given substring. |
| 274 | `public void visualEquals(int page, float dpi, String expectedPng, int channelTolerance, double maxDiffRatio, String diffOutPath)` | Renders a PDF page to an image at the specified DPI and compares it visually against an expected PNG. |
| 292 | `public void metadataContains(String key, String expectedContains)` | Asserts that a metadata entry in the last PDF contains the expected substring. |
| 307 | `public void formFieldEquals(String field, String expected)` | Asserts that a form field value in the last PDF equals the expected string. |
| 325 | `public void printFirstChars(int limit)` | Prints the first N characters of the last PDF's extracted text to standard output and asserts PDF text is non-empty. |

### `XmlSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/XmlSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 70 | `public void iLoadXmlFile(String filePath)` | Load and parse an XML file from the filesystem into the current scenario's XML context. |
| 95 | `public void iLoadXmlFromUiElement(String page, String locator)` | Extract XML content from a visible UI element and load it into the XML context. |
| 129 | `public void xmlNodeEquals(String query, String expected)` | Assert that the value of an XML node equals the expected value exactly. |
| 146 | `public void xmlXPathEquals(String xpathExpression, String expected)` | Assert that the value of an XPath expression equals the expected value exactly. |
| 162 | `public void xmlNodeContains(String query, String expected)` | Assert that the value of an XML node contains the expected substring. |
| 176 | `public void xmlXPathContains(String xpathExpression, String expected)` | Assert that the value of an XPath expression contains the expected substring. |
| 192 | `public void xmlNodeNotEquals(String query, String expected)` | Assert that the value of an XML node does NOT equal the given value. |
| 210 | `public void xmlNodeExists(String query)` | Assert that at least one node matching the query exists in the XML document. |
| 223 | `public void xmlNodeNotExists(String query)` | Assert that no node matching the query exists in the XML document. |
| 239 | `public void xmlXPathCountEquals(String xpathExpression, int expectedCount)` | Assert that the number of nodes matching an XPath expression equals the expected count. |
| 256 | `public void xmlNodeAttributeEquals(String query, String attributeName, String expected)` | Assert that the value of an attribute on the first matching node equals the expected value. |
| 275 | `public void xmlNodeEqualsStoredValue(String query, String variableName)` | Assert that the value of an XML node equals a previously stored variable value. |
| 292 | `public void iExtractXmlNodeAndStoreAs(String query, String variableName)` | Extract the value of an XML node and store it under a named variable for use in later steps. |
| 306 | `public void iExtractXmlXPathAndStoreAs(String xpathExpression, String variableName)` | Extract the value of an XPath expression and store it under a named variable. |
| 319 | `public void clearXmlContext()` | Cucumber {@code @After} hook that clears the XML context and variable store after each scenario. |

### `ZipSteps` — `com.ptaf.stepdefinitions`

Source: [`src/test/java/com/ptaf/stepdefinitions/ZipSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 81 | `public void cleanupZipAfterScenario()` | Cucumber {@code @After} hook that cleans up extracted ZIP files at the end of each scenario if {@code zip.cleanup_after_scenario: true} is set in config.yml. |
| 110 | `public void iUnzipFile(String zipFilePath)` | Unzips a ZIP file from the given path to the configured extraction directory. |
| 139 | `public void iUnzipFileToDirectory(String zipFilePath, String targetDir)` | Unzips a ZIP file from the given path to a specific target directory. |
| 178 | `public void iConvertTxtFileToCsvUsingDelimiter(String txtFileName, String delimiter)` | Converts a TXT file from the extracted ZIP to a CSV file using the specified delimiter. |
| 218 | `public void iConvertTxtFileToCsv(String txtFileName)` | Converts a TXT file from the extracted ZIP to CSV using the default pipe delimiter ({@code \|}). |
| 235 | `public void iLoadCsvFromZipFile(String csvFileName)` | Loads a specific CSV file from the extracted ZIP into the CSV context for validation. |
| 268 | `public void iLoadFirstCsvFileFromZip()` | Automatically finds and loads the first CSV file from the extracted ZIP. |
| 303 | `public void iLoadXmlFromZipFile(String xmlFileName)` | Loads a specific XML file from the extracted ZIP into the XML context for validation. |
| 335 | `public void iLoadFirstXmlFileFromZip()` | Automatically finds and loads the first XML file from the extracted ZIP. |
| 369 | `public void zipContainsFile(String fileName)` | Verifies that a file with the given name exists within the extracted ZIP. |
| 393 | `public void zipContainsAFileWithExtension(String extension)` | Verifies that at least one file with the given extension exists within the extracted ZIP. |
| 421 | `public void iCleanupExtractedZipFiles()` | Explicitly deletes all files extracted from the ZIP in the current scenario. |

## Isolated ui_performance

### `UiPerformanceConfiguration` — `com.ptaf.ui_performance.config`

Source: [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 29 | `public static boolean isEnabled()` | Returns true only when a tester explicitly enables UI performance execution. |
| 34 | `public static String getConfiguredTargetUrl()` | Builds the target from separate protocol, bare host, port, and optional base path. |
| 56 | `public static String getConfiguredRoute(String routeName)` | Returns a clean relative route from the dedicated target.routes map. |
| 71 | `public static String getConfiguredTargetHost()` | Returns only the non-sensitive host for report metadata. |
| 80 | `public static UiPerformanceExecutionPlan getExecutionPlan()` | Reads and validates the named load, stress, spike, or soak profile selected in YAML. |
| 100 | `public static UiPerformanceRunProfile getRunProfile()` | Builds the complete browser runtime, threshold, and selected workload profile. |
| 125 | `public static int getMaxVirtualUsers()` | Maximum users is tester-controlled in YAML; no hidden compiled maximum is applied. |
| 130 | `public static long getSynchronizedStartTimeoutMs()` | Backward-compatible accessor for the shared browser preparation timeout. |
| 134 | `public static String getUsersCsvPath()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 142 | `public static boolean isCsvUserDataEnabled()` | Returns whether the journey requires CSV-backed virtual-user data. |
| 146 | `public static boolean isUserReuseAllowed()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 150 | `public static String getReportOutputDirectory()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 154 | `public static boolean isExistingPerformanceReporterEnabled()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 158 | `public static boolean isHtmlReportEnabled()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 159 | `public static boolean isPdfReportEnabled()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 160 | `public static boolean isCsvReportEnabled()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 161 | `public static boolean isJsonReportEnabled()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 164 | `public static void requireEnabled()` | Guards a live run so the checked-in template never targets an application accidentally. |

### `UiPerformanceLocatorRepository` — `com.ptaf.ui_performance.config`

Source: [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 25 | `public static String getLocatorDefinition(String group, String key)` | Returns the configured TYPE_value locator definition for a separate group/key pair. |
| 44 | `public static String getCssSelector(String group, String key)` | Legacy alias retained for the initial UI performance template. |

### `UiPerformanceYamlReader` — `com.ptaf.ui_performance.config`

Source: [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 32 | `public static Object get(String key)` | Returns a configuration value addressed by a dot-separated key path, or null when absent. |
| 51 | `public static String getString(String key, String defaultValue)` | Returns the configured value as a string, or the supplied default. |
| 57 | `public static int getInt(String key, int defaultValue)` | Returns the configured value as an integer, or the supplied default. |
| 70 | `public static long getLong(String key, long defaultValue)` | Returns the configured value as a long, or the supplied default. |
| 83 | `public static double getDouble(String key, double defaultValue)` | Returns the configured value as a double, or the supplied default. |
| 96 | `public static boolean getBoolean(String key, boolean defaultValue)` | Returns the configured boolean value, accepting only true or false. |
| 110 | `public static Map<String, Object> getMap(String key)` | Returns a configured YAML map or an immutable empty map when the key is absent. |
| 122 | `public static List<?> getList(String key)` | Returns a configured YAML list or an immutable empty list when the key is absent. |
| 134 | `public static Map<String, Object> snapshot()` | Exposes an immutable copy for configuration diagnostics that never contains data-file values. |

### `UiPerformanceEngine` — `com.ptaf.ui_performance.core`

Source: [`src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java`](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 67 | `public UiPerformanceRunResult execute(UiPerformanceJourney journey)` | Runs the complete profile selected in the separate UI performance YAML. |
| 255 | `public List<UiPerformanceIterationResult> call()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceStartGate` — `com.ptaf.ui_performance.core`

Source: [`src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java`](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 21 | `public UiPerformanceStartGate(int virtualUsers)` | Creates a gate for exactly the configured number of virtual users. |
| 29 | `public void markVirtualUserReady()` | Records that one user browser is ready to begin, or that its startup attempt has completed with a failure. |
| 34 | `public void awaitStartSignal()` | Blocks an individual prepared virtual user until the engine releases the common start signal. |
| 44 | `public void awaitAllUsers(long timeoutMs)` | Waits until every configured user has completed browser preparation. |
| 64 | `public void awaitAllUsersThenRelease(long timeoutMs)` | Backward-compatible convenience that waits for preparation and releases all users together. |
| 70 | `public void release()` | Releases waiting users exactly once. |

### `UiPerformanceUserDataReader` — `com.ptaf.ui_performance.data`

Source: [`src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java`](../../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 26 | `public static List<UiPerformanceUser> readUsers(String classpathResource)` | Loads users from the requested classpath CSV file. |
| 81 | `public static List<UiPerformanceUser> createSyntheticUsers(int userCount)` | Creates non-sensitive virtual-user identities for journeys that require no CSV fields. |

### `UiPerformanceMetrics` — `com.ptaf.ui_performance.metrics`

Source: [`src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java`](../../../src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 10 | `public record UiPerformanceMetrics( int totalIterations, int passedIterations, int failedIterations, double failureRatePercent, long minimumJourneyDurationMs, long averageJourneyDurationMs, long medianJourneyDurationMs, long p90JourneyDurationMs, long p95JourneyDurationMs,` | Calculates browser-journey load metrics from completed real-browser samples. |
| 26 | `public static UiPerformanceMetrics from(List<UiPerformanceIterationResult> results, long runDurationMs)` | Produces a performance summary from final journey samples and wall-clock stage/run duration. |

### `UiPerformanceExecutionPlan` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 6 | `public record UiPerformanceExecutionPlan( String profileName, UiPerformanceTestType testType, List<UiPerformanceStage> stages)` | A named load, stress, spike, or soak schedule selected entirely from UI performance YAML. |
| 17 | `public void validate(int maxVirtualUsers)` | Validates every stage and the expected structural shape of the selected profile. |
| 38 | `public int maximumVirtualUsers()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 42 | `public int totalPlannedRampSeconds()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 46 | `public int totalPlannedHoldSeconds()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 50 | `public int maximumIterationsPerUser()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceIterationResult` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 28 | `public UiPerformanceIterationResult(String stageName, String userId, int iteration, boolean passed, long startedAtEpochMs, long completedAtEpochMs, long durationMs, Map<String, Long> stepDurationsMs, String failureCategory, String failureMessage,` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 55 | `public UiPerformanceIterationResult(String userId, int iteration, boolean passed, long durationMs, Map<String, Long> stepDurationsMs, String failureCategory, String failureMessage, String failureScreenshotPath, List<String> consoleErrors)` | Backward-compatible constructor used by existing offline tests and report adapters. |
| 68 | `public String getStageName()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 69 | `public String getUserId()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 70 | `public int getIteration()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 71 | `public boolean isPassed()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 72 | `public long getStartedAtEpochMs()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 73 | `public long getCompletedAtEpochMs()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 74 | `public long getDurationMs()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 75 | `public Map<String, Long> getStepDurationsMs()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 76 | `public String getFailureCategory()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 77 | `public String getFailureMessage()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 78 | `public String getFailureScreenshotPath()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 79 | `public List<String> getConsoleErrors()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceJourney` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 18 | `public UiPerformanceJourney(String name, String baseUrl)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 29 | `public String getName()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 33 | `public String getBaseUrl()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 37 | `public void addStep(UiPerformanceStep step)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 44 | `public List<UiPerformanceStep> getSteps()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 49 | `public void validateForExecution()` | Ensures a configured run has a safe target and an executable journey. |

### `UiPerformanceRunProfile` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 7 | `public record UiPerformanceRunProfile( UiPerformanceExecutionPlan executionPlan, long synchronizedStartTimeoutMs, long betweenIterationsMs, long actionTimeoutMs, long navigationTimeoutMs, boolean headless, boolean ignoreHttpsErrors, String browserIsolation, String userAgent,` | Immutable runtime, browser, safety, and threshold settings for one UI performance run. |
| 25 | `public void validate()` | Validates every config-controlled runtime setting before a browser is launched. |

### `UiPerformanceRunResult` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 22 | `public UiPerformanceRunResult(String runId, String journeyName, String targetHost, Instant startedAt, Instant completedAt, UiPerformanceRunProfile profile, List<UiPerformanceStageResult> stageResults, List<UiPerformanceIterationResult> iterations, UiPerformanceMetrics metrics, Path reportDirectory)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 45 | `public UiPerformanceRunResult(String runId, String journeyName, String targetHost, Instant startedAt, Instant completedAt, UiPerformanceRunProfile profile, List<UiPerformanceIterationResult> iterations, UiPerformanceMetrics metrics, Path reportDirectory)` | Backward-compatible constructor for report contract tests representing one synthetic stage. |
| 60 | `public String getRunId()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 61 | `public String getJourneyName()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 62 | `public String getTargetHost()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 63 | `public Instant getStartedAt()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 64 | `public Instant getCompletedAt()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 65 | `public UiPerformanceRunProfile getProfile()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 66 | `public UiPerformanceExecutionPlan getExecutionPlan()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 67 | `public List<UiPerformanceStageResult> getStageResults()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 68 | `public List<UiPerformanceIterationResult> getIterations()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 69 | `public UiPerformanceMetrics getMetrics()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 70 | `public Path getReportDirectory()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceStage` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 11 | `public record UiPerformanceStage( String name, int virtualUsers, int rampUpSeconds, int holdSeconds, int iterationsPerUser)` | One sequential stage in a real-browser UI performance profile. |
| 23 | `public void validate(int maxVirtualUsers)` | Validates this stage against the tester-controlled safety maximum. |
| 44 | `public boolean isDurationBased()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 48 | `public boolean isIterationBased()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 52 | `public int plannedDurationSeconds()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceStageResult` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 17 | `public UiPerformanceStageResult(UiPerformanceStage stage, Instant startedAt, Instant completedAt, List<UiPerformanceIterationResult> iterations, UiPerformanceMetrics metrics)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 29 | `public UiPerformanceStage getStage()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 30 | `public Instant getStartedAt()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 31 | `public Instant getCompletedAt()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 32 | `public List<UiPerformanceIterationResult> getIterations()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 33 | `public UiPerformanceMetrics getMetrics()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 34 | `public long getDurationMs()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 37 | `public long getFirstIterationStartSpreadMs()` | Difference between the earliest and latest first-iteration start for this stage. |

### `UiPerformanceStep` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 10 | `public record UiPerformanceStep(String name, Action action, String target, String valueTemplate)` | One intentionally small browser action in an isolated UI performance journey. |

### `UiPerformanceTestType` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 18 | `public String displayName()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 23 | `public static UiPerformanceTestType fromConfig(String value)` | Parses a profile type without accepting ambiguous or unsupported values. |

### `UiPerformanceUser` — `com.ptaf.ui_performance.model`

Source: [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 17 | `public UiPerformanceUser(String id, Map<String, String> values)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 25 | `public String getId()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 29 | `public String getRequiredValue(String fieldName)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 37 | `public Map<String, String> getValues()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 42 | `public String toString()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceDurationFormatter` — `com.ptaf.ui_performance.reporting`

Source: [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 25 | `public static String format(long milliseconds)` | Returns an easily scannable duration using milliseconds below one second, seconds below one minute, and minute/hour components for longer operations. |

### `UiPerformanceExistingReporterAdapter` — `com.ptaf.ui_performance.reporting`

Source: [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 35 | `public Path write(UiPerformanceRunResult uiResult)` | Writes existing Performance reporter artifacts into the same isolated UI performance run folder. |

### `UiPerformanceReportManager` — `com.ptaf.ui_performance.reporting`

Source: [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 14 | `public Path createRunDirectory(String outputDirectory, String journeyName)` | Creates the report directory and its failure-evidence child directory. |
| 27 | `public static String sanitize(String value)` | Converts a journey name to a safe report-directory identifier. |

### `UiPerformanceReportWriter` — `com.ptaf.ui_performance.reporting`

Source: [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 32 | `public Path write(UiPerformanceRunResult result, boolean htmlEnabled, boolean pdfEnabled, boolean csvEnabled, boolean jsonEnabled)` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceSensitiveTextSanitizer` — `com.ptaf.ui_performance.reporting`

Source: [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 16 | `public static String sanitize(String text)` | Returns a bounded, sanitized diagnostic message that is safe to include in reports. |

### `UiPerformanceConcurrentBrowserIntegrationTest` — `com.ptaf.ui_performance`

Source: [`src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 21 | `public void executesEveryRequestedRealBrowserUserInOneConcurrentLoadStage() throws Exception` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceModuleContractTest` — `com.ptaf.ui_performance`

Source: [`src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 33 | `public void loadsDedicatedTargetActiveProfileDataBrowserControlAndLocators()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 57 | `public void validatesLoadStressSpikeSoakAndConfigurableSafetyLimit()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 76 | `public void writesFullBrowserPerformanceReportsAndExistingPerformanceWorkbook() throws Exception` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 160 | `public void formatsHumanReadableDurationsWithoutDiscardingSubSecondPrecision()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceStartGateTest` — `com.ptaf.ui_performance`

Source: [`src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 18 | `public void releasesPreparedVirtualUsersTogetherOnlyAfterEveryUserIsReady() throws Exception` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `UiPerformanceRunner` — `com.ptaf.ui_performance.runners`

Source: [`src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java)

No public or protected callable declaration was detected by the generator. Open the source to inspect fields, package-private helpers, and implementation details.

### `UiPerformanceSteps` — `com.ptaf.ui_performance.stepdefinitions`

Source: [`src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`](../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 27 | `public void createJourneyUsingConfiguredTarget(String journeyName)` | Starts a journey using the protocol, host, port, and base path from isolated YAML configuration. |
| 36 | `public void createJourney(String journeyName, String baseUrl)` | Retained for backward compatibility with the first UI performance template. |
| 42 | `public void addConfiguredNavigation(String routeName)` | Adds navigation using a named, relative route from the isolated UI performance YAML. |
| 50 | `public void addNavigation(String route)` | Retained for backward compatibility; new features should navigate through a configured route name. |
| 56 | `public void addDataFill(String locatorGroup, String locatorKey, String dataField)` | Adds a field-fill step whose value is read only from the separate CSV data column at runtime. |
| 65 | `public void addDataSelection(String locatorGroup, String locatorKey, String dataField)` | Adds a select-list step whose visible option label comes from the isolated CSV row. |
| 74 | `public void addLiteralFill(String locatorGroup, String locatorKey, String value)` | Adds a non-sensitive literal fill. |
| 89 | `public void addClick(String locatorGroup, String locatorKey)` | Adds a click step resolved only from the separate regular-style UI performance locator YAML. |
| 98 | `public void addPopupClick(String locatorGroup, String locatorKey)` | Adds an action that clicks a configured locator and continues the journey in its popup page. |
| 107 | `public void addVisibilityCheck(String locatorGroup, String locatorKey)` | Adds a visible-state validation step resolved only from the separate regular-style locator YAML. |
| 116 | `public void executeJourney()` | Executes all configured concurrent browser users and writes standalone performance reports. |
| 122 | `public void verifyPerformanceReport()` | Verifies that a completed run was collected; configured performance thresholds are enforced during execution. |
| 128 | `public void verifyReport()` | Retained for the initial feature wording. |

## Reporting, hooks, and shared utilities

### `DatabaseHooks` — `com.ptaf.hooks`

Source: [`src/main/java/com/ptaf/hooks/DatabaseHooks.java`](../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 81 | `public void closeDatabaseConnectionAfterScenario(Scenario scenario)` | Closes the active database connection after each database scenario. |

### `Hooks` — `com.ptaf.hooks`

Source: [`src/main/java/com/ptaf/hooks/Hooks.java`](../../../src/main/java/com/ptaf/hooks/Hooks.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 126 | `public Hooks()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 130 | `public void setUp(Scenario scenario)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 245 | `public void tearDown(Scenario scenario)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 433 | `public static void waitForCurrentPageToLoad()` | Waits until current page is loaded. |
| 459 | `public static void setPage(Page page)` | Sets the active page for the current thread. |
| 478 | `public static void closeBrowserResources()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 661 | `public static void markBrowserClosedIntentionally()` | Marks the current @LastScenario feature's close operation as deliberate. |
| 1030 | `public static boolean maximizeBrowserWindow(Page page)` | Retains compatibility with the framework's explicit maximize action without changing browser lifecycle behavior. |
| 1062 | `public static Page getPage()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1070 | `public static Browser getBrowser()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1078 | `public static Scenario getCurrentScenario()` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1082 | `public static void setCurrentScenario(Scenario scenario)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 1086 | `public static BrowserContext getContext()` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `MobileHooks` — `com.ptaf.hooks`

Source: [`src/main/java/com/ptaf/hooks/MobileHooks.java`](../../../src/main/java/com/ptaf/hooks/MobileHooks.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 58 | `public void setUpMobile(Scenario scenario)` | Cucumber @Before hook that prepares mobile/Appium resources for scenarios that are marked as mobile tests. |
| 93 | `public void tearDownMobile(Scenario scenario)` | Cucumber @After hook that tears down mobile/Appium resources after mobile-marked scenarios. |

### `GlassPdfSubprocessGenerator` — `com.ptaf.reporting`

Source: [`src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java`](../../../src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 65 | `public static void main(String[] args)` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `PerFeatureReportListener` — `com.ptaf.reporting`

Source: [`src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 162 | `public void setEventPublisher(EventPublisher publisher)` | Register event handlers with the Cucumber event publisher. |

### `SoftAssertionReportListener` — `com.ptaf.reporting`

Source: [`src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java`](../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 60 | `public void setEventPublisher(EventPublisher publisher)` | Register the {@link TestStepFinished} event handler with the Cucumber event bus. |

### `SoftAssertionContext` — `com.ptaf.softassert`

Source: [`src/main/java/com/ptaf/softassert/SoftAssertionContext.java`](../../../src/main/java/com/ptaf/softassert/SoftAssertionContext.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 60 | `public static int getUnreportedFailureCount()` | Returns the number of soft failures that have not yet been reported by the step-level listener. |
| 70 | `public static List<SoftFailure> getUnreportedFailures()` | Returns the list of soft failures that have not yet been reported by the step-level listener. |
| 82 | `public static void markFailuresReported()` | Mark all currently recorded failures as reported so the next call to {@link #getUnreportedFailureCount()} returns 0 until new failures are added. |
| 104 | `public static void recordFailure(String stepDescription, String errorMessage, String screenshotPath)` | Record a soft failure for the current scenario. |
| 119 | `public static boolean hasFailed()` | Check whether any soft failures have been recorded for the current scenario. |
| 128 | `public static int getFailureCount()` | Get the number of soft failures recorded for the current scenario. |
| 137 | `public static List<SoftFailure> getFailures()` | Get an unmodifiable view of all recorded soft failures for the current scenario. |
| 152 | `public static String buildSummary()` | Build a formatted failure summary message for use in the scenario failure assertion. |
| 186 | `public static void clear()` | Clear all recorded soft failures for the current thread. |

### `BrowserFactory` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/BrowserFactory.java`](../../../src/main/java/com/ptaf/utils/BrowserFactory.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 90 | `public static Browser createBrowser(BrowserTypeEnum browserTypeEnum)` | Create a Playwright Browser instance for a given BrowserTypeEnum. |
| 120 | `public static Browser createBrowser(String profileName)` | Create a Playwright Browser instance that corresponds to a mobile browser profile. |
| 151 | `public static boolean isMobileBrowserProfile(String browserName)` | Check whether a given browserName corresponds to a known mobile browser profile. |
| 160 | `public static boolean hasActiveMobileBrowserProfile()` | Returns true if there is an active mobile browser profile set for the current thread. |
| 292 | `public static BrowserContext createContextWithVideo(Browser browser)` | Create a new BrowserContext with video recording and mobile profile support as configured. |

### `ConfigurationProperties` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 56 | `public static String getBaseUrl(String URL)` | Retrieves the base URL from the YAML configuration using the specified key. |
| 71 | `public static String getBrowser()` | Retrieves the configured browser type from config.yml. |
| 86 | `public static String getHeadlessMode()` | Retrieves the configured headless mode value from config.yml. |
| 115 | `public static String getIgnoreHTTPSErrors()` | Retrieves the HTTPS / SSL certificate error handling configuration from config.yml. |
| 139 | `public static String getYamlStoreLocation()` | Retrieves the YAML store location from config.yml. |
| 153 | `public static String getExcelDocumentLocation()` | Retrieves the Excel document location from config.yml. |
| 173 | `public static String getVideoCapture()` | Retrieves the video capture configuration from config.yml. |
| 203 | `public static long getRuntimeTimeoutMillis()` | Retrieves the runtime timeout in milliseconds from config.yml. |
| 242 | `public static String getValue(String value)` | Generic accessor that delegates to YamlReader to fetch a value by key. |
| 281 | `public static boolean isPerFeatureReportsEnabled()` | Whether per-feature-file Extent Reports are enabled. |
| 297 | `public static String getPerFeatureReportsOutputDir()` | The output directory for per-feature Extent HTML reports. |
| 312 | `public static boolean isPerFeaturePdfEnabled()` | Whether a PDF version of each per-feature report should also be generated. |
| 328 | `public static boolean isPerFeatureGlassPdfEnabled()` | Whether a Glass-style PDF (via cucumber-pdf-report subprocess) should be generated for each per-feature report, written to a separate directory. |
| 341 | `public static String getPerFeatureGlassPdfOutputDir()` | The output directory for per-feature Glass-style PDF reports. |
| 359 | `public static String getZipExtractionDir()` | The directory where ZIP file contents are extracted during test execution. |
| 376 | `public static boolean isZipCleanupAfterScenario()` | Whether extracted ZIP files should be automatically deleted at the end of each scenario. |
| 394 | `public static boolean isZipRecursiveUnzip()` | Whether nested ZIP files found inside an extracted ZIP should also be extracted recursively. |
| 418 | `public static boolean isSoftAssertionsEnabled()` | Whether soft assertion mode is enabled. |
| 436 | `public static int getSoftAssertionRetrySeconds()` | The number of seconds to retry a failed step before recording it as a soft failure. |
| 448 | `public static String getPropertyValue(String value)` | No Javadoc summary was detected; inspect the declaration and implementation. |

### `ExcelReader` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ExcelReader.java`](../../../src/main/java/com/ptaf/utils/ExcelReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 59 | `public static String getData(String filePath, String testCaseName, String columnName)` | Reads an Excel file and returns the string value of the cell located at the intersection of the row matching testCaseName (compares against the first column of each row) and the column identified by columnName in the header row. |

### `ExcelToYaml` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ExcelToYaml.java`](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 59 | `public static void convertExcelToYaml(String testcaseId, String excelFilePath, String yamlFilePath)` | Main entry point to convert Excel contents to YAML. |
| 200 | `public static Map<String, Object> getDataByTestcaseId(String testcaseId)` | Finds and returns the first row map whose "testcase_id" key equals the provided testcaseId. |

### `ExcelWriter` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ExcelWriter.java`](../../../src/main/java/com/ptaf/utils/ExcelWriter.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 57 | `public static void writeData(String filePath, String testCaseName, String columnName, String valueToWrite)` | Writes or overwrites data in a specific cell of an Excel file, creating columns and rows as needed. |

### `FeatureArtifactNameResolver` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 45 | `public static String resolveFeatureName(Scenario scenario)` | Resolves the exact declared {@code Feature:} title for the supplied scenario. |
| 60 | `public static String resolveFeatureName(URI uri)` | Resolves the declared {@code Feature:} title from a Cucumber feature URI. |
| 90 | `public static Path buildArtifactPath(Path outputDirectory, Scenario scenario, String originalFileName)` | Creates a unique artifact path within {@code outputDirectory}. |
| 102 | `public static Path buildArtifactPath(Path outputDirectory, URI featureUri, String originalFileName)` | Creates a unique artifact path from a feature URI while preserving the source extension. |
| 116 | `public static Path createFeatureDirectory(Path outputRoot, Scenario scenario) throws IOException` | Creates and returns a filesystem-safe subdirectory named after the declared {@code Feature:} title. |
| 128 | `public static Path createFeatureDirectory(Path outputRoot, URI featureUri) throws IOException` | Creates and returns a filesystem-safe Feature-name subdirectory from a feature URI. |
| 140 | `public static String buildArtifactFileName(Scenario scenario, String originalFileName)` | Creates a feature-based artifact file name with a microsecond timestamp. |
| 151 | `public static String buildArtifactFileName(URI featureUri, String originalFileName)` | Creates a feature-based artifact file name from a feature URI with a microsecond timestamp. |

### `PropertiesReader` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/PropertiesReader.java`](../../../src/main/java/com/ptaf/utils/PropertiesReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 69 | `public static Object get(String key)` | Retrieves a value from the loaded properties data based on a dot-separated key. |

### `ScenarioUtil` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ScenarioUtil.java`](../../../src/main/java/com/ptaf/utils/ScenarioUtil.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 38 | `public static void handleScenarioTeardown(Scenario scenario, Page page, String status)` | Handles the teardown process for the given scenario. |
| 53 | `public static void handleScenarioTeardownFailier(Scenario scenario, Page page, String status)` | Smaller/faster viewport screenshot attachment. |
| 68 | `public static void handleScenarioTeardownLocator(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String status)` | Element/iframe-targeted screenshot attachment. |
| 103 | `public static void reportAllDropdownOptionsMultiline(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String dropdownLocator)` | No Javadoc summary was detected; inspect the declaration and implementation. |
| 156 | `public static void reportString(Scenario scenario, String title, String value)` | Attach and log any arbitrary string value. |
| 175 | `public static String captureElementString(Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator)` | Capture a string value from a target element, respecting up to three nested iframes. |
| 222 | `public static void reportElementString(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String label)` | Convenience: capture + attach/log a string value from an element (page/frames aware). |
| 234 | `public static void reportJson(Scenario scenario, String title, String json)` | Attach JSON (auto pretty-prints safely; falls back to raw if needed). |
| 248 | `public static void reportTable(Scenario scenario, String title, List<String> rows)` | Attach a simple one-column table rendered as numbered lines (for lists, options, etc.). |
| 267 | `public static void reportKeyValues(Scenario scenario, String title, Map<String, ?> map)` | Attach key–value pairs (configs, env, headers). |
| 289 | `public static void reportElementStringWithScreenshot(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String label)` | Value + Screenshot combo: captures element text/value AND its screenshot. |

### `ScreenshotHandler` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/ScreenshotHandler.java`](../../../src/main/java/com/ptaf/utils/ScreenshotHandler.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 55 | `public static void handleScenarioTeardown(Scenario scenario, Page page, String status)` | Handles the teardown process for the given scenario by attempting to capture a full-page screenshot from the provided Playwright Page and attach it to the Cucumber scenario report. |
| 110 | `public static void handleScenarioTeardownLocator(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String status)` | Handles the teardown process for a scenario where the target for the screenshot may be inside one or more nested iframes. |

### `YamlReader` — `com.ptaf.utils`

Source: [`src/main/java/com/ptaf/utils/YamlReader.java`](../../../src/main/java/com/ptaf/utils/YamlReader.java)

| Source line | Callable declaration | Nearest Javadoc summary |
|---:|---|---|
| 159 | `public static Object get(String key)` | Retrieves a value from the loaded YAML data based on a dot-separated key. |
| 202 | `public static void setSuppressLogs(boolean suppress)` | No Javadoc summary was detected; inspect the declaration and implementation. |

## All source files

The following complete list prevents a class outside a topic grouping from being invisible. If a class appears in more than one conceptual group above, use the first group for explanation and this section for complete discovery.

| Source | Package | Public/protected declarations |
|---|---|---:|
| [`src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java`](../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java) | `com.ptaf.api.handlers` | 2 |
| [`src/main/java/com/ptaf/api/implementation/ApiActionImpl.java`](../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java) | `com.ptaf.api.implementation` | 10 |
| [`src/main/java/com/ptaf/api/interfaces/ApiAction.java`](../../../src/main/java/com/ptaf/api/interfaces/ApiAction.java) | `com.ptaf.api.interfaces` | 0 |
| [`src/main/java/com/ptaf/api/methods/ApiCommonMethods.java`](../../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java) | `com.ptaf.api.methods` | 11 |
| [`src/main/java/com/ptaf/api/performer/ApiActionPerformer.java`](../../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java) | `com.ptaf.api.performer` | 2 |
| [`src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java`](../../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java) | `com.ptaf.api.wrapper` | 5 |
| [`src/main/java/com/ptaf/csv/CsvCommonMethods.java`](../../../src/main/java/com/ptaf/csv/CsvCommonMethods.java) | `com.ptaf.csv` | 15 |
| [`src/main/java/com/ptaf/csv/CsvContext.java`](../../../src/main/java/com/ptaf/csv/CsvContext.java) | `com.ptaf.csv` | 4 |
| [`src/main/java/com/ptaf/csv/CsvFileHandler.java`](../../../src/main/java/com/ptaf/csv/CsvFileHandler.java) | `com.ptaf.csv` | 11 |
| [`src/main/java/com/ptaf/db/handlers/DatabaseHandler.java`](../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java) | `com.ptaf.db.handlers` | 2 |
| [`src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java`](../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java) | `com.ptaf.db.implementation` | 6 |
| [`src/main/java/com/ptaf/db/interfaces/DatabaseAction.java`](../../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java) | `com.ptaf.db.interfaces` | 0 |
| [`src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java`](../../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java) | `com.ptaf.db.pages` | 8 |
| [`src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java`](../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java) | `com.ptaf.db.performer` | 3 |
| [`src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java`](../../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java) | `com.ptaf.db.validators` | 2 |
| [`src/main/java/com/ptaf/hooks/DatabaseHooks.java`](../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java) | `com.ptaf.hooks` | 1 |
| [`src/main/java/com/ptaf/hooks/Hooks.java`](../../../src/main/java/com/ptaf/hooks/Hooks.java) | `com.ptaf.hooks` | 13 |
| [`src/main/java/com/ptaf/hooks/MobileHooks.java`](../../../src/main/java/com/ptaf/hooks/MobileHooks.java) | `com.ptaf.hooks` | 2 |
| [`src/main/java/com/ptaf/mobile/assertions/MobileAssert.java`](../../../src/main/java/com/ptaf/mobile/assertions/MobileAssert.java) | `com.ptaf.mobile.assertions` | 5 |
| [`src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java`](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java) | `com.ptaf.mobile.config` | 23 |
| [`src/main/java/com/ptaf/mobile/config/MobilePlatform.java`](../../../src/main/java/com/ptaf/mobile/config/MobilePlatform.java) | `com.ptaf.mobile.config` | 3 |
| [`src/main/java/com/ptaf/mobile/config/MobileYamlReader.java`](../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java) | `com.ptaf.mobile.config` | 4 |
| [`src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java`](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java) | `com.ptaf.mobile.drivers` | 2 |
| [`src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java`](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java) | `com.ptaf.mobile.drivers` | 7 |
| [`src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java`](../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java) | `com.ptaf.mobile.evidence` | 8 |
| [`src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java`](../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java) | `com.ptaf.mobile.handlers` | 1 |
| [`src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java`](../../../src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java) | `com.ptaf.mobile.implementation` | 44 |
| [`src/main/java/com/ptaf/mobile/interfaces/MobileAction.java`](../../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java) | `com.ptaf.mobile.interfaces` | 0 |
| [`src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java`](../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java) | `com.ptaf.mobile.pages` | 48 |
| [`src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java`](../../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java) | `com.ptaf.mobile.permissions` | 7 |
| [`src/main/java/com/ptaf/pdf/PdfMeta.java`](../../../src/main/java/com/ptaf/pdf/PdfMeta.java) | `com.ptaf.pdf` | 4 |
| [`src/main/java/com/ptaf/pdf/PdfOcr.java`](../../../src/main/java/com/ptaf/pdf/PdfOcr.java) | `com.ptaf.pdf` | 2 |
| [`src/main/java/com/ptaf/pdf/PdfRenderDiff.java`](../../../src/main/java/com/ptaf/pdf/PdfRenderDiff.java) | `com.ptaf.pdf` | 3 |
| [`src/main/java/com/ptaf/pdf/PdfStore.java`](../../../src/main/java/com/ptaf/pdf/PdfStore.java) | `com.ptaf.pdf` | 5 |
| [`src/main/java/com/ptaf/pdf/PdfUtils.java`](../../../src/main/java/com/ptaf/pdf/PdfUtils.java) | `com.ptaf.pdf` | 11 |
| [`src/main/java/com/ptaf/pdf/PdfValidator.java`](../../../src/main/java/com/ptaf/pdf/PdfValidator.java) | `com.ptaf.pdf` | 15 |
| [`src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java`](../../../src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java) | `com.ptaf.performance.assertions` | 4 |
| [`src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java`](../../../src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java) | `com.ptaf.performance.auth` | 10 |
| [`src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java) | `com.ptaf.performance.builders` | 8 |
| [`src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java) | `com.ptaf.performance.builders` | 19 |
| [`src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java) | `com.ptaf.performance.builders` | 2 |
| [`src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java`](../../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java) | `com.ptaf.performance.config` | 8 |
| [`src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java`](../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java) | `com.ptaf.performance.config` | 5 |
| [`src/main/java/com/ptaf/performance/core/BasePerformanceEngine.java`](../../../src/main/java/com/ptaf/performance/core/BasePerformanceEngine.java) | `com.ptaf.performance.core` | 1 |
| [`src/main/java/com/ptaf/performance/core/PerformanceEngine.java`](../../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java) | `com.ptaf.performance.core` | 9 |
| [`src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java`](../../../src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java) | `com.ptaf.performance.core` | 1 |
| [`src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java`](../../../src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java) | `com.ptaf.performance.headers` | 11 |
| [`src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java) | `com.ptaf.performance.models` | 10 |
| [`src/main/java/com/ptaf/performance/models/PerformanceExecutionResult.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionResult.java) | `com.ptaf.performance.models` | 70 |
| [`src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java) | `com.ptaf.performance.models` | 9 |
| [`src/main/java/com/ptaf/performance/models/PerformanceProfile.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceProfile.java) | `com.ptaf.performance.models` | 12 |
| [`src/main/java/com/ptaf/performance/models/PerformanceRequest.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceRequest.java) | `com.ptaf.performance.models` | 32 |
| [`src/main/java/com/ptaf/performance/models/PerformanceRunReport.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceRunReport.java) | `com.ptaf.performance.models` | 45 |
| [`src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java`](../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java) | `com.ptaf.performance.payloads` | 1 |
| [`src/main/java/com/ptaf/performance/payloads/PayloadSourceType.java`](../../../src/main/java/com/ptaf/performance/payloads/PayloadSourceType.java) | `com.ptaf.performance.payloads` | 0 |
| [`src/main/java/com/ptaf/performance/payloads/PerformancePayloadDefinition.java`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadDefinition.java) | `com.ptaf.performance.payloads` | 13 |
| [`src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java) | `com.ptaf.performance.payloads` | 4 |
| [`src/main/java/com/ptaf/performance/reports/PerformanceExcelFormatHelper.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelFormatHelper.java) | `com.ptaf.performance.reports` | 16 |
| [`src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java) | `com.ptaf.performance.reports` | 1 |
| [`src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java) | `com.ptaf.performance.reports` | 9 |
| [`src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java) | `com.ptaf.performance.reports` | 4 |
| [`src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java`](../../../src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java) | `com.ptaf.performance.utils` | 12 |
| [`src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java`](../../../src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java) | `com.ptaf.reporting` | 1 |
| [`src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) | `com.ptaf.reporting` | 1 |
| [`src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java`](../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java) | `com.ptaf.reporting` | 1 |
| [`src/main/java/com/ptaf/softassert/SoftAssertionContext.java`](../../../src/main/java/com/ptaf/softassert/SoftAssertionContext.java) | `com.ptaf.softassert` | 9 |
| [`src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java`](../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) | `com.ptaf.ui.action_performer` | 3 |
| [`src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java`](../../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java) | `com.ptaf.ui.action_performer` | 17 |
| [`src/main/java/com/ptaf/ui/assertions/UIAssert.java`](../../../src/main/java/com/ptaf/ui/assertions/UIAssert.java) | `com.ptaf.ui.assertions` | 35 |
| [`src/main/java/com/ptaf/ui/handlers/LocatorHandler.java`](../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java) | `com.ptaf.ui.handlers` | 3 |
| [`src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java`](../../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java) | `com.ptaf.ui.helpers` | 5 |
| [`src/main/java/com/ptaf/ui/interfaces/ElementAction.java`](../../../src/main/java/com/ptaf/ui/interfaces/ElementAction.java) | `com.ptaf.ui.interfaces` | 0 |
| [`src/main/java/com/ptaf/ui/interfaces/ElementLocator.java`](../../../src/main/java/com/ptaf/ui/interfaces/ElementLocator.java) | `com.ptaf.ui.interfaces` | 0 |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java) | `com.ptaf.ui.mobilebrowser` | 1 |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java) | `com.ptaf.ui.mobilebrowser` | 16 |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java) | `com.ptaf.ui.mobilebrowser` | 17 |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java) | `com.ptaf.ui.mobilebrowser` | 3 |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java) | `com.ptaf.ui.mobilebrowser` | 1 |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java) | `com.ptaf.ui.mobilebrowser` | 0 |
| [`src/main/java/com/ptaf/ui/page_helper/PageHelper.java`](../../../src/main/java/com/ptaf/ui/page_helper/PageHelper.java) | `com.ptaf.ui.page_helper` | 1 |
| [`src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java`](../../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java) | `com.ptaf.ui.pages` | 61 |
| [`src/main/java/com/ptaf/ui/pages/PageCommonMethods.java`](../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java) | `com.ptaf.ui.pages` | 65 |
| [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java) | `com.ptaf.ui_performance.config` | 18 |
| [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java) | `com.ptaf.ui_performance.config` | 2 |
| [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java) | `com.ptaf.ui_performance.config` | 9 |
| [`src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java`](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java) | `com.ptaf.ui_performance.core` | 2 |
| [`src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java`](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java) | `com.ptaf.ui_performance.core` | 6 |
| [`src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java`](../../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java) | `com.ptaf.ui_performance.data` | 2 |
| [`src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java`](../../../src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java) | `com.ptaf.ui_performance.metrics` | 2 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java) | `com.ptaf.ui_performance.model` | 6 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java) | `com.ptaf.ui_performance.model` | 14 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java) | `com.ptaf.ui_performance.model` | 6 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java) | `com.ptaf.ui_performance.model` | 2 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java) | `com.ptaf.ui_performance.model` | 13 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java) | `com.ptaf.ui_performance.model` | 5 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java) | `com.ptaf.ui_performance.model` | 8 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java) | `com.ptaf.ui_performance.model` | 1 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java) | `com.ptaf.ui_performance.model` | 2 |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java) | `com.ptaf.ui_performance.model` | 5 |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java) | `com.ptaf.ui_performance.reporting` | 1 |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java) | `com.ptaf.ui_performance.reporting` | 1 |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java) | `com.ptaf.ui_performance.reporting` | 2 |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java) | `com.ptaf.ui_performance.reporting` | 1 |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java) | `com.ptaf.ui_performance.reporting` | 1 |
| [`src/main/java/com/ptaf/utils/BrowserFactory.java`](../../../src/main/java/com/ptaf/utils/BrowserFactory.java) | `com.ptaf.utils` | 5 |
| [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) | `com.ptaf.utils` | 20 |
| [`src/main/java/com/ptaf/utils/ExcelReader.java`](../../../src/main/java/com/ptaf/utils/ExcelReader.java) | `com.ptaf.utils` | 1 |
| [`src/main/java/com/ptaf/utils/ExcelToYaml.java`](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java) | `com.ptaf.utils` | 2 |
| [`src/main/java/com/ptaf/utils/ExcelWriter.java`](../../../src/main/java/com/ptaf/utils/ExcelWriter.java) | `com.ptaf.utils` | 1 |
| [`src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) | `com.ptaf.utils` | 8 |
| [`src/main/java/com/ptaf/utils/PropertiesReader.java`](../../../src/main/java/com/ptaf/utils/PropertiesReader.java) | `com.ptaf.utils` | 1 |
| [`src/main/java/com/ptaf/utils/ScenarioUtil.java`](../../../src/main/java/com/ptaf/utils/ScenarioUtil.java) | `com.ptaf.utils` | 11 |
| [`src/main/java/com/ptaf/utils/ScreenshotHandler.java`](../../../src/main/java/com/ptaf/utils/ScreenshotHandler.java) | `com.ptaf.utils` | 2 |
| [`src/main/java/com/ptaf/utils/YamlReader.java`](../../../src/main/java/com/ptaf/utils/YamlReader.java) | `com.ptaf.utils` | 2 |
| [`src/main/java/com/ptaf/xml/XmlCommonMethods.java`](../../../src/main/java/com/ptaf/xml/XmlCommonMethods.java) | `com.ptaf.xml` | 13 |
| [`src/main/java/com/ptaf/xml/XmlContext.java`](../../../src/main/java/com/ptaf/xml/XmlContext.java) | `com.ptaf.xml` | 4 |
| [`src/main/java/com/ptaf/xml/XmlFileHandler.java`](../../../src/main/java/com/ptaf/xml/XmlFileHandler.java) | `com.ptaf.xml` | 8 |
| [`src/main/java/com/ptaf/zip/ZipContext.java`](../../../src/main/java/com/ptaf/zip/ZipContext.java) | `com.ptaf.zip` | 6 |
| [`src/main/java/com/ptaf/zip/ZipFileHandler.java`](../../../src/main/java/com/ptaf/zip/ZipFileHandler.java) | `com.ptaf.zip` | 9 |
| [`src/test/java/com/ptaf/runner/TestRunner.java`](../../../src/test/java/com/ptaf/runner/TestRunner.java) | `com.ptaf.runner` | 0 |
| [`src/test/java/com/ptaf/runners/ApiTestRunner.java`](../../../src/test/java/com/ptaf/runners/ApiTestRunner.java) | `com.ptaf.runners` | 0 |
| [`src/test/java/com/ptaf/runners/DatabaseTestRunner.java`](../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java) | `com.ptaf.runners` | 0 |
| [`src/test/java/com/ptaf/runners/MobileTestRunner.java`](../../../src/test/java/com/ptaf/runners/MobileTestRunner.java) | `com.ptaf.runners` | 0 |
| [`src/test/java/com/ptaf/runners/ParallelRun.java`](../../../src/test/java/com/ptaf/runners/ParallelRun.java) | `com.ptaf.runners` | 1 |
| [`src/test/java/com/ptaf/runners/PerformanceTestRunner.java`](../../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java) | `com.ptaf.runners` | 0 |
| [`src/test/java/com/ptaf/runners/Regression_Runner.java`](../../../src/test/java/com/ptaf/runners/Regression_Runner.java) | `com.ptaf.runners` | 0 |
| [`src/test/java/com/ptaf/runners/TestRunner.java`](../../../src/test/java/com/ptaf/runners/TestRunner.java) | `com.ptaf.runners` | 0 |
| [`src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java) | `com.ptaf.stepdefinitions` | 10 |
| [`src/test/java/com/ptaf/stepdefinitions/CsvSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java) | `com.ptaf.stepdefinitions` | 15 |
| [`src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java) | `com.ptaf.stepdefinitions` | 11 |
| [`src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java) | `com.ptaf.stepdefinitions` | 3 |
| [`src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java) | `com.ptaf.stepdefinitions` | 2 |
| [`src/test/java/com/ptaf/stepdefinitions/MobileSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java) | `com.ptaf.stepdefinitions` | 49 |
| [`src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java) | `com.ptaf.stepdefinitions` | 89 |
| [`src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java) | `com.ptaf.stepdefinitions` | 30 |
| [`src/test/java/com/ptaf/stepdefinitions/PdfSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java) | `com.ptaf.stepdefinitions` | 16 |
| [`src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java) | `com.ptaf.stepdefinitions` | 44 |
| [`src/test/java/com/ptaf/stepdefinitions/XmlSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java) | `com.ptaf.stepdefinitions` | 15 |
| [`src/test/java/com/ptaf/stepdefinitions/ZipSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java) | `com.ptaf.stepdefinitions` | 12 |
| [`src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java) | `com.ptaf.ui_performance` | 1 |
| [`src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java) | `com.ptaf.ui_performance` | 4 |
| [`src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java) | `com.ptaf.ui_performance` | 1 |
| [`src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) | `com.ptaf.ui_performance.runners` | 0 |
| [`src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`](../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java) | `com.ptaf.ui_performance.stepdefinitions` | 13 |

## Regeneration

Run the following command from the repository root after Java source changes:

```bash
python3 scripts/generate_java_callable_catalog.py
```

The generator writes only this appendix. It does not modify Java code, configuration, tests, or reports.

## Source references

- **[1]** [`scripts/generate_java_callable_catalog.py`](../../../scripts/generate_java_callable_catalog.py) — Java callable catalog generator.
- **[2]** [`src/main/java/com/ptaf`](../../../src/main/java/com/ptaf) — production framework sources cataloged by this appendix.
- **[3]** [`src/test/java/com/ptaf`](../../../src/test/java/com/ptaf) — runner, step-definition, contract-test, and support sources cataloged by this appendix.

## References

[1]: ../../../scripts/generate_java_callable_catalog.py "FNB-ETAF Java callable catalog generator"
[2]: ../../../src/main/java/com/ptaf "FNB-ETAF production framework source catalog"
[3]: ../../../src/test/java/com/ptaf "FNB-ETAF test source catalog"
