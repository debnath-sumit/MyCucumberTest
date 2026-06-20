# MyCucumberTest — Framework Document

UI test-automation framework for web applications, built on **Behaviour-Driven
Development (BDD)**. Tests are written in plain-English Gherkin, driven through a
real browser with **Playwright**, executed on the **JUnit 5 Platform**, and
reported as HTML — with a scheduled **GitHub Actions** job that runs the suite
and emails the result.

The suite currently targets the public demo site <https://www.saucedemo.com/> and
exercises its login screen, but the structure is reusable for any web app.

---

## 1. Technology stack

| Concern | Choice | Version |
| --- | --- | --- |
| Language / build | Java + Maven | Java 17 |
| BDD framework | Cucumber (`cucumber-java`, `cucumber-junit-platform-engine`) | 7.34.3 |
| Test platform | JUnit 5 Platform Suite | BOM 5.14.2 |
| Browser automation | Playwright for Java | 1.60.0 |
| Assertions | JUnit 5 (`org.junit.jupiter.api.Assertions`) | via BOM |
| JSON (data + report) | Gson | 2.11.0 |
| Logging | SLF4J + Logback | 2.0.16 / 1.5.12 |
| CI | GitHub Actions (`dawidd6/action-send-mail`) | — |

> **Assertion note:** always use JUnit 5 (`org.junit.jupiter.api.Assertions`).
> JUnit 4 imports (`org.junit.Assert`) may compile in the IDE but fail on a
> clean Maven CLI build.

---

## 2. Design philosophy

The framework is a layered **Page Object Model (POM)** with a strict separation
of responsibilities. Each layer only knows about the one directly beneath it:

```
Gherkin feature (.feature)        ← what to test, in business language
        │
Step definitions (LoginSteps)     ← glue: maps Gherkin lines to page actions
        │
Page object (LoginPage)           ← user actions ("login", "enterCredentials")
        │
Locators (LoginPageLocators)      ← CSS/selectors only — the single place to edit
        │
Playwright Page                   ← the actual browser
```

Cross-cutting concerns sit beside these layers:

- **Hooks** own the browser lifecycle (start before each scenario, tear down after).
- **utils** provide configuration, test data, the browser factory, and placeholder resolution.
- **report** turns Cucumber's JSON output into a human-friendly HTML summary.

The guiding rules:

1. **Selectors live in exactly one file per page** (`*Locators`). A UI change is a one-file edit.
2. **No credentials or URLs hard-coded in tests** — they come from `users.json` / `config.properties`.
3. **Features read like business requirements**, free of selectors and Java.

---

## 3. Project structure

```
MyCucumberTest/
├── pom.xml                                       # Maven build, deps, plugins (surefire + exec)
├── README.md                                     # how to run + CI setup
├── FRAMEWORK.md                                  # this document
├── .github/workflows/
│   └── scheduled-tests-email.yml                 # CI: scheduled run + emailed report
└── src/
    ├── main/java/com/mycucumbertest/
    │   ├── pages/                                 # Page Object Model
    │   │   ├── BasePage.java                      #   common page helpers (title, url)
    │   │   └── login/
    │   │       ├── LoginPage.java                 #   login actions
    │   │       └── LoginPageLocators.java         #   login selectors only
    │   ├── report/
    │   │   └── CucumberReportGenerator.java       # cucumber.json -> test-summary.html
    │   └── utils/
    │       ├── ConfigReader.java                  # config.properties + -D overrides
    │       ├── DataResolver.java                  # resolves ${...} tokens
    │       ├── PlaywrightFactory.java             # builds the browser per config
    │       ├── TestUser.java                      # immutable user record
    │       └── TestUsers.java                     # loads users.json
    └── test/
        ├── java/com/mycucumbertest/
        │   ├── hooks/Hooks.java                   # @Before/@After browser lifecycle + tracing
        │   ├── runners/TestRunner.java            # JUnit Platform @Suite -> Cucumber engine
        │   └── steps/LoginSteps.java              # step definitions (glue)
        └── resources/
            ├── config.properties                  # base.url, browser, headless
            ├── logback-test.xml                    # logging config
            ├── features/login.feature              # Gherkin scenarios
            └── testdata/users.json                 # test credentials (referenced via ${...})
```

> `src/main/java/org/example/Main.java` is a leftover archetype stub and is not
> part of the test framework.

---

## 4. Component reference

### 4.1 Pages (the Page Object Model)

**`BasePage`** — abstract parent holding the Playwright `Page` and small shared
helpers (`title()`, `url()`). Every page object extends it.

**`LoginPage`** — exposes *user intentions* as methods (`openLoginPage()`,
`login(TestUser)`, `enterCredentials(...)`, `clickLogin()`,
`isHomePageDisplayed()`, `isLoginErrorDisplayed()`). It holds **no selectors** —
it delegates all element lookups to `LoginPageLocators`.

