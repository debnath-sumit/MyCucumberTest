package steps;


import static org.junit.Assert.assertTrue;

import hooks.Hooks;
import io.cucumber.java.en.*;
import pages.LoginPage;


public class LoginSteps {

	LoginPage loginPage = new LoginPage(Hooks.page);

	@Given("user opens the login page")
	public void user_opens_the_login_page() {
		loginPage.openLoginPage();
	}

	@When("user enters username {string} and password {string}")
	public void user_enters_username_and_password(String username, String password) {
		loginPage.enterCredentials(username, password);
	}

	@When("user clicks on login button")
	public void user_clicks_on_login_button() {
		loginPage.clickLogin();
	}

	@Then("user should see the home page")
	public void user_should_see_the_home_page() {
		assertTrue(loginPage.isHomePageDisplayed());
	}
}