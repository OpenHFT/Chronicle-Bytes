# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Chronicle Bytes is a low-level memory access library that provides a high-performance alternative to Java's `ByteBuffer`. It offers off-heap memory management with deterministic resource cleanup, support for 63-bit sizes, and rich APIs for reading/writing primitives, strings (UTF-8/ISO-8859-1), and complex data structures.

**Key concepts:**
- **Bytes vs BytesStore**: `Bytes` instances are elastic and track read/write positions; `BytesStore` instances have fixed capacity and no position tracking
- **Off-heap memory**: Most implementations work with native memory outside the Java heap
- **Reference counting**: All off-heap resources must be explicitly released via `releaseLast()` or similar
- **Flyweight pattern**: Bytes objects act as views over underlying memory

## Build Commands

### Basic Build and Test

```bash
# Clean build with tests
mvn clean verify

# Build without tests (faster iteration)
mvn clean install -DskipTests

# Quiet mode (less output)
mvn -q clean verify
```

### Running Single Tests

```bash
# Run specific test class
mvn -Dtest=BytesTest test

# Run specific test method
mvn -Dtest=BytesTest#testAllocateElasticDirect test
```

### Code Quality Checks

```bash
# Run Checkstyle (checks coding standards)
mvn checkstyle:check

# Run SpotBugs (static analysis)
mvn spotbugs:check

# Quality profile with all checks
mvn -P quality clean verify

# Code coverage with JaCoCo
mvn -P sonar clean verify
```

### Benchmarks

```bash
# Run microbenchmarks
mvn -P run-benchmarks clean test
```

## Project Structure

```
src/main/java/net/openhft/chronicle/bytes/
  ├── Bytes.java              # Main interface - elastic, position-aware
  ├── BytesStore.java         # Fixed-size memory block interface
  ├── BytesIn.java            # Read operations interface
  ├── BytesOut.java           # Write operations interface
  ├── BytesMarshallable.java  # Serialization support
  ├── MappedBytes.java        # Memory-mapped file wrapper
  ├── NativeBytes.java        # Off-heap implementation
  ├── VanillaBytes.java       # Standard implementation
  ├── HexDumpBytes.java       # Debug wrapper with hex output
  ├── algo/                   # Algorithms (hashing, compression)
  ├── internal/               # Internal implementation classes
  ├── pool/                   # Object pooling
  ├── ref/                    # Reference types
  └── util/                   # Utility classes

src/main/docs/                # AsciiDoc documentation (canonical location)
  ├── project-requirements.adoc
  ├── architecture-overview.adoc
  ├── decision-log.adoc
  └── security-review.adoc
```

## Architecture Principles

### Memory Management
- Chronicle Bytes uses **reference counting** for deterministic cleanup of off-heap resources
- Always call `bytes.releaseLast()` when done (or use try-with-resources)
- Tests MUST use `assertReferencesReleased()` from `Chronicle-Test-Framework` to verify cleanup

### Position Tracking
Every `Bytes` instance maintains four key positions:
- `readPosition`: where to read from next
- `writePosition`: where to write to next
- `readLimit`: maximum position that can be read
- `writeLimit`: maximum position that can be written

Unlike `ByteBuffer`, you don't need to flip between reading and writing.

### Threading
- `Bytes` instances are NOT thread-safe by default
- `BytesStore` can be shared across threads if data access is synchronized
- Atomic operations (CAS, volatile reads/writes) are available for `int`, `long`, `float`, `double`

