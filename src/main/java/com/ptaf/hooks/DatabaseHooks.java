package com.ptaf.hooks;

import com.ptaf.db.handlers.DatabaseHandler;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DatabaseHooks manages database-specific cleanup after Cucumber scenarios.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This hook ensures that database connections opened during DB automation
 * are closed safely after each database scenario. This prevents stale SQL Server
 * sessions, connection leaks, locked resources, and unexpected behavior during
 * parallel or repeated test execution.
 * </p>
 *
 * <p>
 * Why this is separate from the main Hooks class:
 * Keeping DB teardown separate makes the framework more modular and easier to
 * maintain. UI browser lifecycle remains in Hooks, while database lifecycle
 * cleanup is handled here.
 * </p>
 *
 * <p>
 * This hook runs only for scenarios tagged with:
 * </p>
 *
 * <ul>
 *     <li>@db</li>
 *     <li>@database</li>
 *     <li>@sql</li>
 * </ul>
 *
 * <p>
 * Your current DatabaseTestRunner already uses @db, so this hook will be picked
 * up automatically because your runner glue includes com/ptaf/hooks.
 * </p>
 *
 * @see com.ptaf.db.handlers.DatabaseHandler
 */
public class DatabaseHooks {

    /**
     * SLF4J logger for this hook class.
     *
     * <p>
     * Use this logger to record lifecycle events related to database teardown.
     * Log messages include the scenario name so that failures can be correlated
     * with test runs in CI logs.
     * </p>
     */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHooks.class);

    /**
     * Closes the active database connection after each database scenario.
     *
     * <p>
     * This method is intentionally defensive. Even if no DB connection was created
     * during the scenario, calling DatabaseHandler.closeConnection() is safe because
     * the handler checks the ThreadLocal connection before closing.
     * </p>
     *
     * <p>
     * Notes for testers and maintainers:
     * - This hook is invoked by Cucumber after the scenario completes.
     * - The @After annotation uses a tag expression; it will only run for scenarios
     *   that carry any of the tags: @db, @database, or @sql.
     * - DatabaseHandler is expected to manage connection state (e.g. a ThreadLocal
     *   connection), so closeConnection will clear or close what exists and do nothing
     *   if there is nothing to close.
     * - Any unexpected exceptions during cleanup are logged but do not rethrow here;
     *   the goal is to surface errors in CI logs so developers can investigate.
     * </p>
     *
     * @param scenario current Cucumber scenario provided by the Cucumber runtime.
     */
    @After("@db or @database or @sql")
    public void closeDatabaseConnectionAfterScenario(Scenario scenario) {
        try {
            // Log start of database teardown with scenario metadata to aid troubleshooting.
            logger.info(
                    "Starting database teardown for scenario: {} with status: {}",
                    scenario.getName(),
                    scenario.getStatus()
            );

            // Delegate actual connection closing to the DatabaseHandler.
            // DatabaseHandler.closeConnection() should:
            // - check if a ThreadLocal or global connection exists for this thread,
            // - attempt to rollback/commit as appropriate (handler responsibility),
            // - close the JDBC connection and release resources,
            // - clear the ThreadLocal reference to avoid leaks between scenarios.
            //
            // Calling this even when no connection was created is safe and intentional:
            // it centralizes cleanup logic and avoids conditional branching here.
            DatabaseHandler.closeConnection();

            // Confirm successful teardown in the logs so the test run trace is complete.
            logger.info("Database teardown completed successfully for scenario: {}", scenario.getName());

        } catch (Exception e) {
            /*
             * We do not hide cleanup issues. If DB cleanup fails, the error is logged
             * clearly so the team can investigate connection/session problems.
             *
             * Important: do not throw from hooks unless you have a specific reason.
             * Logging allows the test framework to continue teardown and ensures CI
             * logs capture the stack trace for diagnostics.
             */
            logger.error(
                    "Database teardown failed for scenario: {}. Review SQL Server connection lifecycle.",
                    scenario.getName(),
                    e
            );
        }
    }
}
