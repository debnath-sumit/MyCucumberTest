package com.mycucumbertest.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycucumbertest.hooks.Hooks;
import com.mycucumbertest.pages.inventory.InventoryPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class InventorySteps {

    InventoryPage inventoryPage = new InventoryPage(Hooks.page);

    @When("user adds {string} to the cart")
    public void user_adds_to_the_cart(String product) {
        inventoryPage.addToCart(product);
    }

    @Then("the remove option should be available for {string}")
    public void remove_option_available(String product) {
        assertTrue(inventoryPage.isRemoveAvailable(product),
                "Remove option not visible for: " + product);
    }
}