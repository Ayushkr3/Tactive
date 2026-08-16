# AI Change-Loop Evidence

## Feature request

> Make `./mvnw test` runnable on a fresh checkout without requiring a local PostgreSQL server, while retaining PostgreSQL for the production application.

## Tools used

| Tool | Use |
|---|---|
| Codex (GPT-5) | Inspected the assessment, audited the Maven project, implemented and verified the change. |
| Maven Wrapper | Compiled and executed the JUnit suite. |

## Attempts

| Attempt | Change / observation | Result | Correction |
|---:|---|---|---|
| 1 | Ran `./mvnw test`. The Spring context attempted to connect to `jdbc:postgresql://localhost:5432/jobs`. | Red: `SchedulerApplicationTests.contextLoads` failed because PostgreSQL was unavailable. | Add a test-scoped H2 database and test-only datasource configuration. |
| 2 | Added H2 and a test datasource. Maven selected the JUnit 3 provider because the project did not declare the standard Spring test starter. | Red: zero tests ran and the forked JVM reported `org/mockito/verification/VerificationMode`. | Add `spring-boot-starter-test` with test scope. |
| 3 | Removed duplicate Maven dependencies, retained PostgreSQL at runtime, and configured H2 only under `src/test/resources`. | Green: all 27 tests passed. | None required. |

## Deliberate red run

To prove the full application-context check catches a broken configuration, the test datasource was temporarily changed to `jdbc:invalid:deliberate-red-run` and the suite was executed. It failed as expected:

```text
Driver org.h2.Driver claims to not accept jdbcUrl, jdbc:invalid:deliberate-red-run
Tests run: 27, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```

The datasource was restored immediately afterward. The final, restored run is green:

```text
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Files changed in the loop

- `pom.xml` — normalized duplicate dependencies; added H2 and Spring's test starter in test scope.
- `src/test/resources/application.properties` — H2 configuration isolated from production settings.

## Manual intervention

No manual code correction was necessary after the third attempt. Dependency downloads were allowed once so Maven could obtain the new test-only artifacts.
