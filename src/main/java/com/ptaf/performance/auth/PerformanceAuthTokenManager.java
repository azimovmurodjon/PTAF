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

    private final Map<String, AuthToken> tokenStore = new ConcurrentHashMap<>();

    /**
     * Saves a token without expiration.
     *
     * @param alias logical token alias (example: "default", "admin", "customerA")
     * @param token token value
     */
    public void saveToken(String alias, String token) {
        validateAlias(alias);
        validateToken(token);

        tokenStore.put(alias.trim(), new AuthToken(token.trim(), null));
    }

    /**
     * Saves a token with expiration timestamp.
     *
     * @param alias logical token alias
     * @param token token value
     * @param expiresAt token expiration timestamp
     */
    public void saveToken(String alias, String token, Instant expiresAt) {
        validateAlias(alias);
        validateToken(token);

        tokenStore.put(alias.trim(), new AuthToken(token.trim(), expiresAt));
    }

    /**
     * Returns true if token exists for alias.
     *
     * @param alias logical token alias
     * @return true when token exists
     */
    public boolean hasToken(String alias) {
        validateAlias(alias);
        return tokenStore.containsKey(alias.trim());
    }

    /**
     * Returns token value for alias.
     *
     * @param alias logical token alias
     * @return token value
     */
    public String getToken(String alias) {
        validateAlias(alias);

        AuthToken authToken = tokenStore.get(alias.trim());
        if (authToken == null) {
            throw new IllegalArgumentException("No token found for alias: " + alias);
        }

        return authToken.token();
    }

    /**
     * Returns token object for alias.
     *
     * @param alias logical token alias
     * @return auth token metadata
     */
    public AuthToken getTokenDetails(String alias) {
        validateAlias(alias);

        AuthToken authToken = tokenStore.get(alias.trim());
        if (authToken == null) {
            throw new IllegalArgumentException("No token found for alias: " + alias);
        }

        return authToken;
    }

    /**
     * Returns true if token exists and is expired.
     *
     * <p>If token has no expiration metadata, returns false.</p>
     *
     * @param alias logical token alias
     * @return true when expired
     */
    public boolean isExpired(String alias) {
        AuthToken authToken = getTokenDetails(alias);
        return authToken.expiresAt() != null && Instant.now().isAfter(authToken.expiresAt());
    }

    /**
     * Applies bearer token to the given header manager using the specified alias.
     *
     * @param alias logical token alias
     * @param headerManager target header manager
     * @return same header manager for fluent chaining
     */
    public PerformanceHeaderManager applyBearerToken(String alias,
                                                     PerformanceHeaderManager headerManager) {
        Objects.requireNonNull(headerManager, "PerformanceHeaderManager cannot be null.");

        String token = getToken(alias);
        return headerManager.addBearerToken(token);
    }

    /**
     * Removes token for alias.
     *
     * @param alias logical token alias
     */
    public void removeToken(String alias) {
        validateAlias(alias);
        tokenStore.remove(alias.trim());
    }

    /**
     * Clears all stored tokens.
     */
    public void clearAll() {
        tokenStore.clear();
    }

    /**
     * Immutable token metadata holder.
     *
     * @param token token value
     * @param expiresAt expiration timestamp, null if unknown
     */
    public record AuthToken(String token, Instant expiresAt) {
    }

    private void validateAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Token alias cannot be null or blank.");
        }
    }

    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token value cannot be null or blank.");
        }
    }
}