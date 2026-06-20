package com.mycucumbertest.report;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the Cucumber results (target/cucumber.json) and produces a pass/fail
 * scenario report, both to the console and as an HTML summary
 * (target/test-summary.html).
 *
 * Run with:  mvn exec:java
 */
public class CucumberReportGenerator {

    /** One row in the report: a single Cucumber scenario and its outcome. */
    static final class ScenarioResult {
        final String feature;
        final String scenario;
        final boolean passed;
        final String failedStep;   // null when passed
        final String error;        // null when passed
        final double durationSeconds;

        ScenarioResult(String feature, String scenario, boolean passed,
                       String failedStep, String error, double durationSeconds) {
            this.feature = feature;
            this.scenario = scenario;
            this.passed = passed;
            this.failedStep = failedStep;
            this.error = error;
            this.durationSeconds = durationSeconds;
        }
    }

    public static void main(String[] args) throws IOException {
        Path jsonPath = Paths.get(args.length > 0 ? args[0] : "target/cucumber.json");
        Path htmlOut = Paths.get(args.length > 1 ? args[1] : "target/test-summary.html");

        if (!Files.exists(jsonPath)) {
            System.err.println("Cucumber results not found: " + jsonPath.toAbsolutePath());
            System.err.println("Run the tests first (e.g. `mvn test`) to generate it.");
            System.exit(1);
        }

        List<ScenarioResult> results = parse(jsonPath);
        printConsole(results);
        Files.writeString(htmlOut, buildHtml(results), StandardCharsets.UTF_8);
        System.out.println("\nHTML summary written to: " + htmlOut.toAbsolutePath());
    }

    /** Parse the cucumber.json tree into a flat list of scenario results. */
    static List<ScenarioResult> parse(Path jsonPath) throws IOException {
        String content = Files.readString(jsonPath, StandardCharsets.UTF_8);
        JsonArray features = new Gson().fromJson(content, JsonArray.class);
        List<ScenarioResult> results = new ArrayList<>();
        if (features == null) {
            return results;
        }

        for (JsonElement fe : features) {
            JsonObject feature = fe.getAsJsonObject();
            String featureName = optString(feature, "name", "(unnamed feature)");
            JsonArray elements = feature.getAsJsonArray("elements");
            if (elements == null) {
                continue;
            }

            for (JsonElement ee : elements) {
                JsonObject element = ee.getAsJsonObject();
                // Only real scenarios are test cases; skip backgrounds.
                if (!"scenario".equalsIgnoreCase(optString(element, "type", ""))
                        && !"scenario_outline".equalsIgnoreCase(optString(element, "type", ""))) {
                    continue;
                }

                String scenarioName = optString(element, "name", "(unnamed scenario)");
                boolean passed = true;
                String failedStep = null;
                String error = null;
                long durationNanos = 0;

                // Walk before-hooks, steps, and after-hooks. A scenario passes
                // only if every one of them has status "passed".
                List<JsonObject> phases = new ArrayList<>();
                addAll(phases, element.getAsJsonArray("before"));
                addAll(phases, element.getAsJsonArray("steps"));
                addAll(phases, element.getAsJsonArray("after"));

                for (JsonObject item : phases) {
                    JsonObject result = item.getAsJsonObject("result");
                    if (result == null) {
                        continue;
                    }
                    durationNanos += result.has("duration") ? result.get("duration").getAsLong() : 0;
                    String status = optString(result, "status", "unknown");
                    if (passed && !"passed".equalsIgnoreCase(status)) {
                        passed = false;
                        failedStep = stepLabel(item) + " [" + status + "]";
                        error = optString(result, "error_message", null);
                    }
                }

                results.add(new ScenarioResult(featureName, scenarioName, passed,
                        failedStep, error, durationNanos / 1_000_000_000.0));
            }
        }
        return results;
    }

    private static String stepLabel(JsonObject item) {
        String keyword = optString(item, "keyword", "").trim();
        String name = optString(item, "name", "");
        if (name.isEmpty()) {
            // Hooks have no name; fall back to the matched location.
            JsonObject match = item.getAsJsonObject("match");
            name = match != null ? optString(match, "location", "hook") : "hook";
        }
        return (keyword + " " + name).trim();
    }

