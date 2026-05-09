# Claude Code Project Instructions — logos-kotlin

## Project Overview

Kotlin Multiplatform port of upstream Rust crate `logos` (from [maciejhirsz/logos](https://github.com/maciejhirsz/logos)). Logos is a fast lexer generator: you declare a typed token enum with regex/literal annotations on each variant, and at compile time it generates an efficient state-machine lexer.

This Kotlin port covers the runtime types and the state-machine engine. The `#[derive(Logos)]` procedural macro has no Kotlin equivalent — it gets replaced with a declarative builder API.

Upstream Rust source lives at `tmp/logos/` and is the read-only translation oracle. **Never edit `tmp/`.**

## Translator's mindset

This is a translation project, not a software-engineering project. While porting a file, you are
the Kotlin author of the same document a Rust author wrote. Architecture, optimization, design
critique, drift measurement — all later. While translating, the only job is the translation.

The discipline:

1. **Read the whole upstream file before you type.** A line-by-line port composes only when you
   know how the file ends. If the file is too long to read in one sitting, split your turn into
   "read the file" and "write the file" — never start typing on a file you've only half-read.

2. **One Rust file → one Kotlin file. Always.** No splitting one `.rs` across several `.kt`. No
   merging several `.rs` into one `.kt`. The 1:1 mapping is the contract; everything downstream
   (ast_distance, port-lint headers, code review) assumes it. If a `.rs` is genuinely too big for
   one Kotlin file, that's a sign you're in `mod.rs`-equivalent territory and the upstream itself
   is a re-export — verify, don't split.

3. **Translate top to bottom in upstream order.** Preserve the declaration order. Don't reorder
   for "logical flow" — the upstream's order *is* the logical flow. The reader who already knows
   the Rust file should be able to scroll the Kotlin file and find every item in the same place.

4. **Comments are content.** License header, module-level doc, every `///` block, every inline
   `//` note, every upstream `// TODO`/`// FIXME` — all translate. Rust syntax inside doc comments
   gets rewritten to Kotlin equivalents (`Vec<T>` → `List<T>`, `Self::foo()` → `foo()`, lifetimes
   dropped, `cfg(test)` and `#[derive(...)]` lifted into prose). You are translating a *document*,
   not just the code.

5. **When a Rust idiom has no Kotlin analog, apply the mapping rule and move on.** `Box<T>`,
   `Arc<T>`, `Cell<T>`, `RefCell<T>`, `Rc<T>`, lifetimes, `PhantomData`, `mem::forget`,
   `drop_in_place`, `Pin`, `MaybeUninit`, `dyn Trait` — all collapse per the mapping table.
   Don't relitigate. A proc-macro becomes a builder/runtime API, not nothing. An upstream Rust
   crate with no KMP equivalent becomes a *separate Kotlin port*, not a `// TODO` placeholder.
   Pay the snowball cost upfront — the next consumer will thank you.

6. **Don't measure mid-port.** ast_distance, FnSim, similarity reports — useful *after* a file is
   done, useless *during*. Mid-translation measurement is procrastination dressed as rigor. Run
   the tools when a file lands or when a port phase wraps, not while you're choosing between
   `Result<T>` and `T?`.

7. **Don't optimize the translation.** "This Kotlin shape would be simpler" is the wrong
   thought. The upstream shape is the spec. If a faithful translation produces a function that
   takes a parameter you'd never write in Kotlin from scratch, take it. Optimization is a
   separate, named pass after parity is reached — never blended into the translation.

8. **Don't re-architect mid-port.** "This whole module would be cleaner if..." — write the
   thought on a sticky note, throw the sticky note away, finish the file. The current architecture
   is the upstream's architecture. Earn the right to redesign by first reaching parity.

9. **Compile errors during translation are normal and expected.** A bottom-of-tree file compiles
   when its deps are ported, not before. Don't pause to "make it compile" mid-port — that pulls
   you into stub-shaped fixes that you'll have to undo. Climb the dep tree bottom-up; the leaves
   compile first, then their parents, then everything compiles together at the end.

10. **Bottom-up always.** Port dependencies before consumers. If `state.rs` uses `EvalException`,
    port `eval_exception.rs` first. If `eval_exception.rs` uses `Error`/`WithDiagnostic`/`CallStack`,
    port those first. The order isn't optional; trying to port top-down produces a tree of stubs
    that all need replacing.

11. **Hard files are not skippable.** logos-codegen, lalrpop's table generator, an annotate-snippets
    equivalent — when you hit one, port it. Skipping leaves a `// TODO`-shaped hole that grows
    every time another consumer needs it. The snowball is the whole point: each hard port done
    makes the next port easier, because the dep is now in Kotlin.

12. **Warnings are real, but `@Suppress` is never the answer.** `UNUSED_PARAMETER` on a callback
    helper means the function shape doesn't fit Kotlin — restructure the signature, don't suppress.
    `UNCHECKED_CAST` means the type system is missing an invariant — encode it. Every warning is
    either a real bug or a translation choice that needs revisiting; treat them as compile errors.

13. **Stop at file boundaries, not function boundaries.** After every completed file, exhale,
    commit, move on. Don't pause mid-function to second-guess a choice. The whole-file context
    is what makes individual choices coherent.

14. **Doc-port discipline applies even when the upstream doc is awkward.** If the upstream
    author wrote a tortured English sentence in a doc comment, translate the tortured sentence.
    Don't smooth it. Don't paraphrase. Their doc is the contract for the Kotlin doc.

15. **The cheat detector is your friend.** If `ast_distance` forces your file's score to 0
    because you left snake_case identifiers or `pub` keywords in Kotlin comments, take it as a
    literal instruction: rewrite those comments to be Kotlin-native. Rust syntax in Kotlin source
    — code or comments — is the cheat we're catching.

The sticky-note version: **"Read the file. Translate it. Don't think about anything else."**

## Project Goals (the contract)

When a rule below seems to conflict with these, the goals win.

1. **Functional parity with upstream runtime.** Every public type and function in `logos/src/` has a Kotlin counterpart that behaves identically against the same fixtures.
2. **All tests pass.** Every `@Test` in `commonTest/` runs and passes on every shipped target.
3. **Kotlin source looks like Kotlin source.** No carried-over Rust idioms in code, KDoc, or API shapes.
4. **No hacks.** No stubs, no `TODO()`, no `FIXME`, no `@Suppress`, no JVM imports, no synthetic typealiases.

## Verification

The build gate is **`./gradlew test`**.

```bash
./gradlew macosArm64Test
./gradlew linuxX64Test
./gradlew jsNodeTest
./gradlew wasmJsNodeTest
```

## Targets — Kotlin Multiplatform, no JVM

- `macosArm64`
- `linuxX64`, `mingwX64`
- `iosArm64`, `iosSimulatorArm64`
- `js`, `wasmJs`, `androidLibrary`

### Forbidden imports

- `import kotlin.jvm.*`
- any `import java.*`
- any `import javax.*`

### Approved dependencies

- `kotlinx-coroutines-core`
- `kotlinx-serialization-core`, `kotlinx-serialization-json`
- `kotlinx-collections-immutable`

## Naming Conventions

| Kind | Form |
|---|---|
| Functions, parameters, locals | `camelCase` |
| Classes, data classes, sealed types | `PascalCase` |
| Interfaces | `PascalCase`, no `I` prefix |
| `const val`, `enum` entries | `SCREAMING_SNAKE_CASE` permitted |
| Type parameters | `T`, `K`, `V` (single uppercase) |
| Packages | all lowercase, no camelCase |

## Port-lint headers (REQUIRED)

```kotlin
// port-lint: source <path-relative-to-tmp/logos/>
package io.github.kotlinmania.logos.<module>
```

## Code Discipline

### No `@Suppress`. Warnings are errors.

### No stubs, no shims

### Translation discipline

- **Doc comments are first-class.** Translate KDoc word-for-word.
- **No no-op shells.** GC-subsumed Rust types (`Box<T>`, `Cell<T>`, `RefCell<T>`, `Arc<T>`, `Rc<T>`, `Pin`, `mem::forget`, `drop_in_place`, `MaybeUninit<T>`, `dyn Trait`) get deleted in the port.
- **`#[derive(Logos)]` doesn't translate as a macro.** Instead, the equivalent runtime gets a Kotlin builder API (`LogosBuilder` / declarative DSL) that constructs the same state machine at runtime. A future Gradle codegen plugin can recover the compile-time-generated path; runtime API is the v1 deliverable.
- **Procedural macro internals (`logos-derive`, regex parsing, NFA/DFA construction) port as ordinary Kotlin code.** The macro that consumes them is what doesn't translate.

### Blast Radius Rule

- No repo-wide scripting.
- Changes are task-scoped, not pattern-scoped.

### Operational rules

- **Don't write to `tmp/` or to project-local `tmp/` for staging.** `tmp/` holds upstream Rust source and is read-only.
- **Commit after every file edit.** One file edited → one commit.
- **Deletions require `git rm` plus reference scrubs.**

### Do not delegate `.kt` edits to subagents

## Trait default methods with `where` clauses

Rust trait default methods gated by `where T: SomeBound` translate to
Kotlin **extension functions whose own generic type parameter carries
the bound** — never tighten the interface, never add a runtime
`is Comparable<*>` cast, never make the method abstract just to dodge
the issue. Concrete subtypes specialise by declaring a same-named
member (no `override` keyword); Kotlin resolves to the member for the
concrete static receiver type and to the extension for the interface
type, exactly mirroring Rust's per-impl override of a trait default.
When the bound lives on a *class* parameter rather than a trait method,
fall back to the `Comparator<in K>` field pattern with a
comparator-or-natural dispatch helper. See
[AGENTS.md](./AGENTS.md) §"Trait default methods with `where` clauses"
for the worked recipe and rationale.

## File Organization

```
src/
├── commonMain/kotlin/io/github/kotlinmania/logos/
│   ├── (top-level Logos trait, Lexer, Source, Span, Skip, Filter, etc.)
│   ├── source/      # source.rs — the input source abstraction
│   ├── internalfsm/ # internal state-machine construction (graph, regex, NFA, DFA)
│   └── ...
└── commonTest/
    └── kotlin/io/github/kotlinmania/logos/
```

Final layout will mirror upstream's actual `logos/src/` tree.

## Cross-Project Coordination

Downstream consumer (Maven, never include-build):

- `starlark-syntax-kotlin` — uses logos for `lexer.rs`'s tokenizer.

When you bump a version here, check that every consumer's pinned version matches what's published.

## CI

```bash
gh run list --workflow ci.yml --limit 5
gh pr checks <pr-number>
```

## Final Report

When a run finishes, post a status update via the Slack MCP / skill / connector.

## Commit Messages

- No AI branding or attribution.
- Clear, descriptive, focused on what changed and why.
- No "Co-Authored-By" lines.
- No emoji or robot references.

## Re-exports from upstream `mod.rs` files

When an upstream Rust `mod.rs` is **only re-exporting** something that actually lives elsewhere
(`pub use <crate-path>::<Name>;`, often under a different name), do **not** preserve that
re-export shape in Kotlin as a "central alias" API. Do not write a `typealias` for the
re-exported name. The existing `Forbidden` rule against "Re-export typealias files at root
packages" is enforced through this procedure.

Workflow:

1. **Identify what the `mod.rs` is re-exporting and the name it's exported as.** Record both
   the original symbol's fully-qualified upstream path and the (possibly different) re-export
   name.

2. **Find callers across the kotlinmania monorepo.** A caller is any Kotlin file in another
   `*-kotlin` repo that has both a `tmp/` folder and a Cargo.toml depending on the Rust
   counterpart of *this* crate, where the file references the re-exported name. Search for:
   - direct imports: `import <reexport-package>.<Name>`
   - wildcard imports of the re-export package, when `<Name>` is used in the file body
   - fully-qualified inline references

3. **Rewrite each caller to reference the upstream/original symbol directly.** If the caller
   still needs to write `<Name>` unchanged, use Kotlin aliasing:
   `import <upstream-fully-qualified-name> as <Name>`. Never bridge with a Kotlin `typealias`.

4. **Keep `Mod.kt` (or the equivalent file for that package) as a tracking file.** It carries
   the translated upstream module-level comments and a literal-quoted reference to each upstream
   `pub use` line (e.g. `// pub use crate::lib::result::Result;`). Each time a caller is migrated
   off the re-export, append the caller's absolute path under a `// Callers migrated:` ledger in
   `Mod.kt`. Append, never delete. Once all callers are migrated, the `typealias` (if any) is
   removed; the tracking file remains as the ledger of the migration.

Reference example: [/Volumes/stuff/Projects/kotlinmania/serde-kotlin/tmp/serde/serde_core/src/private/mod.rs](/Volumes/stuff/Projects/kotlinmania/serde-kotlin/tmp/serde/serde_core/src/private/mod.rs)
re-exports `Result` from `crate::lib::result`. The Kotlin tracking file lives at
[/Volumes/stuff/Projects/kotlinmania/serde-kotlin/src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Mod.kt](/Volumes/stuff/Projects/kotlinmania/serde-kotlin/src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Mod.kt).
A caller that previously did `import io.github.kotlinmania.serde.core.private.Result` is
rewritten to `import kotlin.Result as Result` (or just removes the import and relies on the
auto-imported `kotlin.Result`).
