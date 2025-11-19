# Chronicle Bytes Project Analysis

**Date Created:** 2025-11-19
**Purpose:** AI-generated summary for improving Chronicle Bytes development and onboarding.

---

## 1. Project Overview

**Chronicle Bytes** is a high-performance Java library for low-level memory access, acting as an advanced alternative to Java's `ByteBuffer`. It is built on `Chronicle Core` and provides direct memory and OS-level system call access.

The library is designed for performance-critical applications, offering features like:
- Support for 63-bit addressing.
- Off-heap, thread-safe memory operations.
- Deterministic resource management via reference counting.
- Elastic buffers that resize dynamically.
- Efficient UTF-8 and ISO-88-59-1 string encoding/decoding.
- Data compression (stop bit encoding).
- Direct parsing and manipulation of text in off-heap memory.

The project is structured as a standard Maven project. The core source code is located in `src/main/java`, with tests in `src/test/java`. Extensive documentation, including architectural notes and requirements, is present in `src/main/docs`.

## 2. Building and Running

The project is built and managed using **Apache Maven**.

### Key Commands

*   **Compile the project:**
    ```bash
    mvn compile
    ```

*   **Run tests:**
    ```bash
    mvn test
    ```

*   **Package the project (create JAR):**
    ```bash
    mvn package
    ```

*   **Install the artifact to your local Maven repository:**
    ```bash
    mvn install
    ```

*   **Run Benchmarks:** The `pom.xml` defines a specific profile for running benchmarks.
    ```bash
    mvn test -P run-benchmarks
    ```

*   **Enable Assertions:** A profile exists to run with zero-cost assertions enabled.
    ```bash
    mvn test -P assertions
    ```
*   **Run static analysis and coverage:**
    ```bash
    # From repo root
    mvn -P quality clean verify
    mvn -P sonar clean verify
    ```
## 3. Development Conventions

### Testing
- The project uses **JUnit 5** for unit testing (`junit-jupiter-api`, `junit-jupiter-params`).
- There is a strong emphasis on data-driven testing, with test data stored in text files under `src/test/resources`. These tests use a custom `BytesTextMethodTester` harness.
- The project includes performance benchmarks located in the `microbenchmarks` directory and also within the main source tree under `net.openhft.chronicle.bytes.perf`.
- Code coverage is monitored, with thresholds defined in the `pom.xml`.

### Code Style & Dependencies
- The project is part of the OpenHFT family and follows its conventions.
- Dependencies are managed centrally through a `chronicle-bom` (Bill of Materials).
- The code includes annotations (`org.jetbrains.annotations`) to improve code quality and static analysis.
- Logging is handled via SLF4J.
- The project follows British English spelling.
- The character-set is ISO-88-59-1.

### Documentation
- The project maintains extensive documentation in AsciiDoc format (`.adoc`) under `src/main/docs`. Key documents include:
    - `project-requirements.adoc`: Detailed functional and non-functional requirements.
    - `architecture-overview.adoc`: High-level architecture.
    - `decision-log.adoc`: A log of important design decisions.
- The main `README.adoc` is comprehensive and serves as the primary entry point for understanding the library's features.
- Javadoc should only explain what is not manifest from the signature.

### Commits and Pull Requests
- Commit messages should have a subject line of 72 characters or less, written in the imperative mood.
- The body of the commit message should explain the root cause, the fix, and the measurable impact.
- Pull requests should be focused on a single issue and should be linked to the relevant issue or decision record.
- The build must pass (`mvn -q clean verify`) before opening a pull request.

## 4. AI Agent Guidelines

- AI agents are expected to follow all the development conventions mentioned above.
- AI-generated content should be reviewed for accuracy, relevance, and adherence to the project's documentation standards.
- AI agents should focus on clarity and avoid redundancy.

## 5. Key Files and Directories

*   `pom.xml`: The Maven project configuration file. Defines dependencies, build profiles, and plugins.
*   `README.adoc`: The main project documentation with detailed usage examples.
*   `AGENTS.md`: Guidelines for AI agents, bots, and human contributors.
*   `TODO.md`: Tracks work specific to Chronicle-Bytes that feeds into the master architecture documentation.
*   `CI_DATA_CHECKLIST.md`: A TODO list for tracking CI data and evidence needed for the project to meet its architecture and compliance requirements.
*   `src/main/java/net/openhft/chronicle/bytes/`: The root package for the core library source code.
*   `src/main/java/net/openhft/chronicle/bytes/Bytes.java`: A central interface, likely defining the core `Bytes` API.
*   `src/main/java/net/openhft/chronicle/bytes/BytesStore.java`: An interface for fixed-size blocks of memory.
*   `src/test/java/net/openhft/chronicle/bytes/`: The root package for tests.
*   `src/main/docs/`: Contains detailed project documentation in AsciiDoc format.
*   `microbenchmarks/`: A separate module for running performance benchmarks.