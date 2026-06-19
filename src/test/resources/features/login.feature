Feature: Login functionality

  Scenario: User should login with valid credentials
    Given user opens the login page
    When user enters username "${standardUser.username}" and password "${standardUser.password}"
    And user clicks on login button
    Then user should see the home page

  Scenario: User should not login with Invalid credentials
    Given user opens the login page
    When user enters username "${invalidUser.username}" and password "${invalidUser.password}"
    And user clicks on login button
    Then user should get an error message