package com.mycucumbertest.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads a per-page object repository from {@code locators/<name>.properties} on
 * the classpath and resolves selectors by key.
 *
 * <p>Each page's locators class owns one repository, e.g.
 * {@code new LocatorRepository("login")} reads {@code locators/login.properties}.
 * When a selector changes in the UI, edit the properties file — no Java change.
 */
public final class LocatorRepository {

    private final String path;
    private final Properties props = new Properties();

    public LocatorRepository(String repository) {
        this.path = "locators/" + repository + ".properties";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException(path + " not found on the classpath");
            }
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + path, e);
        }
    }

    /** Selector string for {@code key} (e.g. "login.username"). Throws if absent. */
    public String selector(String key) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("No selector for key '" + key + "' in " + path);
        }
        return v.trim();
    }
}