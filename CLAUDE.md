# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build       # Full build (includes tests)
./gradlew bootRun     # Run the application
./gradlew test        # Run all tests
./gradlew clean build # Clean rebuild
./gradlew bootJar     # Build executable JAR
```

Run a single test class:
```bash
./gradlew test --tests "id.nahsbyte.pos_service_revamp.SomeTestClass"
```

## Stack

- **Language:** Kotlin (Java 17 toolchain)
- **Framework:** Spring Boot 4.0.5
- **Build:** Gradle (Kotlin DSL — `build.gradle.kts`)
- **Database:** PostgreSQL via Spring Data JPA
- **Security:** Spring Security
- **Monitoring:** Spring Boot Actuator

## Architecture

This is a POS (Point-of-Sale) backend service in early development. The base package is `id.nahsbyte.pos_service_revamp`.

**Kotlin/Spring notes:**
- The build uses the `kotlin-allopen` plugin so JPA entity classes do not need `open` modifiers explicitly.
- The `kotlin-jpa` plugin generates no-arg constructors required by JPA.
- Compiler flag `-Xjsr305=strict` enforces nullability annotations from Java libraries.

**Configuration:**
- `src/main/resources/application.properties` — main config file. PostgreSQL datasource and Spring Security settings need to be added before the app will fully start.
