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
 * Writes a single Excel report for the entire performance run.
 *
 * Enterprise-safe design principles:
 * - Keep all existing report sheets
 * - Improve only reporting / presentation logic
 * - Avoid changing execution behavior or scenario result calculations
 * - Keep report generation deterministic and reusable for large-scale framework use
 */
public class PerformanceExcelReportWriter {

    private static final String REPORT_FILE_NAME = "performance-run-report.xlsx";
    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";

    public Path writeRunReport(PerformanceRunReport runReport) {
        validateRunReport(runReport);

        Path runRootPath = Path.of(runReport.getRunRootPath());
        Path reportPath = runRootPath.resolve(REPORT_FILE_NAME);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);
            CellStyle passStyle = createStatusStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle failStyle = createStatusStyle(workbook, IndexedColors.ROSE);
            CellStyle warningStyle = createStatusStyle(workbook, IndexedColors.LIGHT_YELLOW);
            CellStyle infoStyle = createStatusStyle(workbook, IndexedColors.PALE_BLUE);

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

            autoSizeAllColumns(workbook);
            enforceBusinessFriendlyColumnWidths(workbook);

            try (OutputStream outputStream = Files.newOutputStream(reportPath)) {
                workbook.write(outputStream);
            }

            return reportPath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel performance report: " + reportPath, e);
        }
    }

    // ========================================================================
    // EXECUTIVE SUMMARY
    // ========================================================================

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

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Performance Run Executive Summary", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, 0, "Business-friendly summary of run health, major risks, and critical scenario highlights.", normalStyle);

        rowIndex++;

        PerformanceExecutionResult slowestP95Scenario = runReport.getSlowestP95Scenario();
        PerformanceExecutionResult longestDurationScenario = runReport.getLongestDurationScenario();
        PerformanceExecutionResult shortestDurationScenario = runReport.getShortestDurationScenario();

        Row section1 = sheet.createRow(rowIndex++);
        createCell(section1, 0, "Run Overview", sectionStyle);

        rowIndex = createKeyValueRow(sheet, rowIndex, "Run Folder Name", runReport.getRunFolderName(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Run Root Path", runReport.getRunRootPath(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Execution Timestamp", runReport.getExecutionTimestamp(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Overall Conclusion", runReport.getOverallConclusion(), headerStyle, normalStyle);

        rowIndex++;

        Row section2 = sheet.createRow(rowIndex++);
        createCell(section2, 0, "Key Numbers", sectionStyle);

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

        Row section3 = sheet.createRow(rowIndex++);
        createCell(section3, 0, "Critical Business Highlights", sectionStyle);

        int importantHeaderRow = rowIndex++;
        Row importantHeader = sheet.createRow(importantHeaderRow);
        createCell(importantHeader, 0, "Metric", headerStyle);
        createCell(importantHeader, 1, "Scenario", headerStyle);
        createCell(importantHeader, 2, "Display Value", headerStyle);
        createCell(importantHeader, 3, "Chart Value", headerStyle);
        createCell(importantHeader, 4, "Unit", headerStyle);

        int importantDataStart = rowIndex;

        Row r1 = sheet.createRow(rowIndex++);
        createCell(r1, 0, "Slowest P95 (sec)", normalStyle);
        createCell(r1, 1, slowestP95Scenario == null ? "N/A" : slowestP95Scenario.getTestName(), normalStyle);
        createCell(r1, 2, PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getSlowestP95ResponseTimeMs()), normalStyle);
        createCell(r1, 3, roundToThreeDecimals(toSeconds(runReport.getSlowestP95ResponseTimeMs())), normalStyle);
        createCell(r1, 4, "sec", normalStyle);

        Row r2 = sheet.createRow(rowIndex++);
        createCell(r2, 0, "Longest Duration (sec)", normalStyle);
        createCell(r2, 1, longestDurationScenario == null ? "N/A" : longestDurationScenario.getTestName(), normalStyle);
        createCell(r2, 2, longestDurationScenario == null
                ? "N/A"
                : PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getHighestScenarioDurationMs()), normalStyle);
        createCell(r2, 3, roundToThreeDecimals(toSeconds(runReport.getHighestScenarioDurationMs())), normalStyle);
        createCell(r2, 4, "sec", normalStyle);

        Row r3 = sheet.createRow(rowIndex++);
        createCell(r3, 0, "Shortest Duration (sec)", normalStyle);
        createCell(r3, 1, shortestDurationScenario == null ? "N/A" : shortestDurationScenario.getTestName(), normalStyle);
        createCell(r3, 2, shortestDurationScenario == null
                ? "N/A"
                : PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getShortestScenarioDurationMs()), normalStyle);
        createCell(r3, 3, roundToThreeDecimals(toSeconds(runReport.getShortestScenarioDurationMs())), normalStyle);
        createCell(r3, 4, "sec", normalStyle);

        int importantDataEnd = rowIndex - 1;

        createExecutiveHighlightsBarChart(
                sheet,
                "Critical Performance Highlights",
                importantDataStart,
                importantDataEnd,
                0,
                3,
                6,
                3,
                17,
                17,
                "Seconds"
        );

        rowIndex += 2;

        Row noteRow = sheet.createRow(rowIndex++);
        createCell(noteRow, 0, "Chart values are time-based and shown in seconds for cleaner executive review.", normalStyle);

        rowIndex++;

        Row section4 = sheet.createRow(rowIndex++);
        createCell(section4, 0, "Distribution Snapshots", sectionStyle);

        int businessOutcomeHeaderRow = rowIndex++;
        Row businessOutcomeHeader = sheet.createRow(businessOutcomeHeaderRow);
        createCell(businessOutcomeHeader, 0, "Business Outcome", headerStyle);
        createCell(businessOutcomeHeader, 1, "Count", headerStyle);

        int businessOutcomeDataStart = rowIndex;

        Row bo1 = sheet.createRow(rowIndex++);
        createCell(bo1, 0, "Passed", normalStyle);
        createCell(bo1, 1, runReport.getPassedScenarios(), normalStyle);

        Row bo2 = sheet.createRow(rowIndex++);
        createCell(bo2, 0, "Failed", normalStyle);
        createCell(bo2, 1, runReport.getFailedScenarios(), normalStyle);

        Row bo3 = sheet.createRow(rowIndex++);
        createCell(bo3, 0, "Expected Fail Confirmed", normalStyle);
        createCell(bo3, 1, runReport.getExpectedFailConfirmedScenarios(), normalStyle);

        Row bo4 = sheet.createRow(rowIndex++);
        createCell(bo4, 0, "Expected Fail Not Triggered", normalStyle);
        createCell(bo4, 1, runReport.getExpectedFailNotTriggeredScenarios(), normalStyle);

        Row bo5 = sheet.createRow(rowIndex++);
        createCell(bo5, 0, "Skipped", normalStyle);
        createCell(bo5, 1, runReport.getSkippedScenarios(), normalStyle);

        int businessOutcomeDataEnd = rowIndex - 1;

        int attentionHeaderRow = rowIndex++;
        Row attentionHeader = sheet.createRow(attentionHeaderRow);
        createCell(attentionHeader, 0, "Attention Area", headerStyle);
        createCell(attentionHeader, 1, "Count", headerStyle);

        int attentionDataStart = rowIndex;

        Row at1 = sheet.createRow(rowIndex++);
        createCell(at1, 0, "No Issue Detected", normalStyle);
        createCell(at1, 1, countNoIssueScenarios(runReport), normalStyle);

        Row at2 = sheet.createRow(rowIndex++);
        createCell(at2, 0, "Threshold Breach", normalStyle);
        createCell(at2, 1, countThresholdBreachScenarios(runReport), normalStyle);

        Row at3 = sheet.createRow(rowIndex++);
        createCell(at3, 0, "Errors Present", normalStyle);
        createCell(at3, 1, countErrorScenarios(runReport), normalStyle);

        Row at4 = sheet.createRow(rowIndex++);
        createCell(at4, 0, "High / Critical Risk", normalStyle);
        createCell(at4, 1, countHighOrCriticalRiskScenarios(runReport), normalStyle);

        int attentionDataEnd = rowIndex - 1;

        createPieChart(
                sheet,
                "Business Outcome Mix",
                businessOutcomeDataStart,
                businessOutcomeDataEnd,
                0,
                1,
                6,
                23,
                13,
                38
        );

        createPieChart(
                sheet,
                "Attention Needed Mix",
                attentionDataStart,
                attentionDataEnd,
                0,
                1,
                14,
                23,
                21,
                38
        );

        sheet.createFreezePane(0, 2);
    }

    // ========================================================================
    // SCENARIO SUMMARY
    // ========================================================================

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

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Scenario Performance Summary", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, 0, "Detailed scenario-level metrics with added business-review charts.", normalStyle);

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
            createCell(headerRow, i, headers[i], headerStyle);
        }

        int headerRowIndex = rowIndex - 1;

        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;

            createCell(row, col++, result.getTestName(), normalStyle);

            Cell statusCell = row.createCell(col++);
            statusCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

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

        rowIndex += 2;

        Row chartSection = sheet.createRow(rowIndex++);
        createCell(chartSection, 0, "Scenario Summary Charts", sectionStyle);

        Row chartNote = sheet.createRow(rowIndex++);
        createCell(chartNote, 0, "The charts below focus on latency, errors, risk, and run duration. Layout spacing has been widened to prevent chart overlap.", normalStyle);

        rowIndex++;

        List<PerformanceExecutionResult> topLatencyScenarios = runReport.getScenarioResults().stream()
                .sorted(Comparator.comparingLong(PerformanceExecutionResult::getP95ResponseTimeMs).reversed())
                .limit(10)
                .collect(Collectors.toList());

        int responseTableHeaderRow = rowIndex++;
        Row responseHeader = sheet.createRow(responseTableHeaderRow);
        createCell(responseHeader, 0, "Scenario", headerStyle);
        createCell(responseHeader, 1, "Avg Response (sec)", headerStyle);
        createCell(responseHeader, 2, "P95 Response (sec)", headerStyle);
        createCell(responseHeader, 3, "Max Response (sec)", headerStyle);

        int responseTableDataStart = rowIndex;

        for (PerformanceExecutionResult result : topLatencyScenarios) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, result.getTestName(), normalStyle);
            createCell(row, 1, toSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, 2, toSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, 3, toSeconds(result.getMaxResponseTimeMs()), normalStyle);
        }

        int responseTableDataEnd = rowIndex - 1;

        // First chart sits higher and ends earlier.
        createMultiSeriesBarChart(
                sheet,
                "Top Latency Scenarios",
                responseTableDataStart,
                responseTableDataEnd,
                0,
                new int[]{1, 2, 3},
                new String[]{"Avg Response (sec)", "P95 Response (sec)", "Max Response (sec)"},
                5,
                responseTableHeaderRow,
                16,
                responseTableHeaderRow + 14,
                "Seconds"
        );

        // Add deliberate blank space between first and second chart.
        rowIndex += 16;

        List<PerformanceExecutionResult> topRiskAndErrorScenarios = runReport.getScenarioResults().stream()
                .sorted(Comparator
                        .comparingDouble(PerformanceExecutionResult::getErrorPercent).reversed()
                        .thenComparing(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore).reversed())
                        .thenComparing(Comparator.comparingLong(PerformanceExecutionResult::getTotalScenarioDurationMs).reversed()))
                .limit(10)
                .collect(Collectors.toList());

        int stabilityTableHeaderRow = rowIndex++;
        Row stabilityHeader = sheet.createRow(stabilityTableHeaderRow);
        createCell(stabilityHeader, 0, "Scenario", headerStyle);
        createCell(stabilityHeader, 1, "Error %", headerStyle);
        createCell(stabilityHeader, 2, "Risk Score", headerStyle);
        createCell(stabilityHeader, 3, "Duration (sec)", headerStyle);
        createCell(stabilityHeader, 4, "Total Errors", headerStyle);

        int stabilityTableDataStart = rowIndex;

        for (PerformanceExecutionResult result : topRiskAndErrorScenarios) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, result.getTestName(), normalStyle);
            createCell(row, 1, result.getErrorPercent(), normalStyle);
            createCell(row, 2, result.getRiskScore(), normalStyle);
            createCell(row, 3, toSeconds(result.getTotalScenarioDurationMs()), normalStyle);
            createCell(row, 4, result.getTotalErrors(), normalStyle);
        }

        int stabilityTableDataEnd = rowIndex - 1;

        // Second chart is intentionally anchored much lower to prevent overlap.
        createMultiSeriesBarChart(
                sheet,
                "Error, Risk and Duration Focus",
                stabilityTableDataStart,
                stabilityTableDataEnd,
                0,
                new int[]{1, 2, 3, 4},
                new String[]{"Error %", "Risk Score", "Duration (sec)", "Total Errors"},
                5,
                stabilityTableHeaderRow,
                16,
                stabilityTableHeaderRow + 14,
                "Value"
        );

        sheet.createFreezePane(0, 3);
        sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, Math.max(headerRowIndex, scenarioTableEndRow), 0, headers.length - 1));
    }

    // ========================================================================
    // RISK ANALYSIS
    // ========================================================================

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

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Risk Analysis", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, 0, "Scenarios ranked by highest risk first.", normalStyle);

        List<PerformanceExecutionResult> sortedByRisk = runReport.getScenarioResults().stream()
                .sorted(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore).reversed())
                .collect(Collectors.toList());

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
            createCell(headerRow, i, headers[i], headerStyle);
        }

        int rank = 1;
        for (PerformanceExecutionResult result : sortedByRisk) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;

            createCell(row, col++, rank++, normalStyle);
            createCell(row, col++, result.getTestName(), normalStyle);

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

        sheet.createFreezePane(0, 2);
        sheet.setAutoFilter(new CellRangeAddress(2, Math.max(2, rowIndex - 1), 0, headers.length - 1));
    }

    // ========================================================================
    // ANOMALIES
    // ========================================================================

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

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Anomalies / Attention Needed", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, 0, "Scenarios requiring the most immediate review.", normalStyle);

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
            createCell(headerRow, i, headers[i], headerStyle);
        }

        if (anomalies.isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, 0, "No anomalies detected in this run.", normalStyle);
            sheet.createFreezePane(0, 2);
            return;
        }

        for (PerformanceExecutionResult result : anomalies) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;

            createCell(row, col++, result.getTestName(), normalStyle);

            Cell statusCell = row.createCell(col++);
            statusCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

            createCell(row, col++, result.getRiskScore(), normalStyle);
            createCell(row, col++, result.getRiskLevel(), normalStyle);
            createCell(row, col++, buildAnomalyReason(result), normalStyle);
            createCell(row, col++, result.getThresholdBreachSummary(), normalStyle);
            createCell(row, col++, result.getRecommendedAction(), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getTotalScenarioDurationMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatPercent(result.getErrorPercent()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, col++, PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxResponseTimeMs()), normalStyle);
            createCell(row, col++, safe(result.getFailureMessage()), normalStyle);
        }

        sheet.createFreezePane(0, 2);
        sheet.setAutoFilter(new CellRangeAddress(2, Math.max(2, rowIndex - 1), 0, headers.length - 1));
    }

    // ========================================================================
    // READABLE REPORT
    // ========================================================================

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

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Readable Performance Report", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, 0, "Scenario-by-scenario narrative view for mixed business and technical readers.", normalStyle);

        rowIndex++;

        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row scenarioTitleRow = sheet.createRow(rowIndex++);
            createCell(scenarioTitleRow, 0, "Scenario: " + safe(result.getTestName()), titleStyle);

            Row statusRow = sheet.createRow(rowIndex++);
            createCell(statusRow, 0, "Execution Status", headerStyle);

            Cell statusValueCell = statusRow.createCell(1);
            statusValueCell.setCellValue(safe(result.getExecutionStatus() == null ? null : result.getExecutionStatus().name()));
            statusValueCell.setCellStyle(resolveStatusStyle(result.getExecutionStatus(), passStyle, failStyle, warningStyle, infoStyle));

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

        sheet.createFreezePane(0, 2);
    }

    // ========================================================================
    // CHARTS SHEET
    // ========================================================================

    private void writeChartsSheet(XSSFWorkbook workbook,
                                  PerformanceRunReport runReport,
                                  CellStyle titleStyle,
                                  CellStyle headerStyle,
                                  CellStyle normalStyle) {
        XSSFSheet sheet = workbook.createSheet("Charts");

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Main Performance Chart", titleStyle);

        Row noteRow = sheet.createRow(rowIndex++);
        createCell(noteRow, 0,
                "One combined view of the most important scenario metrics. Numeric source values stay visible below for business review.",
                normalStyle);

        rowIndex += 2;

        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, "Scenario", headerStyle);
        createCell(headerRow, 1, "Avg Response (sec)", headerStyle);
        createCell(headerRow, 2, "P95 Response (sec)", headerStyle);
        createCell(headerRow, 3, "Error %", headerStyle);
        createCell(headerRow, 4, "Total Samples", headerStyle);
        createCell(headerRow, 5, "Total Errors", headerStyle);
        createCell(headerRow, 6, "Risk Score", headerStyle);
        createCell(headerRow, 7, "Duration (sec)", headerStyle);

        int dataStartRow = rowIndex;

        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, result.getTestName(), normalStyle);
            createCell(row, 1, toSeconds(result.getAverageResponseTimeMs()), normalStyle);
            createCell(row, 2, toSeconds(result.getP95ResponseTimeMs()), normalStyle);
            createCell(row, 3, result.getErrorPercent(), normalStyle);
            createCell(row, 4, result.getTotalSamples(), normalStyle);
            createCell(row, 5, result.getTotalErrors(), normalStyle);
            createCell(row, 6, result.getRiskScore(), normalStyle);
            createCell(row, 7, toSeconds(result.getTotalScenarioDurationMs()), normalStyle);
        }

        int dataEndRow = rowIndex - 1;

        createCombinedMainChart(
                sheet,
                "Combined Performance Overview",
                dataStartRow,
                dataEndRow,
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                9,
                2,
                24,
                22
        );

        sheet.createFreezePane(0, 2);
    }

    // ========================================================================
    // GLOSSARY
    // ========================================================================

    private void writeGlossarySheet(XSSFWorkbook workbook,
                                    CellStyle titleStyle,
                                    CellStyle headerStyle,
                                    CellStyle normalStyle) {
        Sheet sheet = workbook.createSheet("Glossary");

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Glossary", titleStyle);

        Row subtitleRow = sheet.createRow(rowIndex++);
        createCell(subtitleRow, 0, "Definitions of terms used throughout the report.", normalStyle);

        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, "Term", headerStyle);
        createCell(headerRow, 1, "Meaning", headerStyle);

        rowIndex = createKeyValueRow(sheet, rowIndex, "P95 Response Time", "95% of responses finished at or below this time.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Average Response Time", "Average time for requests in the scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Error %", "Percentage of requests that failed.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Risk Score", "Framework-computed risk severity score for a scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "PASS", "Scenario completed within configured rules and thresholds.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "FAIL", "Scenario failed thresholds or execution validations.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "EXPECTED_FAIL_CONFIRMED", "Scenario was designed to fail and did fail as expected.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "EXPECTED_FAIL_NOT_TRIGGERED", "Scenario was supposed to fail, but the expected failure did not happen.", headerStyle, normalStyle);
        createKeyValueRow(sheet, rowIndex, "SKIPPED", "The scenario did not run.", headerStyle, normalStyle);

        sheet.createFreezePane(0, 3);
    }

    // ========================================================================
    // COUNTERS / ANALYSIS HELPERS
    // ========================================================================

    private long countHighOrCriticalRiskScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> {
                    String level = r.getRiskLevel();
                    return level != null && ("High".equalsIgnoreCase(level) || "Critical".equalsIgnoreCase(level));
                })
                .count();
    }

    private long countErrorScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> r.getTotalErrors() > 0 || r.getErrorPercent() > 0.0)
                .count();
    }

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

    private long countNoIssueScenarios(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(r -> !isAnomaly(r))
                .count();
    }

    private List<PerformanceExecutionResult> getAnomalies(PerformanceRunReport runReport) {
        return runReport.getScenarioResults().stream()
                .filter(this::isAnomaly)
                .sorted(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore).reversed())
                .collect(Collectors.toList());
    }

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

        if (lastRow < firstRow) {
            return;
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, anchorCol1, anchorRow1, anchorCol2, anchorRow2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(chartTitle);
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

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

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        data.setVaryColors(true);

        XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(categories, values);
        series.setTitle("Critical Time Values", null);

        chart.plot(data);
    }

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

        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(firstRow, lastRow, categoryColumn, categoryColumn)
        );

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        data.setVaryColors(true);

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

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Scenario");

        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Seconds / Risk Score");

        XDDFValueAxis rightAxis = chart.createValueAxis(AxisPosition.RIGHT);
        rightAxis.setTitle("Error % / Volumes");
        rightAxis.setCrosses(AxisCrosses.MAX);

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

        XDDFBarChartData barData = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        barData.setBarDirection(BarDirection.COL);
        barData.setVaryColors(true);

        barData.addSeries(categories, avgValues).setTitle("Avg Response (sec)", null);
        barData.addSeries(categories, p95Values).setTitle("P95 Response (sec)", null);
        barData.addSeries(categories, riskValues).setTitle("Risk Score", null);
        barData.addSeries(categories, durationValues).setTitle("Duration (sec)", null);

        chart.plot(barData);

        XDDFNumericalDataSource<Double> errorValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, errorPercentColumn, errorPercentColumn));
        XDDFNumericalDataSource<Double> sampleValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, totalSamplesColumn, totalSamplesColumn));
        XDDFNumericalDataSource<Double> totalErrorValues = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(firstRow, lastRow, totalErrorsColumn, totalErrorsColumn));

        XDDFLineChartData lineData = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, rightAxis);

        XDDFLineChartData.Series errorSeries = (XDDFLineChartData.Series) lineData.addSeries(categories, errorValues);
        errorSeries.setTitle("Error %", null);
        errorSeries.setSmooth(false);
        errorSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        XDDFLineChartData.Series sampleSeries = (XDDFLineChartData.Series) lineData.addSeries(categories, sampleValues);
        sampleSeries.setTitle("Total Samples", null);
        sampleSeries.setSmooth(false);
        sampleSeries.setMarkerStyle(MarkerStyle.DIAMOND);

        XDDFLineChartData.Series totalErrorSeries = (XDDFLineChartData.Series) lineData.addSeries(categories, totalErrorValues);
        totalErrorSeries.setTitle("Total Errors", null);
        totalErrorSeries.setSmooth(false);
        totalErrorSeries.setMarkerStyle(MarkerStyle.SQUARE);

        chart.plot(lineData);
    }

    // ========================================================================
    // STYLE HELPERS
    // ========================================================================

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

    private int createKeyValueRow(Sheet sheet,
                                  int rowIndex,
                                  String key,
                                  String value,
                                  CellStyle keyStyle,
                                  CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, key, keyStyle);
        createCell(row, 1, safe(value), valueStyle);
        return rowIndex + 1;
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(safe(value));
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int columnIndex, long value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int columnIndex, int value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int columnIndex, double value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createSectionStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        setThinBorders(style);
        return style;
    }

    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        setThinBorders(style);
        return style;
    }

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

    private void setThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    // ========================================================================
    // SHEET SIZING
    // ========================================================================

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

    private void enforceBusinessFriendlyColumnWidths(Workbook workbook) {
        Sheet executive = workbook.getSheet("Executive_Summary");
        if (executive != null) {
            setMinimumWidth(executive, 0, 7500);
            setMinimumWidth(executive, 1, 10000);
            setMinimumWidth(executive, 2, 7000);
            setMinimumWidth(executive, 3, 4500);
            setMinimumWidth(executive, 4, 3500);
        }

        Sheet scenarioSummary = workbook.getSheet("Scenario_Summary");
        if (scenarioSummary != null) {
            setMinimumWidth(scenarioSummary, 0, 8000);
            setMinimumWidth(scenarioSummary, 1, 5000);
            setMinimumWidth(scenarioSummary, 2, 3500);
            setMinimumWidth(scenarioSummary, 3, 3500);
            setMinimumWidth(scenarioSummary, 4, 8500);
            setMinimumWidth(scenarioSummary, 5, 10000);
            setMinimumWidth(scenarioSummary, 28, 8500);
            setMinimumWidth(scenarioSummary, 29, 8500);
            setMinimumWidth(scenarioSummary, 30, 8500);
            setMinimumWidth(scenarioSummary, 31, 8500);
            setMinimumWidth(scenarioSummary, 32, 10000);
        }
    }

    private void setMinimumWidth(Sheet sheet, int columnIndex, int width) {
        if (sheet.getColumnWidth(columnIndex) < width) {
            sheet.setColumnWidth(columnIndex, width);
        }
    }

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

    private void validateRunReport(PerformanceRunReport runReport) {
        if (runReport == null) {
            throw new IllegalArgumentException("PerformanceRunReport cannot be null.");
        }

        if (runReport.getRunRootPath() == null || runReport.getRunRootPath().isBlank()) {
            throw new IllegalArgumentException("Run root path cannot be null or blank.");
        }
    }

    private double toSeconds(long milliseconds) {
        return milliseconds / 1000.0;
    }

    private double roundToThreeDecimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String safe(String value) {
        return PerformanceExcelFormatHelper.safeText(value);
    }
}