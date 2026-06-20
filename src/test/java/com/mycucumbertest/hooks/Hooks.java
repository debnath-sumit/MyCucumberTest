package com.mycucumbertest.hooks;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.mycucumbertest.utils.PlaywrightFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Owns the Playwright lifecycle for every scenario — the Cucumber equivalent of
 * parent-circle-test's BaseTest. Starts a traced browser context before each
 * scenario and, on failure, attaches a screenshot to the report and saves the
 * trace under target/traces.
 */
public class Hooks {

    private static final Path TRACE_DIR = Paths.get("target/traces");
    private static final Path SCREENSHOT_DIR = Paths.get("target/screenshots");

    public static Playwright playwright;
    public static Browser browser;
    public static BrowserContext context;
    public static Page page;

    @Before
    public void setup() throws Exception {
        Files.createDirectories(TRACE_DIR);

        playwright = Playwright.create();
        browser = PlaywrightFactory.launch(playwright);
        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        page = context.newPage();
    }

    @After
    public void tearDown(Scenario scenario) throws Exception {
        String name = scenario.getName().replaceAll("[^a-zA-Z0-9.-]", "_");

        if (scenario.isFailed() && page != null) {
            // Save the screenshot to a file (so test-summary.html can link to it
            // by the same sanitized name) and also attach it to the Cucumber HTML.
            Files.createDirectories(SCREENSHOT_DIR);
            byte[] png = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setPath(SCREENSHOT_DIR.resolve(name + ".png")));
            scenario.attach(png, "image/png", scenario.getName());
        }

        if (context != null) {
            try {
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(TRACE_DIR.resolve(name + ".zip")));
            } catch (Exception ignored) {
            }
            context.close();
        }
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
