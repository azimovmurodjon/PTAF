package com.ptaf.performance.auth;

import com.ptaf.performance.headers.PerformanceHeaderManager;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Framework-owned authentication token manager for performance execution.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>storing tokens by logical alias</li>
 *   <li>supporting token chaining across requests</li>
 *   <li>tracking expiration metadata when available</li>
 *   <li>injecting bearer tokens into PerformanceHeaderManager</li>
 * </ul>
 * </p>
 *
 * <p>This class does not directly call an auth API yet.
 * That responsibility can be added later through a dedicated auth client/service.
 * For now, it provides the reusable token lifecycle layer required by the framework.</p>
 */
public class PerformanceAuthTokenManager {

    /**
     * Internal concurrent token storage.
     *
     * Key: logical token alias (trimmed)
     * Value: AuthToken containing the token value and optional expiry.
     *
     * A ConcurrentHashMap is used to allow safe concurrent access from multiple threads
     * without external synchronization. Tokens stored here may be overwritten by
     * subsequent saveToken calls using the same alias.
     */
    private final Map<String, AuthToken> tokenStore = new ConcurrentHashMap<>();

    /**
     * Saves a token without expiration.
     *
     * <p>This method:
     * <ul>
     *   <li>validates alias and token</li>
     *   <li>trims both alias and token</li>
     *   <li>stores an immutable AuthToken with a null expiration</li>
     * </ul>
     *
     * If a token already exists for the alias, it will be overwritten.
     *
     * @param alias logical token alias (example: "default", "admin", "customerA")
     *              must not be null or blank
     * @param token token value, must not be null or blank
     * @throws IllegalArgumentException when alias or token is null/blank
     */
    public void saveToken(String alias, String token) {
        // Validate inputs before any state mutation
        validateAlias(alias);
        validateToken(token);

        // Store a trimmed token under a trimmed alias; expiration unknown (null)
        tokenStore.put(alias.trim(), new AuthToken(token.trim(), null));
    }

    /**
     * Saves a token with expiration timestamp.
     *
     * <p>Behaviors:
     * <ul>
     *   <li>validates alias and token</li>
     *   <li>trims alias and token</li>
     *   <li>stores the token along with the provided expiration Instant</li>
     * </ul>
     *
     * If a token already exists for the alias, it will be overwritten with the
     * new value and expiration.
     *
     * @param alias logical token alias; must not be null or blank
     * @param token token value; must not be null or blank
     * @param expiresAt token expiration timestamp, may be null to indicate unknown/no-expiry
     * @throws IllegalArgumentException when alias or token is null/blank
     */
    public void saveToken(String alias, String token, Instant expiresAt) {
        // Validate the textual inputs
        validateAlias(alias);
        validateToken(token);

        // Store token and expiration; null expiresAt indicates an unknown/no-expiry token
        tokenStore.put(alias.trim(), new AuthToken(token.trim(), expiresAt));
    }

    /**
     * Returns true if a token exists for the given alias.
     *
     * <p>The alias is trimmed before lookup.
     *
     * @param alias logical token alias; must not be null or blank
     * @return true when a token exists for the alias, false otherwise
     * @throws IllegalArgumentException when alias is null/blank
     */
    public boolean hasToken(String alias) {
        // Validate alias and perform a concurrency-safe contains check
        validateAlias(alias);
        return tokenStore.containsKey(alias.trim());
    }

    /**
     * Returns the token value (string) for the given alias.
     *
     * <p>The alias is trimmed prior to lookup. If no token exists for the alias,
     * an IllegalArgumentException is thrown to signal a missing token.</p>
     *
     * @param alias logical token alias; must not be null or blank
     * @return token value associated with the alias
     * @throws IllegalArgumentException when alias is null/blank or no token is found
     */
    public String getToken(String alias) {
        // Validate alias and retrieve the AuthToken object
        validateAlias(alias);

        AuthToken authToken = tokenStore.get(alias.trim());
        if (authToken == null) {
            // Clear and explicit error for callers/testers when a token is missing
            throw new IllegalArgumentException("No token found for alias: " + alias);
        }

        // Return the raw token string (was trimmed on save)
        return authToken.token();
    }

