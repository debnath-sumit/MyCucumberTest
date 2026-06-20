package com.mycucumbertest.pages.login;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mycucumbertest.utils.LocatorRepository;

/**
 * Locators only. No actions, no clicks, no fills.
 * Selectors live in locators/login.properties (the object repository); if a
 * selector changes in the UI, edit that file — not this class.
 */
public class LoginPageLocators {

    private final Page page;
    private final LocatorRepository repo = new LocatorRepository("login");

    public LoginPageLocators(Page page) {
        this.page = page;
    }

    public Locator usernameInput() {
        return page.locator(repo.selector("login.username"));
    }

    public Locator passwordInput() {
        return page.locator(repo.selector("login.password"));
    }

    public Locator loginButton() {
        return page.locator(repo.selector("login.button"));
    }

    public Locator errorMessage() {
        return page.locator(repo.selector("login.error"));
    }

    public Locator homePageTitle() {
        return page.locator(repo.selector("login.homeTitle"));
    }
}