    private static void addAll(List<JsonObject> target, JsonArray array) {
        if (array == null) {
            return;
        }
        for (JsonElement e : array) {
            target.add(e.getAsJsonObject());
        }
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : fallback;
    }

    // ---- Console output -----------------------------------------------------

    static void printConsole(List<ScenarioResult> results) {
        long passed = results.stream().filter(r -> r.passed).count();
        long failed = results.size() - passed;

        System.out.println("============================================================");
        System.out.println(" My Dummy Cucumber Test Summary");
        System.out.println("============================================================");
        System.out.printf(" Total: %d   Passed: %d   Failed: %d%n",
                results.size(), passed, failed);
        System.out.println("------------------------------------------------------------");

        System.out.println(" PASSED (" + passed + "):");
        if (passed == 0) {
            System.out.println("   (none)");
        }
        for (ScenarioResult r : results) {
            if (r.passed) {
                System.out.printf("   ✓ %s  >  %s  (%.2fs)%n", r.feature, r.scenario, r.durationSeconds);
            }
        }

        System.out.println(" FAILED (" + failed + "):");
        if (failed == 0) {
            System.out.println("   (none)");
        }
        for (ScenarioResult r : results) {
            if (!r.passed) {
                System.out.printf("   ✗ %s  >  %s  (%.2fs)%n", r.feature, r.scenario, r.durationSeconds);
                System.out.println("       at: " + r.failedStep);
                if (r.error != null) {
                    String firstLine = r.error.split("\\R", 2)[0];
                    System.out.println("       error: " + firstLine);
                }
            }
        }
        System.out.println("============================================================");
    }

    // ---- HTML output --------------------------------------------------------

