# logos-kotlin — agent guide

This repo is the Kotlin Multiplatform port of the upstream Rust `logos` crate (from [maciejhirsz/logos](https://github.com/maciejhirsz/logos)). Upstream source lives at `tmp/logos/` and is the **read-only** translation oracle. Never edit `tmp/`.

## Scope

`logos` is a fast lexer generator. The crate ships:

- a runtime (`Lexer<T>`, `Source`, `Span`, `Skip`, `Filter`, `Logos` trait)
- a procedural derive macro that generates the lexer state machine at compile time
- internal NFA/DFA construction, regex parsing, literal table compilation

This Kotlin port covers the runtime and the state-machine engine. The `#[derive(Logos)]` proc-macro has no Kotlin equivalent; the port replaces it with a declarative builder API and (eventually) a Gradle/codegen plugin for users who want a static lexer.

## Maven coordinates

`io.github.kotlinmania:logos:<version>`

Package root: `io.github.kotlinmania.logos`. Subpackages mirror the upstream Rust module tree (`logos/src/source.rs` → `io.github.kotlinmania.logos.source`).

## Port-lint headers

Every Kotlin file MUST start with:

```kotlin
// port-lint: source <path-relative-to-tmp/logos/>
package io.github.kotlinmania.logos.<module>
```

Path is relative to `tmp/logos/`. This is how `ast_distance` tracks provenance — never remove or alter unless re-targeting a different Rust source.

## Translation discipline

Line-by-line transliteration. Read the Rust file end to end, then port. Don't reorder, summarize, or "improve."

- **Doc comments translate word-for-word.** Rust syntax inside KDoc gets rewritten to Kotlin equivalents (`Vec<T>` → `List<T>`, `Option<&str>` → `String?`, lifetimes dropped, `Self::foo()` → `foo()`).
- **No no-op shells.** Rust constructs the GC subsumes (`Box<T>`, `Cell<T>`, `RefCell<T>`, `Arc<T>`, `Rc<T>`, `Pin`, `mem::forget`, `drop_in_place`, `MaybeUninit<T>`, `dyn Trait`) get **deleted** in the port.
- **No `mod.rs` → `Mod.kt`.** Re-home implementation, rewire callers.
- **Tests live in `commonTest`.** Inline `#[cfg(test)] mod tests` ports to `commonTest` mirroring the same package path.
- **Procedural macros translate to runtime APIs, not to nothing.** A `#[derive(Logos)]` site gets a Kotlin builder/factory that constructs the equivalent lexer at runtime; future codegen can replace it with static code.

## Code discipline

- **No `@Suppress`.** Warnings are errors.
- **No stubs.** No `TODO()`, no `error("not implemented")`, no empty class bodies on types that have fields and methods.
- **No JVM imports.** No `kotlin.jvm.*`, no `java.*`, no `javax.*`. Pure Kotlin Multiplatform.
- **No synthetic typealiases for ergonomics.**

## Blast radius

- No repo-wide scripting (`find -exec`, blanket `sed`/`perl`, regex over many files).
- Changes are task-scoped. Every touched file is named up front.
- Small multi-file changes are allowed when mechanically coupled — primary file plus its `commonTest` and any required call-site rewires.
- More than ~5 files in a single change? Stop and ask.

## Verification

The build gate is **`./gradlew test`**.

```bash
./gradlew macosArm64Test
./gradlew linuxX64Test
./gradlew jsNodeTest
./gradlew wasmJsNodeTest
```

`./gradlew jvmTest` is **not** valid — there is no JVM target.

## Approved dependencies

- `kotlinx-coroutines-core`
- `kotlinx-serialization-core`, `kotlinx-serialization-json`
- `kotlinx-collections-immutable`

Add a new dependency only after confirming it publishes for **every** target above.

## Dependents

Downstream Kotlin consumers (must stay on a published version of this artifact):

- `starlark-syntax-kotlin` — uses logos for `lexer.rs`'s tokenizer.
- (Future) any other Kotlin port of a Rust crate that depends on `logos` upstream.

## Subagent policy

Do not delegate `.kt` writes to subagents. Search and read-only reports via subagents are fine. Edits happen in the main loop.

## Commit style

No AI branding, no Co-Authored-By lines, no emoji. Clear, descriptive messages focused on what changed and why. One file → one commit.
