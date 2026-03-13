package com.ptaf.performance.core;

import com.ptaf.performance.auth.PerformanceAuthTokenManager;
import com.ptaf.performance.builders.PerformanceTestPlanBuilder;
import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.headers.PerformanceHeaderManager;
import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import com.ptaf.performance.models.PerformanceRequest.AuthStrategy;
import com.ptaf.performance.reports.PerformanceReportManager;
import com.ptaf.performance.reports.PerformanceSummaryWriter;

import java.nio.file.Path;
import java.util.Map;

/**
 * Main enterprise entry point for performance execution.
 *
 * <p>This engine coordinates:
 * <ul>
 *   <li>request preparation</li>
 *   <li>auth strategy resolution</li>
 *   <li>header generation</li>
 *   <li>test plan creation</li>
 *   <li>execution</li>
 *   <li>assertions</li>
 *   <li>reporting</li>
 * </ul>
 * </p>
 */
public class PerformanceEngine extends BasePerformanceEngine {

    private final PerformanceAuthTokenManager authTokenManager = new PerformanceAuthTokenManager();

    public PerformanceExecutionResult runHttpTest(PerformanceRequest request) {
        return runHttpTest(
                request,
                PerformanceConfigurationProperties.getDefaultProfile(),
                PerformanceConfigurationProperties.getDefaultAssertionProfile()
        );
    }

    public PerformanceExecutionResult runHttpTest(PerformanceRequest request,
                                                  PerformanceProfile profile,
                                                  PerformanceAssertionProfile assertionProfile) {

        reportManager.ensureReportFoldersExist();

        Path dashboardPath = reportManager.prepareDashboardPath(request.getRequestName());
        Path jtlFilePath = reportManager.prepareJtlFilePath(request.getRequestName());
        Path summaryFilePath = reportManager.prepareSummaryFilePath(request.getRequestName());

        Map<String, String> resolvedHeaders = resolveHeaders(request);

        var testPlan = testPlanBuilder.buildHttpTestPlan(
                request,
                profile,
                resolvedHeaders,
                dashboardPath,
                jtlFilePath
        );

        PerformanceExecutionResult result = executionManager.execute(
                request.getRequestName(),
                testPlan,
                dashboardPath,
                jtlFilePath,
                summaryFilePath
        );

        assertionEngine.validate(result, assertionProfile);
        summaryWriter.writeTextSummary(result);

        return result;
    }

    /**
     * Exposes token manager for higher architect-controlled flows.
     *
     * <p>Useful for API + performance chaining and future token acquisition layers.</p>
     *
     * @return token manager
     */
    public PerformanceAuthTokenManager getAuthTokenManager() {
        return authTokenManager;
    }

    /**
     * Resolves final headers using request definition and auth strategy.
     *
     * @param request request model
     * @return final merged headers
     */
    private Map<String, String> resolveHeaders(PerformanceRequest request) {
        PerformanceHeaderManager headerManager = new PerformanceHeaderManager()
                .addRequestHeaders(request.getHeaders());

        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            headerManager.addContentType(request.getContentType());
        }

        if (request.getAcceptType() != null && !request.getAcceptType().isBlank()) {
            headerManager.addAccept(request.getAcceptType());
        }

        if (request.getAuthStrategy() == AuthStrategy.BEARER_TOKEN) {
            authTokenManager.applyBearerToken(request.getTokenAlias(), headerManager);
        } else if (request.getAuthStrategy() == AuthStrategy.BASIC_AUTH) {
            headerManager.addBasicAuth(
                    request.getBasicAuthUsername(),
                    request.getBasicAuthPassword()
            );
        }

        return headerManager.build();
    }
}