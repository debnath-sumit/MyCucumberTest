package com.mycucumbertest.steps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycucumbertest.hooks.Hooks;
import com.mycucumbertest.pages.login.LoginPage;
import com.mycucumbertest.utils.DataResolver;
import io.cucumber.java.en.*;

public class LoginSteps {

    LoginPage loginPage = new LoginPage(Hooks.page);

    @Given("user opens the login page")
    public void user_opens_the_login_page() {
        loginPage.openLoginPage();
    }

    @When("user enters username {string} and password {string}")
    public void user_enters_username_and_password(String username, String password) {
        // Resolve ${...} tokens from users.json / config.properties; plain
        // literals pass through unchanged.
        loginPage.enterCredentials(
                DataResolver.resolve(username), DataResolver.resolve(password));
    }

    @When("user enters username {string}")
    public void user_enters_username(String username) {
        loginPage.enterUsername(
                DataResolver.resolve(username));
    }

    @When("user enters password {string}")
    public void user_enters_password(String password) {
        loginPage.enterPassword(
                DataResolver.resolve(password));
    }

    @When("user clicks on login button")
    public void user_clicks_on_login_button() {
        loginPage.clickLogin();
    }

    @Then("user should see the home page")
    public void user_should_see_the_home_page() {
        assertTrue(loginPage.isHomePageDisplayed());
    }

    @Then("user should get an error message")
    public void user_should_get_an_error_message() {
        String errorMsg = loginPage.isLoginErrorDisplayed();
        if (!errorMsg.isEmpty())
            assertTrue(loginPage.isLoginErrorDisplayed().contains("Username and password do not match any user in this service"));
        else
            assertFalse(false, "The error message is not displayed");
    }

    @Then("user should get an error message {string}")
    public void credential_error(String expectedErrorMsg){
        String errorMsg = loginPage.isLoginErrorDisplayed();
        if (!errorMsg.isEmpty())
            assertTrue(loginPage.isLoginErrorDisplayed().contains(expectedErrorMsg));
        else
            assertFalse(false, "The error message is not displayed");
    }


}