**`LoginPageLocators`** — returns Playwright `Locator`s and nothing else
(`#user-name`, `#password`, `#login-button`, `.error-button`, `.title`). When the
UI changes, this is the only file to touch.

> **Adding a new page:** create `pages/<area>/<Area>Page.java` (extends
> `BasePage`) and `pages/<area>/<Area>PageLocators.java`. Keep actions in the
> page, selectors in the locators.

### 4.2 Hooks (browser lifecycle)

`Hooks` is the Cucumber equivalent of a BaseTest. Annotated with Cucumber's
`@Before` / `@After`, it runs around **every scenario**:

- **`@Before setup()`** — creates Playwright, launches the browser via
  `PlaywrightFactory`, opens a fresh `BrowserContext` with **tracing**
  (screenshots + snapshots + sources) enabled, and a new `Page`.
- **`@After tearDown(Scenario)`** — on failure, attaches a full-page screenshot
  to the Cucumber report; always stops tracing and saves the trace zip under
  `target/traces/<scenario>.zip`; then closes context, browser, and Playwright.

The `page`, `browser`, `context`, and `playwright` are exposed as `public
static` fields so step definitions can grab the live `Page`
(`new LoginPage(Hooks.page)`).

### 4.3 Step definitions (glue)

`LoginSteps` maps each Gherkin line to a page action. Steps stay thin — they
resolve `${...}` data tokens via `DataResolver`, call the page object, and assert
with JUnit 5. Step ↔ Gherkin mapping:

| Gherkin step | Method |
| --- | --- |
| `Given user opens the login page` | `openLoginPage()` |
| `When user enters username {string} and password {string}` | `enterCredentials(...)` |
| `When user enters username {string}` | `enterUsername(...)` |
| `When user enters password {string}` | `enterPassword(...)` |
| `When user clicks on login button` | `clickLogin()` |
| `Then user should see the home page` | `assertTrue(isHomePageDisplayed())` |
| `Then user should get an error message` | asserts default error text |
| `Then user should get an error message {string}` | asserts the supplied text |

### 4.4 Runner

`TestRunner` is a JUnit 5 **Platform Suite** that boots the Cucumber engine:

- `@SelectClasspathResource("features")` — where the `.feature` files live.
- `GLUE` = `com.mycucumbertest.steps, com.mycucumbertest.hooks` — where steps and hooks live.
- `PLUGIN` = `pretty, html:target/cucumber-report.html, json:target/cucumber.json` — report outputs.

Surefire picks it up because the filename matches `**/TestRunner.java`.

### 4.5 Utilities

| Class | Responsibility |
| --- | --- |
| **`ConfigReader`** | Resolves config in priority order: **`-Dkey` system property → `config.properties` → caller fallback**. Provides `get`, `get(key, default)`, `getBool`. |
| **`PlaywrightFactory`** | Launches the browser named by `browser` (chromium/firefox/webkit) honouring the `headless` flag. |
| **`TestUser`** | Immutable `record(username, password)`. |
| **`TestUsers`** | Loads `testdata/users.json` once; provides `standard()`, `lockedOut()`, `user(key)`, and `field(userKey, field)` (used by `DataResolver`). |
| **`DataResolver`** | Replaces `${...}` tokens. For `${key.field}` it tries `users.json` first (key before the first dot, field after), then `ConfigReader`. Plain literals pass through; an unresolved token throws. |

### 4.6 Reporting

`CucumberReportGenerator` is a standalone `main` that reads
`target/cucumber.json`, flattens every scenario (walking its before-hooks, steps,
and after-hooks — a scenario passes only if **all** are `passed`), and emits:

- a **console** pass/fail summary, and
- **`target/test-summary.html`** — a styled report listing failed scenarios
  first (with the failing step + error), then passed ones, with totals and
  durations.

It runs automatically during `mvn test` via the `exec-maven-plugin` bound to the
`test` phase.

---

## 5. Data-driven testing (`${...}` placeholders)

Feature files never hard-code credentials. They reference tokens that
`DataResolver` resolves at runtime:

```gherkin
When user enters username "${standardUser.username}" and password "${standardUser.password}"
```

Resolution order for `${key.field}`:

1. **`testdata/users.json`** — `key` before the first dot is the user, the rest is the field.
2. **`config.properties` / `-Dkey`** — for flat tokens like `${base.url}`.

`testdata/users.json` currently defines `standardUser`, `lockedOutUser`, and
`invalidUser`. **To add a user:** add a block to `users.json`, then reference
`${yourKey.username}` in a feature — no Java change needed.

---

## 6. Configuration

`src/test/resources/config.properties` holds the defaults:

```properties
base.url=https://www.saucedemo.com/
browser=chromium      # chromium | firefox | webkit
headless=true
```

