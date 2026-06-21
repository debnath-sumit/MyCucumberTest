---
name: CreateNewTest
description: Use when adding a new UI test, scenario, or page to the MyCucumberTest framework — author a feature, step definitions, page object, and object-repository selectors from plain-English instructions OR from a manual-test-case JSON exported by the ManualTestGenerateTool, in Java + Playwright with mandatory assertions.
---

# CreateNewTest

## Overview

Turn a user's plain-English test description into a complete, runnable test that
follows this repo's layered Page Object Model. The user supplies the intent (and,
for a new page, the selectors); you generate every layer — **object repository →
locators class → page object → feature → step definitions** — in Java with
Playwright, ending in a real assertion.

**Core principle:** selectors live in `.properties`, data lives in `users.json` /
`config.properties`, Java holds only keys and behaviour, and **every scenario ends
in a JUnit 5 assertion that can actually fail.**

## When to use

- "Add a test/scenario for …", "create a new page test", "automate the X flow".
- The user describes steps in plain English (open page, enter X, click Y, expect Z).
- The user gives you a **manual-test-case JSON** file (from ManualTestGenerateTool)
  and says "automate these" / "create the test from this JSON". → See
  **"Importing from a manual-test-case JSON"** below, then follow the normal build order.

## Importing from a manual-test-case JSON

The companion **ManualTestGenerateTool** exports a JSON file describing one story
and its manual test cases. The user drops that file into this repo (suggested:
`manual-tests/<story>.json`) and asks you to automate it. Read the file and map it
onto the framework — then proceed with the normal **build order** below.

### Input schema

```json
{
  "shortDescription": "User can reset password via email",
  "description": "...",
  "acceptanceCriteria": "- link expires after 30 min\n- min 8 chars",
  "generatedAt": "2026-06-20T10:00:00Z",
  "testCases": [
    {
      "id": "TC-01",
      "title": "Successful password reset with valid email",
      "preconditions": "User has a registered account",
      "steps": ["Navigate to the login page", "Click 'Forgot Password'", "..."],
      "expectedResult": "Password is reset and user can log in",
      "priority": "High",
      "type": "Positive"
    }
  ]
}
```

### Mapping rules (JSON → Gherkin + layers)

| JSON | Becomes |
| --- | --- |
| one JSON file (one story) | one `src/test/resources/features/<slug(shortDescription)>.feature` |
| `shortDescription` | `Feature:` name (and a `# <description>` comment line) |
| each `testCases[]` item | one `Scenario:` (name = `title`) |
| `type` + `priority` | scenario tags, e.g. `@positive @high` (`@negative`/`@edge`, `@medium`/`@low`) |
| `preconditions` | leading `Given` step(s); if identical across all cases, lift into a `Background:` |
| `steps[]` | `When` (first) + `And` (rest), rewritten as **intention-style** phrases |
| `expectedResult` | the `Then` step — must map to a real JUnit 5 assertion |

### Rewrite the steps — do NOT paste them literally

Manual steps are descriptive ("Enter the registered email in the Email field").
Convert them to the repo's **intention-style** Gherkin and, critically:

- **Reuse existing step phrases** wherever the action already exists. Grep
  `src/test/java/com/mycucumbertest/steps/` first — e.g. if "open the login page"
  already exists as `Given user opens the login page`, use that exact wording so no
  new glue is needed.
- **Replace literal data with `${...}` tokens** (emails, usernames, passwords,
  amounts). Add new values to `testdata/users.json`; never hard-code in the feature.
- **Collapse trivial UI minutiae.** "Click the Email field, then type…" → one
  `When user enters email "${...}"`.
- **One assertion per scenario minimum**, derived from `expectedResult`.

### Then continue with the normal build order

For any **new page** referenced by the steps, you still need selectors: ask the
user to create `locators/<page>.properties` first (per the build order). Reuse
existing pages/steps for anything already in the repo. Finish with
`mvn -q -B test-compile`.

### Example (one test case → one scenario)

From the TC-01 above, with an existing login page and a new reset page:

```gherkin
Feature: User can reset password via email

  @positive @high
  Scenario: Successful password reset with valid email
    Given user has a registered account "${standardUser.username}"
    When user opens the login page
    And user clicks forgot password
    And user requests a reset link for "${standardUser.username}"
    And user opens the reset link and sets password "${standardUser.newPassword}"
    Then user should be able to log in with the new password
```

## The build order (always this sequence)

For a **new page**, create all five layers. For a **new scenario on an existing
page**, reuse the page/locators and usually just add to the feature + steps.

1. **Object repository** — `src/main/resources/locators/<page>.properties`
   Key → Playwright selector. **The user usually creates this file first** — so
   **always `Read` it before generating the Java**, and bind the locators/page
   classes to the user's actual keys (don't invent your own key scheme). One file
   per page.
   ```properties
   # locators/checkout.properties
   checkout.firstName = #first-name
   checkout.continue  = #continue
   checkout.error     = [data-test="error"]
   ```

