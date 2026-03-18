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
 * <p>Design goals:
 * <ul>
 *   <li>Executive summary should be leadership-friendly</li>
 *   <li>Only the most important charts should be shown</li>
 *   <li>Durations should be readable in seconds instead of confusing raw ms values</li>
 *   <li>Percentages should visibly include %</li>
 *   <li>One combined chart should summarize the main scenario-level metrics</li>
 * </ul>
 * </p>
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
                    workbook, runReport, titleStyle, headerStyle, normalStyle,
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

            try (OutputStream outputStream = Files.newOutputStream(reportPath)) {
                workbook.write(outputStream);
            }

            return reportPath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel performance report: " + reportPath, e);
        }
    }

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
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Performance Run Executive Summary");
        titleCell.setCellStyle(titleStyle);

        rowIndex++;

        PerformanceExecutionResult slowestP95Scenario = runReport.getSlowestP95Scenario();
        PerformanceExecutionResult highestErrorScenario = runReport.getHighestErrorScenario();
        PerformanceExecutionResult highestRiskScenario = runReport.getHighestRiskScenario();
        PerformanceExecutionResult longestDurationScenario = runReport.getLongestDurationScenario();

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
        createCell(section3, 0, "Important Scenarios", sectionStyle);

        rowIndex = createKeyValueRow(sheet, rowIndex, "Slowest P95 Scenario",
                slowestP95Scenario == null ? "N/A" : slowestP95Scenario.getTestName(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Slowest P95 Value",
                PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getSlowestP95ResponseTimeMs()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Highest Error Scenario",
                highestErrorScenario == null ? "N/A" : highestErrorScenario.getTestName(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Highest Risk Scenario",
                highestRiskScenario == null ? "N/A" : highestRiskScenario.getTestName(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Highest Risk Score",
                String.valueOf(runReport.getHighestRiskScore()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Longest Duration Scenario",
                longestDurationScenario == null ? "N/A" : longestDurationScenario.getTestName(), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Longest Duration",
                PerformanceExcelFormatHelper.formatMillisecondsDetailed(runReport.getHighestScenarioDurationMs()), headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Anomaly Count",
                String.valueOf(getAnomalies(runReport).size()), headerStyle, normalStyle);

        rowIndex += 2;

        int statusTableStartRow = rowIndex;
        Row statusHeader = sheet.createRow(rowIndex++);
        createCell(statusHeader, 0, "Execution Status", headerStyle);
        createCell(statusHeader, 1, "Count", headerStyle);

        Row status1 = sheet.createRow(rowIndex++);
        createCell(status1, 0, "PASS", normalStyle);
        createCell(status1, 1, runReport.getPassedScenarios(), normalStyle);

        Row status2 = sheet.createRow(rowIndex++);
        createCell(status2, 0, "FAIL", normalStyle);
        createCell(status2, 1, runReport.getFailedScenarios(), normalStyle);

        Row status3 = sheet.createRow(rowIndex++);
        createCell(status3, 0, "EXPECTED_FAIL_CONFIRMED", normalStyle);
        createCell(status3, 1, runReport.getExpectedFailConfirmedScenarios(), normalStyle);

        Row status4 = sheet.createRow(rowIndex++);
        createCell(status4, 0, "EXPECTED_FAIL_NOT_TRIGGERED", normalStyle);
        createCell(status4, 1, runReport.getExpectedFailNotTriggeredScenarios(), normalStyle);

        Row status5 = sheet.createRow(rowIndex++);
        createCell(status5, 0, "SKIPPED", normalStyle);
        createCell(status5, 1, runReport.getSkippedScenarios(), normalStyle);

        int statusTableEndRow = rowIndex - 1;

        rowIndex += 2;

        int riskTableStartRow = rowIndex;
        Row riskHeader = sheet.createRow(rowIndex++);
        createCell(riskHeader, 0, "Risk Level", headerStyle);
        createCell(riskHeader, 1, "Count", headerStyle);

        Row risk1 = sheet.createRow(rowIndex++);
        createCell(risk1, 0, "Low", normalStyle);
        createCell(risk1, 1, countRiskLevel(runReport, "Low"), normalStyle);

        Row risk2 = sheet.createRow(rowIndex++);
        createCell(risk2, 0, "Medium", normalStyle);
        createCell(risk2, 1, countRiskLevel(runReport, "Medium"), normalStyle);

        Row risk3 = sheet.createRow(rowIndex++);
        createCell(risk3, 0, "High", normalStyle);
        createCell(risk3, 1, countRiskLevel(runReport, "High"), normalStyle);

        Row risk4 = sheet.createRow(rowIndex++);
        createCell(risk4, 0, "Critical", normalStyle);
        createCell(risk4, 1, countRiskLevel(runReport, "Critical"), normalStyle);

        int riskTableEndRow = rowIndex - 1;

        createPieChart(sheet, "Execution Status Distribution",
                statusTableStartRow + 1, statusTableEndRow, 0, 1,
                4, 1, 11, 16);

        createPieChart(sheet, "Risk Distribution",
                riskTableStartRow + 1, riskTableEndRow, 0, 1,
                12, 1, 19, 16);

        sheet.createFreezePane(0, 2);
    }

    private void writeScenarioSummarySheet(XSSFWorkbook workbook,
                                           PerformanceRunReport runReport,
                                           CellStyle titleStyle,
                                           CellStyle headerStyle,
                                           CellStyle normalStyle,
                                           CellStyle passStyle,
                                           CellStyle failStyle,
                                           CellStyle warningStyle,
                                           CellStyle infoStyle) {
        Sheet sheet = workbook.createSheet("Scenario_Summary");

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Scenario Performance Summary", titleStyle);
        rowIndex++;

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

        sheet.createFreezePane(0, 2);
        sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, Math.max(headerRowIndex, rowIndex - 1), 0, headers.length - 1));
    }

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
        rowIndex++;

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
        sheet.setAutoFilter(new CellRangeAddress(1, Math.max(1, rowIndex - 1), 0, headers.length - 1));
    }

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
        rowIndex++;

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
        sheet.setAutoFilter(new CellRangeAddress(1, Math.max(1, rowIndex - 1), 0, headers.length - 1));
    }

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
        rowIndex += 2;

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

            rowIndex = createKeyValueRow(sheet, rowIndex, "What was tested?",
                    safe(result.getHttpMethod()) + " " + safe(result.getFullTargetUrl()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Purpose",
                    safe(result.getTestPurpose()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Test Type",
                    safe(result.getPerformanceTestType()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Goal",
                    safe(result.getTestGoal()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "How was it tested?",
                    "Users=" + result.getUsers()
                            + ", Ramp-Up=" + result.getRampUpSeconds()
                            + " sec, Hold=" + result.getHoldSeconds()
                            + " sec, Iterations=" + result.getIterations()
                            + ", Mode=" + safe(result.getExecutionMode()),
                    headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Allowed thresholds",
                    "Error % <= " + PerformanceExcelFormatHelper.formatPercent(result.getMaxAllowedErrorPercent())
                            + ", Avg <= " + PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxAllowedAverageResponseTimeMs())
                            + ", P95 <= " + PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getMaxAllowedP95ResponseTimeMs()),
                    headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Scenario duration",
                    PerformanceExcelFormatHelper.formatMillisecondsDetailed(result.getTotalScenarioDurationMs()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "What happened?",
                    "Samples=" + result.getTotalSamples()
                            + ", Errors=" + result.getTotalErrors()
                            + ", Error %=" + PerformanceExcelFormatHelper.formatPercent(result.getErrorPercent())
                            + ", Avg=" + PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getAverageResponseTimeMs())
                            + ", P95=" + PerformanceExcelFormatHelper.formatMillisecondsAsSeconds(result.getP95ResponseTimeMs()),
                    headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Response Time Assessment",
                    safe(result.getResponseTimeAssessment()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Error Assessment",
                    safe(result.getErrorAssessment()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Stability Assessment",
                    safe(result.getStabilityAssessment()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Where failures started",
                    safe(result.getFirstFailureIndicator()), headerStyle, normalStyle);

            rowIndex = createKeyValueRow(sheet, rowIndex, "Final Conclusion",
                    safe(result.getFinalConclusion()), headerStyle, normalStyle);

            rowIndex += 2;
        }

        sheet.createFreezePane(0, 1);
    }

    private void writeChartsSheet(XSSFWorkbook workbook,
                                  PerformanceRunReport runReport,
                                  CellStyle titleStyle,
                                  CellStyle headerStyle,
                                  CellStyle normalStyle) {
        XSSFSheet sheet = workbook.createSheet("Charts");

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Main Performance Chart", titleStyle);

        Row descriptionRow = sheet.createRow(rowIndex++);
        createCell(descriptionRow, 0,
                "One combined view of the most important scenario metrics. Time values below are shown in readable seconds in tables, while chart source values stay numeric for plotting.",
                normalStyle);

        rowIndex += 2;

        int dataStartRow = rowIndex;
        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, "Scenario", headerStyle);
        createCell(headerRow, 1, "Avg Response (sec)", headerStyle);
        createCell(headerRow, 2, "P95 Response (sec)", headerStyle);
        createCell(headerRow, 3, "Error %", headerStyle);
        createCell(headerRow, 4, "Total Samples", headerStyle);
        createCell(headerRow, 5, "Total Errors", headerStyle);
        createCell(headerRow, 6, "Risk Score", headerStyle);
        createCell(headerRow, 7, "Duration (sec)", headerStyle);

        for (PerformanceExecutionResult result : runReport.getScenarioResults()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(safe(result.getTestName()));
            row.createCell(1).setCellValue(toSeconds(result.getAverageResponseTimeMs()));
            row.createCell(2).setCellValue(toSeconds(result.getP95ResponseTimeMs()));
            row.createCell(3).setCellValue(result.getErrorPercent());
            row.createCell(4).setCellValue(result.getTotalSamples());
            row.createCell(5).setCellValue(result.getTotalErrors());
            row.createCell(6).setCellValue(result.getRiskScore());
            row.createCell(7).setCellValue(toSeconds(result.getTotalScenarioDurationMs()));

            for (int i = 0; i <= 7; i++) {
                row.getCell(i).setCellStyle(normalStyle);
            }
        }

        int dataEndRow = rowIndex - 1;

        createCombinedMainChart(
                sheet,
                "Main Scenario Metrics",
                dataStartRow + 1,
                dataEndRow,
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                0,
                rowIndex + 1,
                18,
                rowIndex + 26
        );

        sheet.createFreezePane(0, 3);
    }

    private void writeGlossarySheet(XSSFWorkbook workbook,
                                    CellStyle titleStyle,
                                    CellStyle headerStyle,
                                    CellStyle normalStyle) {
        Sheet sheet = workbook.createSheet("Glossary");

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, "Performance Report Glossary", titleStyle);
        rowIndex += 2;

        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, "Term", headerStyle);
        createCell(headerRow, 1, "Meaning", headerStyle);

        rowIndex = createKeyValueRow(sheet, rowIndex, "Total Samples", "The total number of requests sent during the scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Total Errors", "The total number of failed or unsuccessful requests.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Error %", "The percentage of requests that failed out of all requests sent.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Min Response Time", "The fastest request response time recorded during the scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Average Response Time", "The average time taken for requests to complete.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "P95 Response Time", "95% of requests completed within this time. This is a very important performance stability indicator.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Max Response Time", "The slowest request response time recorded during the scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Total Scenario Duration", "The total end-to-end execution time for the full scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Users", "The number of virtual users simulated during the test.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Ramp-Up Seconds", "How long it takes to gradually start all virtual users.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Hold Seconds", "How long the test keeps running at the target load level.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Iterations", "How many repeated cycles of requests were configured for the scenario.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Execution Mode", "Whether the scenario ran by time duration or by fixed iterations.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Allowed Error %", "The maximum acceptable failure percentage configured for the test.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Allowed Avg Response", "The maximum acceptable average response time configured for the test.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Allowed P95 Response", "The maximum acceptable p95 response time configured for the test.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Risk Score", "A calculated score from 0 to 100 that estimates how risky the scenario result is.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Risk Level", "A readable severity level derived from the risk score, such as Low, Medium, High, or Critical.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Threshold Breach Summary", "A plain-English summary of which configured thresholds were exceeded.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "Recommended Action", "The suggested next step based on the scenario result and risk.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "PASS", "The scenario completed successfully and stayed within configured thresholds.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "FAIL", "The scenario failed unexpectedly or exceeded configured thresholds.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "EXPECTED_FAIL_CONFIRMED", "The scenario was intentionally designed to fail, and that failure was correctly detected.", headerStyle, normalStyle);
        rowIndex = createKeyValueRow(sheet, rowIndex, "EXPECTED_FAIL_NOT_TRIGGERED", "The scenario was supposed to fail, but the expected failure did not happen.", headerStyle, normalStyle);
        createKeyValueRow(sheet, rowIndex, "SKIPPED", "The scenario did not run.", headerStyle, normalStyle);

        sheet.createFreezePane(0, 3);
    }

    private long countRiskLevel(PerformanceRunReport runReport, String level) {
        return runReport.getScenarioResults().stream()
                .filter(r -> r.getRiskLevel() != null && r.getRiskLevel().equalsIgnoreCase(level))
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

    private int findMaxColumnCount(Sheet sheet) {
        int maxColumns = 0;
        for (Row row : sheet) {
            if (row.getLastCellNum() > maxColumns) {
                maxColumns = row.getLastCellNum();
            }
        }
        return maxColumns;
    }

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

    private String safe(String value) {
        return PerformanceExcelFormatHelper.safeText(value);
    }
}