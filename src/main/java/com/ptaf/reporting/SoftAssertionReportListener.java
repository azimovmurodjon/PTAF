package com.ptaf.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.ptaf.softassert.SoftAssertionContext;
import com.ptaf.softassert.SoftAssertionContext.SoftFailure;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestStepFinished;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;

import java.util.List;

/**
 * SoftAssertionReportListener — a purely additive Cucumber {@link ConcurrentEventListener}
 * that makes soft-failed steps appear as FAILED (red) in Extent HTML and PDF reports.
 *
 * <h3>Why this is needed</h3>
 * <p>In soft assertion mode, {@code FrameCommonMethods.executeStep()} and
 * {@code PageCommonMethods.executeStep()} catch step failures silently and record them
 * in {@link SoftAssertionContext} without rethrowing the exception. Because Cucumber
 * determines a step's status purely from whether the step method threw an exception,
 * silently-caught failures appear as PASSED in the Cucumber/Extent report even though
 * they failed. This listener corrects that by intercepting the
 * {@link TestStepFinished} event after each step and, if new soft failures were recorded
 * during that step, retroactively marking the step as FAILED in the Extent report with
 * the failure message and screenshot note attached.</p>
 *
 * <h3>What this listener does NOT do</h3>
 * <ul>
 *   <li>It does NOT throw exceptions or stop test execution.</li>
 *   <li>It does NOT modify any existing class.</li>
 *   <li>It does NOT affect normal mode (when {@code soft_assertions.enabled: false}).</li>
 *   <li>It does NOT affect hook steps (only Gherkin scenario steps are corrected).</li>
 * </ul>
 *
 * <h3>Registration</h3>
 * <p>Add {@code "com.ptaf.reporting.SoftAssertionReportListener"} to the {@code plugin}
 * array in every {@code @CucumberOptions} runner class.</p>
 *
 * <h3>Thread safety</h3>
 * <p>{@link SoftAssertionContext} uses {@code ThreadLocal} storage, so this listener
 * is safe for parallel scenario execution.</p>
 */
public class SoftAssertionReportListener implements ConcurrentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SoftAssertionReportListener.class);

    /**
     * Register the {@link TestStepFinished} event handler with the Cucumber event bus.
     *
     * @param publisher the Cucumber event publisher provided by the framework
     */
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepFinished.class, this::onTestStepFinished);
    }

    /**
     * Called by Cucumber after every step (both Gherkin steps and hook steps) finishes.
     *
     * <p>If soft assertions are enabled and new failures were recorded in
     * {@link SoftAssertionContext} during this step, the step's Extent report entry is
     * updated to show {@link Status#FAIL} with the failure details attached. The
     * {@link SoftAssertionContext} reported-count is then advanced so the same failures
     * are not reported again for subsequent steps.</p>
     *
     * @param event the {@link TestStepFinished} event containing step result information
     */
    private void onTestStepFinished(TestStepFinished event) {
        // Only apply in soft assertion mode — zero effect in normal mode.
        if (!ConfigurationProperties.isSoftAssertionsEnabled()) {
            return;
        }

        // Skip hook steps (Before/After) — only process Gherkin scenario steps.
        if (event.getTestStep() instanceof HookTestStep) {
            return;
        }

        // Check if any new soft failures were recorded during this step.
        int unreported = SoftAssertionContext.getUnreportedFailureCount();
        if (unreported <= 0) {
            return; // No new failures — step passed normally, nothing to do.
        }

        // Get the failures added during this step.
        List<SoftFailure> newFailures = SoftAssertionContext.getUnreportedFailures();

        // Mark all new failures as reported so they are not processed again.
        SoftAssertionContext.markFailuresReported();

        // Get the step text for logging.
        String stepText = "(unknown step)";
        if (event.getTestStep() instanceof PickleStepTestStep) {
            try {
                stepText = ((PickleStepTestStep) event.getTestStep()).getStep().getText();
            } catch (Exception ignored) {
                // Step text extraction is best-effort.
            }
        }

        // Attempt to get the current Extent test node and mark it as failed.
        try {
            ExtentTest currentTest = ExtentCucumberAdapter.getCurrentStep();
            if (currentTest != null) {
                for (SoftFailure failure : newFailures) {
                    // Build a clear failure message for the report.
                    StringBuilder msg = new StringBuilder();
                    msg.append("<b style='color:red;'>⚠ SOFT ASSERTION FAILURE</b><br/>");
                    msg.append("<b>Step:</b> ").append(stepText).append("<br/>");
                    msg.append("<b>Time:</b> ").append(failure.timestamp).append("<br/>");
                    msg.append("<b>Error:</b> ").append(
                        failure.errorMessage != null ? failure.errorMessage : "(no message)"
                    ).append("<br/>");
                    if (failure.screenshotPath != null) {
                        msg.append("<b>Screenshot:</b> ").append(failure.screenshotPath).append("<br/>");
                    }
                    msg.append("<i>Execution continued to next step (soft assertion mode).</i>");

                    // Mark the step as FAILED in the Extent report with the failure details.
                    currentTest.fail(msg.toString());
                }
                logger.info("PTAF Soft Assert Report | Marked step '{}' as FAILED in Extent report ({} failure(s)).",
                    stepText, newFailures.size());
            } else {
                logger.debug("PTAF Soft Assert Report | ExtentCucumberAdapter.getCurrentStep() returned null for step '{}'. " +
                    "Soft failure will appear in scenario summary only.", stepText);
            }
        } catch (Exception e) {
            // Never let reporting errors affect test execution.
            logger.debug("PTAF Soft Assert Report | Could not update Extent step status for '{}': {}", stepText, e.getMessage());
        }
    }
}
