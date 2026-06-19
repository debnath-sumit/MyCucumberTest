package com.mycucumbertest.utils;

/**
 * Immutable test fixture for a user account. Use {@link TestUsers} to obtain
 * instances rather than constructing directly in tests, so credentials stay
 * configurable from outside the codebase.
 */
public record TestUser(String username, String password) {
}
