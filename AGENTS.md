# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application. Root Gradle files (`build.gradle.kts`, `settings.gradle.kts`, and `gradle/libs.versions.toml`) define plugins, repositories, and dependency versions. Application code lives in `app/src/main/java/com/robson/tmp58printservice/`. The main components are the launcher activity, Android print service, printer discovery session, Bluetooth transport, and ESC/POS command builder. Android resources and service metadata are under `app/src/main/res/`, with permissions and component declarations in `app/src/main/AndroidManifest.xml`.

Local JVM tests belong in `app/src/test/`; device or emulator tests belong in `app/src/androidTest/`. Keep generated output in `build/` directories and out of version control.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper from the repository root:

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew installDebug` installs it on a connected emulator or device.
- `./gradlew testDebugUnitTest` runs local JUnit tests.
- `./gradlew connectedDebugAndroidTest` runs AndroidX tests on a connected device.
- `./gradlew lintDebug` runs Android lint checks.
- `./gradlew clean` removes generated build output.

The Gradle daemon toolchain is configured for JDK 25, while application bytecode targets Java 11. Android Studio can supply the SDK and manage device deployment.

## Coding Style & Naming Conventions

Follow the official Kotlin style configured in `gradle.properties`: four-space indentation, no tabs, and idiomatic null handling. Use `PascalCase` for classes, `camelCase` for functions and properties, and `UPPER_SNAKE_CASE` for constants. Keep package names lowercase under `com.robson.tmp58printservice`. Name resources in lowercase `snake_case`. Run `lintDebug` before submitting changes. Preserve clear separation between Android lifecycle code, Bluetooth I/O, and ESC/POS formatting.

## Testing Guidelines

JUnit 4 is used for host-side tests; AndroidX JUnit and Espresso support instrumentation tests. Name test classes after the subject (for example, `EscPosTest`) and test methods by behavior, such as `teste_includesCutCommand`. Add unit coverage for deterministic byte generation and instrumentation coverage for Android framework interactions. Bluetooth printing changes should also be verified on a paired `IMP-TMP58ABT`, including permission denial and connection failure paths.

## Commit & Pull Request Guidelines

History currently contains only the release-style commit `1.0`, so no established convention exists. Use short, imperative subjects such as `Handle Bluetooth connection failure`. Keep commits focused. Pull requests should explain behavior changes, list verification commands and device/API level, link related issues, and include screenshots only for visible UI changes. Call out changes to permissions, print-service metadata, or supported printer behavior.
