package hooks;

import com.microsoft.playwright.*;
import io.cucumber.java.After;
import io.cucumber.java.Before;

public class Hooks {

	public static Playwright playwright;
	public static Browser browser;
	public static Page page;

	boolean isHeadless = Boolean.parseBoolean(
			System.getProperty("headless", "true")
	);

	@Before
	public void setup() {
		playwright = Playwright.create();

		browser = playwright.chromium().launch(
				new BrowserType.LaunchOptions()
						.setHeadless(isHeadless)
		);

		page = browser.newPage();
	}

	@After
	public void tearDown() {
		page.close();
		browser.close();
		playwright.close();
	}
}