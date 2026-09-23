package com.ptaf.performance.payloads;

/**
 * Supported payload source types for performance request bodies.
 *
 * <p>This enum represents the different ways a request payload can be provided to the
 * performance testing framework. The value chosen by the caller or configuration will
 * determine how the framework interprets and loads the payload data prior to sending
 * requests.</p>
 *
 * <p>Guidance for testers:
 * <ul>
 *   <li>INLINE - The payload is provided directly as a string in the test configuration
 *               or request definition. Use this when the payload is small and can be embedded
 *               directly in the test files.</li>
 *   <li>YAML   - The payload is stored in a YAML file. Use this when you need multiline
 *               structured payloads or prefer YAML syntax for readability and composition.</li>
 *   <li>CSV    - The payloads are provided as rows in a CSV file. Each row typically
 *               represents a separate payload instance (useful for parameterized testing).</li>
 *   <li>EXCEL  - The payloads are provided inside an Excel workbook (XLS/XLSX). Use this
 *               when payload data is maintained by non-developers in spreadsheets.</li>
 * </ul>
 * </p>
 *
 * <p>Note: The enum values are intended to be used as type-safe identifiers in code
 * and configuration. Consumers should map these values to the appropriate file readers
 * or inline handlers implemented elsewhere in the framework.</p>
 */
public enum PayloadSourceType {
    /**
     * Payload data is supplied directly (inline) as part of the test or request definition.
     *
     * <p>Typical usage:
     * - Small JSON or text payloads embedded directly in the configuration.
     * - Quick one-off payloads that do not require external files.</p>
     *
     * <p>Implementation note for testers: When using INLINE, verify that the payload string
     * is properly escaped and encoded as needed by the transport layer.</p>
     */
    INLINE,

    /**
     * Payload is loaded from a YAML formatted file.
     *
     * <p>Typical usage:
     * - Structured payloads where YAML provides readability and nesting.
     * - Multiple documents inside a single YAML file may be supported depending on
     *   the framework's YAML loader behavior.</p>
     *
     * <p>Implementation note for testers: Ensure the YAML file is accessible to the test
     * runtime and conforms to the expected schema (if any).</p>
     */
    YAML,

    /**
     * Payloads are read from a CSV (comma-separated values) file.
     *
     * <p>Typical usage:
     * - Parameterized testing where each CSV row becomes a separate payload instance.
     * - Simple tabular data that maps to flat payload fields.</p>
     *
     * <p>Implementation note for testers: Verify the CSV delimiter, header presence, and
     * encoding. The framework may expect a header row to map CSV columns to payload fields.</p>
     */
    CSV,

    /**
     * Payloads are read from an Excel workbook (XLS or XLSX).
     *
     * <p>Typical usage:
     * - Complex test data managed by teams in spreadsheet form.
     * - Multiple sheets representing different datasets or test scenarios.</p>
     *
     * <p>Implementation note for testers: Confirm which sheet and cell range the framework
     * expects, and ensure the Excel file format (XLS vs XLSX) is supported by the runtime.</p>
     */
    EXCEL
}
