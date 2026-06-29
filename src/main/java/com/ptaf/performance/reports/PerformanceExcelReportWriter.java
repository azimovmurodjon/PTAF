package com.ptaf.performance.reports;

import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceExecutionStatus;
import com.ptaf.performance.models.PerformanceRunReport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a single Excel report for an entire performance run.
 *
 * <p>
 * This class is responsible for turning a PerformanceRunReport model into a user-friendly,
 * multi-sheet Excel workbook. Each sheet is laid out in a business-readable style and
 * includes both numeric tables and charts intended for quick executive and technical review.
 * </p>
 *
 * <p>
 * Important notes for testers:
 * - The writer is deterministic and relies only on the data provided by PerformanceRunReport
 *   and its contained PerformanceExecutionResult objects.
 * - No execution logic is performed here; this class only formats and writes reporting output.
 * - Charts are created using Apache POI XSSF/XDDF APIs; verify charts visually in Excel if needed.
 * </p>
 *
 * <p>
 * Enterprise-safe design principles:
 * - Keep all existing report sheets
 * - Improve only reporting / presentation logic
 * - Avoid changing execution behavior or scenario result calculations
 * - Keep report generation deterministic and reusable for large-scale framework use
 * </p>
 */
public class PerformanceExcelReportWriter {

    // Default report file name written to the run root
    private static final String REPORT_FILE_NAME = "performance-run-report.xlsx";

    // Standard text used by the framework to indicate no threshold breaches
    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";

    /**
     * Visual worksheet margin so content starts from B2 instead of A1.
     *
     * Using BASE_ROW = 1 and BASE_COL = 1 shifts display to a cleaner area of the sheet.
     */
    private static final int BASE_ROW = 1; // Excel row 2
    private static final int BASE_COL = 1; // Excel col B

    /**
     * Create the Excel workbook and write all report sheets to disk.
     *
     * <p>
     * The returned Path points to the generated Excel file inside the run root folder from the
     * provided PerformanceRunReport. This method performs validation on the runReport first and
     * will throw IllegalArgumentException for missing required information, or RuntimeException
     * for IO errors during writing.
     * </p>
     *
     * @param runReport the aggregated performance run report model to render as an Excel file
     * @return Path to the generated Excel file (performance-run-report.xlsx) inside the run root
     * @throws IllegalArgumentException if runReport is null or missing a run root path
     * @throws RuntimeException on IO errors while writing the file
     */
    public Path writeRunReport(PerformanceRunReport runReport) {
        validateRunReport(runReport);

        Path runRootPath = Path.of(runReport.getRunRootPath());
        Path reportPath = runRootPath.resolve(REPORT_FILE_NAME);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Create common cell styles once and reuse across sheets for consistent look & feel.
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);
            CellStyle passStyle = createStatusStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle failStyle = createStatusStyle(workbook, IndexedColors.ROSE);
            CellStyle warningStyle = createStatusStyle(workbook, IndexedColors.LIGHT_YELLOW);
            CellStyle infoStyle = createStatusStyle(workbook, IndexedColors.PALE_BLUE);

            // Build each sheet in the workbook. Each method is focused on a specific page/concern.
            writeExecutiveSummarySheet(
                    workbook, runReport, titleStyle, sectionStyle, headerStyle, normalStyle,
                    passStyle, failStyle, warningStyle, infoStyle
            );

            writeScenarioSummarySheet(
                    workbook, runReport, titleStyle, sectionStyle, headerStyle, normalStyle,
                    passStyle, failStyle, warningStyle, infoStyle
            );

            writeRiskAnalysisSheet(
                    workbook, runReport, titleStyle, headerStyle, normalStyle,
                    passStyle, failStyle, warningStyle, infoStyle
            );

            writeAnomaliesSheet(
                    workbook, runReport, titleStyle, headerStyle, normalStyle,
                    passStyle, failStyle, warningStyle, infoStyle
            );

            writeReadableReportSheet(
                    workbook, runReport, titleStyle, headerStyle, normalStyle,
                    passStyle, failStyle, warningStyle, infoStyle
            );

            writeChartsSheet(
                    workbook, runReport, titleStyle, headerStyle, normalStyle
            );

            writeGlossarySheet(
                    workbook, titleStyle, headerStyle, normalStyle
            );

            // Auto-size columns based on contents, then apply some business-friendly minimum widths.
            autoSizeAllColumns(workbook);
            enforceBusinessFriendlyColumnWidths(workbook);

            // Write workbook to the target output file.
            try (OutputStream outputStream = Files.newOutputStream(reportPath)) {
                workbook.write(outputStream);
            }

