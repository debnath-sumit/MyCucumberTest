package com.mycucumbertest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves configuration in priority order:
 *   1st - System property (-Dbase.url from Maven / Jenkins)
 *   2nd - config.properties on the classpath
 *   3rd - caller-supplied fallback
 */
public final class ConfigReader {

    private static final Properties PROPS = load();

    private ConfigReader() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
        return p;
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    public static boolean getBool(String key, boolean fallback) {
        String v = get(key);
        return v == null ? fallback : Boolean.parseBoolean(v);
    }
}
