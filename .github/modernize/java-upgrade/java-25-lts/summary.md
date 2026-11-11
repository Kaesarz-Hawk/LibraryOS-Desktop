# Java Runtime Upgrade Summary: LibraryOS

## Result

Java 25 LTS (`25.0.5`) is now the Maven compilation target and the application documentation and launcher messaging identify Java 25 as the supported runtime.

## Changes

- Updated `maven.compiler.source`, `maven.compiler.target`, and `maven.compiler.release` from 21 to 25.
- Updated the in-app runtime label, native launcher fallback message, roadmap, and deployment guide.
- Added the upgrade plan and execution progress records.

## Validation

- Baseline on Java 21.0.11: compile and tests succeeded.
- Java 25.0.5: `mvn clean test-compile -q` succeeded.
- Java 25.0.5: `mvn clean test -q` succeeded.
- Test result: 104 passed, 0 failures, 0 errors.

## Notes

- The workspace is not a Git repository, so no branch or commit was created.
- The checked-in `dist\LibraryOS\runtime` remains a generated Java 21.0.11 runtime. Regenerate the distributable package with JDK 25 before release.
- Automated CVE validation could not be invoked because the upgrade integration tool was unavailable in this environment.
