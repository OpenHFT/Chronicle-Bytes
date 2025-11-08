# Chronicle Bytes – In-Flight Test Additions

Short log of the extra tests added while working through `TESTS_TODO.md`. Keep
this in sync with `TESTS_TODO.md` so the next session can immediately see what
landed vs. what is pending.

## Implemented This Pass

1. **`MappedFileTest.insideHonoursSafeLimitWhenPageSizeDiffers`**
   * Exercises a chunk acquired with a page size larger than the OS default.
   * Verifies that `MappedBytesStore#inside` respects `safeLimit()` so elastic
     mappings do not overrun chunk boundaries.
2. **`MappedBytesTest.zeroOutRespectsCustomPageSize`**
   * Confirms `MappedBytes.zeroOut` clears all bytes when the mapping page size
     is tuned (captures previous regressions when the OS default was assumed).
3. **`TempDirectoryIntegrationTest`**
   * Uses `IOTools.createTempDirectory` end-to-end and asserts the artefacts are
     rooted under `OS.getTarget()` and removed afterwards.
4. **`ReferenceTracingLeakTest`**
   * Leaks a direct `Bytes` on purpose and ensures
     `AbstractReferenceCounted.assertReferencesReleased()` raises the expected
     diagnostic with `createdHere()`.
5. **`DecoratedBufferOverflowExceptionTest`**
   * Documents the null-cause semantics: passing `null` mirrors the
     single-argument constructor, while a real `Throwable` is preserved as the
     cause.
6. **`NativeBytesOverflowTest#overflowWithoutTracingKeepsCauseNull`**
   * Forces a capacity overflow with reference tracing disabled to ensure the
     `DecoratedBufferOverflowException` thrown by `NativeBytes.newDBOE(...)`
     retains a `null` cause instead of throwing.

All of the above pass under `mvn -q -Dtest=... test` (see latest run in shell
history) and under the module-wide `mvn -q verify`.

## Still Outstanding

* Safe-page defaults for builder APIs (Windows vs. other OSes) – see
  `TESTS_TODO.md` item “Safe-Page Block Size for Builders”.
* Additional `OS.pageAlign` coverage for chunked mappings – see
  `TESTS_TODO.md` item “OS.pageAlign Consumers”.

Once those tests are implemented, rerun:

```bash
mvn -q -Dtest=MappedFileTest,MappedBytesTest,TempDirectoryIntegrationTest,ReferenceTracingLeakTest,DecoratedBufferOverflowExceptionTest,NativeBytesOverflowTest test
mvn -q verify
```

Remember to update the Matching AsciiDoc (especially `memory-management.adoc`)
whenever diagnostics or behavioural guarantees change, and keep the AGENTS
guidelines (British English + ISO-8859-1) in mind.
