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
 */
public class DatabaseHooks {

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
     * @param scenario current Cucumber scenario.
     */
    @After("@db or @database or @sql")
    public void closeDatabaseConnectionAfterScenario(Scenario scenario) {
        try {
            logger.info(
                    "Starting database teardown for scenario: {} with status: {}",
                    scenario.getName(),
                    scenario.getStatus()
            );

            DatabaseHandler.closeConnection();

            logger.info("Database teardown completed successfully for scenario: {}", scenario.getName());

        } catch (Exception e) {
            /*
             * We do not hide cleanup issues. If DB cleanup fails, the error is logged
             * clearly so the team can investigate connection/session problems.
             */
            logger.error(
                    "Database teardown failed for scenario: {}. Review SQL Server connection lifecycle.",
                    scenario.getName(),
                    e
            );
        }
    }
}