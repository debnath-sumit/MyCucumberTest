package com.mycucumbertest.pages.login;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Locators only. No actions, no clicks, no fills.
 * If a selector changes in the UI, this is the single file to edit.
 */
public class LoginPageLocators {

    private final Page page;

    public LoginPageLocators(Page page) {
        this.page = page;
    }

    public Locator usernameInput() {
        return page.locator("#user-name");
    }

    public Locator passwordInput() {
        return page.locator("#password");
    }

    public Locator loginButton() {
        return page.locator("#login-button");
    }
    public Locator errorMessage() {
        return page.locator(".error-button");
    }

    public Locator homePageTitle() {
        return page.locator(".title");
    }
}