    static String buildHtml(List<ScenarioResult> results) {
        long passed = results.stream().filter(r -> r.passed).count();
        long failed = results.size() - passed;
        String generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
          .append("<title>Cucumber Test Summary</title><style>")
          .append("body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;margin:0;padding:2rem;background:#f5f6f8;color:#1c1e21}")
          .append("h1{margin:0 0 .25rem}.sub{color:#666;margin-bottom:1.5rem}")
          .append(".cards{display:flex;gap:1rem;margin-bottom:2rem;flex-wrap:wrap}")
          .append(".card{flex:1;min-width:140px;background:#fff;border-radius:10px;padding:1rem 1.25rem;box-shadow:0 1px 3px rgba(0,0,0,.08)}")
          .append(".card .n{font-size:2rem;font-weight:700}.card .l{color:#666;font-size:.85rem;text-transform:uppercase;letter-spacing:.05em}")
          .append(".pass .n{color:#1a7f37}.fail .n{color:#cf222e}")
          .append("table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.08);margin-bottom:2rem}")
          .append("th,td{text-align:left;padding:.7rem 1rem;border-bottom:1px solid #eee;font-size:.92rem;vertical-align:top}")
          .append("th{background:#fafbfc;font-size:.78rem;text-transform:uppercase;letter-spacing:.05em;color:#666}")
          .append("tr:last-child td{border-bottom:none}")
          .append(".badge{display:inline-block;padding:.15rem .5rem;border-radius:6px;font-size:.75rem;font-weight:600}")
          .append(".b-pass{background:#dafbe1;color:#1a7f37}.b-fail{background:#ffebe9;color:#cf222e}")
          .append(".err{color:#cf222e;font-family:ui-monospace,Menlo,monospace;font-size:.82rem;white-space:pre-wrap}")
          .append("h2{margin:1.5rem 0 .5rem}.dur{color:#888;white-space:nowrap}")
          .append(".links{margin-top:.4rem;font-size:.82rem}.links a{color:#0969da;text-decoration:none}.links a:hover{text-decoration:underline}")
          .append(".shot{margin-top:.5rem}.shot img{max-width:480px;width:100%;border:1px solid #ddd;border-radius:6px;display:block}")
          .append("</style></head><body>");

        sb.append("<h1>Cucumber Test Summary</h1>")
          .append("<div class=\"sub\">Generated ").append(escape(generated)).append("</div>");

        sb.append("<div class=\"cards\">")
          .append("<div class=\"card\"><div class=\"n\">").append(results.size()).append("</div><div class=\"l\">Total</div></div>")
          .append("<div class=\"card pass\"><div class=\"n\">").append(passed).append("</div><div class=\"l\">Passed</div></div>")
          .append("<div class=\"card fail\"><div class=\"n\">").append(failed).append("</div><div class=\"l\">Failed</div></div>")
          .append("</div>");

        // Scenarios grouped by feature. Features that contain at least one
        // failure are listed first, and within each feature failed scenarios
        // come before passed ones — that's what people look at.
        if (results.isEmpty()) {
            sb.append("<p>No scenarios were executed.</p>");
        } else {
            Map<String, List<ScenarioResult>> byFeature = new LinkedHashMap<>();
            for (ScenarioResult r : results) {
                byFeature.computeIfAbsent(r.feature, k -> new ArrayList<>()).add(r);
            }

            List<String> features = new ArrayList<>(byFeature.keySet());
            features.sort(Comparator.comparingInt(
                    f -> byFeature.get(f).stream().anyMatch(r -> !r.passed) ? 0 : 1));

            for (String feature : features) {
                List<ScenarioResult> rows = new ArrayList<>(byFeature.get(feature));
                long fPassed = rows.stream().filter(r -> r.passed).count();
                long fFailed = rows.size() - fPassed;
                rows.sort(Comparator.comparing((ScenarioResult r) -> r.passed));

                sb.append("<h2>").append(escape(feature)).append("</h2>")
                  .append("<div class=\"sub\">").append(rows.size()).append(" scenario(s) — ")
                  .append(fPassed).append(" passed, ").append(fFailed).append(" failed</div>");

                sb.append("<table><tr><th>Scenario</th><th>Status</th><th>Failed at / Error</th><th>Time</th></tr>");
                for (ScenarioResult r : rows) {
                    sb.append("<tr><td>").append(escape(r.scenario)).append("</td><td>");
                    if (r.passed) {
                        sb.append("<span class=\"badge b-pass\">PASSED</span></td><td>—");
                    } else {
                        sb.append("<span class=\"badge b-fail\">FAILED</span></td>")
                          .append("<td><strong>").append(escape(r.failedStep)).append("</strong>");
                        if (r.error != null) {
                            sb.append("<div class=\"err\">").append(escape(r.error)).append("</div>");
                        }
                        sb.append(artifactLinks(r.scenario));
                    }
                    sb.append("</td><td class=\"dur\">")
                      .append(String.format("%.2fs", r.durationSeconds)).append("</td></tr>");
                }
                sb.append("</table>");
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * On-failure artifacts saved by Hooks, matched by the same sanitized scenario
     * name. The screenshot is inlined as a base64 data URI so the report is
     * self-contained and the image travels inside the emailed HTML body (no
     * external file needed). The image is also linked to the on-disk file for
     * full-size local viewing, and the Playwright trace is linked when present.
     * Only emits markup when a file actually exists.
     *
     * <p>Note: some mail clients (notably Gmail web) block {@code data:} image
     * URIs, so the CI workflow also attaches the PNGs to the email as a fallback.
     */
    private static String artifactLinks(String scenarioName) {
        String name = sanitize(scenarioName);
        StringBuilder out = new StringBuilder();

        Path shot = Paths.get("target/screenshots", name + ".png");
        if (Files.exists(shot)) {
            try {
                String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(shot));
                out.append("<div class=\"shot\"><a href=\"screenshots/").append(name).append(".png\">")
                   .append("<img alt=\"Screenshot at failure\" src=\"data:image/png;base64,")
                   .append(b64).append("\"></a></div>");
            } catch (IOException e) {
                out.append("<div class=\"links\">(screenshot unreadable: ")
                   .append(escape(e.getMessage())).append(")</div>");
            }
        }

        Path trace = Paths.get("target/traces", name + ".zip");
        if (Files.exists(trace)) {
            out.append("<div class=\"links\"><a href=\"traces/").append(name)
               .append(".zip\">🔍 Trace</a></div>");
        }
        return out.toString();
    }

    /** Mirror of Hooks' scenario-name sanitization, so artifact filenames match. */
    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
