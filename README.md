> This project is one example of the test cases and frameworks I have created as part of my work. It is provided for demonstration purposes and does not cover all of my skills and techniques. I am happy to answer questions — feel free to contact the author for further details.
>
> This repository uses a separate Task Board demo application; code and data from work projects are not published.

# Java + Playwright · QA Portfolio

![Java](https://img.shields.io/badge/Java-17_LTS-ED8B00?logo=openjdk&logoColor=white)
![Playwright](https://img.shields.io/badge/Playwright-Java-2EAD33?logo=playwright&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white)
![GitLab](https://img.shields.io/badge/GitLab-CI%2FCD-FC6D26?logo=gitlab&logoColor=white)

A small project demonstrating mid-level QA Automation skills: API and UI tests in **Java**, Page Objects, typed models, environment configuration, browser session isolation, and CI. Playwright officially supports Java, so JavaScript is not required to write tests. JavaScript is used only in the demo application's interface.

## Quick start

You need **JDK 17+** and an internet connection for the initial download of Maven, dependencies, and Chromium. Neither Node.js nor a preinstalled Maven is required: the project uses Playwright Java and Maven Wrapper.

```bash
cp .env.example .env
./mvnw test-compile exec:java -Dexec.args="install chromium"
./mvnw clean verify
./mvnw surefire-report:report-only
```

On Windows, use `mvnw.cmd` and copy `.env.example` using your operating system's tools. On Linux, if system libraries are missing, install them with `./mvnw exec:java -Dexec.args="install --with-deps chromium"` (installing system packages may require administrator privileges).

By default, the tests start a local Task Board at `127.0.0.1:8080` and stop it after each test class. Data is stored in memory. Free up the port or specify another one in `BASE_URL`. The first API test run also requires downloading the Playwright Java driver, but API tests do not need a browser.

```bash
./mvnw test -Dgroups=api            # backend only
./mvnw test -Dgroups=ui             # UI tests with API checks
./mvnw test -Dgroups=unit           # configuration checks, without Playwright
./mvnw test -Dgroups=ui -DHEADLESS=false
./mvnw validate                    # Spotless + Checkstyle
./mvnw spotless:apply              # automatically format Java code
```

In IntelliJ IDEA, open `pom.xml` as a Maven project, select JDK 17+, and run the classes under `tests/`. Set the run configuration's working directory to the project root so that `.env` can be read.

## BASE_URL and environments

Precedence: **JVM `-DKEY=value` → environment variable → `.env` → `src/test/resources/test.properties`**. The `.env` file is loaded through dotenv-java, does not require `source`, and is excluded from Git.

| Key | Default | Purpose |
|---|---|---|
| `BASE_URL` | `http://127.0.0.1:8080` | Shared base address for UI and API |
| `DEMO_MODE` | `true` | Start the bundled local application |
| `HEADLESS` | `true` | Run Chromium without a visible window |
| `TIMEOUT_MS` | `10000` | Timeout for requests, actions, and navigation |

Example using a different port and path prefix:

```bash
./mvnw test -DBASE_URL=http://127.0.0.1:9090/qa/
```

Example `.env` for an already deployed environment:

```dotenv
BASE_URL=https://your-test-host.example/qa/
DEMO_MODE=false
HEADLESS=true
TIMEOUT_MS=15000
```

`your-test-host.example` is a placeholder and must be replaced. Endpoints are relative: `api/tasks` becomes `https://your-test-host.example/qa/api/tasks`. A trailing `/` is added automatically. Demo mode only allows HTTP on `127.0.0.1` with an explicit port, preventing an accidental local server launch when a remote environment was intended.

**Changing BASE_URL changes the environment, not the application contract.** These scenarios expect Task Board with the API described below and its corresponding UI. For another product, add its API client, Page Objects, and tests. The tests create, modify, and delete data; use an environment intended for automated testing.

## Architecture and files

```text
.
├── .env.example                    # sample environment settings
├── .gitlab-ci.yml                   # lint → test → deploy report
├── pom.xml                         # dependencies and plugins with pinned versions
├── mvnw / mvnw.cmd                  # run Maven without a global installation
├── config/checkstyle.xml           # linter rules
└── src/test/
    ├── java/com/portfolio/
    │   ├── config/TestConfig.java   # load, prioritize, and validate settings
    │   ├── api/Endpoints.java       # static relative paths
    │   ├── api/TasksClient.java     # HTTP operations via Playwright APIRequestContext
    │   ├── model/Task.java          # Java record for JSON responses
    │   ├── pages/TasksPage.java     # UI locators and actions
    │   ├── support/BaseTest.java   # environment and API context lifecycle
    │   ├── support/UiTest.java     # browser, context, screenshot, and trace
    │   ├── tests/                  # API, UI, and unit scenarios
    │   └── demo/DemoServer.java    # local system under test using JDK HttpServer
    └── resources/
        ├── test.properties        # default settings
        ├── junit-platform.properties
        └── demo/index.html        # demo application UI
```

**Test → Page Object / API client → application.** A test defines the scenario and assertions. The Page Object encapsulates locators, the client encapsulates HTTP calls, and the record represents data. The client returns the HTTP response so that both success and error statuses can be explicitly checked.

Inheritance is used only for shared setup and teardown (`BaseTest` → `UiTest`); the client and Page Object receive dependencies through their constructors. Interfaces and factories without multiple implementations have not been added. All code lives under `src/test`: this is a test repository, and a separate library is not needed yet.

Each test gets a separate API context; each UI test gets a new browser and BrowserContext. Test task titles are unique, and created tasks are deleted in `finally` blocks. Tests run sequentially: Playwright objects are confined to their thread, and the demo server uses a shared port. Parallel execution would require a separate lifecycle per thread and isolated servers/data.

UI tests use role/label/test-id locators and Playwright waiting mechanisms instead of `Thread.sleep`. UI assertions automatically retry until the timeout. When creating a task, the UI test explicitly waits for the POST response; backend checks verify persisted data in addition to the visual result.

## Sample test cases

| Area | Scenario | What is verified |
|---|---|---|
| API | Create → Read → Complete → Delete | 201/200/204/404, JSON fields, and persisted changes |
| API | Empty and whitespace-only title | 400 and a validation message, using a parameterized test |
| API | Title length of 120 / 121 characters | The boundary value is accepted; exceeding it is rejected |
| API | Unknown ID | 404 and a clear error message |
| API | Malformed JSON | 400 instead of an unhandled exception |
| UI + API | Create a task through the UI | The record is available through the API and survives a reload |
| API + UI | Prepare through the API, complete in the UI | The change is reflected in the UI, API, and after a reload |
| API + UI | Delete a task through the UI | The row disappears and the API returns 404 |
| UI | Empty title | A visible validation message |
| Unit | Configuration | URLs with a path prefix, invalid URLs, and timeout validation |

## Demo application contract

The paths below are relative to `BASE_URL`. This is the contract of the application **built for this portfolio**, not a claim about a third-party API.

| Method | Path | Body | Result |
|---|---|---|---|
| GET | `api/tasks` | — | 200, an array of tasks |
| POST | `api/tasks` | `{"title":"Review release"}` | 201, the created task |
| GET | `api/tasks/{id}` | — | 200, the task; 404 if it does not exist |
| PATCH | `api/tasks/{id}` | `{"completed":true}` | 200, the updated task |
| DELETE | `api/tasks/{id}` | — | 204; 404 if it does not exist |

Task: `{"id":"UUID","title":"Review release","completed":false}`. Leading and trailing whitespace is stripped from the title; its allowed length is 1–120 Java characters after stripping. Invalid JSON or fields return 400 with `{"error":"..."}`. The application has no authentication or database and serves as a small, reproducible test environment, not a production service.

## Reports and diagnostics

- `target/surefire-reports/` — JUnit results in XML and text formats.
- `target/site/surefire-report.html` — HTML generated by `surefire-report:report-only`.
- `target/ui-artifacts/<test>-<uuid>/` — a screenshot and trace for every UI test, including successful ones. These make it easier to inspect a run and investigate failures.

```bash
./mvnw exec:java -Dexec.args="show-trace target/ui-artifacts/<test-id>/trace.zip"
```

Replace `<test-id>` with the generated folder name. A trace shows actions, the DOM, and network requests. When testing a real environment, it may contain sensitive data; do not make such artifacts publicly accessible. A browser startup failure may occur before a trace is created.

## GitLab CI/CD

1. **lint:** Spotless checks Java formatting; Checkstyle checks imports, naming, required braces, and several basic rules.
2. **test:** runs the entire suite in the official Playwright Java Docker image and preserves JUnit XML, HTML, screenshots, and traces even when tests fail. JUnit results are available in the pipeline interface.
3. **deploy-report:** publishes the HTML report to **GitLab Pages** after successful checks on the default branch. Merge requests run lint/test without deployment. Failed tests do not replace the published successful report; failure results remain available as artifacts.

Deployment here means publishing test results; the demo server runs only during tests. You need GitLab with `pages.publish` support (17.10+) and a Docker runner with access to Maven Central and Microsoft Container Registry. Pages must be available on your GitLab instance. The Docker image version must match `playwright.version` in `pom.xml`.

CI variables are defined in YAML; override `BASE_URL` and `DEMO_MODE` in GitLab → Settings → CI/CD → Variables if needed. For environments with private data, restrict access to artifacts and Pages. Screenshots and traces are not copied to Pages, but report text may also contain data from errors.

The pipeline configuration is included in the repository; an actual deployment requires uploading the project to GitLab and having a working runner. A local run does not prove that the GitLab pipeline executes successfully.

## Extending the framework

Add relative paths to `Endpoints`, methods to the API client, a Page Object for a new page, and scenarios tagged `api`/`ui`. For another resource, create a separate client and model. Add authentication, other browsers, and parallel execution when the project needs them.

Documentation: [Playwright Java + JUnit](https://playwright.dev/java/docs/test-runners), [API testing](https://playwright.dev/java/docs/api-testing), [GitLab Pages](https://docs.gitlab.com/user/project/pages/).
