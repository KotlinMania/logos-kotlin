# logos-kotlin

Kotlin Multiplatform port of [maciejhirsz/logos](https://github.com/maciejhirsz/logos), a fast lexer generator originally written in Rust.

Logos turns a typed token enum + per-variant regex/literal annotations into an efficient lexer state machine. The Rust upstream uses a procedural derive macro (`#[derive(Logos)]`) to generate the lexer at compile time. This Kotlin port preserves the runtime semantics and provides a Kotlin-idiomatic way to declare tokenizers without procedural macros.

## Maven coordinates

```kotlin
dependencies {
    implementation("io.github.kotlinmania:logos:0.1.0")
}
```

## Why a separate artifact?

`logos` is a foundational dependency of `starlark_syntax` (and any other Rust crate that uses it for tokenization). The same applies in our Kotlin world — the moment one downstream port (`starlark-syntax-kotlin`) needs logos, it pays to have it as a real KMP artifact rather than as inlined code in each consumer. This mirrors the upstream Cargo convention and keeps the lexer engine reusable across other ports.

## Targets

Kotlin Multiplatform, no JVM-only target:

- `macosArm64`
- `linuxX64`
- `mingwX64`
- `iosArm64`, `iosSimulatorArm64`
- `js` (browser + nodejs)
- `wasmJs` (browser + nodejs)
- `androidLibrary`

## Status

**Phase 1: scaffolding.** The repository is being stood up; source files are being ported file-by-file from `tmp/logos/` (a fresh clone from `maciejhirsz/logos`).

The Rust derive macro `#[derive(Logos)]` is a proc-macro and cannot translate 1:1 to Kotlin. The port replaces it with a Kotlin-friendly declarative API for building the lexer state machine, plus a code-generation path for users who want a static lexer. Runtime types (`Lexer<T>`, `Source`, `Span`, `Skip`, `Filter`, etc.) translate directly.

## License

Dual-licensed Apache-2.0 / MIT, matching upstream. See [LICENSE](./LICENSE).
