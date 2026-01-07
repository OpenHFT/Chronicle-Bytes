# AGENTS.md

## Scope
- Chronicle Bytes provides byte storage and I/O abstractions used across Chronicle libraries.
- Keep changes focused and avoid incidental formatting noise.

## Build and test
- Preferred full check:
  - `mkdir -p logs`
  - `mvn verify -l logs/mvn-verify.log`
- Module-scoped example:
  - `mvn -pl <module> -am verify -l logs/mvn-verify.log`
- Test example:
  - `mvn -Dtest=BytesTest test -l logs/mvn-test.log`
- Review logs:
  - `rg -n '^\[(WARNING|ERROR)\]|SLF4J\(W\)|\bWARNING:|\bwarning:' logs/mvn-verify.log`
- Microbenchmarks (run only when needed):
  - `mvn clean install -Prun-benchmarks -l logs/mvn-bench.log`
- Do not commit logs/.

## Repo map
- Tests live under `src/test/java` and should use JUnit 5 for new tests.
- Docs and decision logs live under `src/main/docs/`.
- System properties are documented in `docs/systemProperties.adoc`.

## Constraints
- Java baseline: 8 (avoid newer language features).
- Source files must stay ISO-8859-1 (code points 0-255). Prefer ASCII; avoid smart quotes and non-breaking spaces.
- Preserve public APIs; the binary-compatibility-enforcer plugin enforces compatibility.
- Treat warnings as defects; keep logs clean.
- Avoid extra allocations or synchronisation on hot paths.
- Release resources deterministically with `releaseLast()` or a try/finally block.

## Docs and review checklist
- Keep AsciiDoc, tests, and code synchronised; update docs for new requirements or behaviour.
- Javadoc must add behaviour, edge cases, thread safety, units, or performance notes.
- For large mechanical changes, declare the transformation rule and keep it consistent.

## References
- `OpenHFT/docs/Company-Wide-Tagging.adoc` for tagging and AsciiDoc conventions.
- `src/main/docs/project-requirements.adoc` and `src/main/docs/decision-log.adoc` for requirements and decisions.
