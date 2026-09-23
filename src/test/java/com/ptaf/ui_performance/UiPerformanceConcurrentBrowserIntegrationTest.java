package com.ptaf.ui_performance;

import com.ptaf.ui_performance.config.UiPerformanceConfiguration;
import com.ptaf.ui_performance.core.UiPerformanceEngine;
import com.ptaf.ui_performance.model.UiPerformanceJourney;
import com.ptaf.ui_performance.model.UiPerformanceRunResult;
import com.ptaf.ui_performance.model.UiPerformanceStep;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Local-only integration contract proving two real browsers begin one measured stage concurrently. */
public class UiPerformanceConcurrentBrowserIntegrationTest {

    @Test
    public void executesEveryRequestedRealBrowserUserInOneConcurrentLoadStage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 18765), 0);
        server.createContext("/ready", exchange -> {
            byte[] response = ("<html><body><main id='ready'>ready</main>"
                    + "<input id='email'>"
                    + "<select id='product'><option>Consumer Deposit</option></select>"
                    + "<button id='open' onclick=\"window.open('/popup','_blank')\">Open URL</button>"
                    + "</body></html>")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.createContext("/popup", exchange -> {
            byte[] response = "<html><body><button id='accept'>View and Accept</button></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            Assert.assertTrue(UiPerformanceConfiguration.isEnabled());
            Assert.assertTrue(UiPerformanceConfiguration.getRunProfile().headless());
            UiPerformanceJourney journey = new UiPerformanceJourney(
                    "Concurrent Browser Contract", UiPerformanceConfiguration.getConfiguredTargetUrl());
            journey.addStep(new UiPerformanceStep("Navigate ready", UiPerformanceStep.Action.NAVIGATE,
                    UiPerformanceConfiguration.getConfiguredRoute("ready"), null));
            journey.addStep(new UiPerformanceStep("Verify ready", UiPerformanceStep.Action.VERIFY_VISIBLE,
                    "CSS_#ready", null));
            journey.addStep(new UiPerformanceStep("Fill email", UiPerformanceStep.Action.FILL,
                    "CSS_#email", "performance@example.test"));
            journey.addStep(new UiPerformanceStep("Select product", UiPerformanceStep.Action.SELECT_OPTION,
                    "CSS_#product", "Consumer Deposit"));
            journey.addStep(new UiPerformanceStep("Open popup", UiPerformanceStep.Action.CLICK_AND_SWITCH_TO_POPUP,
                    "CSS_#open", null));
            journey.addStep(new UiPerformanceStep("Verify popup", UiPerformanceStep.Action.VERIFY_VISIBLE,
                    "CSS_#accept", null));

            UiPerformanceRunResult result = new UiPerformanceEngine().execute(journey);

            Assert.assertEquals(result.getStageResults().size(), 1);
            Assert.assertEquals(result.getStageResults().getFirst().getStage().virtualUsers(), 2);
            Assert.assertEquals(result.getMetrics().totalIterations(), 2,
                    "Both configured browser users must execute the journey.");
            Assert.assertEquals(result.getMetrics().passedIterations(), 2);
            Assert.assertEquals(result.getMetrics().failedIterations(), 0);
            Assert.assertTrue(result.getStageResults().getFirst().getFirstIterationStartSpreadMs() <= 1_000L,
                    "Zero-ramp browser users should start within one second of one another.");
            Assert.assertTrue(Files.size(result.getReportDirectory().resolve("performance-run-report.xlsx")) > 0);
            Assert.assertTrue(Files.size(result.getReportDirectory().resolve("ui_performance-summary.html")) > 0);
        } finally {
            server.stop(0);
        }
    }
}
