package pages;

import com.microsoft.playwright.Page;

public class LoginPage {

	private Page page;

	private String usernameInput = "#user-name";
	private String passwordInput = "#password";
	private String loginButton = "#login-button";
	private String homePageTitle = ".title";

	public LoginPage(Page page) {
		this.page = page;
	}

	public void openLoginPage() {
		page.navigate("https://www.saucedemo.com/");
	}

	public void enterCredentials(String username, String password) {
		page.fill(usernameInput, username);
		page.fill(passwordInput, password);
	}

	public void clickLogin() {
		page.click(loginButton);
	}

	public boolean isHomePageDisplayed() {
		return page.locator(homePageTitle).isVisible();
	}
}