2. **Locators class** — `src/main/java/com/mycucumbertest/pages/<page>/<Page>Locators.java`
   Holds **no literal selectors** — owns a `LocatorRepository("<page>")` and looks each up by key.
   ```java
   public class CheckoutLocators {
       private final Page page;
       private final LocatorRepository repo = new LocatorRepository("checkout");
       public CheckoutLocators(Page page) { this.page = page; }
       public Locator firstName() { return page.locator(repo.selector("checkout.firstName")); }
       public Locator continueBtn() { return page.locator(repo.selector("checkout.continue")); }
       public Locator error() { return page.locator(repo.selector("checkout.error")); }
   }
   ```

3. **Page object** — `src/main/java/com/mycucumbertest/pages/<page>/<Page>.java`
   Extends `BasePage`. Exposes user *intentions* as methods; delegates all element lookup to the locators class. No selectors here.
   ```java
   public class CheckoutPage extends BasePage {
       private final CheckoutLocators loc;
       public CheckoutPage(Page page) { super(page); this.loc = new CheckoutLocators(page); }
       public void enterFirstName(String name) { loc.firstName().fill(name); }
       public void clickContinue() { loc.continueBtn().click(); }
       public String errorText() {
           return loc.error().isVisible() ? loc.error().textContent() : null;
       }
   }
   ```

4. **Feature file** — `src/test/resources/features/<name>.feature`
   Plain Gherkin, no selectors/Java. Reference data via `${...}` tokens, never literals.
   ```gherkin
   Feature: Checkout
     Scenario: User completes checkout with valid details
       Given user opens the checkout page
       When user enters first name "${standardUser.username}"
       And user clicks continue
       Then user should see the overview page
   ```

5. **Step definitions** — `src/test/java/com/mycucumbertest/steps/<Name>Steps.java`
   The glue. Get the live page from `Hooks.page`, resolve `${...}` with `DataResolver`, call the page object, **assert with JUnit 5**.
   ```java
   public class CheckoutSteps {
       CheckoutPage checkout = new CheckoutPage(Hooks.page);

       @When("user enters first name {string}")
       public void enter_first_name(String name) {
           checkout.enterFirstName(DataResolver.resolve(name));
       }

       @Then("user should see the overview page")
       public void see_overview() {
           assertTrue(checkout.isOverviewDisplayed(), "Overview page not shown");
       }
   }
   ```

No runner change is needed — `TestRunner` already globs the `steps` and `hooks`
packages and discovers any `.feature` under `features/`.

## Assertions are mandatory (and must be able to fail)

- Use **JUnit 5 only**: `org.junit.jupiter.api.Assertions` (`assertTrue`,
  `assertEquals`, `assertNotNull`, `fail`). Never `org.junit.Assert` (JUnit 4) — it
  may compile in the IDE but breaks on the Maven CLI build.
- Every scenario must end in a `Then` that asserts an observable outcome.
- For "no element / error not shown" branches use `fail("...")` — **never
  `assertFalse(false, ...)`**, which always passes and silently disables the test.
- Prefer asserting real text: `assertTrue(actual.contains(expected), "msg")`, and
  guard `null` before `.isEmpty()`/`.contains()`.

## New data?

If the scenario needs credentials/values, add them to
`src/test/resources/testdata/users.json` and reference `${key.field}` — never hard-code
in the feature or Java. Flat config (URLs, flags) goes in `config.properties` as `${key}`.

## Quick reference

| Layer | Path | Holds |
| --- | --- | --- |
| Object repository | `src/main/resources/locators/<page>.properties` | key → selector |
| Locators class | `…/pages/<page>/<Page>Locators.java` | `LocatorRepository` + keys |
| Page object | `…/pages/<page>/<Page>.java` | user actions (extends `BasePage`) |
| Feature | `src/test/resources/features/<name>.feature` | Gherkin + `${...}` |
| Steps | `…/steps/<Name>Steps.java` | glue + JUnit 5 assertions |

## After generating — verify

Run `mvn -q -B test-compile` to confirm everything wires up. If the user wants a
real run, `mvn test` (launches browsers); the suite stays green even on failures
(`testFailureIgnore=true`), so read `target/test-summary.html` for actual results.

## Common mistakes

| Mistake | Fix |
| --- | --- |
| Selector string written in Java | Put it in `locators/<page>.properties`; reference by key. |
| Hard-coded username/password/URL | Add to `users.json` / `config.properties`; use `${...}`. |
| `org.junit.Assert` (JUnit 4) import | Use `org.junit.jupiter.api.Assertions` (JUnit 5). |
| `assertFalse(false, ...)` in else branch | Use `fail("...")` — the no-op always passes. |
| Scenario with no `Then`/assertion | Add an assertion on an observable outcome. |
| `new <Page>(...)` with a stale page | Always `new <Page>(Hooks.page)` (set per scenario by `@Before`). |
| Editing `TestRunner` glue for new steps | Not needed — `steps`/`hooks` packages are already globbed. |