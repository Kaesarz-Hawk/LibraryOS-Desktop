# Upgrade Plan: LibraryOS (java-25-lts)

- **Generated**: 2026-11-04 23:42 +06:00
- **HEAD Branch**: N/A
- **HEAD Commit ID**: N/A

## Available Tools

**JDKs**
- JDK 21.0.11: `C:\Program Files\Java\jdk-21.0.11` (baseline)
- JDK 25.0.5: `C:\Users\Kawsar Hoque\.jdk\jdk-25.0.5+8` (target)

**Build Tools**
- Maven 3.9.9: `C:\Users\Kawsar Hoque\tools\apache-maven-3.9.9`

## Guidelines

- Upgrade the Java runtime to the latest LTS release available for this execution: Java 25.
- Preserve application behavior and avoid unrelated dependency upgrades.

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: N/A (workspace is not a Git repository)
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java 25

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| --------------------- | ------- | ---------------------- | ---------------- |
| Java | 21 | 25 | User requested Java 25 LTS |
| JavaFX | 21.0.2 | 21.0.2 | Existing JavaFX line is retained to minimize scope |
| Maven | 3.9.9 | 3.9.9 | Compatible with Java 25 |
| maven-compiler-plugin | 3.13.0 | 3.13.0 | Supports the requested release level |
| maven-surefire-plugin | 3.2.5 | 3.2.5 | Existing test execution remains compatible |

## Derived Upgrades

- Set compiler source, target, and release to 25 so Maven emits Java 25 bytecode.
- Update deployment and release documentation from Java 21 LTS to Java 25 LTS.
- The bundled `dist\LibraryOS\runtime` is generated output from Java 21. It should be regenerated with JDK 25 when producing a distributable installer; it is not used by Maven compilation or tests.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|------------|---------|--------|--------|--------|
| pom.xml | maven.compiler.source | 21 | upgrade | 25 | Compile source at Java 25 |
| pom.xml | maven.compiler.target | 21 | upgrade | 25 | Emit Java 25 class files |
| pom.xml | maven.compiler.release | 21 | upgrade | 25 | Enforce Java 25 API surface |

### Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|-----------------|--------|
| src/main/java/com/libraryos/view/AdminDeskView.java | Runtime Environment profile row | Java 21 LTS | Display Java 25 LTS | Keep the in-app runtime status accurate |
| src/main/csharp/Launcher.cs | Fallback error and standard JDK path | Java 21 | Reference Java 25 | Keep the native launcher aligned with the supported runtime |

### Configuration Changes

No application configuration changes are required.

### CI/CD Changes

No CI/CD files are present. The generated bundled runtime under `dist\LibraryOS\runtime` remains outside Maven source configuration and should be regenerated during packaging.

### Risks & Warnings

- JavaFX 21.0.2 is retained. If the packaged application shows runtime incompatibility on Java 25, upgrade JavaFX as a separate compatibility change rather than changing unrelated dependencies here.
- The checked-in distributable runtime reports Java 21.0.11. Maven verification does not rebuild that generated runtime; release packaging must use JDK 25.

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Confirm Java 25 and Maven 3.9.9 are available.
  - **Changes to Make**: Use the installed JDK 25 and Maven 3.9.9.
  - **Verification**: `java -version` and `mvn -version`; both report the target toolchain.

- Step 2: Setup Baseline
  - **Rationale**: Establish pre-upgrade compilation and test results.
  - **Changes to Make**: None.
  - **Verification**: Java 21 `mvn clean compile test-compile -q` and `mvn clean test -q`; expected success.

- Step 3: Upgrade Java Compiler Target and Documentation
  - **Rationale**: Make the project compile and document the runtime requirement as Java 25.
  - **Changes to Make**: Apply all Dependency Changes, Source Code Changes, and update the roadmap and deployment guide.
  - **Verification**: Java 25 `mvn clean test-compile -q`; expected success.

- Step 4: Final Validation
  - **Rationale**: Confirm all test classes and runtime behavior pass on Java 25.
  - **Changes to Make**: None unless validation exposes a Java 25 compatibility issue.
  - **Verification**: Java 25 `mvn clean test -q`; expected 100% pass rate.
