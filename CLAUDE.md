# Claude Code notes

Read `AGENTS.md` first. This file only adds what Claude needs that AGENTS.md does not cover.

## What this library is

Chronicle Bytes is a low-level off-heap memory access library: a `ByteBuffer` alternative with
63-bit sizes, reference-counted cleanup, and rich primitive/string/struct APIs.

* `Bytes` is elastic and tracks read/write positions; `BytesStore` is fixed-size with no positions.
* Off-heap allocations are reference-counted: every `allocateElasticDirect()` needs `releaseLast()`
  (or use try-with-resources). Tests assert this with `assertReferencesReleased()` from
  `chronicle-test-framework`.
* `Bytes` is not thread-safe. `BytesStore` may be shared if writes are externally synchronised.
  Atomic CAS / volatile ops exist on `int`, `long`, `float`, `double`.
* `writePosition` is also the `readLimit`; you do not flip between modes.

## Where things live

* Production code: `src/main/java/net/openhft/chronicle/bytes/` (sub-packages `algo`, `internal`,
  `pool`, `ref`, `render`, `util`, `domestic`).
* Documentation: `src/main/docs/*.adoc` (canonical). `system-properties.adoc` is the full property
  reference.
* Tests: JUnit 5 preferred; `*Test` suffix; `forkCount=4, reuseForks=true`.
* Dependencies are pinned via `chronicle-bom` / `third-party-bom`; do not hard-code versions.

## Things that bite

* Forgetting `releaseLast()` on direct `Bytes`.
* Using `Bytes` from multiple threads.
* Pasting Unicode (smart quotes, em-dashes) into source or `.adoc` files; the build will accept it
  but it violates the ISO-8859-1 rule in AGENTS.md.
* Adding `@Deprecated` to public API based on this repo's test usage alone: downstream
  (Chronicle-Wire, Chronicle-Queue, customers) almost certainly uses it.