Any key can be overridden at runtime with `-Dkey=value` (system properties win
over the file), so the same suite runs locally headed and in CI headless without
edits.

---

## 7. Running the tests

```bash
# Run everything; generates all reports under target/
mvn test

# Watch the browser, use Firefox, point at a different app
mvn test -Dheadless=false -Dbrowser=firefox -Dbase.url=https://www.saucedemo.com/
```

### Generated artifacts (`target/`, recreated each run)

| Path | Produced by | Purpose |
| --- | --- | --- |
| `cucumber-report.html` | Cucumber plugin | Rich step-level HTML report |
| `cucumber.json` | Cucumber plugin | Machine-readable results (input to the summary) |
| `test-summary.html` | `CucumberReportGenerator` | Pass/fail summary emailed by CI |
| `traces/<scenario>.zip` | `Hooks` | Playwright trace (open with `npx playwright show-trace`) |
| `surefire-reports/` | Maven Surefire | Standard JUnit XML/text reports |

> **`testFailureIgnore=true`** in `pom.xml` keeps `mvn test` green even when
> scenarios fail, so `test-summary.html` is always produced for the email. Set it
> to `false` if CI should fail the build on test failures.

---

## 8. Execution flow (end to end)

```
mvn test
  └─ Surefire runs TestRunner (JUnit Platform Suite)
       └─ Cucumber engine scans src/test/resources/features
            └─ for each Scenario:
                 Hooks.@Before  → launch browser, new traced context + page
                 Steps          → DataResolver resolves ${...} → LoginPage acts via Locators → JUnit asserts
                 Hooks.@After   → screenshot on failure, save trace, close browser
       └─ writes cucumber-report.html + cucumber.json
  └─ exec-maven-plugin runs CucumberReportGenerator
       └─ reads cucumber.json → console summary + target/test-summary.html
```

---

## 9. CI: scheduled run + emailed report

Workflow: `.github/workflows/scheduled-tests-email.yml`

- **Schedule:** daily at **05:00 UTC** (`cron: "0 5 * * *"`). GitHub cron is UTC
  and ignores DST; scheduled runs are best-effort and auto-disable after 60 days
  with no commits. Scheduled runs only fire on the default branch (`main`).
- **On demand:** `workflow_dispatch` lets you trigger from the Actions tab.
- **Concurrency:** a `scheduled-tests` group prevents overlapping runs.
- **Steps:** checkout → set up JDK 17 (Temurin, Maven cache) → install Playwright
  Chromium + OS deps → `mvn -B test` → build email body from `test-summary.html`
  (`if: always()`) → send via Gmail SMTP with `dawidd6/action-send-mail`.

### Required repository secrets

Under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `MAIL_USERNAME` | Sender Gmail address |
| `MAIL_PASSWORD` | Gmail **App Password** (16 chars; requires 2-Step Verification) |
| `MAIL_TO` | Recipient(s), comma-separated |

### Trigger / follow manually

```bash
gh workflow run "scheduled-tests-email.yml" -R debnath-sumit/MyCucumberTest --ref main
gh run list  -R debnath-sumit/MyCucumberTest -L 3
gh run watch -R debnath-sumit/MyCucumberTest
```

> CI note: when building the email body, `test-summary.html` has no trailing
> newline, so the workflow `echo ""`s before the closing heredoc delimiter to
> keep `$GITHUB_OUTPUT` valid.

---

## 10. Extending the framework

| Goal | Steps |
| --- | --- |
| **New scenario** | Add it to an existing `.feature`; reuse existing steps where possible. |
| **New step phrasing** | Add a `@Given/@When/@Then` method in the relevant `*Steps` class. |
| **New page** | Add `<Area>Page` (extends `BasePage`) + `<Area>PageLocators`; register the steps package is already globbed via the `steps` glue. |
| **New test user** | Add a block to `testdata/users.json`; reference `${key.field}`. |
| **New config knob** | Add the key to `config.properties`; read via `ConfigReader.get(...)`; override with `-Dkey`. |
| **Different browser** | `-Dbrowser=firefox|webkit|chromium` (already wired in `PlaywrightFactory`). |

---

## 11. Conventions & gotchas

- Use **JUnit 5** assertions only (`org.junit.jupiter.api.Assertions`); JUnit 4
  imports can break a clean CLI build.
- Keep **selectors out of page objects** — they belong in `*Locators`.
- Keep **credentials/URLs out of Java** — they belong in `users.json` /
  `config.properties`.
- `Hooks` fields are `public static`; step classes are instantiated per scenario
  and read `Hooks.page` after `@Before` has run.
- The runner filename **must** match a Surefire include
  (`**/*Test.java`, `**/*Tests.java`, `**/*TestCase.java`, `**/TestRunner.java`).