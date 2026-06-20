package com.mycucumbertest.pages.inventory;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mycucumbertest.utils.LocatorRepository;

/**
 * Locators only. No actions, no clicks, no fills.
 * Selectors live in locators/inventory.properties (the object repository); if a
 * selector changes in the UI, edit that file — not this class.
 */
public class InventoryPageLocators {

    private final Page page;
    private final LocatorRepository repo = new LocatorRepository("inventory");

    public InventoryPageLocators(Page page) {
        this.page = page;
    }

    /** Resolve a Locator for any object-repository key, e.g. "inventory.addBackpack". */
    public Locator byKey(String key) {
        return page.locator(repo.selector(key));
    }
}