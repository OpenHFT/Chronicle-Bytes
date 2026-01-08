# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chronicle-Bytes is a low-level memory access library providing high-performance, off-heap memory operations with support for 63-bit buffer sizes, memory-mapped files, thread-safe operations, and deterministic resource management through reference counting. It serves as a more feature-rich alternative to Java NIO ByteBuffer.

**Version:** 2026.0-SNAPSHOT
**Group ID:** net.openhft
**Build System:** Apache Maven
**Java Version:** 11+

## Essential Build Commands

### Building the project
```bash
# Clean build with all tests
mvn clean verify

# Build with log output for review
mkdir -p logs
mvn verify -l logs/mvn-verify.log
rg -n '^\[(WARNING|ERROR)\]|SLF4J\(W\)|\bWARNING:|\bwarning:' logs/mvn-verify.log
```

### Running tests
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=BytesTest

# Run a specific test method
mvn test -Dtest=BytesTest#testWriteBoolean

# Run tests matching a pattern
mvn test -Dtest=*MappedFile*
```

### Code coverage
```bash
# Generate JaCoCo coverage report
mvn verify

# Report available at: target/site/jacoco/index.html
# Current thresholds: Line coverage 71.0%, Branch coverage 62.3%
```

### Other commands
```bash
# Skip tests
mvn clean install -DskipTests

# Run microbenchmarks (requires run-benchmarks profile)
mvn clean install -Prun-benchmarks

# Check dependencies
mvn dependency:tree
```

## Architecture Overview

### Core Abstraction Hierarchy

Chronicle-Bytes uses a layered abstraction where `BytesStore` represents fixed-size memory regions and `Bytes` extends it with mutable read/write cursors:

```
RandomCommon
    ├── RandomDataInput (read operations)
    └── RandomDataOutput (write operations)
            └── BytesStore (fixed-size, immutable bounds)
                    └── Bytes (mutable with cursors)
                            ├── VanillaBytes (standard implementation)
                            ├── NativeBytes (off-heap)
                            ├── OnHeapBytes (heap-based)
                            ├── MappedBytes (memory-mapped files)
                            └── HexDumpBytes (debugging)
