package com.mycucumbertest.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

public final class PlaywrightFactory {

    private PlaywrightFactory() {}

    public static Browser launch(Playwright playwright) {
        String browser = ConfigReader.get("browser", "chromium").toLowerCase();
        boolean headless = ConfigReader.getBool("headless", true);

        BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions().setHeadless(headless);

        return switch (browser) {
            case "firefox" -> playwright.firefox().launch(opts);
            case "webkit"  -> playwright.webkit().launch(opts);
            default        -> playwright.chromium().launch(opts);
        };
    }
}
