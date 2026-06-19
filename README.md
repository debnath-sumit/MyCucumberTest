# MyCucumberTest

UI test automation suite using **Cucumber (BDD)** + **Playwright (Java)**, run on
**JUnit 5 Platform**, with an HTML test report emailed on a schedule via **GitHub
Actions**.

- **Build/test runner:** Maven
- **BDD:** Cucumber (`cucumber-java`, `cucumber-junit-platform-engine`)
- **Browser automation:** Playwright for Java
- **Assertions:** JUnit 5 (`org.junit.jupiter.api.Assertions`)
- **Reporting:** Cucumber HTML/JSON + a custom pass/fail summary (`test-summary.html`)

---

## Project structure

```
MyCucumberTest/
├── pom.xml                                  # Maven build, deps, plugins (surefire + exec)
├── README.md
├── .github/
│   └── workflows/
│       └── scheduled-tests-email.yml        # CI: runs tests on schedule + emails the report
└── src/
    ├── main/java/
    │   ├── com/mycucumbertest/
    │   │   ├── pages/                        # Page Object Model
    │   │   │   ├── BasePage.java
    │   │   │   └── login/
    │   │   │       ├── LoginPage.java
    │   │   │       └── LoginPageLocators.java
    │   │   ├── report/
    │   │   │   └── CucumberReportGenerator.java  # reads cucumber.json -> test-summary.html
    │   │   └── utils/
    │   │       ├── ConfigReader.java         # reads config.properties / -D system props
    │   │       ├── DataResolver.java         # resolves ${...} tokens in feature files
    │   │       ├── PlaywrightFactory.java    # builds Playwright browser/page
    │   │       ├── TestUser.java             # immutable user record
    │   │       └── TestUsers.java            # loads users.json
    │   └── org/example/Main.java
    └── test/
        ├── java/com/mycucumbertest/
        │   ├── hooks/Hooks.java              # @Before/@After: browser lifecycle
        │   ├── runners/TestRunner.java       # JUnit Platform @Suite -> Cucumber engine
        │   └── steps/LoginSteps.java         # step definitions
        └── resources/
            ├── config.properties             # base.url, browser, headless
            ├── logback-test.xml              # logging config
            ├── features/
            │   └── login.feature             # Gherkin scenarios
            └── testdata/
                └── users.json                # test credentials (referenced via ${...})
```

---

## Running the tests locally

```bash
# Run all tests; generates reports under target/
mvn test

# Override config at runtime (see config.properties for keys)
mvn test -Dheadless=false -Dbrowser=firefox -Dbase.url=https://www.saucedemo.com/
```

### Generated reports (in `target/`, recreated each run)

| File | Produced by |
| --- | --- |
| `cucumber-report.html` | Cucumber plugin |
| `cucumber.json` | Cucumber plugin |
| `test-summary.html` | `CucumberReportGenerator` (custom pass/fail summary) |
| `surefire-reports/` | Maven Surefire |

> The pom sets `testFailureIgnore=true`, so `mvn test` stays green even when
> scenarios fail — this guarantees `test-summary.html` is always generated for
> the email. Set it to `false` if you want CI to fail on test failures.

---

## Externalizing test data (`${...}` placeholders)

Feature files reference data by token instead of hard-coding values. At runtime
`DataResolver` resolves each `${...}`:

1. First against `testdata/users.json` as `${userKey.field}` (e.g. `${standardUser.username}`)
2. Then against `config.properties` / `-D` system properties (e.g. `${base.url}`)

Plain literals without `${...}` pass through unchanged.

```gherkin
When user enters username "${standardUser.username}" and password "${standardUser.password}"
```

To add a user: add a block to `testdata/users.json`, then reference
`${yourKey.username}` in the feature.

---

## CI: scheduled tests + emailed report

Workflow: `.github/workflows/scheduled-tests-email.yml`

- **Schedule:** daily at **05:00 UTC** (`cron: "0 5 * * *"` = 10 PM Pacific / PDT).
  GitHub cron is UTC and does not observe DST; adjust the hour to change the time.
- **Also runs on demand** via `workflow_dispatch`.
- Runs `mvn test`, then emails `target/test-summary.html` as the HTML body via
  Gmail SMTP (`dawidd6/action-send-mail`).

### Required repository secrets

Set under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `MAIL_USERNAME` | Sender Gmail address |
| `MAIL_PASSWORD` | Gmail **App Password** (16 chars; needs 2-Step Verification — *not* your normal password) |
| `MAIL_TO` | Recipient address (comma-separated for multiple) |

Create the App Password at <https://myaccount.google.com/apppasswords>.

```bash
gh secret set MAIL_USERNAME -R debnath-sumit/MyCucumberTest
gh secret set MAIL_PASSWORD -R debnath-sumit/MyCucumberTest
gh secret set MAIL_TO       -R debnath-sumit/MyCucumberTest
```

### Trigger the workflow manually

```bash
# Trigger a run (uses the workflow_dispatch trigger)
gh workflow run "scheduled-tests-email.yml" -R debnath-sumit/MyCucumberTest --ref main

# Follow it
gh run list  -R debnath-sumit/MyCucumberTest -L 3      # list recent runs + status
gh run watch -R debnath-sumit/MyCucumberTest           # live-tail the latest run
```

Or in the browser: **Actions → "Scheduled Cucumber Tests + Email Report" → Run workflow → branch `main`**.

> Notes:
> - Scheduled workflows run only on the **default branch** (`main`) and
>   auto-disable after 60 days with no commits.
> - `gh workflow run` only *queues* the run; it prints no run ID — use
>   `gh run list` / `gh run watch` to follow it.
