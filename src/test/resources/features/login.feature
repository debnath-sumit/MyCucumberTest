Feature: Login functionality

  Scenario: User should login with valid credentials
    Given user opens the login page
    When user enters username "standard_user" and password "secret_sauce"
    And user clicks on login button
    Then user should see the home page