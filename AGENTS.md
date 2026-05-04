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

## Trait default methods with `where` clauses → method-level Kotlin generic bounds

Rust traits routinely declare a default method whose body only typechecks
when the type parameter satisfies a stricter bound:

```rust
pub trait RangeBounds<T> {
    fn start_bound(&self) -> Bound<&T>;
    fn end_bound(&self) -> Bound<&T>;

    fn is_empty(&self) -> bool
    where T: PartialOrd,
    { /* default body uses < */ }
}
```

The trait stays unconstrained; the *method* picks up the bound via its
own `where` clause. Kotlin has no per-method `where` on an interface
member. Three obvious mappings fail:

1. **Tighten the interface to `<T : Comparable<T>>`.** Breaks every
   caller that holds the unbounded interface type.
2. **Make the method abstract on the interface.** Forces every concrete
   impl to invent a body and pile on `override` boilerplate, even when
   the Rust counterpart inherits the default unchanged.
3. **Runtime cast helper** — `if (left is Comparable<*> ...) ... else throw IllegalStateException(...)`.
   Compile-time bounds become runtime crashes; the cheat detector flags
   this and zeros the file's score.

### The faithful pattern

Translate the default to a Kotlin **extension function whose own type
parameter carries the bound**:

```kotlin
interface RangeBounds<T> {
    fun startBound(): Bound<T>
    fun endBound(): Bound<T>
}

fun <T : Comparable<T>> RangeBounds<T>.isEmpty(): Boolean { /* default body */ }
```

Concrete impls that want to specialise the default supply a same-named
**member function**. Kotlin resolves `range.isEmpty()` to the member
when the static receiver type is the concrete class and to the
extension when it is the interface — exactly mirroring Rust's
"default method, per-impl override". No `override` keyword on the
member; there is nothing on the interface to override.

Recipe:

1. Interface keeps only the methods declared without where-clauses.
2. Each default-method-with-where-clause becomes a Kotlin extension
   whose own type-parameter bound mirrors the where-clause.
3. Concrete subtypes specialise by declaring a same-named member.
4. Callers holding the unbounded interface type cannot invoke the
   comparison-using methods — correct, Rust would reject the same
   call without the bound.

### Pair with the dual-overload pattern when both paths are needed

When a function has to work in both the comparator-aware and natural-order
paths, expose two overloads — the unbounded one takes the comparator
explicitly, the bounded one is sugar:

```kotlin
internal fun <Q> Tree.search(key: Q, compare: (Stored, Q) -> Int): Hit { /* heavy */ }

internal fun <Q : Comparable<Q>> Tree.search(key: Q): Hit
    where Stored : Comparable<Q> =
    search(key) { stored, query -> stored.compareTo(query) }
```

Heavy lifting in the comparator overload; natural-order overload is a
one-line delegation. The canonical implementation lives in
[`btree-kotlin`](../btree-kotlin/) `Search.kt::searchTree` /
`searchNode` / `findLowerBoundEdge` / `findUpperBoundEdge` and
`Navigate.kt::searchTreeForBifurcation` / `lowerBound` / `upperBound`.

### Why this is faithful, not engineering

- Interface mirrors Rust's trait declaration shape exactly.
- Extension's bound mirrors Rust's `where` clause exactly.
- Concrete-class members shadow the extension exactly the way Rust
  inherent-impl methods override a trait default.
- "Unbounded callers can't use these methods" mirrors Rust's
  compile-time rejection without the bound.
- No runtime casts, no `IllegalStateException`, no `is Comparable<*>`.

### When you cannot apply this

When the bound is on a *class* type parameter (e.g. `impl<K: Ord> Map<K, V>`),
Kotlin has no method-level analog — class type parameters bind for the
whole class. Use the `Comparator<in K>` field pattern with a
`compareKeys(a, b)` dispatch helper that prefers the supplied
comparator and falls back to a `Comparable<K>`-based path. The fallback
is the design contract, not a translation hack.

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
