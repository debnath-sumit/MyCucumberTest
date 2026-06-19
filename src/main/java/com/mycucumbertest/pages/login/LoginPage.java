package com.mycucumbertest.pages.login;

import com.microsoft.playwright.Page;
import com.mycucumbertest.pages.BasePage;
import com.mycucumbertest.utils.ConfigReader;
import com.mycucumbertest.utils.TestUser;

/**
 * Actions for the login screen. Holds no selectors of its own — defers to
 * {@link LoginPageLocators} so element queries and user actions are separated.
 */
public class LoginPage extends BasePage {

    private final LoginPageLocators loc;

    public LoginPage(Page page) {
        super(page);
        this.loc = new LoginPageLocators(page);
    }

    public LoginPageLocators locators() {
        return loc;
    }

    public void openLoginPage() {
        page.navigate(ConfigReader.get("base.url", "https://www.saucedemo.com/"));
    }

    public void login(TestUser user) {
        enterCredentials(user.username(), user.password());
        clickLogin();
    }

    public void enterCredentials(String username, String password) {
        loc.usernameInput().fill(username);
        loc.passwordInput().fill(password);
    }

    public void clickLogin() {
        loc.loginButton().click();
    }

    public boolean isHomePageDisplayed() {
        return loc.homePageTitle().isVisible();
    }
}
