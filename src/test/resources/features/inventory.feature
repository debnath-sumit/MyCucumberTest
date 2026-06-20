Feature: Inventory - add to cart

  Scenario: Adding Sauce Labs Backpack to the cart shows the Remove option
    Given user opens the login page
    When user enters username "${standardUser.username}" and password "${standardUser.password}"
    And user clicks on login button
    Then user should see the home page
    When user adds "Sauce Labs Backpack" to the cart
    Then the remove option should be available for "Sauce Labs Backpack"