```

### Key Architectural Concepts

1. **Reference Counting**: All `BytesStore` and `Bytes` instances require manual resource management via `reserve()` and `release()`. Always call `releaseLast()` when done to free resources deterministically.

2. **Streaming vs Random Access**: The library supports both streaming operations (using read/write positions) and random access (using indexed methods). Unlike Java's `ByteBuffer`, no flipping is required between reading and writing.

3. **Elastic vs Fixed Buffers**: `BytesStore` has fixed capacity, while `Bytes` can be elastic and automatically resize as needed (up to a maximum capacity).

4. **On-Heap vs Off-Heap**: Supports both heap-backed memory (byte arrays, ByteBuffers) and native off-heap memory with direct access. Memory-mapped files are also supported with configurable chunk sizes and sync modes.

5. **63-bit Addressing**: Unlike Java NIO's 31-bit limitation, Chronicle-Bytes supports up to 63-bit buffer sizes (8 exbibytes).

### Package Structure

- **Root package** (`net.openhft.chronicle.bytes`): Core interfaces (`Bytes`, `BytesStore`, `StreamingDataInput/Output`) and main implementations
- **`internal`**: Implementation details not part of public API (`NativeBytesStore`, `HeapBytesStore`, `BytesInternal`)
- **`algo`**: Hashing algorithms (`BytesStoreHash`, `XxHash`)
- **`ref`**: Reference types for shared memory access (`BinaryLongReference`, `TextLongReference`)
- **`render`**: Decimal rendering and formatting (`Decimaliser`, `DecimalAppender`)
- **`util`**: String interning, compression, utilities (`UTF8StringInterner`, `Compression`)
- **`pool`**: Object pooling for `Bytes` instances
- **`domestic`**: File utilities (`ReentrantFileLock`)

### Memory Management Pattern

Chronicle-Bytes uses explicit reference counting rather than relying on garbage collection:

```java
Bytes bytes = Bytes.allocateElasticDirect(64);
try {
    // Use bytes
} finally {
    bytes.releaseLast();  // Deterministic cleanup
}
```

Thread-safe sharing requires using `BytesStore` (which is immutable) rather than `Bytes`.

### Marshalling and Serialization

The `BytesMarshallable` interface provides binary serialization without type information (similar to Chronicle Wire's RawWire). The library also supports method reader/writer patterns using dynamic proxies for message-driven architectures.

## Code Style and Standards

### Language and Encoding
- **British English** spelling required (`organisation`, `serialise`, `colour`) except technical US terms (`synchronized`)
- **Source files:** ISO-8859-1 encoding only (code points 0-255)
- **Application I/O:** UTF-8 encoding
- Avoid Unicode characters in source; use textual forms (`>=`, `micro-second`)

### Javadoc Standards
- Only document what is **not** obvious from the method signature
- Focus on behavioural contracts, edge cases, thread-safety guarantees, units, performance characteristics
- Keep first sentence short (becomes summary line)
- Remove or rewrite trivial autogenerated Javadoc for getters/setters
- Never duplicate obvious information ("Gets the value", "Sets the name")

### Testing Requirements
- Tests are in `src/test/java/` mirroring the main source structure
- Test categories: unit tests (main package), performance tests (`perf/`), regression tests (`issue/`), internal tests (`internal/`), documentation examples (`readme/`)
- Use JUnit 5 for new tests; JUnit Vintage Engine supports legacy tests
- Test isolation: Tests run with 4 parallel forks via Maven Surefire
- Resource tracking is enabled via `jvm.resource.tracing=true` in `system.properties`

## Important Notes

### System Properties
Key system properties are documented in `docs/systemProperties.adoc`:
- `bytes.guarded`: Enable guarded mode for bounds checking
- `bytes.bounds.unchecked`: Disable bounds checking per buffer
- `trace.mapped.bytes`: Debug memory-mapped file operations
- `bytes.max-array-len`: Maximum array length (default: 16MB)

### Build Verification Before PRs
Before opening a pull request:
1. Run `mvn verify` to ensure clean build
2. Capture output with `-l logs/mvn-verify.log`
3. Review log for warnings: `rg -n '^\[(WARNING|ERROR)\]|SLF4J\(W\)|\bWARNING:|\bwarning:' logs/mvn-verify.log`
4. Ensure all tests pass and coverage thresholds are met

### Binary Compatibility
This project uses `binary-compatibility-enforcer-plugin` to ensure 100% binary compatibility. Breaking changes to public API are not allowed without careful review.

### Common Patterns
- Always read existing code before making changes
- Use existing abstractions rather than creating new ones
- Prefer editing existing files over creating new ones
- Be careful not to introduce security vulnerabilities (command injection, XSS, SQL injection, etc.)
- Avoid over-engineering: only make changes that are directly requested or clearly necessary

### Commit Message Format
- Subject line <= 72 characters, imperative mood
- Reference JIRA/GitHub issue if exists
- Body format: root cause → fix → measurable impact
- See AGENTS.md for full commit and PR guidelines

## Documentation Structure

- **README.adoc**: Main user-facing documentation with examples
- **AGENTS.md**: Guidelines for AI agents and human contributors
- **docs/systemProperties.adoc**: System property reference
- **src/main/docs/*.adoc**: API guides and architectural overviews
- Javadoc: Available at https://www.javadoc.io/doc/net.openhft/chronicle-bytes/latest/

## Module Structure

The repository contains:
- **Main module**: `/home/peter/2026/Chronicle-Bytes/` (production code and tests)
- **Microbenchmarks**: `/home/peter/2026/Chronicle-Bytes/microbenchmarks/` (JMH performance benchmarks)
