package com.mycucumbertest.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads test users from testdata/users.json on the classpath. Never hard-code
 * credentials in test classes — add them to users.json (or override the file
 * for CI) and pull them through here.
 */
public final class TestUsers {

    private static final JsonObject USERS = load();

    private TestUsers() {}

    private static JsonObject load() {
        try (InputStream in = TestUsers.class.getClassLoader()
                .getResourceAsStream("testdata/users.json")) {
            if (in == null) {
                throw new IllegalStateException("testdata/users.json not found on the classpath");
            }
            return new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load testdata/users.json", e);
        }
    }

    public static TestUser standard() {
        return user("standardUser");
    }

    public static TestUser lockedOut() {
        return user("lockedOutUser");
    }

    /**
     * Returns a single field for a user (e.g. "username"), or {@code null} if
     * either the user key or the field is absent. Used by {@link DataResolver}
     * to resolve {@code ${userKey.field}} placeholders from feature files.
     */
    public static String field(String userKey, String field) {
        JsonObject node = USERS.getAsJsonObject(userKey);
        if (node == null || !node.has(field) || node.get(field).isJsonNull()) {
            return null;
        }
        return node.get(field).getAsString();
    }

    public static TestUser user(String key) {
        JsonObject node = USERS.getAsJsonObject(key);
        if (node == null) {
            throw new IllegalArgumentException("No user '" + key + "' in testdata/users.json");
        }
        return new TestUser(
                node.get("username").getAsString(),
                node.get("password").getAsString());
    }
}
