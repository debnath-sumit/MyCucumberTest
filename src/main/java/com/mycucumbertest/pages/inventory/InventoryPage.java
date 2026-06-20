package com.mycucumbertest.pages.inventory;

import com.microsoft.playwright.Page;
import com.mycucumbertest.pages.BasePage;

import java.util.Map;

/**
 * Actions for the Inventory (Products) screen shown after a successful login.
 * Holds no selectors of its own — defers to {@link InventoryPageLocators}, which
 * reads them from locators/inventory.properties.
 *
 * Product display names are mapped to the object-repository keys defined in that
 * properties file; add a product by adding its two keys there and one line to
 * each map below.
 */
public class InventoryPage extends BasePage {

    private static final Map<String, String> ADD_KEYS = Map.of(
            "Sauce Labs Backpack", "inventory.addBackpack",
            "Sauce Labs Bike Light", "inventory.addBikeLight");

    private static final Map<String, String> REMOVE_KEYS = Map.of(
            "Sauce Labs Backpack", "inventory.removeBack",
            "Sauce Labs Bike Light", "inventory.removeBikeLight");

    private final InventoryPageLocators loc;

    public InventoryPage(Page page) {
        super(page);
        this.loc = new InventoryPageLocators(page);
    }

    public void addToCart(String product) {
        loc.byKey(addKey(product)).click();
    }

    public boolean isRemoveAvailable(String product) {
        return loc.byKey(removeKey(product)).isVisible();
    }

    private static String addKey(String product) {
        return require(ADD_KEYS, product);
    }

    private static String removeKey(String product) {
        return require(REMOVE_KEYS, product);
    }

    private static String require(Map<String, String> keys, String product) {
        String key = keys.get(product);
        if (key == null) {
            throw new IllegalArgumentException("No inventory locator mapping for product: " + product);
        }
        return key;
    }
}