### Encoding
- **Binary encoding**: Fixed-width primitives, stop-bit compression
- **Text encoding**: Parsing and appending primitives as text
- **String encoding**: Both ISO-8859-1 (8-bit) and UTF-8 supported
- **Stop-bit encoding**: Variable-length compression (see https://github.com/OpenHFT/RFC/blob/master/Stop-Bit-Encoding/Stop-Bit-Encoding-1.0.adoc)

## Common Development Tasks

### Creating Bytes Instances

```java
// On-heap, elastic
Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap();

// Off-heap, elastic (must release)
Bytes<?> bytes = Bytes.allocateElasticDirect();
try {
    // use bytes
} finally {
    bytes.releaseLast();
}

// Memory-mapped file
MappedBytes bytes = MappedBytes.mappedBytes(file, chunkSize);
```

### Reading and Writing

```java
// Binary primitives
bytes.writeInt(42);
bytes.writeLong(123L);
int value = bytes.readInt();

// With explicit positions (random access)
bytes.writeInt(offset, 42);
int value = bytes.readInt(offset);

// Strings
bytes.writeUtf8("hello");
bytes.write8bit("world");
String s = bytes.readUtf8();

// Stop-bit compressed
bytes.writeStopBit(1234567L);
long value = bytes.readStopBit();
```

### Testing Resource Cleanup

```java
@Test
public void testBytesCleanup() {
    Bytes<?> bytes = Bytes.allocateElasticDirect();
    bytes.writeInt(42);
    bytes.releaseLast();

    // Verify all off-heap resources released
    assertReferencesReleased();
}
```

## Code Style Requirements

### Language and Character Set
- **British English** spelling (`synchronise`, `behaviour`, `colour`)
- **ISO-8859-1** characters only - no smart quotes, em-dashes, or Unicode
- Use `>=`, `<=` instead of Unicode symbols
- Check with: `iconv -f ascii -t ascii`

### Javadoc
- Only document what's NOT obvious from the method signature
- Explain behaviour, contracts, thread-safety, edge cases, performance
- Remove autogenerated "Gets the X" / "Sets the Y" comments
- Keep first sentence concise (it becomes the summary)

**Good Javadoc:**
```java
/**
 * Reads a stop-bit encoded long value. Values in range [-63, 127]
 * consume 1 byte; larger values use variable-length encoding.
 *
 * @return the decoded long value
 * @throws BufferUnderflowException if insufficient bytes available
 */
long readStopBit();
```

**Bad Javadoc:**
```java
/**
 * Reads stop bit.
 *
 * @return long the long
 */
long readStopBit();
```

### Dependencies
This module depends on:
- `chronicle-core` - foundational utilities (`Jvm`, `OS`, resource management)
- `posix` - native OS calls
- `chronicle-test-framework` - test utilities (use for `assertReferencesReleased()`)

When adding dependencies, use versions from `chronicle-bom` or `third-party-bom`.

## Important System Properties

Set these via `-Dproperty=value`:

| Property | Default | Purpose |
|----------|---------|---------|
| `bytes.guarded` | `false` | Enable additional safety checks |
| `bytes.bounds.unchecked` | `false` | Disable bounds checking (performance) |
| `trace.mapped.bytes` | `false` | Debug mapped file lifecycle |
| `bytes.max-array-len` | `16777216` | Max array length for reads |

See `docs/systemProperties.adoc` for complete list.

## Documentation Standards

- **Format**: AsciiDoc (`.adoc`)
- **Location**: `src/main/docs/` (canonical)
- **Language**: British English
- **Character set**: ISO-8859-1
- **Source highlighter**: `:source-highlighter: rouge`
- **Section numbering**: Use `:sectnums:` in header

Key documentation files:
- `project-requirements.adoc` - Functional requirements with Nine-Box tags (FN, NF-P, etc.)
- `decision-log.adoc` - Architecture Decision Records
- `architecture-overview.adoc` - High-level design
- `security-review.adoc` - Security considerations

## Testing Guidelines

- Use **JUnit 5** for new tests (JUnit 4 supported for legacy)
- Test class naming: `*Test` suffix
- Always verify resource cleanup with `assertReferencesReleased()`
- Use `SystemTimeProvider` for time-dependent tests
- Surefire config: `forkCount=4, reuseForks=true`

## Before Opening a PR

1. Run `mvn clean verify` - must exit with code 0
2. Run `mvn checkstyle:check` if touching production code
3. Verify all new off-heap allocations are released
4. Update relevant `.adoc` documentation
5. Write commit message: imperative mood, ≤72 chars, reference JIRA/GitHub issue
6. Explain: root cause → fix → measurable impact

## Common Pitfalls

1. **Forgetting to release**: Off-heap `Bytes` instances must call `releaseLast()`
2. **Thread safety**: `Bytes` is NOT thread-safe; use `BytesStore` with synchronization
3. **Position confusion**: Remember `writePosition` is also the `readLimit`
4. **Character encoding**: Don't use Unicode in source files - ISO-8859-1 only
5. **Javadoc noise**: Remove "Gets X" / "Sets Y" comments that add no value

## Additional Resources

- README.adoc - User-facing introduction with examples
- AGENTS.md - General AI agent guidelines (Javadoc policy, build commands)
- `docs/systemProperties.adoc` - Complete system property reference
- `src/main/docs/decision-log.adoc` - Architecture decisions
