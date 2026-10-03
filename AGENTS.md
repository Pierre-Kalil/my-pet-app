# Repository Guidelines

## Project Structure & Module Organization

MeuPet is a single-module Android application. The `app/` module contains all
production code and resources. Kotlin sources live in
`app/src/main/java/com/example/`: `ui/` contains Compose screens, UI state, and
view models; `data/` contains Room entities, DAOs, repositories, database
setup, backup, and file utilities; and `notification/` contains scheduling and
receiver code. Android resources are under `app/src/main/res/`, and the
manifest is `app/src/main/AndroidManifest.xml`.

Local JVM/Robolectric tests belong in `app/src/test/`; device and emulator
tests belong in `app/src/androidTest/`. Keep packages aligned with production
code where practical. Dependency versions are centralized in
`gradle/libs.versions.toml`.

## Build, Test, and Development Commands

Run commands from the repository root:

- `./gradlew :app:assembleDebug` builds an installable debug APK.
- `./gradlew :app:testDebugUnitTest` runs local JUnit and Robolectric tests.
- `./gradlew :app:connectedDebugAndroidTest` runs instrumented tests on a
  connected emulator or device.
- `./gradlew :app:lintDebug` performs Android lint checks.

Open the root directory in Android Studio to run the `app` configuration. Copy
`.env.example` to `.env` and set `GEMINI_API_KEY` for features requiring it;
never commit `.env`, keystores, or credentials.

## Coding Style & Naming Conventions

Write Kotlin using four-space indentation and the existing Compose style.
Use PascalCase for classes, composables, and files containing their primary
type (for example, `DashboardScreen.kt`); use camelCase for functions,
properties, and parameters. Name UI state classes `*UiState`, view models
`*ViewModel`, DAOs `*Dao`, and repositories `*Repository`. Prefer immutable
state, `StateFlow` for observable UI state, and small composables with state
hoisted to view models. Let Android Studio format Kotlin before committing.

## Testing Guidelines

Use JUnit 4 for unit tests and Robolectric when Android resources or framework
behavior is needed. Test files should end in `Test` and test names should
describe behavior, such as `savingReminder_schedulesNotification()`. Add or
update unit tests for data and view-model behavior; add instrumentation tests
for device-specific UI or integration behavior. Run the relevant Gradle test
task before opening a change.

## Commit & Pull Request Guidelines

Git history is not available in this checkout, so use concise imperative
subjects such as `feat: add reminder validation` or `fix: restore notification
after boot`. Keep commits focused. Pull requests should explain the user-facing
change, list validation performed, link the related issue when present, and
include screenshots or a short recording for Compose UI changes. Call out
database migrations, permissions, notification behavior, or configuration
changes explicitly.