    /**
     * Returns the AuthToken (token + optional expiry) for the given alias.
     *
     * <p>The alias is trimmed prior to lookup. If no token exists for the alias,
     * an IllegalArgumentException is thrown.</p>
     *
     * @param alias logical token alias; must not be null or blank
     * @return AuthToken instance containing token and optional expiresAt
     * @throws IllegalArgumentException when alias is null/blank or no token is found
     */
    public AuthToken getTokenDetails(String alias) {
        // Validate alias and retrieve the stored token details
        validateAlias(alias);

        AuthToken authToken = tokenStore.get(alias.trim());
        if (authToken == null) {
            // Helpful error for debugging/test failures
            throw new IllegalArgumentException("No token found for alias: " + alias);
        }

        return authToken;
    }

    /**
     * Returns true if the token associated with the given alias is expired.
     *
     * <p>Expiration logic:
     * <ul>
     *   <li>If the token contains an expiresAt Instant, this method compares it to Instant.now()</li>
     *   <li>If the token has no expiresAt (null), it is considered non-expired and the method returns false</li>
     * </ul>
     *
     * @param alias logical token alias; must not be null or blank
     * @return true when a token exists and has expired, false otherwise
     * @throws IllegalArgumentException when alias is null/blank or no token is found
     */
    public boolean isExpired(String alias) {
        // Retrieve token details which will validate alias and existence
        AuthToken authToken = getTokenDetails(alias);

        // If expiration metadata is present, compare with the current instant
        return authToken.expiresAt() != null && Instant.now().isAfter(authToken.expiresAt());
    }

    /**
     * Applies bearer token to the given header manager using the specified alias.
     *
     * <p>Typical usage in tests:
     * <pre>
     *   headerManager = tokenManager.applyBearerToken("default", headerManager);
     * </pre>
     *
     * <p>Notes:
     * <ul>
     *   <li>Throws NullPointerException if headerManager is null</li>
     *   <li>Throws IllegalArgumentException if the alias has no token</li>
     *   <li>Delegates to PerformanceHeaderManager.addBearerToken for actual header mutation</li>
     * </ul>
     *
     * @param alias logical token alias; must not be null or blank and must map to a stored token
     * @param headerManager target header manager where the Authorization: Bearer header will be applied
     * @return the same headerManager instance for fluent chaining
     * @throws NullPointerException when headerManager is null
     * @throws IllegalArgumentException when alias is null/blank or no token is found
     */
    public PerformanceHeaderManager applyBearerToken(String alias,
                                                     PerformanceHeaderManager headerManager) {
        // Ensure header manager is non-null to avoid confusing NPEs deeper in the call chain
        Objects.requireNonNull(headerManager, "PerformanceHeaderManager cannot be null.");

        // Retrieve token (may throw IllegalArgumentException if missing)
        String token = getToken(alias);

        // Delegate to header manager and return it for chaining in tests/scripts
        return headerManager.addBearerToken(token);
    }

    /**
     * Removes the token associated with the given alias.
     *
     * <p>If no token exists for the alias, this method is a no-op.</p>
     *
     * @param alias logical token alias; must not be null or blank
     * @throws IllegalArgumentException when alias is null/blank
     */
    public void removeToken(String alias) {
        // Validate alias and perform concurrent-safe removal
        validateAlias(alias);
        tokenStore.remove(alias.trim());
    }

    /**
     * Clears all stored tokens from the manager.
     *
     * <p>This operation is thread-safe and will remove all entries from the internal store.
     * Useful to reset state between test scenarios.</p>
     */
    public void clearAll() {
        // Clear the underlying concurrent map
        tokenStore.clear();
    }

    /**
     * Immutable token metadata holder.
     *
     * <p>This record bundles a token string with an optional expiration Instant.
     * The token value is expected to be trimmed before being stored by the manager.
     * Clients should not mutate or rely on mutable fields—records are immutable and safe to share.</p>
     *
     * @param token token value (typically trimmed)
     * @param expiresAt expiration timestamp, null if unknown or not provided
     */
    public record AuthToken(String token, Instant expiresAt) {
    }

    /**
     * Validates that the provided alias is non-null and not blank.
     *
     * @param alias alias to validate
     * @throws IllegalArgumentException when alias is null or blank
     */
    private void validateAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Token alias cannot be null or blank.");
        }
    }

    /**
     * Validates that the provided token value is non-null and not blank.
     *
     * @param token token value to validate
     * @throws IllegalArgumentException when token is null or blank
     */
    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token value cannot be null or blank.");
        }
    }
}