            return reportPath;

        } catch (IOException e) {
            // Convert to unchecked to simplify caller handling; retain cause for diagnostics.
            throw new RuntimeException("Failed to write Excel performance report: " + reportPath, e);
        }
    }

    // ========================================================================
    // EXECUTIVE SUMMARY
    // ========================================================================

    /**
     * Build the high-level executive summary sheet with top-level metrics, pie charts, and
     * critical highlights for rapid business review.
     *
     * <p>
     * This sheet contains:
     * - Run overview key/value pairs
     * - Key numbers (counts, averages)
     * - Critical business highlights with a small bar chart
     * - Distribution snapshots with pie charts for overall business outcome and attention areas
     * </p>
     */
    private void writeExecutiveSummarySheet(XSSFWorkbook workbook,
                                            PerformanceRunReport runReport,
                                            CellStyle titleStyle,
                                            CellStyle sectionStyle,
                                            CellStyle headerStyle,
                                            CellStyle normalStyle,
                                            CellStyle passStyle,
                                            CellStyle failStyle,
                                            CellStyle warningStyle,
                                            CellStyle infoStyle) {
        XSSFSheet sheet = workbook.createSheet("Executive_Summary");

        int rowIndex = BASE_ROW;

        // Title and subtitle
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Performance Run Executive Summary", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, BASE_COL, "Business-friendly summary of run health, major risks, and critical scenario highlights.", normalStyle);

        rowIndex++;

        // Pre-computed highlights from the run report (could be null if no scenarios)
        PerformanceExecutionResult slowestP95Scenario = runReport.getSlowestP95Scenario();
        PerformanceExecutionResult longestDurationScenario = runReport.getLongestDurationScenario();
        PerformanceExecutionResult shortestDurationScenario = runReport.getShortestDurationScenario();

        // Run overview section header
        Row section1 = sheet.createRow(rowIndex++);
        createCell(section1, BASE_COL, "Run Overview", sectionStyle);

        // Basic key/value rows describing the run
        rowIndex = createKeyValueRow(sheet, rowIndex, "Run Folder Name", runReport.getRunFolderName(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Run Root Path", runReport.getRunRootPath(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Execution Timestamp", runReport.getExecutionTimestamp(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Overall Conclusion", runReport.getOverallConclusion(), headerStyle, normalStyle);

        rowIndex++;

        // Key numbers for quick glance
        Row section2 = sheet.createRow(rowIndex++);
        createCell(section2, BASE_COL, "Key Numbers", sectionStyle);

        rowIndex = createKeyValueRow(sheet, rowIndex, "Total Scenarios", String.valueOf(runReport.getTotalScenarios()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Passed Scenarios", String.valueOf(runReport.getPassedScenarios()), headerStyle, passStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Failed Scenarios", String.valueOf(runReport.getFailedScenarios()), headerStyle, failStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Expected Fail Confirmed", String.valueOf(runReport.getExpectedFailConfirmedScenarios()), headerStyle, infoStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Expected Fail Not Triggered", String.valueOf(runReport.getExpectedFailNotTriggeredScenarios()), headerStyle, warningStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Skipped Scenarios", String.valueOf(runReport.getSkippedScenarios()), headerStyle, warningStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Average Error Percent", PerformanceExcelFormatHelper.formatPercent(runReport.getAverageErrorPercent()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Average Risk Score", PerformanceExcelFormatHelper.formatDecimal(runReport.getAverageRiskScore()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Total Scenario Duration", PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getTotalScenarioDurationMs()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Average Scenario Duration", PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getAverageScenarioDurationMs()), headerStyle, normalStyle);

        rowIndex++;

        // Critical business highlights block with small numeric table for chart source
        Row section3 = sheet.createRow(rowIndex++);
        createCell(section3, BASE_COL, "Critical Business Highlights", sectionStyle);

        // Header row for the highlights table (metric, scenario, display value, chart value, unit)
        int importantHeaderRow = rowIndex++;
        Row importantHeader = sheet.createRow(importantHeaderRow);
        createCell(importantHeader, BASE_COL + 0, "Metric", headerStyle);
        createCell(importantHeader, BASE_COL + 1, "Scenario", headerStyle);
        createCell(importantHeader, BASE_COL + 2, "Display Value", headerStyle);
        createCell(importantHeader, BASE_COL + 3, "Chart Value", headerStyle);
        createCell(importantHeader, BASE_COL + 4, "Unit", headerStyle);

        int importantDataStart = rowIndex;

        // Populate three important metrics used to surface critical timings
        Row r1 = sheet.createRow(rowIndex++);
        createCell(r1, BASE_COL + 0, "Slowest P95 (sec)", normalStyle);
        createCell(r1, BASE_COL + 1, slowestP95Scenario == null ? "N/A" : slowestP95Scenario.getTestName(), normalStyle);
        createCell(r1, BASE_COL + 2, PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getSlowestP95ResponseTimeMs()), normalStyle);
        createCell(r1, BASE_COL + 3, roundToThreeDecimals(toSeconds(runReport.getSlowestP95ResponseTimeMs())), normalStyle);
        createCell(r1, BASE_COL + 4, "sec", normalStyle);

        Row r2 = sheet.createRow(rowIndex++);
        createCell(r2, BASE_COL + 0, "Longest Duration (sec)", normalStyle);
        createCell(r2, BASE_COL + 1, longestDurationScenario == null ? "N/A" : longestDurationScenario.getTestName(), normalStyle);
        createCell(r2, BASE_COL + 2, longestDurationScenario == null
                ? "N/A"
                : PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getHighestScenarioDurationMs()), normalStyle);
        createCell(r2, BASE_COL + 3, roundToThreeDecimals(toSeconds(runReport.getHighestScenarioDurationMs())), normalStyle);
        createCell(r2, BASE_COL + 4, "sec", normalStyle);

        Row r3 = sheet.createRow(rowIndex++);
        createCell(r3, BASE_COL + 0, "Shortest Duration (sec)", normalStyle);
        createCell(r3, BASE_COL + 1, shortestDurationScenario == null ? "N/A" : shortestDurationScenario.getTestName(), normalStyle);
        createCell(r3, BASE_COL + 2, shortestDurationScenario == null
                ? "N/A"
                : PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getShortestScenarioDurationMs()), normalStyle);
        createCell(r3, BASE_COL + 3, roundToThreeDecimals(toSeconds(runReport.getShortestScenarioDurationMs())), normalStyle);
        createCell(r3, BASE_COL + 4, "sec", normalStyle);

        int importantDataEnd = rowIndex - 1;

        // Create a small bar chart showing the three critical time values; values use seconds for readability
        createExecutiveHighlightsBarChart(
                sheet,
                "Critical Performance Highlights",
                importantDataStart,
                importantDataEnd,
                BASE_COL + 0,
                BASE_COL + 3,
                BASE_COL + 5,
                BASE_ROW + 2,
                BASE_COL + 16,
                BASE_ROW + 16,
                "Seconds"
        );

        rowIndex += 2;

        Row noteRow = sheet.createRow(rowIndex++);
        createCell(noteRow, BASE_COL, "Chart values are time-based and shown in seconds for cleaner executive review.", normalStyle);

        rowIndex++;

        // Distribution snapshots for outcome & attention areas (used for pies)
        Row section4 = sheet.createRow(rowIndex++);
        createCell(section4, BASE_COL, "Distribution Snapshots", sectionStyle);

        int businessOutcomeHeaderRow = rowIndex++;
        Row businessOutcomeHeader = sheet.createRow(businessOutcomeHeaderRow);
        createCell(businessOutcomeHeader, BASE_COL + 0, "Business Outcome", headerStyle);
        createCell(businessOutcomeHeader, BASE_COL + 1, "Count", headerStyle);

        int businessOutcomeDataStart = rowIndex;

        Row bo1 = sheet.createRow(rowIndex++);
        createCell(bo1, BASE_COL + 0, "Passed", normalStyle);
        createCell(bo1, BASE_COL + 1, runReport.getPassedScenarios(), normalStyle);

        Row bo2 = sheet.createRow(rowIndex++);
        createCell(bo2, BASE_COL + 0, "Failed", normalStyle);
        createCell(bo2, BASE_COL + 1, runReport.getFailedScenarios(), normalStyle);

        Row bo3 = sheet.createRow(rowIndex++);
        createCell(bo3, BASE_COL + 0, "Expected Fail Confirmed", normalStyle);
        createCell(bo3, BASE_COL + 1, runReport.getExpectedFailConfirmedScenarios(), normalStyle);

        Row bo4 = sheet.createRow(rowIndex++);
        createCell(bo4, BASE_COL + 0, "Expected Fail Not Triggered", normalStyle);
        createCell(bo4, BASE_COL + 1, runReport.getExpectedFailNotTriggeredScenarios(), normalStyle);

        Row bo5 = sheet.createRow(rowIndex++);
        createCell(bo5, BASE_COL + 0, "Skipped", normalStyle);
        createCell(bo5, BASE_COL + 1, runReport.getSkippedScenarios(), normalStyle);

        int businessOutcomeDataEnd = rowIndex - 1;

        // Attention area mix table and pie chart
        int attentionHeaderRow = rowIndex++;
        Row attentionHeader = sheet.createRow(attentionHeaderRow);
        createCell(attentionHeader, BASE_COL + 0, "Attention Area", headerStyle);
        createCell(attentionHeader, BASE_COL + 1, "Count", headerStyle);

        int attentionDataStart = rowIndex;

        Row at1 = sheet.createRow(rowIndex++);
        createCell(at1, BASE_COL + 0, "No Issue Detected", normalStyle);
        createCell(at1, BASE_COL + 1, countNoIssueScenarios(runReport), normalStyle);

        Row at2 = sheet.createRow(rowIndex++);
        createCell(at2, BASE_COL + 0, "Threshold Breach", normalStyle);
        createCell(at2, BASE_COL + 1, countThresholdBreachScenarios(runReport), normalStyle);

        Row at3 = sheet.createRow(rowIndex++);
        createCell(at3, BASE_COL + 0, "Errors Present", normalStyle);
        createCell(at3, BASE_COL + 1, countErrorScenarios(runReport), normalStyle);

        Row at4 = sheet.createRow(rowIndex++);
        createCell(at4, BASE_COL + 0, "High / Critical Risk", normalStyle);
        createCell(at4, BASE_COL + 1, countHighOrCriticalRiskScenarios(runReport), normalStyle);

        int attentionDataEnd = rowIndex - 1;

        // Create pie charts for outcomes and attention areas; charts are anchored into sheet coordinates
        createPieChart(
                sheet,
                "Business Outcome Mix",
                businessOutcomeDataStart,
                businessOutcomeDataEnd,
                BASE_COL + 0,
                BASE_COL + 1,
                BASE_COL + 5,
                BASE_ROW + 22,
                BASE_COL + 12,
                BASE_ROW + 37
        );

        createPieChart(
                sheet,
                "Attention Needed Mix",
                attentionDataStart,
                attentionDataEnd,
                BASE_COL + 0,
                BASE_COL + 1,
                BASE_COL + 13,
                BASE_ROW + 22,
                BASE_COL + 20,
                BASE_ROW + 37
        );

        // Freeze header area for easier navigation once opened in Excel
        sheet.createFreezePane(BASE_COL, BASE_ROW + 1);
    }

    // ========================================================================
    // SCENARIO SUMMARY
    // ========================================================================

    /**
     * Create the scenario-level detailed summary sheet.
     *
     * <p>
     * This sheet contains a large table of all scenarios and a couple of supporting charts
     * that focus on latency and risk/error signals for the top offenders.
     * </p>
     */
    private void writeScenarioSummarySheet(XSSFWorkbook workbook,
                                           PerformanceRunReport runReport,
                                           CellStyle titleStyle,
                                           CellStyle sectionStyle,
                                           CellStyle headerStyle,
                                           CellStyle normalStyle,
                                           CellStyle passStyle,
                                           CellStyle failStyle,
                                           CellStyle warningStyle,
                                           CellStyle infoStyle) {
        XSSFSheet sheet = workbook.createSheet("Scenario_Summary");

        int rowIndex = BASE_ROW;

        // Title + subtitle
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Scenario Performance Summary", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, BASE_COL, "Detailed scenario-level metrics with added business-review charts.", normalStyle);

        // Column headers for the scenario table
        String[] headers = {
                "Test Name",
                "Execution Status",
                "Risk Score",
                "Risk Level",
                "Threshold Breach Summary",
                "Recommended Action",
                "Test Type",
                "HTTP Method",
                "Target Path",
                "Users",
                "Ramp-Up Seconds",
                "Hold Seconds",
                "Iterations",
                "Execution Mode",
                "Allowed Error %",
                "Allowed Avg Response",
                "Allowed P95 Response",
                "Total Scenario Duration",
                "Total Samples",
                "Total Errors",
                "Actual Error %",
                "Min Response Time",
                "Average Response Time",
                "P95 Response Time",
                "Max Response Time",
                "Execution Passed",
                "Expected Failure Mode",
                "Actual Failure Detected",
                "Response Assessment",
                "Error Assessment",
                "Stability Assessment",
                "First Failure Indicator",
                "Final Conclusion"
        };

        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, BASE_COL + i, headers[i], headerStyle);
        }

        int headerRowIndex = rowIndex - 1;

        // Populate a row per scenario. Styles for execution status vary depending on the status value.
        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row row = sheet.createRow(rowIndex++);
            int col = BASE_COL;

            createCell(row, col++, result.getTestName(), normalStyle);

            // Execution status cell: write string and apply a status-specific background color style
            Cell statusCell = row.createCell(col++);
            statusCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

            // Fill remaining metrics. Helper methods format values consistently (e.g. percent, seconds).
            createCell(row, col++, result.getRiskScore(), normalStyle);
            createCell(row, col++, result.getRiskLevel(), normalStyle);
            createCell(row, col++, result.getThresholdBreachSummary(), normalStyle);
            createCell(row, col++, result.getRecommendedAction(), normalStyle);
            createCell(row, col++, result.getPerformanceTestType(), normalStyle);
            createCell(row, col++, result.getHttpMethod(), normalStyle);
            createCell(row, col++, result.getTargetPath(), normalStyle);
            createCell(row, col++, result.getUsers(), normalStyle);
            createCell(row, col++, result.getRampUpSeconds(), normalStyle);
            createCell(row, col++, result.getHoldSeconds(), normalStyle);
            createCell(row, col++, result.getIterations(), normalStyle);
            createCell(row, col++, result.getExecutionMode(), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatPercent(result.getMaxAllowedErrorPercent()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxAllowedAverageResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxAllowedP95ResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getTotalScenarioDurationMs()), normalStyle);
            createCell(row, col++, result.getTotalSamples(), normalStyle);
            createCell(row, col++, result.getTotalErrors(), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatPercent(result.getErrorPercent()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMinResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxResponseTimeMs()), normalStyle);
            createCell(row, col++, String.valueOf(result.isExecutionPassed()), normalStyle);
            createCell(row, col++, String.valueOf(result.isExpectedFailureMode()), normalStyle);
            createCell(row, col++, String.valueOf(result.isActualFailureDetected()), normalStyle);
            createCell(row, col++, result.getResponseTimeAssessment(), normalStyle);
            createCell(row, col++, result.getErrorAssessment(), normalStyle);
            createCell(row, col++, result.getStabilityAssessment(), normalStyle);
            createCell(row, col++, result.getFirstFailureIndicator(), normalStyle);
            createCell(row, col++, result.getFinalConclusion(), normalStyle);
        }

        int scenarioTableEndRow = rowIndex - 1;

        // Leave some space before chart region
        rowIndex += 2;

        Row chartSection = sheet.createRow(rowIndex++);
        createCell(chartSection, BASE_COL, "Scenario Summary Charts", sectionStyle);

        Row chartNote = sheet.createRow(rowIndex++);
        createCell(chartNote, BASE_COL, "The charts below focus on latency, errors, risk, and run duration. Layout spacing has been widened to prevent chart overlap.", normalStyle);

        rowIndex++;

        // Build a table of the top 10 scenarios by P95 latency for a latency-focused chart
        List<PerformanceExecutionResult> topLatencyScenarios = runReport.getScenarioResults().stream()
                .sorted(Comparator.comparingLong(PerformanceExecutionResult::getP95ResponseTimeMs).reversed())
                .limit(10)
                .collect(Collectors.toList());

        int responseTableHeaderRow = rowIndex++;
        Row responseHeader = sheet.createRow(responseTableHeaderRow);
        createCell(responseHeader, BASE_COL + 0, "Scenario", headerStyle);
        createCell(responseHeader, BASE_COL + 1, "Avg Response (sec)", headerStyle);
        createCell(responseHeader, BASE_COL + 2, "P95 Response (sec)", headerStyle);
        createCell(responseHeader, BASE_COL + 3, "Max Response (sec)", headerStyle);

        int responseTableDataStart = rowIndex;

        for (PerformanceExecutionResult result : topLatencyScenarios) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, BASE_COL + 0, result.getTestName(), normalStyle);
            createCell(row, BASE_COL + 1, toSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, BASE_COL + 2, toSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, BASE_COL + 3, toSeconds(result.getMaxResponseTimeMs()), normalStyle);
        }

        int responseTableDataEnd = rowIndex - 1;

        // Multi-series bar chart comparing avg, p95, and max response times for top latency scenarios
        createMultiSeriesBarChart(
                sheet,
                "Top Latency Scenarios",
                responseTableDataStart,
                responseTableDataEnd,
                BASE_COL + 0,
                new int[]{BASE_COL + 1, BASE_COL + 2, BASE_COL + 3},
                new String[]{"Avg Response (sec)", "P95 Response (sec)", "Max Response (sec)"},
                BASE_COL + 4,
                responseTableHeaderRow,
                BASE_COL + 15,
                responseTableHeaderRow + 14,
                "Seconds"
        );

        // Leave space and prepare next chart area
        rowIndex += 16;

        // Build a combined list of the top scenarios by error %, risk, and duration for multi-metric charting
        List<PerformanceExecutionResult> topRiskAndErrorScenarios = runReport.getScenarioResults().stream()
                .sorted(Comparator
                        .comparingDouble(PerformanceExecutionResult::getErrorPercent).reversed()
                        .thenComparing(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore).reversed())
                        .thenComparing(Comparator.comparingLong(PerformanceExecutionResult::getTotalScenarioDurationMs).reversed()))
                .limit(10)
                .collect(Collectors.toList());

        int stabilityTableHeaderRow = rowIndex++;
        Row stabilityHeader = sheet.createRow(stabilityTableHeaderRow);
        createCell(stabilityHeader, BASE_COL + 0, "Scenario", headerStyle);
        createCell(stabilityHeader, BASE_COL + 1, "Error %", headerStyle);
        createCell(stabilityHeader, BASE_COL + 2, "Risk Score", headerStyle);
        createCell(stabilityHeader, BASE_COL + 3, "Duration (sec)", headerStyle);
        createCell(stabilityHeader, BASE_COL + 4, "Total Errors", headerStyle);

        int stabilityTableDataStart = rowIndex;

        for (PerformanceExecutionResult result : topRiskAndErrorScenarios) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, BASE_COL + 0, result.getTestName(), normalStyle);
            createCell(row, BASE_COL + 1, result.getErrorPercent(), normalStyle);
            createCell(row, BASE_COL + 2, result.getRiskScore(), normalStyle);
            createCell(row, BASE_COL + 3, toSeconds(result.getTotalScenarioDurationMs()), normalStyle);
            createCell(row, BASE_COL + 4, result.getTotalErrors(), normalStyle);
        }

        // Create a multi-series bar chart comparing error %, risk, duration and errors
        createMultiSeriesBarChart(
                sheet,
                "Error, Risk and Duration Focus",
                stabilityTableDataStart,
                rowIndex - 1,
                BASE_COL + 0,
                new int[]{BASE_COL + 1, BASE_COL + 2, BASE_COL + 3, BASE_COL + 4},
                new String[]{"Error %", "Risk Score", "Duration (sec)", "Total Errors"},
                BASE_COL + 7,
                stabilityTableHeaderRow + 1,
                BASE_COL + 18,
                stabilityTableHeaderRow + 15,
                "Value"
        );

        // Freeze the top rows and enable a sheet-level auto-filter for the scenario table
        sheet.createFreezePane(BASE_COL, BASE_ROW + 2);
        sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, Math.max(headerRowIndex, scenarioTableEndRow), BASE_COL, BASE_COL + headers.length - 1));
    }

    // ========================================================================
    // RISK ANALYSIS
    // ========================================================================

    /**
     * Create a sorted risk analysis sheet that ranks scenarios by risk score (descending).
     *
     * <p>
     * Useful for triage – testers and owners should review the top-ranked rows first.
     * </p>
     */
    private void writeRiskAnalysisSheet(XSSFWorkbook workbook,
                                        PerformanceRunReport runReport,
                                        CellStyle titleStyle,
                                        CellStyle headerStyle,
                                        CellStyle normalStyle,
                                        CellStyle passStyle,
                                        CellStyle failStyle,
                                        CellStyle warningStyle,
                                        CellStyle infoStyle) {
        Sheet sheet = workbook.createSheet("Risk_Analysis");

        int rowIndex = BASE_ROW;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Risk Analysis", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, BASE_COL, "Scenarios ranked by highest risk first.", normalStyle);

        // Sort by risk score descending
        List<PerformanceExecutionResult> sortedByRisk = runReport.getScenarioResults().stream()
                .sorted(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore).reversed())
                .collect(Collectors.toList());

        // Column headers
        String[] headers = {
                "Rank",
                "Test Name",
                "Execution Status",
                "Risk Score",
                "Risk Level",
                "Threshold Breach Summary",
                "Recommended Action",
                "Total Scenario Duration",
                "Actual Error %",
                "Average Response Time",
                "P95 Response Time",
                "Max Response Time",
                "Failure Message"
        };

        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, BASE_COL + i, headers[i], headerStyle);
        }

        int rank = 1;
        // Populate ranked rows
        for (PerformanceExecutionResult result : sortedByRisk) {
            Row row = sheet.createRow(rowIndex++);
            int col = BASE_COL;

            createCell(row, col++, rank++, normalStyle);
            createCell(row, col++, result.getTestName(), normalStyle);

            // Execution status with status-specific styling
            Cell statusCell = row.createCell(col++);
            statusCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

            createCell(row, col++, result.getRiskScore(), normalStyle);
            createCell(row, col++, result.getRiskLevel(), normalStyle);
            createCell(row, col++, result.getThresholdBreachSummary(), normalStyle);
            createCell(row, col++, result.getRecommendedAction(), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getTotalScenarioDurationMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatPercent(result.getErrorPercent()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxResponseTimeMs()), normalStyle);
            createCell(row, col++, safe(result.getFailureMessage()), normalStyle);
        }

        // Freeze pane and set auto-filter for convenience
        sheet.createFreezePane(BASE_COL, BASE_ROW + 1);
        sheet.setAutoFilter(new CellRangeAddress(BASE_ROW + 1, Math.max(BASE_ROW + 1, rowIndex - 1), BASE_COL, BASE_COL + headers.length - 1));
    }

    // ========================================================================
    // ANOMALIES
    // ========================================================================

    /**
     * Build the anomalies sheet that highlights scenarios requiring immediate review.
     *
     * <p>
     * Anomalies include unexpected failures, high risk score, threshold breaches, or any non-zero errors.
     * </p>
     */
    private void writeAnomaliesSheet(XSSFWorkbook workbook,
                                     PerformanceRunReport runReport,
                                     CellStyle titleStyle,
                                     CellStyle headerStyle,
                                     CellStyle normalStyle,
                                     CellStyle passStyle,
                                     CellStyle failStyle,
                                     CellStyle warningStyle,
                                     CellStyle infoStyle) {
        Sheet sheet = workbook.createSheet("Anomalies");

        int rowIndex = BASE_ROW;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Anomalies / Attention Needed", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, BASE_COL, "Scenarios requiring the most immediate review.", normalStyle);

        // Filter anomalies from the run report
        List<PerformanceExecutionResult> anomalies = getAnomalies(runReport);

        String[] headers = {
                "Test Name",
                "Execution Status",
                "Risk Score",
                "Risk Level",
                "Why Flagged",
                "Threshold Breach Summary",
                "Recommended Action",
                "Total Scenario Duration",
                "Actual Error %",
                "Average Response Time",
                "P95 Response Time",
                "Max Response Time",
                "Failure Message"
        };

        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, BASE_COL + i, headers[i], headerStyle);
        }

        // If none found, provide a clear note to the reviewer
        if (anomalies.isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, BASE_COL, "No anomalies detected in this run.", normalStyle);
            sheet.createFreezePane(BASE_COL, BASE_ROW + 1);
            return;
        }

        // Populate anomalies rows
        for (PerformanceExecutionResult result : anomalies) {
            Row row = sheet.createRow(rowIndex++);
            int col = BASE_COL;

            createCell(row, col++, result.getTestName(), normalStyle);

            // status styled cell
            Cell statusCell = row.createCell(col++);
            statusCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

            createCell(row, col++, result.getRiskScore(), normalStyle);
            createCell(row, col++, result.getRiskLevel(), normalStyle);
            createCell(row, col++, buildAnomalyReason(result), normalStyle); // human readable reason why flagged
            createCell(row, col++, result.getThresholdBreachSummary(), normalStyle);
            createCell(row, col++, result.getRecommendedAction(), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getTotalScenarioDurationMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatPercent(result.getErrorPercent()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxResponseTimeMs()), normalStyle);
            createCell(row, col++, safe(result.getFailureMessage()), normalStyle);
        }

        // Freeze and filter for easy navigation in Excel
        sheet.createFreezePane(BASE_COL, BASE_ROW + 1);
        sheet.setAutoFilter(new CellRangeAddress(BASE_ROW + 1, Math.max(BASE_ROW + 1, rowIndex - 1), BASE_COL, BASE_COL + headers.length - 1));
    }

    // ========================================================================
    // READABLE REPORT
    // ========================================================================

    /**
     * Build a narrative-style readable report sheet where each scenario is presented as a
     * short human-readable section. Intended for less-technical stakeholders.
     */
    private void writeReadableReportSheet(XSSFWorkbook workbook,
                                          PerformanceRunReport runReport,
                                          CellStyle titleStyle,
                                          CellStyle headerStyle,
                                          CellStyle normalStyle,
                                          CellStyle passStyle,
                                          CellStyle failStyle,
                                          CellStyle warningStyle,
                                          CellStyle infoStyle) {
        Sheet sheet = workbook.createSheet("Readable_Report");

        int rowIndex = BASE_ROW;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Readable Performance Report", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, BASE_COL, "Scenario-by-scenario narrative view for mixed business and technical readers.", normalStyle);

        rowIndex++;

        // For each scenario generate a compact narrative block with labels and content
        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row scenarioTitleRow = sheet.createRow(rowIndex++);
            createCell(scenarioTitleRow, BASE_COL, "Scenario: " + safe(result.getTestName()), titleStyle);

            Row statusRow = sheet.createRow(rowIndex++);
            createCell(statusRow, BASE_COL, "Execution Status", headerStyle);

            Cell statusValueCell = statusRow.createCell(BASE_COL + 1);
            statusValueCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusValueCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

            // Reuse createKeyValueRow to keep label/value presentation consistent
            rowIndex = createKeyValueRow(sheet, rowIndex, "Risk",
                    result.getRiskLevel() + " (Score: " + result.getRiskScore() + ")", headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Threshold breaches",
                    safe(result.getThresholdBreachSummary()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Recommended action",
                    safe(result.getRecommendedAction()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Response time assessment",
                    safe(result.getResponseTimeAssessment()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Error assessment",
                    safe(result.getErrorAssessment()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Stability assessment",
                    safe(result.getStabilityAssessment()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "First failure indicator",
                    safe(result.getFirstFailureIndicator()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Final conclusion",
                    safe(result.getFinalConclusion()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Average response time",
                    PerformanceExcelFormatHelper.formatMillisecondsDetailed(result.getAverageResponseTimeMs()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "P95 response time",
                    PerformanceExcelFormatHelper.formatMillisecondsDetailed(result.getP95ResponseTimeMs()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Maximum response time",
                    PerformanceExcelFormatHelper.formatMillisecondsDetailed(result.getMaxResponseTimeMs()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Actual error percent",
                    PerformanceExcelFormatHelper.formatPercent(result.getErrorPercent()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Failure message",
                    safe(result.getFailureMessage()), headerStyle, normalStyle);

            rowIndex++;
        }

        // Freeze top rows so title remains visible when scrolling
        sheet.createFreezePane(BASE_COL, BASE_ROW + 1);
    }

    // ========================================================================
    // CHARTS SHEET
    // ========================================================================

    /**
     * Build a separate charts sheet containing one combined performance view and the
     * numeric values used as source data immediately below the charts.
     */
    private void writeChartsSheet(XSSFWorkbook workbook,
                                  PerformanceRunReport runReport,
                                  CellStyle titleStyle,
                                  CellStyle headerStyle,
                                  CellStyle normalStyle) {
        XSSFSheet sheet = workbook.createSheet("Charts");

        int rowIndex = BASE_ROW;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Main Performance Chart", titleStyle);

        Row noteRow = sheet.createRow(rowIndex++);
        createCell(noteRow, BASE_COL,
                "One combined view of the most important scenario metrics. Numeric source values stay visible below for business review.",
                normalStyle);

        rowIndex += 2;

        // Create a numeric table to act as the chart's source data
        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, BASE_COL + 0, "Scenario", headerStyle);
        createCell(headerRow, BASE_COL + 1, "Avg Response (sec)", headerStyle);
        createCell(headerRow, BASE_COL + 2, "P95 Response (sec)", headerStyle);
        createCell(headerRow, BASE_COL + 3, "Error %", headerStyle);
        createCell(headerRow, BASE_COL + 4, "Total Samples", headerStyle);
        createCell(headerRow, BASE_COL + 5, "Total Errors", headerStyle);
        createCell(headerRow, BASE_COL + 6, "Risk Score", headerStyle);
        createCell(headerRow, BASE_COL + 7, "Duration (sec)", headerStyle);

        int dataStartRow = rowIndex;

        // Fill numeric rows with scenario values
        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, BASE_COL + 0, result.getTestName(), normalStyle);
            createCell(row, BASE_COL + 1, toSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, BASE_COL + 2, toSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, BASE_COL + 3, result.getErrorPercent(), normalStyle);
            createCell(row, BASE_COL + 4, result.getTotalSamples(), normalStyle);
            createCell(row, BASE_COL + 5, result.getTotalErrors(), normalStyle);
            createCell(row, BASE_COL + 6, result.getRiskScore(), normalStyle);
            createCell(row, BASE_COL + 7, toSeconds(result.getTotalScenarioDurationMs()), normalStyle);
        }

        // Create a combined chart mixing bars and lines to show latency, risk, error percent and volumes
        createCombinedMainChart(
                sheet,
                "Combined Performance Overview",
                dataStartRow,
                rowIndex - 1,
                BASE_COL + 0,
                BASE_COL + 1,
                BASE_COL + 2,
                BASE_COL + 3,
                BASE_COL + 4,
                BASE_COL + 5,
                BASE_COL + 6,
                BASE_COL + 7,
                BASE_COL + 8,
                BASE_ROW + 1,
                BASE_COL + 23,
                BASE_ROW + 21
        );

        sheet.createFreezePane(BASE_COL, BASE_ROW + 1);
    }

    // ========================================================================
    // GLOSSARY
    // ========================================================================

    /**
     * Create a small glossary sheet defining terms used across the report. Useful for readers
     * that are unfamiliar with performance testing terminology.
     */
    private void writeGlossarySheet(XSSFWorkbook workbook,
                                    CellStyle titleStyle,
                                    CellStyle headerStyle,
                                    CellStyle normalStyle) {
        Sheet sheet = workbook.createSheet("Glossary");

        int rowIndex = BASE_ROW;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, BASE_COL, "Glossary", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, BASE_COL, "Definitions of terms used throughout the report.", normalStyle);

        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, BASE_COL + 0, "Term", headerStyle);
        createCell(headerRow, BASE_COL + 1, "Meaning", headerStyle);

        // Populate common terms and definitions
        rowIndex = createKeyValueRow(sheet, rowIndex, "P95 Response Time", "95% of responses finished at or below this time.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Average Response Time", "Average time for requests in the scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Error %", "Percentage of requests that failed.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Risk Score", "Framework-computed risk severity score for a scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "PASS", "Scenario completed within configured rules and thresholds.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "FAIL", "Scenario failed thresholds or execution validations.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "EXPECTED_FAIL_CONFIRMED", "Scenario was designed to fail and did fail as expected.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "EXPECTED_FAIL_NOT_TRIGGERED", "Scenario was supposed to fail, but the expected failure did not happen.", headerStyle, normalStyle);
        createKeyValueRow(sheet, rowIndex, "SKIPPED", "The scenario did not run.", headerStyle, normalStyle);

        sheet.createFreezePane(BASE_COL, BASE_ROW + 2);
    }

    // ========================================================================
    // COUNTERS / ANALYSIS HELPERS
    // ========================================================================

    /**
     * Count scenarios with a risk level of High or Critical (case-insensitive).
     */
    private long countHighOrCriticalRiskScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> {
                    String level = r.getRiskLevel();
                    return level != null && ("High".equalsIgnoreCase(level) || "Critical".equalsIgnoreCase(level));
                })
                .count();
    }

    /**
     * Count scenarios that had any request errors (non-zero errors or error percent > 0).
     */
    private long countErrorScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> r.getTotalErrors() > 0 || r.getErrorPercent() > 0.0)
                .count();
    }

    /**
     * Count scenarios that contain a threshold breach summary (non-blank and not equal to the documented NO_THRESHOLD_BREACHES).
     */
    private long countThresholdBreachScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> {
                    String breachSummary = r.getThresholdBreachSummary();
                    return breachSummary != null
                            && !breachSummary.isBlank()
                            && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(breachSummary.trim());
                })
                .count();
    }

    /**
     * Count scenarios that are not considered anomalies by the isAnomaly() rules.
     */
    private long countNoIssueScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> !isAnomaly(r))
                .count();
    }

    /**
     * Return a sorted list of anomaly scenarios using the anomaly detection rules and sorted by risk descending.
     */
    private List<PerformanceExecutionResult> getAnomalies(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(this::isAnomaly)
                .sorted(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Simple anomaly detection rules used by the report to highlight scenarios.
     *
     * Rules considered anomalies:
     * - Execution status is FAIL or EXPECTED_FAIL_NOT_TRIGGERED
     * - Risk score >= 51 (threshold chosen by framework)
     * - Threshold breach summary exists and is not the default "no breaches" text
     * - Any non-zero total errors
     */
    private boolean isAnomaly(PerformanceExecutionResult result) {
        if (result == null) {
            return false;
        }

        if (result.getExecutionStatus() == PerformanceExecutionStatus.FAIL) {
            return true;
        }

        if (result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED) {
            return true;
        }

        if (result.getRiskScore() >= 51) {
            return true;
        }

        String breachSummary = result.getThresholdBreachSummary();
        if (breachSummary != null
                && !breachSummary.isBlank()
                && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(breachSummary.trim())) {
            return true;
        }

        return result.getTotalErrors() > 0;
    }

    /**
     * Provide a human-readable reason for why a scenario was flagged as an anomaly.
     * The order of checks mirrors isAnomaly() so the reason is consistent with detection.
     */
    private String buildAnomalyReason(PerformanceExecutionResult result) {
        if (result.getExecutionStatus() == PerformanceExecutionStatus.FAIL) {
            return "Scenario failed unexpectedly.";
        }

        if (result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED) {
            return "Expected failure did not trigger.";
        }

        String breachSummary = result.getThresholdBreachSummary();
        if (breachSummary != null
                && !breachSummary.isBlank()
                && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(breachSummary.trim())) {
            return breachSummary;
        }

        if (result.getRiskScore() >= 51) {
            return "High scenario risk score detected.";
        }

        if (result.getTotalErrors() > 0) {
            return "Non-zero request errors detected.";
        }

        return "Attention needed.";
    }

    // ========================================================================
    // CHART HELPERS
    // ========================================================================

    /**
     * Create a simple pie chart anchored in the provided sheet range.
     *
     * @param sheet         target sheet
     * @param chartTitle    visible title on the chart
     * @param firstRow      first row of the category/value table (inclusive)
     * @param lastRow       last row of the category/value table (inclusive)
     * @param categoryColumn column index for category labels
     * @param valueColumn   column index for numeric values
     * @param anchorCol1    left column anchor for the chart
     * @param anchorRow1    top row anchor for the chart
     * @param anchorCol2    right column anchor for the chart
     * @param anchorRow2    bottom row anchor for the chart
     */
    private void createPieChart(XSSFSheet sheet,
                                String chartTitle,
                                int firstRow,
                                int lastRow,
                                int categoryColumn,
                                int valueColumn,
                                int anchorCol1,
                                int anchorRow1,
                                int anchorCol2,
                                int anchorRow2) {

        // Guard for empty data ranges
        if (lastRow < firstRow) {
            return;
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchorCol1, anchorRow1, anchorCol2, anchorRow2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(chartTitle);
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

        // Data sources for categories (strings) and values (numeric doubles)
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, categoryColumn, categoryColumn)
        );

        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, valueColumn, valueColumn)
        );

        XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
        XDDFChartData.Series series = data.addSeries(categories, values);
        series.setTitle(chartTitle, null);

        chart.plot(data);
    }

    /**
     * Create a small bar chart used in the executive summary to highlight critical metrics.
     */
    private void createExecutiveHighlightsBarChart(XSSFSheet sheet,
                                                   String chartTitle,
                                                   int firstRow,
                                                   int lastRow,
                                                   int categoryColumn,
                                                   int valueColumn,
                                                   int anchorCol1,
                                                   int anchorRow1,
                                                   int anchorCol2,
                                                   int anchorRow2,
                                                   String valueAxisTitle) {

        if (lastRow < firstRow) {
            return;
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchorCol1, anchorRow1, anchorCol2, anchorRow2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(chartTitle);
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);

        // Category axis (bottom) is the metric name, value axis (left) is the numeric measure (seconds)
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Metric");

        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(valueAxisTitle);

        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, categoryColumn, categoryColumn)
        );

        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, valueColumn, valueColumn)
        );

        // Bar chart configured to display one series with varied colors
        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        data.setVaryColors(true);

        XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(categories, values);
        series.setTitle("Critical Time Values", null);

        chart.plot(data);
    }

    /**
     * Create a multi-series bar chart anchored into the sheet. Each series corresponds to one numeric column.
     */
    private void createMultiSeriesBarChart(XSSFSheet sheet,
                                           String chartTitle,
                                           int firstRow,
                                           int lastRow,
                                           int categoryColumn,
                                           int[] valueColumns,
                                           String[] seriesTitles,
                                           int anchorCol1,
                                           int anchorRow1,
                                           int anchorCol2,
                                           int anchorRow2,
                                           String valueAxisTitle) {

        // Validate input lengths and non-empty data
        if (lastRow < firstRow || valueColumns == null || seriesTitles == null || valueColumns.length != seriesTitles.length) {
            return;
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchorCol1, anchorRow1, anchorCol2, anchorRow2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(chartTitle);
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Scenario");

        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(valueAxisTitle);

        // Category axis is a column of scenario names
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, categoryColumn, categoryColumn)
        );

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        data.setVaryColors(true);

        // Add each series to the chart using its corresponding title
        for (int i = 0; i < valueColumns.length; i++) {
            XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet,
                    new CellRangeAddress(firstRow, lastRow, valueColumns[i], valueColumns[i])
            );
            XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(categories, values);
            series.setTitle(seriesTitles[i], null);
        }

        chart.plot(data);
    }

    /**
     * Create a combined chart with bars for latency/risk/duration and lines for error percent and volumes.
     * This chart uses two vertical axes (left/right) so mixed units can be displayed.
     */
    private void createCombinedMainChart(XSSFSheet sheet,
                                         String chartTitle,
                                         int firstRow,
                                         int lastRow,
                                         int categoryColumn,
                                         int avgColumn,
                                         int p95Column,
                                         int errorPercentColumn,
                                         int totalSamplesColumn,
                                         int totalErrorsColumn,
                                         int riskScoreColumn,
                                         int durationColumn,
                                         int anchorCol1,
                                         int anchorRow1,
                                         int anchorCol2,
                                         int anchorRow2) {

        if (lastRow < firstRow) {
            return;
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchorCol1, anchorRow1, anchorCol2, anchorRow2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(chartTitle);
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

        // Category axis (scenarios)
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Scenario");

        // Left axis for seconds/risk score (bars)
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Seconds / Risk Score");

        // Right axis for error percent and volumes (lines)
        XDDFValueAxis rightAxis = chart.createValueAxis(AxisPosition.RIGHT);
        rightAxis.setTitle("Error % / Volumes");
        rightAxis.setCrosses(AxisCrosses.MAX);

        // Data sources
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, categoryColumn, categoryColumn)
        );

        XDDFNumericalDataSource<Double> avgValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, avgColumn, avgColumn));
        XDDFNumericalDataSource<Double> p95Values = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, p95Column, p95Column));
        XDDFNumericalDataSource<Double> riskValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, riskScoreColumn, riskScoreColumn));
        XDDFNumericalDataSource<Double> durationValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, durationColumn, durationColumn));

        // Add bar series to the left axis
        XDDFBarChartData barData = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        barData.setBarDirection(BarDirection.COL);
        barData.setVaryColors(true);

        barData.addSeries(categories, avgValues).setTitle("Avg Response (sec)", null);
        barData.addSeries(categories, p95Values).setTitle("P95 Response (sec)", null);
        barData.addSeries(categories, riskValues).setTitle("Risk Score", null);
        barData.addSeries(categories, durationValues).setTitle("Duration (sec)", null);

        chart.plot(barData);

        // Prepare line series for right axis
        XDDFNumericalDataSource<Double> errorValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, errorPercentColumn, errorPercentColumn));
        XDDFNumericalDataSource<Double> sampleValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, totalSamplesColumn, totalSamplesColumn));
        XDDFNumericalDataSource<Double> totalErrorValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, totalErrorsColumn, totalErrorsColumn));

        XDDFLineChartData lineData = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, rightAxis);

        // Error percent line
        XDDFLineChartData.Series errorSeries = (XDDFLineChartData.Series) lineData.addSeries(categories, errorValues);
        errorSeries.setTitle("Error %", null);
        errorSeries.setSmooth(false);
        errorSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        // Total samples line
        XDDFLineChartData.Series sampleSeries = (XDDFLineChartData.Series) lineData.addSeries(categories, sampleValues);
        sampleSeries.setTitle("Total Samples", null);
        sampleSeries.setSmooth(false);
        sampleSeries.setMarkerStyle(MarkerStyle.DIAMOND);

        // Total errors line
        XDDFLineChartData.Series totalErrorSeries = (XDDFLineChartData.Series) lineData.addSeries(categories, totalErrorValues);
        totalErrorSeries.setTitle("Total Errors", null);
        totalErrorSeries.setSmooth(false);
        totalErrorSeries.setMarkerStyle(MarkerStyle.SQUARE);

        chart.plot(lineData);
    }

    // ========================================================================
    // STYLE HELPERS
    // ========================================================================

    /**
     * Map execution status to an appropriate cell style (pass/fail/warning/info).
     */
    private CellStyle resolveStatusStyle(PerformanceExecutionStatus status,
                                         CellStyle passStyle,
                                         CellStyle failStyle,
                                         CellStyle warningStyle,
                                         CellStyle infoStyle) {
        if (status == null) {
            return warningStyle;
        }

        return switch (status) {
            case PASS -> passStyle;
            case FAIL -> failStyle;
            case EXPECTED_FAIL_CONFIRMED -> infoStyle;
            case EXPECTED_FAIL_NOT_TRIGGERED, SKIPPED -> warningStyle;
        };
    }

    /**
     * Create a two-column key/value row. Returns the next row index (rowIndex + 1) for convenience.
     */
    private int createKeyValueRow(Sheet sheet,
                                  int rowIndex,
                                  String key,
                                  String value,
                                  CellStyle keyStyle,
                                  CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, BASE_COL + 0, key, keyStyle);
        createCell(row, BASE_COL + 1, safe(value), valueStyle);
        return rowIndex + 1;
    }

    /**
     * Create a cell with a String value and apply style.
     */
    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(safe(value));
        cell.setCellStyle(style);
    }

    /**
     * Create a cell with a long numeric value and apply style.
     */
    private void createCell(Row row, int columnIndex, long value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Create a cell with an int numeric value and apply style.
     */
    private void createCell(Row row, int columnIndex, int value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Create a cell with a double numeric value and apply style.
     */
    private void createCell(Row row, int columnIndex, double value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Create a title cell style used across sheets.
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    /**
     * Create a section header style used in-sheet to separate content blocks.
     */
    private CellStyle createSectionStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    /**
     * Create a header cell style used for table headers (bold + borders).
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        setThinBorders(style);
        return style;
    }

    /**
     * Create a normal cell style used for most table content (wrap + borders).
     */
    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        setThinBorders(style);
        return style;
    }

    /**
     * Create a status-style with a colored background used for execution status cells.
     */
    private CellStyle createStatusStyle(Workbook workbook, IndexedColors fillColor) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        style.setFillForegroundColor(fillColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorders(style);
        return style;
    }

    /**
     * Apply thin borders to all sides of the given cell style.
     */
    private void setThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    // ========================================================================
    // SHEET SIZING
    // ========================================================================

    /**
     * Auto-size all columns on every sheet, with a cap to avoid extreme widths.
     *
     * <p>
     * After auto-sizing we still enforce an absolute maximum width (approximate characters in Excel units).
     * </p>
     */
    private void autoSizeAllColumns(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            int maxColumns = findMaxColumnCount(sheet);
            for (int col = 0; col < maxColumns; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                int maxWidth = 12000;
                if (currentWidth > maxWidth) {
                    sheet.setColumnWidth(col, maxWidth);
                }
            }
        }
    }

    /**
     * Apply business-friendly minimum widths for certain columns on key sheets so text isn't truncated.
     * These values are tuned for typical report layouts and can be adjusted for specific environments.
     */
    private void enforceBusinessFriendlyColumnWidths(Workbook workbook) {
        Sheet executive = workbook.getSheet("Executive_Summary");
        if (executive != null) {
            setMinimumWidth(executive, BASE_COL + 0, 7500);
            setMinimumWidth(executive, BASE_COL + 1, 10000);
            setMinimumWidth(executive, BASE_COL + 2, 7000);
            setMinimumWidth(executive, BASE_COL + 3, 4500);
            setMinimumWidth(executive, BASE_COL + 4, 3500);
        }

        Sheet scenarioSummary = workbook.getSheet("Scenario_Summary");
        if (scenarioSummary != null) {
            setMinimumWidth(scenarioSummary, BASE_COL + 0, 8000);
            setMinimumWidth(scenarioSummary, BASE_COL + 1, 5000);
            setMinimumWidth(scenarioSummary, BASE_COL + 2, 3500);
            setMinimumWidth(scenarioSummary, BASE_COL + 3, 3500);
            setMinimumWidth(scenarioSummary, BASE_COL + 4, 8500);
            setMinimumWidth(scenarioSummary, BASE_COL + 5, 10000);
            setMinimumWidth(scenarioSummary, BASE_COL + 28, 8500);
            setMinimumWidth(scenarioSummary, BASE_COL + 29, 8500);
            setMinimumWidth(scenarioSummary, BASE_COL + 30, 8500);
            setMinimumWidth(scenarioSummary, BASE_COL + 31, 8500);
            setMinimumWidth(scenarioSummary, BASE_COL + 32, 10000);
        }
    }

    /**
     * Set a minimum width for a column only if the current width is smaller.
     */
    private void setMinimumWidth(Sheet sheet, int columnIndex, int width) {
        if (sheet.getColumnWidth(columnIndex) < width) {
            sheet.setColumnWidth(columnIndex, width);
        }
    }

    /**
     * Determine the maximum number of columns present in a sheet by checking each row's last cell.
     */
    private int findMaxColumnCount(Sheet sheet) {
        int maxColumns = 0;
        for (Row row : sheet) {
            if (row.getLastCellNum() > maxColumns) {
                maxColumns = row.getLastCellNum();
            }
        }
        return maxColumns;
    }

    // ========================================================================
    // VALIDATION / LOW-LEVEL HELPERS
    // ========================================================================

    /**
     * Basic validation of the run report input to ensure required fields exist before creating files.
     *
     * @throws IllegalArgumentException if the runReport is null or missing run root path
     */
    private void validateRunReport(PerformanceRunReport runReport) {
        if (runReport == null) {
            throw new IllegalArgumentException("PerformanceRunReport cannot be null.");
        }

        if (runReport.getRunRootPath() == null || runReport.getRunRootPath().isBlank()) {
            throw new IllegalArgumentException("Run root path cannot be null or blank.");
        }
    }

    /**
     * Convert milliseconds to seconds as a double (for chart source values).
     */
    private double toSeconds(long milliseconds) {
        return milliseconds / 1000.0;
    }

    /**
     * Round a double value to three decimal places (used for small chart labels where precision is unnecessary).
     */
    private double roundToThreeDecimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    /**
     * Safe wrapper to ensure string values written to Excel are never null; formatting helper provides default behavior.
     */
    private String safe(String value) {
        return PerformanceExcelFormatHelper.safeText(value);
    }
}
