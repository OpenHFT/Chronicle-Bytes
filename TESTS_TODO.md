# Chronicle Bytes – Test Coverage TODO

Keeps track of Byte-specific test work (including batch additions beyond the module hand-offs). Follow AGENTS rules (British English, ISO-8859-1) and run `mvn -q verify` before publishing.

## Repo & Commands

* Repo root: `/home/peter/Build-All/Chronicle-Bytes`
* Iteration command (targeted):
  ```bash
  mvn -q -Dtest=BytesLifecycleTest,BytesCopyMatrixTest,UncheckedBytesBehaviourTest test
  ```
* Full suite: `mvn -q verify`

## Completed This Session

1. Added `BytesLifecycleTest` (slice ref-count lifecycle, elastic-growth monotonicity, `copyTo(BytesStore)` correctness).
2. Added `BytesCopyMatrixTest` (heap→native copy and direct→`OutputStream` copy coverage).
3. Extended `UncheckedBytesBehaviourTest` with `uncheckedModeAllowsWritePastLimit`.
4. Targeted tests run via `mvn -q -Dtest=BytesLifecycleTest,BytesCopyMatrixTest,UncheckedBytesBehaviourTest test` – presently passing.

## Outstanding Batches

The following scenarios (from the user brief) remain **unimplemented** and should be prioritised next:

1. **OS/Safe Page Size Matrix** – parameterised test over synthetic page sizes verifying `MappedBytesStore#inside` and `SingleMappedBytes.zapPage` when `OS.defaultOsPageSize()` differs from actual page size. Add to `MappedBytesStoreTest` or `MappedBytesTest`.
2. **Safe-Page Block Size for Builders** – test builder defaults so Windows uses `OS.SAFE_PAGE_SIZE` while other OSes use `OS.pageSize()`. Add a new `MappedBytesQueueBuilderTest` or equivalent.
3. **Temp Directory Integration** – ensure Bytes’ use of `IOTools.createTempDirectory` returns paths under `OS.getTarget()` and cleans up (likely add a dedicated test leveraging `BytesTestCommon`).
4. **Reference Tracing Hook** – add `AbstractBytesReferenceTest` that leaks a `Bytes` subclass intentionally and asserts `enableReferenceTracing()` reports the leak (mirrors similar Chronicle Core test).
5. **OS.pageAlign Consumers** – assert `MappedBytesStore.map()` (or helpers) align offsets using `OS.pageAlign`, especially for large offsets/Windows safe pages; extend `MappedBytesTest` with a synthetic offset scenario.

## Additional Proposed Tests

6. **`BytesComparisonMatrixTest`**
   * Exercise `BytesStore.compareAndSwapInt/Long` across heap, direct, and mapped stores to ensure mixed-type comparisons behave consistently and honour alignment rules. Include negative tests where the address straddles the capacity boundary to confirm deterministic exceptions.

7. **`ElasticByteBufferReuseTest`**
   * Reproduce the pattern from Chronicle Wire: allocate an elastic direct buffer, write past the original capacity with `writePosition` growth, then hand the buffer to `BinaryWire`. Assert that ref-counts stay positive, and once `releaseLast()` is invoked the backing `ByteBuffer` becomes inaccessible (guards against use-after-free regressions).

8. **`TempFileCleanupTest`**
   * Create temporary `Bytes` backed by `IOTools.createTempFile()` and verify that `Bytes.deleteFile()` removes both the data file and the `.tmp` companion. Mimic busy Windows behaviour by holding a channel open and confirming the new retry loop logs the appropriate warning.

9. **`BytesHexDumpDeterminismTest`**
   * Feed `HexDumpBytes` with sequences containing control characters and high-bit values, ensuring `toHexString()` and `parseHexString()` round-trip identically (protects tooling that relies on deterministic dumps when debugging network captures).

10. **`UnsafeDirectStoreAlignmentTest`**
    * Parameterise over odd/even alignment sizes to assert that `UnsafeDirectStore.of()` respects the requested alignment and that `addressForRead`/`addressForWrite` never exceed `realCapacity()`. This mirrors the Chronicle Queue use-case where misalignment breaks memory-mapped header validation.

## Next Steps

1. Design tests for items 1–5 above; consider using `OS.memory()`/wrapper to mock page sizes where needed.
2. After implementing each chunk, re-run targeted tests plus `mvn -q verify`.
3. Update this file (and per-module `NEW_TESTS.md` if you add repo-specific notes) once the outstanding scenarios are covered.

## Additional Ideas (Queue-derived backlog)
| Batch | Scope | Goal | Notes |
|------|-------|------|-------|
| B-07 | Zero-length frame handling | Add Bytes-level tests that write/read empty payloads using `Bytes` and ensure downstream consumers observe zero-length content without blocking the next message. | Mirrors `ReaderResizesFileTest.testZeroLengthDocumentDoesNotBlockTailer`; ensures Bytes APIs surface the same invariants. |
| B-08 | Partial frame mutation guards | Extend Bytes fuzz tests to mutate SPB headers mid-read (length mismatch, corrupted prefix) and assert defensive checks fire before data becomes visible. | Aligns with queue tests for partial frames; keeps raw Bytes primitives honest. |

## Fresh Candidates (add to queue soon)

1. **PinnedMappedBytesShutdownTest** – simulate long-lived `MappedBytes` held open during JVM shutdown, assert `MappedFile.release()` frees native handles and log noise stays below the warning threshold (covers Windows busy-file reports).
2. **BytesUTF8SurrogateMatrixTest** – feed `AppendableUtil` with mixed valid and broken surrogate pairs, verifying encoder paths either emit replacement chars or throw per `StopCharTesters` expectations; ensures UTF-8 helpers keep Chronicle Wire interop safe.
3. **NativeBytesBoundsRaceTest** – run two threads performing `writePosition` + `readPosition` mutations against the same `NativeBytes` instance under a `Pauser` to confirm CAS guards prevent negative capacities and throw `BufferOverflowException` deterministically.
4. **ReadonlyMappedBytesRelaxedViewTest** – open the same mapped file as read-only and read-write Bytes, flip the writable view via `MappedBytes.writingDocument()`, and assert the read-only view never observes partial header writes (protects replication recovery scans).
5. **BytesCompressionRoundTripTest** – integrate `LZ4Compressor`/`SnappyCompressor` adapters to compress into a `Bytes` sink, then decompress via the matching codec to prove direct/native stores do not require intermediary arrays and capacity grows elastically without leaks.
